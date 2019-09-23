package com.satalia.beertest.utilities;

class Parameters {
    private final static int startingFuel = 2000;
    private final static int earthRadius = 6371000;

    /**
     * Value determining how far can plane sidetrack from current target.
     */
    private final static double tolerantOffset = 1.25;

    /**
     * Value giving boundaries how far should plane look for possible candidates.
     */
    private final static int reachableRadius = startingFuel / 2;

    /**
     * Value determining the radius, which is used for finding most dense areas.
     */
    private static int magicValue = 10;

    static void setMagicValue(int magicValue) {
        Parameters.magicValue = magicValue;
    }

    static int getStartingFuel() {
        return startingFuel;
    }

    static int getEarthRadius() {
        return earthRadius;
    }

    static double getTolerantOffset() {
        return tolerantOffset;
    }

    static int getReachableRadius() {
        return reachableRadius;
    }

    static int getMagicValue() {
        return magicValue;
    }
}
