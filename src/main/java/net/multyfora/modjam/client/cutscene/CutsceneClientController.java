package net.multyfora.modjam.client.cutscene;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.multyfora.modjam.client.DialogueSystem;
import net.multyfora.modjam.network.CutsceneCompletePayload;
import net.multyfora.modjam.network.StartCutscenePayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CutsceneClientController {
    private static final CutsceneClientController INSTANCE = new CutsceneClientController();
    private static final int MIN_EXIT_TICKS = 20;
    private static final int FINAL_HOLD_TICKS = 30;
    private static final int RETURN_TICKS = 30;

    private record Key(int time, Vec3 pos, float yaw, float pitch) {
        static final Key ZERO = new Key(0, Vec3.ZERO, 0.0f, 0.0f);
    }

    public record CamState(Vec3 pos, float yaw, float pitch) {
        static final CamState IDENTITY = new CamState(Vec3.ZERO, 0.0f, 0.0f);
    }

    private boolean active;
    private String id = "";
    private static boolean hudHiddenByUs;
    private boolean lineSynced;
    private boolean dialogueDriven;
    private boolean returning;
    private int returnTicks;
    private CamState returnFrom = CamState.IDENTITY;
    private int finalHoldTicks;
    private int tickCounter;
    private int durationTicks;
    private double pathPos;
    private int targetSegment;
    private List<Key> keys = List.of();
    private CamState prev = CamState.IDENTITY;
    private CamState current = CamState.IDENTITY;

    public static CutsceneClientController getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public void start(StartCutscenePayload payload) {
        if (active) return;
        active = true;
        id = payload.id();
        lineSynced = payload.lineSynced();
        dialogueDriven = false;
        returning = false;
        returnTicks = 0;
        targetSegment = 1;
        pathPos = 0.0;
        finalHoldTicks = 0;
        tickCounter = 0;
        durationTicks = Math.max(payload.durationTicks(), 1);
        keys = buildKeys(payload);
        if (!keys.isEmpty()) {
            durationTicks = Math.max(durationTicks, keys.get(keys.size() - 1).time());
            prev = sample(0);
        } else {
            end();
            return;
        }
        current = prev;
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(null);
        KeyMapping.releaseAll();
        hideHud(mc);
        if (!payload.lines().isEmpty() && !DialogueSystem.getInstance().isActive()) {
            DialogueSystem.getInstance().playMarkup(payload.lines(), null, null);
        }
        if (lineSynced && DialogueSystem.getInstance().isActive()) {
            dialogueDriven = true;
        }
    }

    public void advanceSegment() {
        if (!active || !lineSynced || keys.isEmpty()) return;
        targetSegment = Math.min(targetSegment + 1, keys.size() - 1);
    }

    public void onDialogueFinished() {
        if (!active || !lineSynced || keys.isEmpty()) return;
        targetSegment = keys.size() - 1;
    }

    public boolean isLineSynced() {
        return active && lineSynced;
    }

    private static void hideHud(Minecraft mc) {
        if (mc.gui != null && mc.gui.hud != null && !mc.gui.hud.isHidden()) {
            mc.gui.hud.toggle();
            hudHiddenByUs = true;
        }
    }

    private static void restoreHud(Minecraft mc) {
        if (!hudHiddenByUs) return;
        hudHiddenByUs = false;
        if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.isHidden()) {
            mc.gui.hud.toggle();
        }
    }

    private static List<Key> buildKeys(StartCutscenePayload payload) {
        List<StartCutscenePayload.Frame> frames = new ArrayList<>(payload.frames());
        frames.sort(Comparator.comparingInt(StartCutscenePayload.Frame::time));
        List<Key> built = new ArrayList<>(frames.size());

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            built.add(new Key(0, mc.player.getEyePosition(), mc.player.getYRot(), mc.player.getXRot()));
        }

        if (frames.isEmpty()) return List.copyOf(built);
        int shift = Math.max(0, MIN_EXIT_TICKS - frames.get(0).time());
        float lastYaw = 0.0f;
        float lastPitch = 0.0f;
        for (StartCutscenePayload.Frame frame : frames) {
            Vec3 pos = new Vec3(frame.x(), frame.y(), frame.z());
            float yaw;
            float pitch;
            if (frame.hasRot()) {
                yaw = frame.yaw();
                pitch = frame.pitch();
            } else {
                Vec3 target = frame.hasLookAt() ? new Vec3(frame.lookX(), frame.lookY(), frame.lookZ()) : pos.add(lastLookDir(lastYaw, lastPitch));
                double dx = target.x - pos.x;
                double dy = target.y - pos.y;
                double dz = target.z - pos.z;
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
                pitch = horizontal < 1.0e-4
                    ? (dy >= 0 ? -90.0f : 90.0f)
                    : -(float) Math.toDegrees(Math.atan2(dy, horizontal));
            }
            built.add(new Key(frame.time() + shift, pos, yaw, pitch));
            lastYaw = yaw;
            lastPitch = pitch;
        }
        return List.copyOf(built);
    }

    private static Vec3 lastLookDir(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        return new Vec3(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        );
    }

    public void tick() {
        if (!active) return;
        suppressInput();
        if (returning) {
            tickReturn();
            return;
        }
        tickCounter++;
        if (lineSynced) {
            tickLineSynced();
        } else {
            tickTimed();
        }
    }

    private void beginReturn() {
        if (returning) return;
        if (keys.isEmpty()) {
            end();
            return;
        }
        returning = true;
        returnTicks = 0;
        finalHoldTicks = 0;
        returnFrom = current;
    }

    private void tickReturn() {
        returnTicks++;
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player != null ? mc.player.getEyePosition() : returnFrom.pos();
        float yaw = mc.player != null ? mc.player.getYRot() : returnFrom.yaw();
        float pitch = mc.player != null ? mc.player.getXRot() : returnFrom.pitch();
        float u = (float) smootherstep(Math.min(returnTicks / (float) RETURN_TICKS, 1.0));
        prev = current;
        current = new CamState(
            new Vec3(
                Mth.lerp(u, returnFrom.pos().x, eye.x),
                Mth.lerp(u, returnFrom.pos().y, eye.y),
                Mth.lerp(u, returnFrom.pos().z, eye.z)
            ),
            returnFrom.yaw() + Mth.degreesDifference(returnFrom.yaw(), yaw) * u,
            returnFrom.pitch() + (pitch - returnFrom.pitch()) * u
        );
        if (returnTicks >= RETURN_TICKS) {
            end();
        }
    }

    private void tickTimed() {
        if (tickCounter >= durationTicks) {
            beginReturn();
            return;
        }
        prev = current;
        current = sample(tickCounter);
    }

    private void tickLineSynced() {
        int lastSegment = keys.size() - 1;
        boolean dialogueActive = DialogueSystem.getInstance().isActive();

        if (!dialogueDriven && !dialogueActive && targetSegment < lastSegment
            && tickCounter % Math.max(durationTicks / Math.max(lastSegment, 1), 20) == 0) {
            advanceSegment();
        }

        if (tickCounter >= durationTicks * 3L + 200) {
            beginReturn();
            return;
        }

        double targetTime = keys.get(targetSegment).time();
        double diff = targetTime - pathPos;
        if (Math.abs(diff) > 0.001) {
            double step = diff * 0.045;
            double minStep = 0.25 * Math.signum(diff);
            if (Math.abs(step) < Math.abs(minStep)) step = minStep;
            if (Math.abs(step) > Math.abs(diff)) step = diff;
            pathPos += step;
        } else if (dialogueDriven && targetSegment == lastSegment) {
            if (++finalHoldTicks >= FINAL_HOLD_TICKS) {
                beginReturn();
                return;
            }
        }

        prev = current;
        current = sample(pathPos);
    }

    private void end() {
        active = false;
        returning = false;
        String completedId = id;
        id = "";
        keys = List.of();
        prev = current = CamState.IDENTITY;
        Minecraft mc = Minecraft.getInstance();
        restoreHud(mc);
        var connection = mc.getConnection();
        if (connection != null) {
            connection.send(new CutsceneCompletePayload(completedId).toVanillaServerbound());
        }
    }

    private static void suppressInput() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keySprint.setDown(false);
    }

    public CamState getCamera(float partialTicks) {
        if (!active || prev == null || current == null) return null;
        return new CamState(
            new Vec3(
                Mth.lerp(partialTicks, prev.pos().x, current.pos().x),
                Mth.lerp(partialTicks, prev.pos().y, current.pos().y),
                Mth.lerp(partialTicks, prev.pos().z, current.pos().z)
            ),
            lerpAngle(partialTicks, prev.yaw(), current.yaw()),
            lerpAngle(partialTicks, prev.pitch(), current.pitch())
        );
    }

    private static float lerpAngle(float partialTicks, float from, float to) {
        return from + Mth.degreesDifference(from, to) * partialTicks;
    }

    private CamState sample(double t) {
        if (keys.isEmpty()) return CamState.IDENTITY;
        Key first = keys.get(0);
        Key last = keys.get(keys.size() - 1);
        if (t <= first.time()) {
            return new CamState(first.pos(), first.yaw(), first.pitch());
        }
        if (t >= last.time()) {
            return new CamState(last.pos(), last.yaw(), last.pitch());
        }
        double pathTime;
        if (lineSynced) {
            pathTime = t;
        } else {
            pathTime = smootherstep(t / last.time()) * last.time();
        }
        for (int i = 0; i < keys.size() - 1; i++) {
            Key a = keys.get(i);
            Key b = keys.get(i + 1);
            if (pathTime >= a.time() && pathTime <= b.time()) {
                float u = (float)(pathTime - a.time()) / (float)Math.max(b.time() - a.time(), 1);
                Vec3 p0 = keys.get(Math.max(i - 1, 0)).pos();
                Vec3 p1 = a.pos();
                Vec3 p2 = b.pos();
                Vec3 p3 = keys.get(Math.min(i + 2, keys.size() - 1)).pos();
                return new CamState(catmullRom(p0, p1, p2, p3, u),
                    a.yaw() + Mth.degreesDifference(a.yaw(), b.yaw()) * u,
                    a.pitch() + (b.pitch() - a.pitch()) * u);
            }
        }
        return new CamState(last.pos(), last.yaw(), last.pitch());
    }

    private static double smootherstep(double x) {
        double clamped = Mth.clamp(x, 0.0, 1.0);
        return clamped * clamped * clamped * (clamped * (clamped * 6.0 - 15.0) + 10.0);
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return new Vec3(
            0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
            0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
            0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3)
        );
    }
}
