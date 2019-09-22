package com.satalia.beertest;

import com.satalia.beertest.models.BreweryLocation;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;

public class MySQLConnection {
    private Connection connect = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet results = null;

    void ConnectToDatabase() throws SQLException {
        // Setup the connection with the DB
        connect = DriverManager
                .getConnection("jdbc:mysql://127.0.0.1:3306/beertest?"
                        + "user=root&password=root");
    }

    public ArrayList<BreweryLocation> getBreweries() throws SQLException {
        ArrayList<BreweryLocation> breweries = new ArrayList<>();

        Statement statement = connect.createStatement();
        results = statement.executeQuery("select * from breweries");
        while (results.next()) {
            int brewId = results.getInt("brewId");
            String brewName = results.getString("name");
            double latitude = results.getDouble("latitude");
            double longitude = results.getDouble("longitude");
            breweries.add(new BreweryLocation(latitude, longitude, brewId, null, brewName));
        }

        return breweries;
    }

    void updateBeerTypes(BreweryLocation brewery) throws SQLException {
        preparedStatement = connect.prepareStatement("select * from beerTypes where brewId = ?");
        preparedStatement.setInt(1, brewery.getBrew_id());
        results = preparedStatement.executeQuery();
        HashSet<String> beerTypes = new HashSet<>();
        while (results.next()) {
            beerTypes.add(results.getString("beerType"));
        }
        brewery.setBeerTypes(beerTypes);
    }

    public String getBreweryName(int brewId) throws SQLException {
        preparedStatement = connect.prepareStatement("select name from breweries where brewId = ?");
        preparedStatement.setInt(1, brewId);
        results = preparedStatement.executeQuery();
        results.next();
        return results.getString("name");
    }
}
