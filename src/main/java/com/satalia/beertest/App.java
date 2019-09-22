package com.satalia.beertest;

import com.satalia.beertest.models.BreweryLocation;
import com.satalia.beertest.models.Location;
import com.satalia.beertest.models.Logistics;
import com.satalia.beertest.models.Plane;

import java.sql.SQLException;
import java.util.*;

public class App {
    private static Location homeLocation;
    private static HashMap<Integer, HashMap<Integer, Double>> distances = new HashMap<>();
    public static MySQLConnection database = new MySQLConnection();
    private static Plane winnerPlane;

    public static void main(String[] args) {
        
    }

    public static boolean Start(Location home, boolean priority) throws SQLException {
        homeLocation = home;
        database.ConnectToDatabase();
        return FindRoute(priority);
    }

    public static Plane getPlaneData() {
        return winnerPlane;
    }

    private static boolean FindRoute(boolean priority) throws SQLException {
        ArrayList<BreweryLocation> locationList;
        HashMap<Integer, BreweryLocation> reachableArea;
        Plane planeData;
        Location currentLocation;
        locationList = database.getBreweries();
        distances.clear();
        HashMap<Integer, BreweryLocation> copyList = new HashMap<>(getReachableBreweries(locationList));

        if (copyList.size() == 0)
            return false;

        PriorityQueue<Logistics> areaNodes;
        boolean needForBeer;
        int maxBreweries = 0;
        int maxTypes = 0;

        for (int i = 2; i <= 10; i++) {
            Parameters.magicValue = i;
            currentLocation = new Location(Parameters.homelatitude, Parameters.homelongitude);
            reachableArea = new HashMap<>(copyList);
            planeData = new Plane(Parameters.startingFuel, currentLocation, reachableArea);
            areaNodes = new PriorityQueue<>();
            needForBeer = true;
            while (needForBeer) {
                if (planeData.getReachableArea().isEmpty())
                    break;
                Plane.Mode currentMode = planeData.getPlaneMode();
                switch (currentMode) {
                    case LOOKING_FOR_AREA:
                        FindDenseArea(areaNodes, planeData);
                        planeData.setPlaneMode(Plane.Mode.TRAVELLING_TO_AREA);
                        if (planeData.getTargetNode().peek() == null)
                            planeData.setPlaneMode(Plane.Mode.LAST_BREATH);
                        break;
                    case TRAVELLING_TO_AREA:
                        FindNearByNode(planeData);
                        if (planeData.getTargetNode().size() == 1)
                            planeData.setPlaneMode(Plane.Mode.SCAVENGING);

                        if (!FlyToTheTarget(planeData))
                            planeData.setPlaneMode(Plane.Mode.LAST_BREATH);
                        break;
                    case SCAVENGING:
                        if (areaNodes.isEmpty()) {
                            planeData.setPlaneMode(Plane.Mode.LOOKING_FOR_AREA);
                            break;
                        }
                        FindClosestFromArea(areaNodes, planeData);
                        if (!FlyToTheTarget(planeData))
                            planeData.setPlaneMode(Plane.Mode.LAST_BREATH);
                        break;
                    case LAST_BREATH:
                        FindClosest(planeData);
                        if (!FlyToTheTarget(planeData))
                            needForBeer = false;
                        break;

                }
            }
            if (priority) {
                if (planeData.getVisitedBreweries().size() > maxBreweries) {
                    winnerPlane = planeData;
                    maxBreweries = planeData.getVisitedBreweries().size();
                    maxTypes = planeData.getCollectedBeerTypes().size();
                }
                else if (planeData.getVisitedBreweries().size() == maxBreweries) {
                    if (planeData.getCollectedBeerTypes().size() > maxTypes) {
                        winnerPlane = planeData;
                        maxTypes = planeData.getCollectedBeerTypes().size();
                    }
                }
            }
            else {
                if (planeData.getCollectedBeerTypes().size() > maxTypes) {
                    winnerPlane = planeData;
                    maxBreweries = planeData.getVisitedBreweries().size();
                    maxTypes = planeData.getCollectedBeerTypes().size();
                }
                else if (planeData.getCollectedBeerTypes().size() == maxTypes) {
                    if (planeData.getVisitedBreweries().size() > maxBreweries) {
                        winnerPlane = planeData;
                        maxBreweries = planeData.getVisitedBreweries().size();
                    }
                }
            }
        }
        return true;
    }

