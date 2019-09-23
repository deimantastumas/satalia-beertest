package com.satalia.beertest.utilities;

import com.satalia.beertest.models.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HaversineTest {

    @Test
    void calculateDistance() {
        Location vilnius = new Location(54.68723, 25.27163);
        Location kaunas = new Location(54.90257, 23.89834);

        assertEquals(91.23, Haversine.calculateDistance(vilnius, kaunas), 0.01);
    }
}