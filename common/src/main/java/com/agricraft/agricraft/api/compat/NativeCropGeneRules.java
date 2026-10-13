package com.agricraft.agricraft.api.compat;

/** Pure calendar/output rules. They never advance a crop or consume native randomness. */
public final class NativeCropGeneRules {
    private NativeCropGeneRules() {}

    public static double normalized(int value, int min, int max) {
        if (max <= min) return 0;
        return Math.max(0, Math.min(1, ((double) value - min) / ((double) max - min)));
    }

    public static int duration(int nativeDays, double growth) {
        // Zero phases and the native harvest sentinel retain their special meaning.
        if (nativeDays <= 0 || nativeDays == 99999) return nativeDays;
        return Math.max(1, (int) Math.ceil(nativeDays * (1 - 0.5 * growth)));
    }

    public static int[] phases(int[] nativeDays, double growth) {
        if (growth <= 0) return nativeDays;
        int[] result = nativeDays.clone();
        for (int i = 0; i < result.length; i++) result[i] = duration(result[i], growth);
        return result;
    }

    public static int yield(int count, double gain) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(count * (1 + gain)));
    }

    /** Stateless daily sample: equal genes/dates agree, including offline replay. */
    public static boolean recoverSoilSkip(int absoluteDay, double strength) {
        if (strength <= 0) return false;
        if (strength >= 1) return true;
        long sample = absoluteDay + 0x9e3779b97f4a7c15L;
        sample = (sample ^ (sample >>> 30)) * 0xbf58476d1ce4e5b9L;
        sample = (sample ^ (sample >>> 27)) * 0x94d049bb133111ebL;
        sample ^= sample >>> 31;
        return (sample >>> 11) * 0x1.0p-53 < strength;
    }

    public static int resistanceDays(double resistance) {
        return Math.max(0, Math.min(3, (int) Math.floor(3 * resistance)));
    }

    /** First out-of-season day is inclusive; no mutable countdown or planting timestamp. */
    public static int graceRemaining(int absoluteDay, int graceDays) {
        if (absoluteDay <= 0 || graceDays <= 0) return 0;
        return Math.max(0, graceDays - Math.floorMod(absoluteDay - 1, 28));
    }
}