    private static HashMap<Integer, BreweryLocation> getReachableBreweries(ArrayList<BreweryLocation> locationList) throws SQLException {
        HashMap<Integer, BreweryLocation> reachableBrews = new HashMap<>();

        for (BreweryLocation a : locationList) {
            double distance = getDistance(homeLocation, a);
            if (distance <= Parameters.reachableRadius) {
                reachableBrews.put(a.getBrew_id(), a);
            }
            database.updateBeerTypes(a);
        }

        return reachableBrews;
    }

    private static void FindClosest(Plane planeData) {
        PriorityQueue<Logistics> closestBrewery = new PriorityQueue<>();
        HashMap<Integer, BreweryLocation> reachableArea = planeData.getReachableArea();
        Location currentLocation = planeData.getCurrentLocation();

        for(Map.Entry<Integer, BreweryLocation> breweries : reachableArea.entrySet()) {
            int brewId = breweries.getKey();
            BreweryLocation breweryLocation = breweries.getValue();
            double dist_currentToBrewery = getDistance(currentLocation, breweryLocation);
            closestBrewery.add(new Logistics(brewId, dist_currentToBrewery));
        }

        planeData.setTargetNode(closestBrewery.poll());
    }

    private static void FindClosestFromArea(PriorityQueue<Logistics> areaNodes, Plane planeData) {
        PriorityQueue<Logistics> tempNodes = new PriorityQueue<>();
        HashMap<Integer, BreweryLocation> reachableArea = planeData.getReachableArea();
        Location currentLocation = planeData.getCurrentLocation();
        for (Logistics areaNode : areaNodes) {
            BreweryLocation areaNodeLocation = reachableArea.get(areaNode.getDestinationId());
            if (areaNodeLocation == null)
                continue;
            tempNodes.add(new Logistics(areaNode.getDestinationId(), getDistance(currentLocation, areaNodeLocation)));
        }
        areaNodes.clear();
        areaNodes.addAll(tempNodes);
        planeData.setTargetNode(areaNodes.poll());
    }

    private static boolean FlyToTheTarget(Plane planeData) {
        HashMap<Integer, BreweryLocation> reachableArea = planeData.getReachableArea();
        Logistics targetLocation = planeData.getTargetNode().pop();
        Location currentLocation = planeData.getCurrentLocation();
        BreweryLocation targetBrewery = reachableArea.get(targetLocation.getDestinationId());

        double dist_currentToTarget = Math.ceil(getDistance(currentLocation, targetBrewery));
        double dist_targetToHome = Math.ceil(getDistance(targetBrewery, homeLocation));
        int fuel = planeData.getFuelLeft();

        if (fuel < Math.ceil(dist_currentToTarget) || fuel < dist_currentToTarget + dist_targetToHome)
            return false;

        planeData.setFuelLeft((int) (fuel - dist_currentToTarget));
        planeData.addBear(targetBrewery.getBeerTypes());
        targetLocation.setDistance(dist_currentToTarget);
        planeData.addVisitedBrewery(targetLocation);

        if (currentLocation instanceof BreweryLocation)
            ((BreweryLocation) currentLocation).changeLocation(targetBrewery);
        else
            planeData.setCurrentLocation(new BreweryLocation(
                    targetBrewery.getLatitude(),
                    targetBrewery.getLongitude(),
                    targetBrewery.getBrew_id(),
                    targetBrewery.getBeerTypes(),
                    targetBrewery.getBreweryName())
            );

        reachableArea.remove(targetBrewery.getBrew_id());
        return true;
    }

    private static void FindNearByNode(Plane planeData) {
        PriorityQueue<Logistics> alongTheWayNodes = new PriorityQueue<>();
        Location currentLocation = planeData.getCurrentLocation();
        HashMap<Integer, BreweryLocation> reachableArea = planeData.getReachableArea();
        int targetBrewId = planeData.getTargetNode().peek().getDestinationId();
        double dist_currentToTarget = getDistance(currentLocation, reachableArea.get(targetBrewId));

        for(Map.Entry<Integer, BreweryLocation> nearByBreweries : reachableArea.entrySet()) {
            int brewId = nearByBreweries.getKey();
            BreweryLocation nearByBrewery = nearByBreweries.getValue();

            double dist_currentToNearby = getDistance(currentLocation, nearByBrewery);
            double dist_nearbyToTarget = getDistance(nearByBrewery, reachableArea.get(targetBrewId));
            double newPathDistance = dist_currentToNearby + dist_nearbyToTarget;
            double tolerantPath = newPathDistance / Parameters.tolerantOffset;
            if (dist_currentToNearby < dist_currentToTarget && tolerantPath < dist_currentToTarget)
                alongTheWayNodes.add(new Logistics(brewId, dist_currentToNearby));
        }

        if (!alongTheWayNodes.isEmpty())
            planeData.setTargetNode(alongTheWayNodes.poll());
    }

