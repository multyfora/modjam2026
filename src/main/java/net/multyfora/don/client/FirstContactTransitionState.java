package net.multyfora.don.client;

public class FirstContactTransitionState {
    public enum Phase { NONE, LEAVING, ENTERING }

    private static FirstContactTransitionState instance;

    private Phase phase = Phase.NONE;
    private float intensity = 0.0f;
    private long startTime = 0;

    public static FirstContactTransitionState getInstance() {
        if (instance == null) instance = new FirstContactTransitionState();
        return instance;
    }

    public void startLeaving() {
        phase = Phase.LEAVING;
        intensity = 0.0f;
        startTime = System.currentTimeMillis();
    }

    public void startEntering() {
        phase = Phase.ENTERING;
        intensity = 1.0f;
        startTime = System.currentTimeMillis();
    }

    public void tick() {
        if (phase == Phase.NONE) return;

        long elapsed = System.currentTimeMillis() - startTime;
        float duration = 2000.0f;

        if (phase == Phase.LEAVING) {
            intensity = Math.min(1.0f, elapsed / duration);
            if (intensity >= 1.0f) {
                phase = Phase.NONE;
                intensity = 0.0f;
            }
        } else if (phase == Phase.ENTERING) {
            intensity = Math.max(0.0f, 1.0f - elapsed / duration);
            if (intensity <= 0.0f) {
                phase = Phase.NONE;
            }
        }
    }

    public float getIntensity() { return intensity; }
    public Phase getPhase() { return phase; }
    public boolean isActive() { return phase != Phase.NONE; }
}
