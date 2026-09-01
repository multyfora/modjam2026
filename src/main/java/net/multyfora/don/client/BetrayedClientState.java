package net.multyfora.don.client;

public final class BetrayedClientState {
    private static boolean betrayed = false;

    private BetrayedClientState() {}

    public static boolean isBetrayed() {
        return betrayed;
    }

    public static void setBetrayed(boolean v) {
        betrayed = v;
    }
}