    private static void FindDenseArea(PriorityQueue<Logistics> areaNodes, Plane planeData) {
        Location currentLocation = planeData.getCurrentLocation();
        HashMap<Integer, BreweryLocation> reachableArea = planeData.getReachableArea();
        Logistics targetArea = new Logistics();
        int fuel = planeData.getFuelLeft();

        int nodesInTheArea;
        int maxNodesInTheArea = 0;
        ArrayList<Logistics> tempAreaNodes = new ArrayList<>();

        for (Map.Entry<Integer, BreweryLocation> reachableNode : reachableArea.entrySet()) {
            int brewId = reachableNode.getKey();
            BreweryLocation possibleCenter = reachableNode.getValue();

            double radius = (fuel - getDistance(currentLocation, possibleCenter)) / Parameters.magicValue;
            nodesInTheArea = 0;
            tempAreaNodes.clear();
            for (Map.Entry<Integer, BreweryLocation> areaNode : reachableArea.entrySet()) {
                int areaNodeId = areaNode.getKey();
                BreweryLocation areaNodeLoc = areaNode.getValue();

                if (brewId != areaNodeId) {
                    double dist_currentToAreaNode = getDistance(currentLocation, areaNodeLoc);
                    if (getDistance(possibleCenter, areaNodeLoc) <= radius) {
                        nodesInTheArea++;
                        tempAreaNodes.add(new Logistics(areaNodeId, dist_currentToAreaNode));
                    }
                }
            }

            if (nodesInTheArea >= maxNodesInTheArea) {
                double dist_currentToTarget = getDistance(currentLocation, possibleCenter);
                if (nodesInTheArea == maxNodesInTheArea) {
                    if (dist_currentToTarget > targetArea.getDistance())
                        continue;
                }
                maxNodesInTheArea = nodesInTheArea;
                targetArea.setDistance(dist_currentToTarget);
                areaNodes.clear();
                areaNodes.add(new Logistics(brewId, dist_currentToTarget));
                areaNodes.addAll(tempAreaNodes);
            }
        }
        planeData.setTargetNode(areaNodes.poll());
    }

    public static double getDistance(Location loc1, Location loc2) {
        if (loc1 instanceof BreweryLocation && loc2 instanceof BreweryLocation) {
            int brewId1 = ((BreweryLocation) loc1).getBrew_id();
            int brewId2 = ((BreweryLocation) loc2).getBrew_id();
            if (!distances.containsKey(brewId1)) {
                HashMap<Integer, Double> tempDist = new HashMap<>();
                tempDist.put(brewId2, calculateDistance(loc1, loc2));
                distances.put(brewId1, tempDist);
            }
            else {
                if (!distances.get(brewId1).containsKey(brewId2)) {
                    HashMap<Integer, Double> tempDist = distances.get(brewId1);
                    tempDist.put(brewId2, calculateDistance(loc1, loc2));
                }
            }
            return distances.get(brewId1).get(brewId2);
        }
        return calculateDistance(loc1, loc2);
    }

    private static double calculateDistance(Location loc1, Location loc2) {
        double lat1 = loc1.getLatitude() * Math.PI / 180;
        double long1 = loc1.getLongitude() * Math.PI / 180;
        double lat2 = loc2.getLatitude() * Math.PI / 180;
        double long2 = loc2.getLongitude() * Math.PI / 180;

        double first = Math.pow(Math.sin((lat2 - lat1) / 2), 2);
        double second2 = Math.pow(Math.sin((long2 - long1) / 2), 2);
        double second1 = Math.cos(lat1)*Math.cos(lat2);
        double second = second1 * second2;
        return (2*Parameters.earthRadius*Math.asin(Math.sqrt(first + second)) / 1000);
    }
}
