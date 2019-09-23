package com.satalia.beertest.utilities;

import com.satalia.beertest.models.Logistics;
import com.satalia.beertest.models.Plane;
import com.satalia.beertest.models.Results;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Stack;

public class Printer {
    private static double executionTime;
    private static boolean routeFound;
    private static Plane planeData;

    public static void setPrinterData(Results results) {
        Printer.executionTime = results.getExecutionTime();
        Printer.routeFound = results.isRouteFound();
        Printer.planeData = results.getPlaneData();
    }

    public static void print() throws SQLException {
        if (!routeFound) {
            System.out.println("No reachable brewerious were found! Try different starting location");
            return;
        }

        MySQLConnection database = new MySQLConnection();

        Stack<Logistics> node = planeData.getVisitedBreweries();
        System.out.println("\nTrip journal:\n");
        System.out.printf("%-8s%-8s%-10s%s\n", "From", "To", "Distance", "Brewery name");
        System.out.printf("%-8s%-8s%-10s%s\n", "----", "--", "--------", "------------");
        System.out.printf("%-8s", "Home");

        for (Logistics a : node) {
            int brewId = a.getDestinationId();
            System.out.printf("%-8d%-10s%s\n", brewId, (int)a.getDistance() + "km.", database.getBreweryName(brewId));
            System.out.printf("%-8d", brewId);
        }
        double lastTripHome = Math.ceil(SearchingAlgorithm.getDistance(planeData.getCurrentLocation(), planeData.getHomeLocation()));
        planeData.setFuelLeft((int) (planeData.getFuelLeft() - lastTripHome));
        System.out.printf("%-8s%-10s\n", "Home", (int)lastTripHome + "km.");
        System.out.println("\nVisited breweries:");

        HashSet<String> beer = planeData.getCollectedBeerTypes();
        for (String a : beer)
            System.out.printf(" -> %s\n", a);

        System.out.println("\nInformation about the trip:");
        System.out.println("Breweries visited: " + planeData.getVisitedBreweries().size());
        System.out.println("Different beer types collected: " + planeData.getCollectedBeerTypes().size());
        System.out.println("Fuel left: " + planeData.getFuelLeft());
        System.out.println("Calculation time: " + executionTime);
    }
}
