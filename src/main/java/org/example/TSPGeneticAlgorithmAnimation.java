package org.example;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;


public class TSPGeneticAlgorithmAnimation {

    private static final int NUMBER_OF_CITIES = 30;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int WIDTH2 = 1550;
    private static final int HEIGHT2 = 600;
    private static final int POPULATION_SIZE = 80;
    public static final int ELITE_SIZE = 8;
    private static final int MAX_GENERATIONS = 100000;
    private static final int MAX_GENERATIONS_WITH_NO_IMPROVEMENT = 20000;
    private static final double mutationRatePopulation = 0.02;
    private static final Random random = new Random();
    private static final List<Point> cities = new ArrayList<>();
    private static List<int[]> population;
    private static final double[] cumulativeProportions = new double[POPULATION_SIZE];
    private static final int startEndCityIndex = random.nextInt(NUMBER_OF_CITIES);


    public static void main(String[] args) {
        generateCities(NUMBER_OF_CITIES);
        generateInitialPopulation();

        XYChart chart = new XYChart(WIDTH, HEIGHT);
        XYSeries citiesSeries = chart.addSeries("Cities", getXData(), getYData());
        citiesSeries.setMarker(SeriesMarkers.CIRCLE);
        citiesSeries.setLineStyle(SeriesLines.NONE);
        SwingWrapper<XYChart> sw = new SwingWrapper<>(chart);
        sw.displayChart();

        int generationCount = 0;
        int genWithBestDistance = 0;
        int generationsWithNoImprovement = 0;
        double bestDistance = Integer.MAX_VALUE;
        int[] bestRoute = population.get(0);
        List<Double> bestDistanceList = new ArrayList<>();

        while (generationCount < MAX_GENERATIONS && generationsWithNoImprovement < MAX_GENERATIONS_WITH_NO_IMPROVEMENT) {
            evolvePopulation();
            double currentBestDistance = getBestDistanceInPop(); // Get distance of the best route
            bestDistanceList.add(currentBestDistance);
            int[] currentBestRoute = getBestRouteInPop();
            System.out.println(currentBestDistance);

            if (currentBestDistance < bestDistance) {
                bestDistance = currentBestDistance;
                bestRoute = currentBestRoute;
                genWithBestDistance = generationCount;
                generationsWithNoImprovement = 0; // Reset count of generations with no improvement
            } else generationsWithNoImprovement++; // Increment count of generations with no improvement

            updatePlot(chart, bestRoute); // Update plot with the best route in this generation
            sw.repaintChart();

            generationCount++; // Increment generation count
        }

        List<Integer> XAxisData = generateXAxisData(bestDistanceList.size());
        XYChart chart2 = new XYChart(WIDTH2, HEIGHT2);
        XYSeries bestDistInGenSeries = chart2.addSeries("Best Distance in Generation", XAxisData, bestDistanceList);
        XYSeries bestDistEverSeries = chart2.addSeries("Best Distance Ever",
                Collections.singletonList(genWithBestDistance),
                Collections.singletonList(bestDistance));
        bestDistInGenSeries.setMarker(SeriesMarkers.NONE);
        bestDistEverSeries.setMarkerColor(Color.red);
        XYStyler styler = chart2.getStyler();
        styler.setLegendPosition(Styler.LegendPosition.InsideNE);
        SwingWrapper<XYChart> sw2 = new SwingWrapper<>(chart2);
        sw2.displayChart();
    }


    private static void generateCities(int numCities) {
        for (int i = 0; i < numCities; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            cities.add(new Point(x, y));
        }
    }


    private static double[] getXData() {
        return cities.stream().mapToDouble(Point::getX).toArray();
    }

    private static double[] getYData() {
        return cities.stream().mapToDouble(Point::getY).toArray();
    }


