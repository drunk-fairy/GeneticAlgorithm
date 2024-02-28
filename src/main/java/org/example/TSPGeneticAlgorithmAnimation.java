package org.example;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
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
    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATIONS = 20000;
    private static final int MAX_GENERATIONS_WITH_NO_IMPROVEMENT = 2000;
    private static final Random random = new Random();
    private static final List<Point> cities = new ArrayList<>();
    private static List<int[]> population;
    private static final double[] cumulativeProportions = new double[POPULATION_SIZE];
    private static int count = 0;
    private static int count2 = 0;


    public static void main(String[] args) {
        generateCities(NUMBER_OF_CITIES);
        generateInitialPopulation();

        XYChart chart = QuickChart.getChart("TSP Genetic Algorithm", "X", "Y", "Cities", getXData(), getYData());
        //chart.getStyler().setSeriesRenderStyle("Cities", XYSeries.XYSeriesRenderStyle.Scatter);
        SwingWrapper<XYChart> sw = new SwingWrapper<>(chart);
        sw.displayChart();

        int generationCount = 0;
        int generationsWithNoImprovement = 0;
        double bestDistance = Integer.MAX_VALUE;
        int[] bestRoute = population.get(0);

        while (generationCount < MAX_GENERATIONS && generationsWithNoImprovement < MAX_GENERATIONS_WITH_NO_IMPROVEMENT) {
            evolvePopulation();
            double currentBestDistance = getBestDistanceInPop();
            int[] currentBestRoute = getBestRouteInPop(); // Calculate distance of the best route
            // TODO it seems to pick the first element in generation rather than the best one. Change that.
            // upd: nevermind. there was a length-based sorting implemented in evolvePopulation()
            System.out.println(currentBestDistance);
            if (currentBestDistance < bestDistance) {
                bestDistance = currentBestDistance;
                bestRoute = currentBestRoute;
                generationsWithNoImprovement = 0; // Reset count of generations with no improvement
            } else {
                generationsWithNoImprovement++; // Increment count of generations with no improvement
            }
            updatePlot(chart, bestRoute); // Update plot with the best route in this generation
            sw.repaintChart();
            try {
                Thread.sleep(10); // Add a short delay to see the animation
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            generationCount++; // Increment generation count
        }
    }

    private static void generateCities(int numCities) {
        //System.out.println("generateCities");
        for (int i = 0; i < numCities; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            cities.add(new Point(x, y));
        }
    }

    private static void generateInitialPopulation() {
        //System.out.println("generatePopulation");
        population = new ArrayList<>(); // initialising previously declared _population_
        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<Integer> cityIndices = new ArrayList<>();
            for (int j = 0; j < cities.size(); j++) { // _cites_ is declared and initialised before _main_
                // and filled with randomly generated Points that represent cities in _generateCities_ function
                // here we create as many city indices as there are cities
                cityIndices.add(j);
            }
            Collections.shuffle(cityIndices); // shuffle _cityIndices_ randomly to create a root
            int[] route = cityIndices.stream().mapToInt(Integer::intValue).toArray();
            // TODO figure out how exactly the previous line works
            population.add(route); // add the newly created route to the initial population
        }
    }

    private static void evolvePopulation() {
        //System.out.println("-----EVOLVE POPULATION--EVOLVE POPULATION---EVOLVE POPULATION--EVOLVE POPULATION");
        updateCumulativeProportions();
        List<int[]> newPopulation = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            int[] parent1 = selectParent(); // TODO parents are now selected randomly and i think it needs to be changed
            int[] parent2 = selectParent();
            while (parent1==parent2) parent2 = selectParent();
            int[] child = crossover(parent1, parent2);
            //mutate(child);
            newPopulation.add(child);
        }
        double mutationRatePopulation = 0.005;
        //population = newPopulation;
        population = mutatePopulation(newPopulation, mutationRatePopulation);;
        //Collections.sort(population, Comparator.comparingDouble(TSPGeneticAlgorithmAnimation::getDistance));
        //Collections.sort(population, (route1, route2) -> calculateRouteDistance(route1) - calculateRouteDistance(route2));
    }

    private static int[] selectParent() {
        //System.out.println("selectParent");
        //return population.get(random.nextInt(population.size()));
        if (random.nextDouble()>0.5) {
            return tournamentSelection();
        }
        else {
            return biasedRandomSelection();
        }
        //return biasedRandomSelection();
    }

    private static int[] tournamentSelection() {
        //System.out.println("tournament");
        int[] candidate1 = population.get(random.nextInt(population.size()));
        int[] candidate2 = population.get(random.nextInt(population.size()));

        while(candidate1==candidate2) {
            candidate2 = population.get(random.nextInt(population.size()));
        }

        if (getDistance(candidate1) < getDistance(candidate2)) {
            return candidate1; // choosing the candidate with smaller distance
        }
        else return candidate2;
    }

    private static int[] biasedRandomSelection() {
        //System.out.println("biased");
        double selectedValue = random.nextDouble();
        for (int i=0; i<cumulativeProportions.length; i++) {
            double value = cumulativeProportions[i];
            if(value > selectedValue) return population.get(i);
        }
        return null;
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


    public static void updateCumulativeProportions() {
        //System.out.println("--------------UPDATE CUM FUN------------------------");
        double sum = 0;
        double proportionSum = 0.0;
        double[] proportions = new double[population.size()];
        double[] normalisedProportions = new double[proportions.length];
        //double[] cumulativeProportions = new double[proportions.length];
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
        // double proportionSum = Arrays.stream(proportions).sum();

        for(int i=0; i< proportions.length; i++) {
            normalisedProportions[i] = proportions[i]/proportionSum;
        }

        for(int i=0; i<proportions.length; i++) {
            cumulativeTotal += normalisedProportions[i];
            cumulativeProportions[i] = cumulativeTotal;
        }
    }


    private static int[] crossover(int[] parent1, int[] parent2) {
        //System.out.println("crossover");
        int startPos = random.nextInt(parent1.length);
        int endPos = random.nextInt(parent1.length);
        int[] child = new int[parent1.length];
        for (int i = 0; i < parent1.length; i++) {
            if (startPos < endPos && i > startPos && i < endPos) {
                child[i] = parent1[i];
            } else if (startPos > endPos && !(i < startPos && i > endPos)) {
                child[i] = parent1[i];
            }
        }
        for (int i = 0; i < parent2.length; i++) {
            if (!contains(child, parent2[i])) {
                for (int j = 0; j < child.length; j++) {
                    if (child[j] == 0) {
                        child[j] = parent2[i];
                        break;
                    }
                }
            }
        }
        return child;
    }

//    private static void mutate(int[] route) {
//        count++;
//        //System.out.println("mutate; count =");
//        //System.out.println(count);
//        //System.out.println();
//        int pos1 = random.nextInt(route.length);
//        int pos2 = random.nextInt(route.length);
//        int temp = route[pos1];
//        route[pos1] = route[pos2];
//        route[pos2] = temp;
//    }

    public static int[] mutate(int[] individual, double mutationRate) {
        Random random = new Random();
        for (int swapped = 0; swapped < individual.length; swapped++) {
            if (random.nextDouble() < mutationRate) {
                int swapWith = (int) (random.nextDouble() * individual.length);

                int city1 = individual[swapped];
                int city2 = individual[swapWith];

                individual[swapped] = city2;
                individual[swapWith] = city1;
            }
        }
        return individual;
    }


    public static List<int[]> mutatePopulation(List<int[]> population, double mutationRate) {
        List<int[]> mutatedPopulation = new ArrayList<>();

        for (int[] individual : population) {
            int[] mutatedIndividual = mutate(individual, mutationRate);
            mutatedPopulation.add(mutatedIndividual);
        }
        return mutatedPopulation;
    }


    private static boolean contains(int[] array, int value) {
        //System.out.println("contains");
        for (int i : array) {
            if (i == value) {
                return true;
            }
        }
        return false;
    }

    private static double[] getXData() {
        System.out.println("getX");
        return cities.stream().mapToDouble(Point::getX).toArray();
    }

    private static double[] getYData() {
        System.out.println("getY");
        return cities.stream().mapToDouble(Point::getY).toArray();
    }

//    private static int calculateRouteDistance(int[] route) {
//        int distance = 0;
//        for (int i = 0; i < route.length - 1; i++) {
//            Point city1 = cities.get(route[i]);
//            Point city2 = cities.get(route[i + 1]);
//            distance += calculateDistance(city1, city2);
//        }
//        return distance;
//    }

    public static double getDistance(int[] route) {
        // TODO create a function like getBestDistance
        //System.out.println("get distance");
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

//    private static double calculateDistance(Point p1, Point p2) {
//        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
//    }

    private static void updatePlot(XYChart chart, int[] route) {
        count2++;
        //System.out.println("-----------------UPDATE PLOT-----------------------count2 =");
        //System.out.println(count2);
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
            chart.addSeries("Route", xData, yData);
        } else if (routeSeriesExists && route.length > 0) {
            // If the "Route" series already exists, update it with the new data
            double[] xData = new double[route.length];
            double[] yData = new double[route.length];
            for (int i = 0; i < route.length; i++) {
                Point city = cities.get(route[i]);
                xData[i] = city.getX();
                yData[i] = city.getY();
            }
            chart.updateXYSeries("Route", xData, yData, null);
        }
    }

//    private static void updatePlot(XYChart chart, int[] route) {
//        // Check if the "Route" series already exists
//        boolean routeSeriesExists = chart.getSeriesMap().containsKey("Route");
//
//        // If the "Route" series doesn't exist and the route data is not empty, create and add it to the chart
//        if (!routeSeriesExists && route.length > 0) {
//            double[] xData = new double[route.length];
//            double[] yData = new double[route.length];
//            for (int i = 0; i < route.length; i++) {
//                Point city = cities.get(route[i]);
//                xData[i] = city.getX();
//                yData[i] = city.getY();
//            }
//            chart.addSeries("Route", xData, yData);
//        } else if (routeSeriesExists && route.length > 0) {
//            // If the "Route" series already exists, update it with the new data
//            double[] xData = new double[route.length];
//            double[] yData = new double[route.length];
//            for (int i = 0; i < route.length; i++) {
//                Point city = cities.get(route[i]);
//                xData[i] = city.getX();
//                yData[i] = city.getY();
//            }
//            chart.updateXYSeries("Route", xData, yData, null);
//        }
//    }
}
