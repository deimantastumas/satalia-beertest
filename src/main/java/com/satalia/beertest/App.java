package com.satalia.beertest;

import com.satalia.beertest.models.BreweryLocation;
import com.satalia.beertest.models.Location;
import com.satalia.beertest.models.Logistics;
import com.satalia.beertest.models.Plane;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class App {
    private final static double homelatitude = 51.74250300;
    private final static double homelongitude = 19.43295600;
    private final static Location homeLocation = new Location(homelatitude, homelongitude);
    private final static int startingFuel = 2000;
    private final static int earthRadius = 6371000;
    private final static double tolerantOffset = 1.3;
    private final static int reachableRadius = startingFuel / 2;

    public static void main(String[] args) {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/Users/deimantastumas/Desktop/beertest/satalia-beertest/src/main/java/com/satalia/beertest/resources/geocodes.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<List<String>> records2 = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("/Users/deimantastumas/Desktop/beertest/satalia-beertest/src/main/java/com/satalia/beertest/resources/beer.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records2.add(Arrays.asList(values));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        records.remove(0);
        records2.remove(0);

        List<BreweryLocation> locationList = new ArrayList<>();
        HashMap<Integer, HashSet<String>> beerTypesWithId = new HashMap<>();

        for (List<String> row : records2) {
            if (row.get(0).equals(""))
                continue;
            if (row.size() != 13)
                continue;
            if (row.get(1).matches(".*[a-zA-Z].*"))
                continue;
            int breweryId = Integer.parseInt(row.get(1));
            String beerType = row.get(2);
            if (!beerTypesWithId.containsKey(breweryId)) {
                HashSet<String> temp = new HashSet<>();
                temp.add(beerType);
                beerTypesWithId.put(breweryId, temp);
            } else {
                HashSet<String> temp = beerTypesWithId.get(breweryId);
                temp.add(beerType);
            }
        }

        for (List<String> row : records) {
            int breweryId = Integer.parseInt(row.get(1));
            HashSet<String> beerTypes = null;
            if (beerTypesWithId.containsKey(breweryId))
                beerTypes = beerTypesWithId.get(breweryId);
            locationList.add(
                    new BreweryLocation(
                            Float.parseFloat(row.get(2)),
                            Float.parseFloat(row.get(3)),
                            breweryId,
                            beerTypes)
            );
        }


        Location currentLocation = new Location(homelatitude, homelongitude);

        HashMap<Integer, BreweryLocation> reachableArea = new HashMap<>();

        for (BreweryLocation a : locationList) {
            double distance = getDistance(homeLocation, a);
            if (distance <= reachableRadius) {
                reachableArea.put(a.getBrew_id(), a);
            }
        }

        Plane planeData = new Plane(startingFuel, currentLocation, reachableArea);
        PriorityQueue<Logistics> areaNodes = new PriorityQueue<>();
        boolean needForBeer = true;

        while (needForBeer) {
            if (planeData.getReachableArea().isEmpty())
                break;
            Plane.Mode currentMode = planeData.getPlaneMode();
            switch (currentMode) {
                case LOOKING_FOR_AREA:
                    FindDenseArea(areaNodes, planeData); //Find a center node in the most populated area AND get the closest node : current_location -> areaNodes
                    planeData.setPlaneMode(Plane.Mode.TRAVELLING_TO_AREA);
                    if (planeData.getTargetNode().peek() == null)
                        planeData.setPlaneMode(Plane.Mode.LAST_BREATH);
                    break;
                case TRAVELLING_TO_AREA:
                    FindNearByNode(planeData);
                    if (planeData.getTargetNode().size() == 1) //nearbynode wasn't found :'( !
                        planeData.setPlaneMode(Plane.Mode.LOOKING_FOR_AREA);
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

        printStack(planeData.getVisitedBreweries(), planeData);
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
                    targetBrewery.getBeerTypes())
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
            double tolerantPath = newPathDistance / tolerantOffset;
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

            double radius = (fuel - getDistance(currentLocation, possibleCenter)) / 2;
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

    private static void printStack(Stack<Logistics> node, Plane planeData) {

        System.out.print("BEER JOURNEY: \n0 -> ");
        for (Logistics a : node) {
            System.out.println(a.getDestinationId() + " (" + a.getDistance() + ")");
            System.out.print(a.getDestinationId() + " -> ");
        }
        double lastTripHome = Math.ceil(getDistance(planeData.getCurrentLocation(), homeLocation));
        planeData.setFuelLeft((int) (planeData.getFuelLeft() - lastTripHome));
        System.out.println("0 (" + lastTripHome + ")\n");
        System.out.println("Fuel left: " + planeData.getFuelLeft());
        System.out.println("Breweries visited: " + planeData.getVisitedBreweries().size());
        System.out.println();
        int num = 1;
        System.out.println("Collected beer types:");
        HashSet<String> beer = planeData.getCollectedBeerTypes();
        for (String a : beer) {
            System.out.println(num++ + ": " + a);
        }
    }

    private static double getDistance(Location loc1, Location loc2) {
        double lat1 = loc1.getLatitude() * Math.PI / 180;
        double long1 = loc1.getLongitude() * Math.PI / 180;
        double lat2 = loc2.getLatitude() * Math.PI / 180;
        double long2 = loc2.getLongitude() * Math.PI / 180;

        double first = Math.pow(Math.sin((lat2 - lat1) / 2), 2);
        double second2 = Math.pow(Math.sin((long2 - long1) / 2), 2);
        double second1 = Math.cos(lat1)*Math.cos(lat2);
        double second = second1 * second2;
        return (2*earthRadius*Math.asin(Math.sqrt(first + second)) / 1000);
    }
}
