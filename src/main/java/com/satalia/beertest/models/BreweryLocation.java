package com.satalia.beertest.models;

import java.util.HashSet;

public class BreweryLocation extends Location {
    private int brew_id;
    private HashSet<String> beerTypes = new HashSet<>();

    public BreweryLocation(double latitude, double longitude, int brew_id, HashSet<String> beerTypes) {
        super(latitude, longitude);
        this.brew_id = brew_id;
        if (beerTypes != null)
            this.beerTypes.addAll(beerTypes);
    }

    public HashSet<String> getBeerTypes() {
        return beerTypes;
    }

    public void setBeerTypes(HashSet<String> beerTypes) {
        this.beerTypes = beerTypes;
    }

    public int getBrew_id() {
        return brew_id;
    }

    public void setBrew_id(int brew_id) {
        this.brew_id = brew_id;
    }

    public void changeLocation(BreweryLocation targetLocation) {
        this.setLatitude(targetLocation.getLatitude());
        this.setLongitude(targetLocation.getLongitude());
        this.setBrew_id(targetLocation.getBrew_id());
    }
}
