package com.satalia.beertest.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class Plane {
    private int fuelLeft;
    private Location currentLocation;
    private Logistics alongTheWay;
    private HashMap<Integer, BreweryLocation> reachableArea;
    private Stack<Logistics> visitedBreweries = new Stack<>();
    private Stack<Logistics> targetNode = new Stack<>();
    private HashSet<String> collectedBeerTypes = new HashSet<>();

    public enum Mode {
        SCAVENGING,
        TRAVELLING_TO_AREA,
        LOOKING_FOR_AREA,
        LAST_BREATH
    }

    private Mode planeMode = Mode.LOOKING_FOR_AREA;

    public Plane(int fuelLeft, Location currentLocation, HashMap<Integer, BreweryLocation> reachableArea) {
        this.fuelLeft = fuelLeft;
        this.currentLocation = currentLocation;
        this.reachableArea = reachableArea;
    }

    public void addBear(HashSet<String> beerTypes) {
        this.collectedBeerTypes.addAll(beerTypes);
    }

    public HashSet<String> getCollectedBeerTypes() {
        return collectedBeerTypes;
    }

    public Mode getPlaneMode() {
        return planeMode;
    }

    public void setPlaneMode(Mode mode) {
        planeMode = mode;
    }

    public Logistics getAlongTheWay() {
        return alongTheWay;
    }

    public void setAlongTheWay(Logistics alongTheWay) {
        this.alongTheWay = alongTheWay;
    }

    public Stack<Logistics> getVisitedBreweries() {
        return visitedBreweries;
    }

    public void addVisitedBrewery(Logistics travelLogistics) {
        visitedBreweries.push(travelLogistics);
    }

    public Stack<Logistics> getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(Logistics targetNode) {
        this.targetNode.push(targetNode);
    }

    public HashMap<Integer, BreweryLocation> getReachableArea() {
        return reachableArea;
    }

    public void setReachableArea(HashMap<Integer, BreweryLocation> reachableArea) {
        this.reachableArea = reachableArea;
    }

    public int getFuelLeft() {
        return fuelLeft;
    }

    public void setFuelLeft(int fuelLeft) {
        this.fuelLeft = fuelLeft;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }
}
