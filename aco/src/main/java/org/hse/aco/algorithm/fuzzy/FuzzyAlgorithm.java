package org.hse.aco.algorithm.fuzzy;

import javafx.util.Pair;
import org.hse.aco.algorithm.fuzzy.dto.Ant;
import org.hse.aco.algorithm.fuzzy.dto.IterationSolutions;
import org.hse.aco.algorithm.fuzzy.dto.IterationSolutions.Solution;
import org.hse.aco.component.DatasetLoader;
import org.hse.aco.model.Cluster;
import org.hse.aco.model.ClusteringResult;
import org.hse.aco.model.Document;
import org.tinylog.Logger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.hse.aco.controller.StartAlgorithmController.*;

public class FuzzyAlgorithm {
    private final DatasetLoader datasetLoader = new DatasetLoader();

    private final boolean idealClustering = false;

    public ClusteringResult clusterDocuments() throws Exception {
        List<Document> dataset = datasetLoader.loadDataset();
        int documentsNumber = dataset.size();
        int alphabetSize = dataset.get(0).getAlphabetSize();

        Map<Pair<Integer, Integer>, BigDecimal> pheromone = initPheromoneMatrix(documentsNumber);
        Map<Pair<Integer, Integer>, BigDecimal> calculatedDistances = new HashMap<>();

        var startTime = Instant.now();
        Solution globalBestSolution = new Solution(null, null);

        int iteration = 0, iterationsWithoutImprovement = 0;
        for (; iteration < ITERATIONS_LIMIT && iterationsWithoutImprovement < ITERATIONS_WITHOUT_IMPROVEMENT_LIMIT; iteration++, iterationsWithoutImprovement++) {
            var iterationSolutions = new IterationSolutions();

            for (int antId = 0; antId < ANTS_NUMBER; antId++) {
                Ant ant = new Ant(documentsNumber);
                ant.buildSolution(dataset, calculatedDistances, pheromone);
                var solution = ant.getSolution();

                if (idealClustering) {
                    ITERATIONS_LIMIT = 1;
                    for (int i = 0; i < solution.assignedClusters.length; i++) {
                        solution.assignedClusters[i] = 0;
                        if (i > solution.assignedClusters.length / 2) {
                            solution.assignedClusters[i] = 1;
                        }
                    }
                }

                ant.evaluateSolution(dataset, alphabetSize);
                Logger.debug("Solution of ant " + ant + " is " + Arrays.toString(solution.assignedClusters));
                Logger.debug("Objective function of ant " + ant + " is " + solution.fitness);

                updatePheromones(pheromone, solution);
                iterationSolutions.addSolution(solution);
            }

            // The current iteration's best ant in the colony performs an additional pheromone update
            var bestSolution = iterationSolutions.getBestSolution();
            updatePheromones(pheromone, bestSolution);

            Logger.debug("Best objective function of iteration " + iteration + " is " + bestSolution.fitness);
            Logger.debug("Best solution of iteration " + iteration + " is " + Arrays.toString(bestSolution.assignedClusters) + "\n");

            if (bestSolution.betterThan(globalBestSolution)) {
                BigDecimal prevFitness = null;
                if (globalBestSolution.fitness != null)
                    prevFitness = globalBestSolution.fitness.setScale(4, RoundingMode.HALF_UP);
                Logger.info("Best solution improved from " + prevFitness + " to " + bestSolution.fitness.setScale(4, RoundingMode.HALF_UP) + " at iteration " + iteration);
                globalBestSolution = bestSolution;
                iterationsWithoutImprovement = -1;
            }
        }
        var endTime = Instant.now();

        Logger.info("Finishing at iteration = " + iteration + " after " + iterationsWithoutImprovement + " iterations without improvement");
        Logger.info("Algorithm execution time is " + Duration.between(startTime, endTime).toMillis() + " milliseconds\n");
        Logger.info("Final objective function is " + globalBestSolution.fitness);
        outputPheromoneMatrix(pheromone);

        return getClusteringResult(documentsNumber, globalBestSolution.assignedClusters, dataset);
    }

    private static void updatePheromones(Map<Pair<Integer, Integer>, BigDecimal> pheromone, Solution solution) {
        for (var pheromoneEntry : pheromone.entrySet()) {
            var documentId = pheromoneEntry.getKey().getKey();
            var clusterId = pheromoneEntry.getKey().getValue();
            var currentPheromone = pheromoneEntry.getValue();
            var pheromoneDelta = BigDecimal.ZERO;

            if (solution.assignedClusters[documentId] == clusterId) {
                pheromoneDelta = solution.fitness.pow(2, new MathContext(128));
            }
            pheromoneEntry.setValue(currentPheromone.multiply(BigDecimal.valueOf(EVAPORATION_COEF)).add(pheromoneDelta));
        }
    }

    private ClusteringResult getClusteringResult(int documentsNumber, int[] globalBestSolution, List<Document> dataset) {
        var clusteringResult = new ClusteringResult(new ArrayList<>());
        for (int clusterId = 0; clusterId < CLUSTERS_AMOUNT; clusterId++) {
            var cluster = new Cluster(clusterId, new HashMap<>());

            for (int documentId = 0; documentId < documentsNumber; documentId++) {
                if (globalBestSolution[documentId] == clusterId) {
                    var docClass = dataset.get(documentId).clusterName;
                    cluster.addDocument(docClass, documentId);
                }
            }
            clusteringResult.addCluster(cluster);
        }
        Logger.info(clusteringResult.toString(dataset));
        return clusteringResult;
    }

    private Map<Pair<Integer, Integer>, BigDecimal> initPheromoneMatrix(int documentsNumber) {
        Map<Pair<Integer, Integer>, BigDecimal> pheromone = new HashMap<>();
        for (int i = 0; i < documentsNumber; i++) {
            for (int j = 0; j < CLUSTERS_AMOUNT; j++) {
                pheromone.put(new Pair<>(i, j), new BigDecimal(PHEROMONE_INIT));
            }
        }
        return pheromone;
    }

    private void outputPheromoneMatrix(Map<Pair<Integer, Integer>, BigDecimal> pheromone) {
        Logger.debug("Final pheromone matrix is:");
        for (var pheromoneEntry : pheromone.entrySet()) {
            var documentId = pheromoneEntry.getKey().getKey();
            var clusterId = pheromoneEntry.getKey().getValue();
            var currentPheromone = pheromoneEntry.getValue();
            Logger.debug("Document " + documentId + " to cluster " + clusterId + " pheromone is " + currentPheromone.setScale(4, RoundingMode.HALF_UP));
        }
    }
}