    private static List<Integer> generateXAxisData(int size) {
        List<Integer> xAxisData = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            xAxisData.add(i);
        }
        return xAxisData;
    }


    private static void generateInitialPopulation() {
        population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<Integer> cityIndices = new ArrayList<>();
            for (int j = 0; j < cities.size(); j++) cityIndices.add(j);

            Collections.shuffle(cityIndices); // Shuffle the city indices randomly
            cityIndices.remove((Integer) startEndCityIndex); // Remove start/end city from middle of route
            cityIndices.add(0, startEndCityIndex); // Add start/end city at beginning of route
            cityIndices.add(startEndCityIndex); // Add start/end city at end of route
            int[] route = cityIndices.stream().mapToInt(Integer::intValue).toArray();
            population.add(route);
        }
    }


    private static void evolvePopulation() {
        updateCumulativeProportions();
        List<int[]> newPopulation = new ArrayList<>();
        for (int i = ELITE_SIZE; i < POPULATION_SIZE; i++) {
            int[] parent1 = selectParent();
            int[] parent2 = selectParent();
            while (parent1==parent2) parent2 = selectParent();
            int[] child = crossover(parent1, parent2);
            newPopulation.add(child);
        }
        population.sort(Comparator.comparingDouble(TSPGeneticAlgorithmAnimation::getDistance));
        for (int i = 0; i<ELITE_SIZE; i++) {
            newPopulation.add(population.get(i));
        }
        population = mutatePopulation(newPopulation, mutationRatePopulation);
    }


    private static int[] selectParent() {
        if (random.nextDouble()>0.5) return tournamentSelection();
        else
            return biasedRandomSelection();
    }


    private static int[] tournamentSelection() {
        int[] candidate1 = population.get(random.nextInt(population.size()));
        int[] candidate2 = population.get(random.nextInt(population.size()));
        while(candidate1==candidate2) {
            candidate2 = population.get(random.nextInt(population.size()));
        }
        if (getDistance(candidate1) < getDistance(candidate2)) {
            return candidate1;
        }
        else return candidate2;
    }


    private static int[] biasedRandomSelection() {
        double selectedValue = random.nextDouble();
        for (int i=0; i<cumulativeProportions.length; i++) {
            double value = cumulativeProportions[i];
            if(value > selectedValue) return population.get(i);
        }
        return null;
    }


    private static int[] crossover(int[] parent1, int[] parent2) {
        int[] tempparent1 = new int[parent1.length-2];
        int[] tempparent2 = new int[parent2.length-2];
        System.arraycopy(parent1, 1, tempparent1, 0, parent1.length-2);
        System.arraycopy(parent2, 1, tempparent2, 0, parent2.length-2);

        int startPos = random.nextInt(tempparent1.length);
        int endPos = random.nextInt(tempparent1.length);
        if (startPos > endPos) {
            int tempPos = startPos;
            startPos = endPos;
            endPos = tempPos;
        }
        int[] child = new int[parent1.length];
        int fillnum = NUMBER_OF_CITIES+10;
        Arrays.fill(child, fillnum);
        child[0] = parent1[0];
        child[child.length-1] = parent1[parent1.length-1];

        // Copy the segment between startPos and endPos from tempparent1 to the child
        System.arraycopy(tempparent1, startPos, child, startPos+1, endPos - startPos);

        for (int i = 0; i < tempparent2.length; i++) {
            if (!contains(child, tempparent2[i])) {
                for (int j = 1; j < child.length-1; j++) {
                    if (child[j]==fillnum) {
                        child[j] = tempparent2[i];
                        break;
                    }
                }
            }
        }

        return child;
    }


    private static boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value) return true;
        }
        return false;
    }


    public static int[] mutate(int[] individual, double mutationRate) {
        Random random = new Random();
        int[] tempindiv = new int[individual.length-2];
        System.arraycopy(individual, 1, tempindiv, 0, individual.length-2);

        for (int swapped = 0; swapped < tempindiv.length; swapped++) {
            if (random.nextDouble() < mutationRate) {
                int swapWith = (int) (random.nextDouble() * tempindiv.length);
                int city1 = tempindiv[swapped];
                int city2 = tempindiv[swapWith];
                tempindiv[swapped] = city2;
                tempindiv[swapWith] = city1;
            }
        }
        int[] mutatedindiv = new int[individual.length];
        mutatedindiv[0] = individual[0];
        mutatedindiv[mutatedindiv.length-1] = individual[individual.length-1];
        System.arraycopy(tempindiv, 0, mutatedindiv, 1, tempindiv.length);
        return mutatedindiv;
    }


    public static List<int[]> mutatePopulation(List<int[]> population, double mutationRate) {
        List<int[]> mutatedPopulation = new ArrayList<>();
        for (int[] individual : population) {
            int[] mutatedIndividual = mutate(individual, mutationRate);
            mutatedPopulation.add(mutatedIndividual);
        }
        return mutatedPopulation;
    }


    public static void updateCumulativeProportions() {
        double sum = 0;
        double proportionSum = 0.0;
        double[] proportions = new double[population.size()];
        double[] normalisedProportions = new double[proportions.length];
        double cumulativeTotal = 0.0;

        for (int i=0; i<population.size(); i++) {
            sum += getDistance(population.get(i));
        }
        for (int i=0; i<population.size(); i++) {
            proportions[i] = (sum/getDistance(population.get(i)));
        }
        for(int i=0; i< proportions.length; i++) {
            proportionSum += proportions[i];
        }
        for(int i=0; i< proportions.length; i++) {
            normalisedProportions[i] = proportions[i]/proportionSum;
        }
        for(int i=0; i<proportions.length; i++) {
            cumulativeTotal += normalisedProportions[i];
            cumulativeProportions[i] = cumulativeTotal;
        }
    }


    public static double getDistance(int[] route) {
        double totalDistance = 0.0;
        for (int i=0; i < route.length-1; i++) {
            Point fromTown = cities.get(route[i]);
            Point toTown = cities.get(route[i+1]);
            int x = toTown.x - fromTown.x;
            int y = toTown.y - fromTown.y;
            double distance = Math.sqrt(x*x + y*y);
            totalDistance += distance;
        }
        return totalDistance;
    }


    public static int[] getBestRouteInPop() {
        int[] bestRouteInPop = population.get(0);
        double bestDistanceInPop = getDistance(bestRouteInPop);
        for (int[] route : population) {
            double currentDistance = getDistance(route);
            if(currentDistance < bestDistanceInPop) {
                bestDistanceInPop=currentDistance;
                bestRouteInPop=route;
            }
        }
        return bestRouteInPop;
    }


    public static double getBestDistanceInPop() {
        double bestDistanceInPop = getDistance(population.get(0));
        for (int[] route : population) {
            double currentDistance = getDistance(route);
            if(currentDistance < bestDistanceInPop) {
                bestDistanceInPop=currentDistance;
            }
        }
        return bestDistanceInPop;
    }


    private static void updatePlot(XYChart chart, int[] route) {
        // Calculate the total distance of the route
        double totalDistance = getDistance(route);
        // Update the title of the chart to display the current total distance
        chart.setTitle("TSP Genetic Algorithm (Total Distance: " + totalDistance + ")");

        // Check if the "Route" series already exists
        boolean routeSeriesExists = chart.getSeriesMap().containsKey("Route");
        // If the "Route" series doesn't exist and the route data is not empty, create and add it to the chart
        if (!routeSeriesExists && route.length > 0) {
            double[] xData = new double[route.length];
            double[] yData = new double[route.length];
            for (int i = 0; i < route.length; i++) {
                Point city = cities.get(route[i]);
                xData[i] = city.getX();
                yData[i] = city.getY();
            }
            chart.addSeries("Route", xData, yData).setLineColor(Color.blue);
        }
        else if (routeSeriesExists && route.length > 0) {
            // If the "Route" series already exists, update it with the new data
            double[] xData = new double[route.length];
            double[] yData = new double[route.length];
            for (int i = 0; i < route.length; i++) {
                Point city = cities.get(route[i]);
                xData[i] = city.getX();
                yData[i] = city.getY();
            }
            chart.updateXYSeries("Route", xData, yData, null).setMarker(SeriesMarkers.NONE);;
        }
    }
}
