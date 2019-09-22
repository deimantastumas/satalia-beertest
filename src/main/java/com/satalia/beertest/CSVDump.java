package com.satalia.beertest;

import com.opencsv.CSVReader;
import com.satalia.beertest.models.BreweryLocation;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CSVDump {
    private static void ParseData() {
        HashMap<Integer, BreweryLocation> locationList = new HashMap<>();
        HashMap<Integer, HashSet<String>> beerTypesWithId = new HashMap<>();
        HashMap<Integer, String> breweryNames = new HashMap<>();

        String startPath = "/Users/deimantastumas/Desktop/beertest/satalia-beertest/src/main/java/com/satalia/beertest/resources/";
        String beerData = startPath + "/beer.csv";
        String breweryData = startPath + "/breweries.csv";
        String locationData = startPath + "/geocodes.csv";

        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(breweryData));
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line[0].equals("id"))
                    continue;
                int breweryId = Integer.parseInt(line[0]);
                String breweryName = line[1];
                breweryNames.put(breweryId, breweryName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            reader = new CSVReader(new FileReader(beerData));
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line[0].equals("id"))
                    continue;
                int breweryId = Integer.parseInt(line[1]);
                String beerType = line[2];
                if (!beerTypesWithId.containsKey(breweryId)) {
                    HashSet<String> temp = new HashSet<>();
                    temp.add(beerType);
                    beerTypesWithId.put(breweryId, temp);
                } else {
                    HashSet<String> temp = beerTypesWithId.get(breweryId);
                    temp.add(beerType);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            reader = new CSVReader(new FileReader(locationData));
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line[0].equals("id"))
                    continue;
                int breweryId = Integer.parseInt(line[1]);
                HashSet<String> beerTypes = null;
                String breweryName = null;
                if (breweryNames.containsKey(breweryId))
                    breweryName = breweryNames.get(breweryId);
                else continue;
                if (beerTypesWithId.containsKey(breweryId))
                    beerTypes = beerTypesWithId.get(breweryId);
                locationList.put(breweryId,
                        new BreweryLocation(
                                Float.parseFloat(line[2]),
                                Float.parseFloat(line[3]),
                                breweryId,
                                beerTypes,
                                breweryName)
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        //SQL STATEMENTS
        //table:breweries
        for (Map.Entry<Integer, String> reachableNode : breweryNames.entrySet()) {
            int brewId = reachableNode.getKey();
            String breweryName = reachableNode.getValue();

            if (!locationList.containsKey(brewId))
                continue;
            double latitude = locationList.get(brewId).getLatitude();
            double longitude = locationList.get(brewId).getLongitude();
            System.out.println("INSERT INTO breweries");
            System.out.println("VALUES (" + brewId + ", \"" + breweryName + "\" , " + latitude + ", " + longitude + ");");
            System.out.println();
        }

        //table:beerTypes
        int id = 1;
        for (Map.Entry<Integer, HashSet<String>> reachableNode : beerTypesWithId.entrySet()) {
            int brewId = reachableNode.getKey();
            if (!locationList.containsKey(brewId))
                continue;
            HashSet<String> beerTypes = reachableNode.getValue();
            for (String beerType : beerTypes) {
                String repl1 = "\\";

                String repl2 = repl1 + "\"";
                String replaceFrom = "\"";

                String correctType = beerType.replace(replaceFrom, repl2);
                System.out.println("INSERT INTO beerTypes");
                System.out.println("VALUES (" + id++ + ", " + brewId + ", \"" + correctType + "\");");
                System.out.println();
            }
        }
    }
}
