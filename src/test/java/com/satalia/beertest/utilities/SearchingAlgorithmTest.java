package com.satalia.beertest.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchingAlgorithmTest {


    @Test
    void findRoute() {
        boolean routeFound = SearchingAlgorithm.FindRoute(false, null, null).isRouteFound();
        assertFalse(routeFound);

        boolean anotherRouteFound = SearchingAlgorithm.FindRoute(true, null, null).isRouteFound();
        assertFalse(anotherRouteFound);
    }
}