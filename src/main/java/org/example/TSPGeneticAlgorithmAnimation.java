package org.example;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import java.util.Arrays;


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
    private static final double mutationRate = 0.02;
    private static final Random random = new Random();
    private static final List<Point> cities = new ArrayList<>();
    private static List<int[]> population;
    private static final double[] cumulativeProportions = new double[POPULATION_SIZE];


    public static void main(String[] args) {
        generateCities(); // Generating cities as points with random locations
        generateInitialPopulation();
        // Generating and displaying initial route chart:
        XYChart chart = new XYChart(WIDTH, HEIGHT);
        XYSeries citiesSeries = chart.addSeries("Cities", getXData(), getYData());
        citiesSeries.setMarker(SeriesMarkers.CIRCLE);
        citiesSeries.setLineStyle(SeriesLines.NONE);
        SwingWrapper<XYChart> sw = new SwingWrapper<>(chart);
        sw.displayChart();

        int generationCount = 0;
        int genWithBestDistance = 0; // generation that contains the best distance ever
        int generationsWithNoImprovement = 0;
        double bestDistance = Integer.MAX_VALUE; // best distance ever/best distance in all generations yet
        int[] bestRoute = population.getFirst();
        List<Double> bestDistanceList = new ArrayList<>(); // best distance in every
        // generation will be added to this list; the list will be used as data for the progress chart

        while (generationCount < MAX_GENERATIONS && generationsWithNoImprovement < MAX_GENERATIONS_WITH_NO_IMPROVEMENT) {
            evolvePopulation(); // performing crossover and mutation on the current generation
            double currentBestDistance = getBestDistanceInPop(); // Getting distance of the best route
            // in current generation...
            bestDistanceList.add(currentBestDistance); // ...and adding it to best distance list
            int[] currentBestRoute = getBestRouteInPop(); // getting the best route in current generation
            System.out.println(currentBestDistance); // printing best distance in current generation

            if (currentBestDistance < bestDistance) { // if best distance in current generation
                // is better (shorter) than best distance ever, then we update...
                bestDistance = currentBestDistance; // ...best distance ever to equal current best distance...
                bestRoute = currentBestRoute; // ...best route ever to equal current best route...
                genWithBestDistance = generationCount; // ...generation with best distance to equal current generation...
                generationsWithNoImprovement = 0; // ...and reset count of generations with no improvement...
            } else generationsWithNoImprovement++; // ...otherwise increment count of generations with no improvement

            updatePlot(chart, bestRoute, bestDistance, generationCount, genWithBestDistance); // Update plot with the
            // best route in this generation...
            sw.repaintChart(); // ...and repaint the chart
            generationCount++; // Increment generation count
        }
        // Generating and displaying progress chart:
        List<Integer> XAxisData = generateXAxisData(bestDistanceList.size()); // creating X axis data
        // aka a list of integers/generation numbers
        XYChart chart2 = new XYChart(WIDTH2, HEIGHT2);
        // A plot that illustrates the progress of best distance value in each generation:
        XYSeries bestDistInGenSeries = chart2.addSeries("Best Distance in Generation", XAxisData, bestDistanceList);
        // A point that marks the best distance value ever:
        XYSeries bestDistEverSeries = chart2.addSeries("Best Distance Ever",
                Collections.singletonList(genWithBestDistance),
                Collections.singletonList(bestDistance));
        // Some style changes:
        bestDistInGenSeries.setMarker(SeriesMarkers.NONE);
        bestDistEverSeries.setMarkerColor(Color.red);
        XYStyler styler = chart2.getStyler();
        styler.setLegendPosition(Styler.LegendPosition.InsideNE);
        SwingWrapper<XYChart> sw2 = new SwingWrapper<>(chart2);
        sw2.displayChart(); // displaying the chart

        saveCharts(chart, chart2); // saving both charts as images
    }



    private static void generateCities() {
        // generating cities as Points with random coordinates and adding
        // them to the cities list
        for (int i = 0; i < NUMBER_OF_CITIES; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            cities.add(new Point(x, y));
        }
    }


    // Retrieving coordinate values of all Point objects in the cities list
    private static double[] getXData() {
        // Converting the list of Point objects into a stream;
        // mapToDouble applies the given function (getting the X coordinate of the Point)
        // to each element of the stream and returns the results converted to double;
        // Converting the stream of X-coordinate values to an array;
        return cities.stream().mapToDouble(Point::getX).toArray();
    }
    private static double[] getYData() {
        // Converting the list of Point objects into a stream;
        // mapToDouble applies the given function (getting the Y coordinate of the Point)
        // to each element of the stream and returns the results converted to double;
        // Converting the stream of Y-coordinate values to a double array;
        return cities.stream().mapToDouble(Point::getY).toArray();
    }


    private static List<Integer> generateXAxisData(int size) {
        // creating X axis data aka a list of
        // integers/generation numbers
        List<Integer> xAxisData = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            xAxisData.add(i);
        }
        return xAxisData;
    }


    private static void generateInitialPopulation() { // generating the initial population
        population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<Integer> cityIndices = new ArrayList<>();
            for (int j = 0; j < cities.size(); j++) cityIndices.add(j); // generating city indices

            Collections.shuffle(cityIndices); // Shuffling the city indices randomly
            cityIndices.add(cityIndices.getFirst()); // Adding whatever city happened to be first
            // to the end of the route to make sure the route is cyclical.

            // Creating a Stream from the List of cityIndices,
            // converting Integer (the wrapper object) to it's corresponding int (the primitive type) value,
            // and converting the Stream of primitive integers into an array:
            int[] route = cityIndices.stream().mapToInt(Integer::intValue).toArray();
            population.add(route); // adding a newly generated route to the population
        }
    }


    private static void evolvePopulation() {
        updateCumulativeProportions(); // updating cumulative proportions for the current generation
        List<int[]> newPopulation = new ArrayList<>();
        for (int i = ELITE_SIZE; i < POPULATION_SIZE; i++) { // i starts with ELITE_SIZE and not zero to
            // make room for the elite
            int[] parent1 = selectParent(); // Selecting first parent
            int[] parent2 = selectParent(); // Selecting second parent
            while (parent1==parent2) parent2 = selectParent(); // if parents are the same, select second
            // parent again until they are different
            int[] child = crossover(parent1, parent2); // performing crossover
            newPopulation.add(child); // adding new individual to the new population
        }
        population.sort(Comparator.comparingDouble(TSPGeneticAlgorithmAnimation::getDistance)); // sorting the previous
        // population from best to worst
        for (int i = 0; i<ELITE_SIZE; i++) {
            newPopulation.add(population.get(i)); // adding an ELITE_SIZE number of best individuals (aka the elite)
            // from the previous population to the new population without performing crossover on them
        }
        population = mutatePopulation(newPopulation, mutationRate); // mutating the population
    }


    private static int[] selectParent() { // randomly picking a way of parent selection with a 50%
        // chance to get any of the two ways
        if (random.nextDouble()>0.5) return tournamentSelection();
        else
            return biasedRandomSelection();
    }


    private static int[] tournamentSelection() {
        // randomly picking two candidates from the population:
        int[] candidate1 = population.get(random.nextInt(population.size()));
        int[] candidate2 = population.get(random.nextInt(population.size()));
        while(candidate1==candidate2) { // if they are the same, pick candidate2 again until they are different
            candidate2 = population.get(random.nextInt(population.size()));
        }
        // selecting the candidate with shorter distance to be a parent and take part in crossover:
        if (getDistance(candidate1) < getDistance(candidate2)) {
            return candidate1;
        }
        else return candidate2;
    }


    private static int[] biasedRandomSelection() {
        // biased random selection where individuals with shorter distances
        // take up a bigger fraction on a scale from 0 to 1 and therefore
        // have a higher chance of being picked
        double selectedValue = random.nextDouble(); // Generating a random value between 0 and 1
        // Iterating through the cumulative proportions array:
        for (int i=0; i<cumulativeProportions.length; i++) {
            double value = cumulativeProportions[i]; // Getting the cumulative proportion of the current individual
            // If the cumulative proportion is greater than the selected value,
            // choose the corresponding individual from the population:
            if(value > selectedValue) return population.get(i);
        }
        return null; // if something goes wrong, return null
    }


    private static int[] crossover(int[] parent1, int[] parent2) {
        // Performing crossover operation to create a child from two parent routes.
        // The last city should always be the same as the first one to make sure the route is cyclical. We may
        // change the order of all cities except the last one, and we will set the last city to be equal to the first.

        // Creating temporary arrays to hold parent routes without the last city:
        int[] tempparent1 = new int[parent1.length-1];
        int[] tempparent2 = new int[parent2.length-1];
        // Copying parent routes without the last city to temporary arrays:
        System.arraycopy(parent1, 0, tempparent1, 0, parent1.length-1);
        System.arraycopy(parent2, 0, tempparent2, 0, parent2.length-1);
        // Generating random start and end positions for crossover
        int startPos = random.nextInt(tempparent1.length);
        int endPos = random.nextInt(tempparent1.length);
        if (startPos > endPos) { // Ensuring startPos is less than endPos
            int tempPos = startPos;
            startPos = endPos;
            endPos = tempPos;
        }
        int[] child = new int[parent1.length]; // Creating a child route array
        // Setting a fill number to mark empty positions in the child route (otherwise after
        // performing crossover the city with index 0 will show up in the route multiple times):
        int fillnum = NUMBER_OF_CITIES+10;
        Arrays.fill(child, fillnum);
        // Copying the segment between startPos and endPos from tempparent1 to the child:
        System.arraycopy(tempparent1, startPos, child, startPos, endPos - startPos);
        // Inserting missing cities from tempparent2 into the child:
        for (int i = 0; i < tempparent2.length; i++) { // iterating through cities of tempparent2
            if (!contains(child, tempparent2[i])) { // if the child doesn't contain the current city of tempparent2...
                for (int j = 0; j < child.length-1; j++) { // ...then iterate through all cities
                    // of the child except the last one...
                    if (child[j]==fillnum) { // ...and if the current city of the child equals fillnum (is empty)...
                        child[j] = tempparent2[i]; // ... then make the city of the child equal
                        // to the current city of tempparent2...
                        break; // ... and move on to the next city of tempparent2
                    }
                }
            }
        }
        child[child.length-1] = child[0]; // make the last city of the child equal to the first city of the child
        return child;
    }


    private static boolean contains(int[] array, int value) {
        for (int i : array) { // Iterate through the array to check if the value is present
            if (i == value) return true; // If current element of the array equals value, return true
        }
        return false; // otherwise return false
    }


    public static int[] mutate(int[] individual, double mutationRate) {
        // Mutating the individual (randomly swapping cities) with a given mutation rate (probability).
        // The last city should always be the same as the first one to make sure the route is cyclical. We may
        // change the order of all cities except the last one, and we will set the last city to be equal to the first.
        Random random = new Random();
        int[] tempindiv = new int[individual.length-1];  // Creating a temporary array
        // Copying the individual to the temporary array without the last element:
        System.arraycopy(individual, 0, tempindiv, 0, individual.length-1);
        // Iterating over the genes/cities in the route to apply mutation:
        for (int swapped = 0; swapped < tempindiv.length; swapped++) {
            if (random.nextDouble() < mutationRate) { // Checking if mutation should occur based on the mutation rate
                // Determining which city to swap the current city with:
                int swapWith = (int) (random.nextDouble() * tempindiv.length);
                // Swapping the genes:
                int city1 = tempindiv[swapped];
                int city2 = tempindiv[swapWith];
                tempindiv[swapped] = city2;
                tempindiv[swapWith] = city1;
            }
        }
        // Creating a new array to store the mutated individual:
        int[] mutatedindiv = new int[individual.length];
        // Copying the cities from the temporary array to the mutated individual:
        System.arraycopy(tempindiv, 0, mutatedindiv, 0, tempindiv.length);
        // Setting the last city of the mutated individual to be the same as its first city:
        mutatedindiv[mutatedindiv.length-1] = mutatedindiv[0];

        return mutatedindiv;
    }


    public static List<int[]> mutatePopulation(List<int[]> population, double mutationRate) {
        // Mutating the entire population with a given mutation rate.
        List<int[]> mutatedPopulation = new ArrayList<>(); // Creating a new list to store the mutated population
        for (int[] individual : population) {  // Iterating over each individual in the population
            // Mutating the individual using the mutate method:
            int[] mutatedIndividual = mutate(individual, mutationRate);
            mutatedPopulation.add(mutatedIndividual); // Adding the mutated individual to the mutated population
        }
        return mutatedPopulation;
    }


    public static void updateCumulativeProportions() {
        double sum = 0; // Sum of distances of all routes in the population
        double proportionSum = 0.0; // Sum of proportions of distances
        double[] proportions = new double[population.size()]; // an array that holds values of proportions separately
        double[] normalisedProportions = new double[proportions.length]; // an array that holds normalised proportions
        double cumulativeTotal = 0.0; // Cumulative total used to calculate cumulative proportions

        // Calculate the sum of distances of all routes in the population:
        for (int i=0; i<population.size(); i++) {
            sum += getDistance(population.get(i)); // get the distance of the current route and add it to the sum
        }
        // Calculate inverse proportions of distances for each route in the population and add them to the array
        // The shorter the route - the higher the value, so shorter individuals are more likely to get picked
        for (int i=0; i<population.size(); i++) {
            proportions[i] = (sum/getDistance(population.get(i)));
        }
        // Normalising proportions to make sure each value falls between 0 and 1 and their sum equals 1:
        for(int i=0; i< proportions.length; i++) {  // Calculate the sum of all proportions
            proportionSum += proportions[i];
        }
        for(int i=0; i< proportions.length; i++) { // Normalize the proportions by dividing
            // each proportion by the sum of proportions
            normalisedProportions[i] = proportions[i]/proportionSum;
        } // Now each value falls between 0 and 1 and their sum equals 1
        for(int i=0; i<proportions.length; i++) { // Calculate the cumulative proportions
            cumulativeTotal += normalisedProportions[i];  // Add the normalized proportion to the cumulative total
            cumulativeProportions[i] = cumulativeTotal; // Store the cumulative total for each route
        }
    }


    public static double getDistance(int[] route) { // Calculating the total distance of a route.
        double totalDistance = 0.0;
        for (int i=0; i < route.length-1; i++) { // Iterating over each pair of consecutive cities in the route
            // Getting the coordinates of the current city and the next city in the route:
            Point fromTown = cities.get(route[i]);
            Point toTown = cities.get(route[i+1]);
            // Calculating the distance between the current city and the next city
            // using the Euclidean distance formula:
            int x = toTown.x - fromTown.x;
            int y = toTown.y - fromTown.y;
            double distance = Math.sqrt(x*x + y*y);
            totalDistance += distance; // Adding the calculated distance to the total distance
        }
        return totalDistance;
    }


    public static int[] getBestRouteInPop() {
        // Retrieving the best (shortest) route in the current population
        // Initializing variables to store the best route and its distance:
        int[] bestRouteInPop = population.getFirst();
        double bestDistanceInPop = getDistance(bestRouteInPop);
        for (int[] route : population) {  // Iterating over each route in the population
            double currentDistance = getDistance(route); // Calculating the distance of the current route
            if(currentDistance < bestDistanceInPop) {  // Checking if the current route has a shorter distance
                // than the best known distance
                // Updating the best distance and best route if a shorter distance is found:
                bestDistanceInPop=currentDistance;
                bestRouteInPop=route;
            }
        }
        return bestRouteInPop; // Returning the best route found in the population
    }


    public static double getBestDistanceInPop() {
        // Retrieving the shortest distance among all route distances in the current population.
        // Initializing the best distance with the distance of the first route in the population:
        double bestDistanceInPop = getDistance(population.getFirst());
        for (int[] route : population) { // Iterating over each route in the population
            double currentDistance = getDistance(route); // Calculating the distance of the current route
            if(currentDistance < bestDistanceInPop) {
                bestDistanceInPop=currentDistance;
            }
        }
        return bestDistanceInPop; // Returning the best distance found in the population
    }


    private static void updatePlot(XYChart chart, int[] bestRoute,
                                   double bestDistance, int genCount, int genWithBestDistance) {
        // Updating the title of the chart to display the current best distance & other info
        chart.setTitle("BD: " + bestDistance + // Best known distance
                "   BDG: " + genWithBestDistance + // Best known distance generation
                "   CG: " + genCount + // Current generation
                "   PS: " + POPULATION_SIZE + // population size
                "   ES: " + ELITE_SIZE + // elite size
                "   MG: " + MAX_GENERATIONS + // maximum generations
                "   MGNI: " + MAX_GENERATIONS_WITH_NO_IMPROVEMENT + // maximum generations with no improvement
                "   MR: " + mutationRate); // mutation rate
        // Checking if the "Route" series already exists:
        boolean routeSeriesExists = chart.getSeriesMap().containsKey("Route");
        // If the "Route" series doesn't exist and the route data is not empty, create it
        // and add it to the chart (normally happens when the method is called for the first time):
        if (!routeSeriesExists && bestRoute.length > 0) {
            // Creating arrays to store data for X and Y axes (city coordinates):
            double[] xData = new double[bestRoute.length];
            double[] yData = new double[bestRoute.length];
            // Populating the arrays with city coordinates from the best known route:
            for (int i = 0; i < bestRoute.length; i++) {
                Point city = cities.get(bestRoute[i]); // getting the city with
                // the index equal to bestRoute[i] from the list of cities.
                // Adding X and Y coordinates of the city to respective arrays:
                xData[i] = city.getX();
                yData[i] = city.getY();
            }
            // Adding the "Route" series with the provided data to the chart:
            chart.addSeries("Route", xData, yData).setLineColor(Color.blue);
        }
        // If the "Route" series already exists and the route data is not empty,
        // update the series with the new data:
        else if (routeSeriesExists && bestRoute.length > 0) {
            // Creating arrays to store data for X and Y axes (city coordinates):
            double[] xData = new double[bestRoute.length];
            double[] yData = new double[bestRoute.length];
            // Populating the arrays with city coordinates from the best known route:
            for (int i = 0; i < bestRoute.length; i++) {
                Point city = cities.get(bestRoute[i]); // getting the city with
                // the index equal to bestRoute[i] from the list of cities.
                // Adding X and Y coordinates of the city to respective arrays:
                xData[i] = city.getX();
                yData[i] = city.getY();
            }
            // Updating the "Route" series in the chart with the new data:
            chart.updateXYSeries("Route", xData, yData, null).setMarker(SeriesMarkers.NONE);;
        }
    }


    private static void saveCharts(XYChart chart1, XYChart chart2) {
        // Saving the best route chart and the progress chart as images
        // Creating a directory to store the charts if it doesn't exist:
        File folder = new File("charts");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String directoryPath = "charts"; // Defining the save directory path
        // Defining base names for the chart filenames
        String baseName1 = "chartroute";
        String baseName2 = "chartprogress";
        // Getting the filenames of existing charts in the directory:
        String[] filenames = getFilenames(directoryPath);
        // Determining the next available number for the filenames:
        int nextNumber = getNextNumber(filenames, baseName1);
        // Generating filenames for the charts based on the next available number:
        String nextFilename1 = baseName1 + nextNumber;
        String nextFilename2 = baseName2 + nextNumber;
        try {
            // Saving the first and second charts as images:
            BitmapEncoder.saveBitmap(chart1, "charts/" + nextFilename1, BitmapEncoder.BitmapFormat.PNG);
            BitmapEncoder.saveBitmap(chart2, "charts/" + nextFilename2, BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String[] getFilenames(String directoryPath) {
        // Getting the filenames of files in the specified directory
        File directory = new File(directoryPath);  // Creating a File object representing the directory
        return directory.list(); // Getting the list of filenames in the directory
    }

    private static int getNextNumber(String[] filenames, String baseName) {
        // Determining the next available number for filenames with the specified base name
        int maxNumber = 0;
        // Iterating through filenames in the array to find the maximum number:
        for (String filename : filenames) {
            if (filename.startsWith(baseName)) { // Checking if the filename starts with the specified base name
                // Extracting the number string from the filename (substracting 4 from filename length to exclude ".png")
                String numberString = filename.substring(baseName.length(), filename.length()-4);
                try {
                    int number = Integer.parseInt(numberString); // Parsing the extracted number
                    maxNumber = Math.max(maxNumber, number); // If current number is higher than the highest
                    // number known before, update the highest number.
                } catch (NumberFormatException e) {
                    // Print a warning message if the number cannot be parsed:
                    System.err.println("Warning: Unable to parse number from filename: " + filename);
                }
            }
        }
        return maxNumber + 1; // Returning the next available number
    }
}