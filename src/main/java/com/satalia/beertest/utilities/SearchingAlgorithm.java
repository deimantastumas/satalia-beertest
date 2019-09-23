package com.satalia.beertest.utilities;

import com.satalia.beertest.models.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import static com.satalia.beertest.utilities.Haversine.calculateDistance;
import static com.satalia.beertest.utilities.Parameters.*;
import static com.satalia.beertest.utilities.Parameters.getMagicValue;

public class SearchingAlgorithm {

    /**
     * Used for getDistance() optimization. Stores all queried distances
     */
    private static HashMap<Integer, HashMap<Integer, Double>> distances = new HashMap<>();
    private static MySQLConnection database;

    /**
     * Object containing travel information, which will be used in results output
     */
    private static Plane winnerPlane;

    /**
     * Initializes class for connecting with database
     * @return all elements from 'breweries' table
     * @throws SQLException MySQL connection failure
     */
    private static ArrayList<BreweryLocation> initData() throws SQLException {
        database = new MySQLConnection();
        return database.getBreweries();
    }

    /**
     *
     * @param priority Shows which heuristic value is more important. true - beer types, false - breweries
     * @param homeLocation Object containing starting location
     * @return Object containing all needed information for output
     * @throws SQLException MySQL connection failure
     */
    public static Results FindRoute(boolean priority, Location homeLocation) throws SQLException {
        distances.clear();
        ArrayList<BreweryLocation> locationList = initData();
        HashMap<Integer, BreweryLocation> copyList = new HashMap<>(getReachableBreweries(locationList, homeLocation));

        return FindRoute(priority, copyList, homeLocation);
    }

    /**
     *
     * @param copyList Map containing all reachable locations.
     *                 Key - brewery ID, value - breweryLocation object
     * @return Object containing all needed information for output
     */
    static Results FindRoute(boolean priority, HashMap<Integer, BreweryLocation> copyList, Location homeLocation) {
        HashMap<Integer, BreweryLocation> reachableArea;
        Plane planeData;
        Location currentLocation;

        if (copyList == null || copyList.size() == 0)
            return new Results(false, null);

        PriorityQueue<Logistics> areaNodes;
        boolean needForBeer;
        int maxBreweries = 0;
        int maxTypes = 0;

        for (int i = 2; i <= 10; i++) {
            setMagicValue(i);
            currentLocation = homeLocation;
            reachableArea = new HashMap<>(copyList);
            planeData = new Plane(getStartingFuel(), currentLocation, reachableArea, homeLocation);
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
        return new Results(true, winnerPlane);
    }

    /**
     * @param locationList List containing breweries from database
     * @param homeLocation Object containing starting location
     * @return map containing only those breweryLocation objects, which are reachable from home location
     * @throws SQLException MySQL connection failure
     */
    private static HashMap<Integer, BreweryLocation> getReachableBreweries(ArrayList<BreweryLocation> locationList, Location homeLocation) throws SQLException {
        HashMap<Integer, BreweryLocation> reachableBrews = new HashMap<>();

        for (BreweryLocation a : locationList) {
            double distance = getDistance(homeLocation, a);
            if (distance <= getReachableRadius()) {
                reachableBrews.put(a.getBrew_id(), a);
            }
            database.updateBeerTypes(a);
        }

        return reachableBrews;
    }

    /**
     * Finds nearest brewery
     * @param planeData Plane object containing information about the trip.
     *                  Nearest brewery gets stored inside this object
     */
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

    /**
     * Finds nearest brewery from specified area
     * @param areaNodes specified area containing breweryLocation objects
     * @param planeData object containing information about the trip.
     */
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

    /**
     * Moves plane from current to target location. Changes all needed values inside plane object
     * @param planeData object containing information about the trip.
     * @return true if plane was able to reach the target, false - otherwise.
     */
    private static boolean FlyToTheTarget(Plane planeData) {
        HashMap<Integer, BreweryLocation> reachableArea = planeData.getReachableArea();
        Logistics targetLocation = planeData.getTargetNode().pop();
        Location currentLocation = planeData.getCurrentLocation();
        BreweryLocation targetBrewery = reachableArea.get(targetLocation.getDestinationId());

        double dist_currentToTarget = Math.ceil(getDistance(currentLocation, targetBrewery));
        double dist_targetToHome = Math.ceil(getDistance(targetBrewery, planeData.getHomeLocation()));
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

    /**
     * Finds nearest brewery while not sidetracking from target destination for too far.
     * @param planeData object containing information about the trip.
     */
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
            double tolerantPath = newPathDistance / getTolerantOffset();
            if (dist_currentToNearby < dist_currentToTarget && tolerantPath < dist_currentToTarget)
                alongTheWayNodes.add(new Logistics(brewId, dist_currentToNearby));
        }

        if (!alongTheWayNodes.isEmpty())
            planeData.setTargetNode(alongTheWayNodes.poll());
    }

    /**
     * Finds most promising area containing most breweries in specified radius.
     * Magic value from properties is used for determining radius
     * @param areaNodes Area containing the most breweries.
     *                  First elements from priority queue will be the nearest brewery
     * @param planeData object containing information about the trip.
     */
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

            double radius = (fuel - getDistance(currentLocation, possibleCenter)) / getMagicValue();
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

    /**
     * Uses Haversine formula to find distance between two points.
     * If 'distances' structure already contains needed distance, it will be returned without calculations.
     * If 'distances' structore doesn't contain needed distance, value will be calculated and then stored.
     * @param loc1 Location A
     * @param loc2 Location B
     * @return distance between two points
     */
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
}
