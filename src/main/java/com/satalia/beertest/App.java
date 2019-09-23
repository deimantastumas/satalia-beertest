package com.satalia.beertest;

import com.satalia.beertest.models.Location;
import com.satalia.beertest.models.Results;
import com.satalia.beertest.utilities.Printer;
import com.satalia.beertest.utilities.SearchingAlgorithm;

import java.sql.SQLException;
import java.util.*;

public class App {
    private static double homeLatitude;
    private static double homeLongitude;
    private static boolean priority;
    private static Results results;

    public static void main(String[] args) throws SQLException {
        readUserInput();
        executeAlgorithm();
        printResults();
    }

    private static void printResults() throws SQLException {
        Printer.setPrinterData(results);
        Printer.print();
        promtIfRepeat();
    }

    private static void promtIfRepeat() throws SQLException {
        Scanner myObj = new Scanner(System.in);
        System.out.println("\nTry different location? (0 - no, 1 - yes):");
        boolean repeat = Integer.parseInt(myObj.nextLine()) == 1;
        if (repeat) main(null);
    }

    private static void executeAlgorithm() throws SQLException {
        results = Start(new Location(homeLatitude, homeLongitude), priority);
    }

    private static void readUserInput() {
        Scanner myObj = new Scanner(System.in);
        System.out.println("Enter home latitude (0 - 51.74250300):");
        homeLatitude = Double.parseDouble(myObj.nextLine());

        System.out.println("Enter home longitude (0 - 19.43295600):");
        homeLongitude = Double.parseDouble(myObj.nextLine());

        System.out.println("Enter your priority (0 - more breweries, 1 - more beer types):");
        priority = Integer.parseInt(myObj.nextLine()) == 0;

        if (homeLatitude == 0)
            homeLatitude = 51.74250300;
        if (homeLongitude == 0)
            homeLongitude = 19.43295600;
    }

    public static Results Start(Location home, boolean priority) throws SQLException {
        long startTime = System.nanoTime();
        Results results = SearchingAlgorithm.FindRoute(priority, home);
        long endTime = System.nanoTime();
        double executionTime = (double) (endTime - startTime) / 1000000000;
        results.setExecutionTime(executionTime);

        return results;
    }
}
