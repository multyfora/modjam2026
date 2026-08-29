package net.multyfora.don.light;

public record LightEnergy(double intensity, double mysticalComponent) {
    public static final LightEnergy NONE = new LightEnergy(0, 0);

    public boolean isPresent() {
        return intensity > 0;
    }
}
