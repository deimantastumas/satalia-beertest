package com.satalia.beertest.javafx;

import com.satalia.beertest.App;
import com.satalia.beertest.models.Location;
import com.satalia.beertest.models.Logistics;
import com.satalia.beertest.models.Plane;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Stack;

public class Controller {

    @FXML RadioButton rb_brews = new RadioButton();
    @FXML RadioButton rb_beerTypes = new RadioButton();
    @FXML TextField tf_latitude = new TextField();
    @FXML TextField tf_longitude = new TextField();
    @FXML VBox vb_collectedBeer = new VBox();
    @FXML VBox vb_visitedBrews = new VBox();
    private boolean priority = true; // true - brews, false - beer

    public void findRoute() throws SQLException, ClassNotFoundException {
        vb_collectedBeer.getChildren().clear();
        vb_visitedBrews.getChildren().clear();
        Location homeLocation = new Location((Double.parseDouble(tf_latitude.getText())),
                Double.parseDouble(tf_longitude.getText()));

        long startTime = System.nanoTime();
        if (!App.Start(homeLocation, priority)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information about the trip");
            alert.setHeaderText("Trip status");
            alert.setContentText("No reachable breweries found!");
            alert.showAndWait();
            return;
        }
        long endTime   = System.nanoTime();
        double totalTime = (double)(endTime - startTime) / 1000000000;

        Plane planeData = App.getPlaneData();
        Stack<Logistics> node = planeData.getVisitedBreweries();

        StringBuilder text = new StringBuilder();
        text.append("[Home] -> ");
        for (Logistics a : node) {
            int brewId = a.getDestinationId();
            text.append(String.format("[%d] - %dkm. (%s)", brewId, (int)a.getDistance(), App.database.getBreweryName(brewId)));
            Text beerText = new Text(text.toString());
            beerText.setFill(Color.web("#f5f6fa"));
            beerText.setFont(Font.font ("Verdana", 11));
            vb_visitedBrews.getChildren().add(beerText);
            text = new StringBuilder();
            text.append(" -> ");
        }
        double lastTripHome = Math.ceil(App.getDistance(planeData.getCurrentLocation(), homeLocation));
        String lastLine = String.format(" -> [HOME] - %dkm", (int)lastTripHome);
        Text lastText = new Text(lastLine);
        lastText.setFill(Color.web("#f5f6fa"));
        lastText.setFont(Font.font ("Verdana", 11));
        vb_visitedBrews.getChildren().add(lastText);

        planeData.setFuelLeft((int) (planeData.getFuelLeft() - lastTripHome));

        int num = 1;
        HashSet<String> beer = planeData.getCollectedBeerTypes();
        for (String a : beer) {
            String beerLine = num++ + ": " + a;
            Text beerText = new Text(beerLine);
            beerText.setFill(Color.web("#f5f6fa"));
            beerText.setFont(Font.font ("Arial", 11));
            vb_collectedBeer.getChildren().add(beerText);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information about the trip");
        alert.setHeaderText("Trip status");
        alert.setContentText(String.format("Breweries visited: %d\n" +
                        "Different beer types collected: %d\n" +
                        "Fuel left: %d\n" +
                        "Calculation time: %f seconds.",
                planeData.getVisitedBreweries().size(),
                planeData.getCollectedBeerTypes().size(),
                planeData.getFuelLeft(),
                totalTime));

        alert.showAndWait();
    }

    public void selectPriority_brews() {
        rb_beerTypes.selectedProperty().setValue(false);
        rb_brews.selectedProperty().setValue(true);
        priority = true;
    }

    public void selectPriority_beerTypes() {
        rb_brews.selectedProperty().setValue(false);
        rb_beerTypes.selectedProperty().setValue(true);
        priority = false;
    }
}
