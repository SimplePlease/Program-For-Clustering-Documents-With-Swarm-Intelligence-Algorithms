package org.hse.aco.algorithm.graph;

import javafx.util.Pair;
import org.hse.aco.algorithm.graph.dto.Ant;
import org.hse.aco.algorithm.graph.dto.IterationResult;
import org.hse.aco.component.DatasetLoader;
import org.hse.aco.component.ProbabilityCollection;
import org.hse.aco.model.Cluster;
import org.hse.aco.model.ClusteringResult;
import org.hse.aco.model.Document;
import org.tinylog.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.ThreadLocalRandom.current;
import static org.hse.aco.controller.StartAlgorithmController.*;
import static org.hse.aco.model.DistanceFunction.COSINE;
import static org.hse.aco.model.DistanceFunction.EUCLIDEAN;

public class GraphAlgorithm {
    private final DatasetLoader datasetLoader = new DatasetLoader();

    private Map<Pair<Integer, Integer>, BigDecimal> calculatedDistances;

    private BigDecimal bestResult;

    private List<Integer> bestDocumentClusterIds;

    private int iterationsWithoutImprovement;

    private int skippedEdgesNumber;

    private int alphabetSize;

    public ClusteringResult clusterDocuments() throws Exception {
        List<Document> dataset = datasetLoader.loadDataset();
        alphabetSize = dataset.get(0).getAlphabetSize();
        int documentsNumber = dataset.size();
        Map<Pair<Integer, Integer>, BigDecimal> pheromone = new HashMap<>();

        calculatedDistances = new HashMap<>();
        bestResult = null;
        bestDocumentClusterIds = new ArrayList<>();
        iterationsWithoutImprovement = 0;
        skippedEdgesNumber = 0;

        var startTime = Instant.now();
        var iteration = 0;
        for (; iteration < ITERATIONS_LIMIT && iterationsWithoutImprovement < ITERATIONS_WITHOUT_IMPROVEMENT_LIMIT; iteration++) {
            List<Ant> ants = placeAnts(documentsNumber);
            antsVisitAllDocuments(ants, documentsNumber, dataset, pheromone);
            evaporatePheromone(documentsNumber, pheromone);
            increasePheromone(ants, pheromone);
            var iterationResult = calculateResult(pheromone, documentsNumber, dataset);

            Logger.debug("After iteration " + iteration + " average distance to cluster centroid is " + iterationResult.result() + ", skipping " + iterationResult.skippedEdgesNumber() + " edges");
            checkStoppingCriteria(iterationResult, iteration);
        }
        var duration = Duration.between(startTime, Instant.now()).toMillis();
        return getClusteringResult(dataset, iteration, duration);
    }

    private List<Ant> placeAnts(int documentsNumber) {
        return Stream.generate(() -> {
            var ant = new Ant(documentsNumber);
            Logger.debug("Ant placed: " + ant);
            return ant;
        }).limit(ANTS_NUMBER).toList();
    }

    private void antsVisitAllDocuments(
            List<Ant> ants,
            int documentsNumber,
            List<Document> documentsModel,
            Map<Pair<Integer, Integer>, BigDecimal> pheromone
    ) {
        for (var ant : ants) {
            while (ant.visitedDocumentsNumber() < documentsNumber) {
                ProbabilityCollection<Integer> nextDocumentProbabilityDistribution = new ProbabilityCollection<>();
                var currentDocument = documentsModel.get(ant.getCurrentDocument());

                var allProbabilities = IntStream.range(0, documentsNumber)
                        .filter(documentId -> (!ant.getVisitedDocuments().contains(documentId)) && documentId != currentDocument.id)
                        .mapToObj(documentId -> {
                            var documentPair = new Pair<>(Math.min(currentDocument.id, documentId), Math.max(currentDocument.id, documentId));
                            var distanceToDocument = distance(currentDocument, documentsModel.get(documentId));
                            if (distanceToDocument.compareTo(BigDecimal.ZERO) == 0) {
                                distanceToDocument = BigDecimal.valueOf(0.00000001);
                            }
                            if (DISTANCE_FUNCTION == EUCLIDEAN) {
                                distanceToDocument = BigDecimal.valueOf(DISTANCE_NUMERATOR).divide(distanceToDocument, 128, RoundingMode.HALF_UP);
                            }
                            var pheromoneBetweenDocuments = pheromone.getOrDefault(documentPair, BigDecimal.valueOf(PHEROMONE_INIT));
                            var probabilityToVisit = pheromoneBetweenDocuments.pow(PHEROMONE_POWER).multiply(
                                    distanceToDocument.pow(DISTANCE_POWER)
                            );
                            return new Pair<>(documentId, probabilityToVisit);
                        }).toList();

                BigDecimal probabilitiesSum = BigDecimal.ZERO;
                for (Pair<Integer, BigDecimal> documentIdToProbability : allProbabilities) {
                    probabilitiesSum = probabilitiesSum.add(documentIdToProbability.getValue());
                }
                BigDecimal finalProbabilitiesSum = probabilitiesSum;

                allProbabilities.forEach(documentIdToProbability -> {
                    var probability = documentIdToProbability.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(finalProbabilitiesSum, 128, RoundingMode.HALF_UP);
                    try {
                        nextDocumentProbabilityDistribution.add(documentIdToProbability.getKey(), Math.max(probability.longValue(), 1));
                    } catch (Exception e) {
                        Logger.debug("allProbabilities: " + allProbabilities);
                        Logger.debug("documentIdToProbability: " + documentIdToProbability);
                        Logger.debug("probabilitiesSum: " + finalProbabilitiesSum);
                        Logger.debug("probability: " + probability);
                        throw new RuntimeException(e);
                    }
                });

                var nextDocument = nextDocumentProbabilityDistribution.get();
                var distanceToNextDocument = distance(currentDocument, documentsModel.get(nextDocument));
                ant.visit(nextDocument, distanceToNextDocument);
            }
            Logger.debug("Ant visited following documents: " + ant.getVisitedDocuments() + " with total distance of " + ant.getDistanceTravelled());
        }
    }

    private BigDecimal distance(Document documentFrom, Document documentTo) {
        var key = new Pair<>(documentFrom.id, documentTo.id);
        var distance = BigDecimal.ZERO;
        if (DISTANCE_FUNCTION == EUCLIDEAN) {
            distance = documentFrom.distance(documentTo);
        } else {
            distance = documentFrom.cosine(documentTo);
        }
        BigDecimal finalDistance = distance;
        calculatedDistances.computeIfAbsent(key, k -> finalDistance);
        return calculatedDistances.get(key);
    }

    private void evaporatePheromone(int documentsNumber, Map<Pair<Integer, Integer>, BigDecimal> pheromone) {
        for (int documentMin = 0; documentMin < documentsNumber; documentMin++) {
            for (int documentMax = documentMin + 1; documentMax < documentsNumber; documentMax++) {
                var documentPair = new Pair<>(documentMin, documentMax);
                var currentPheromone = pheromone.getOrDefault(documentPair, BigDecimal.valueOf(PHEROMONE_INIT));
                pheromone.put(documentPair, currentPheromone.multiply(BigDecimal.valueOf(EVAPORATION_COEF)));
            }
        }
    }

    private void increasePheromone(List<Ant> ants, Map<Pair<Integer, Integer>, BigDecimal> pheromone) {
        for (var ant : ants) {
            var prevDocumentId = -1;
            for (Integer documentId : ant.getVisitedDocuments()) {
                if (prevDocumentId != -1) {
                    var documentPair = new Pair<>(Math.min(prevDocumentId, documentId), Math.max(prevDocumentId, documentId));
                    var distanceTravelled = ant.getDistanceTravelled();
                    if (distanceTravelled.compareTo(BigDecimal.ZERO) == 0) {
                        distanceTravelled = BigDecimal.valueOf(0.00000001);
                    }
                    var distanceTravelledFinal = distanceTravelled;
                    pheromone.compute(documentPair, (key, currentPheromone) ->
                            currentPheromone.add(BigDecimal.valueOf(PHEROMONE_DELTA_NUMERATOR).divide(distanceTravelledFinal, 128, RoundingMode.HALF_UP)));
                }
                prevDocumentId = documentId;
            }
        }
    }

    private IterationResult calculateResult(
            Map<Pair<Integer, Integer>, BigDecimal> pheromone,
            int documentsNumber,
            List<Document> documentsModel
    ) {
        var pheromoneMean = pheromone.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(pheromone.size()), 128, RoundingMode.HALF_UP);
        var sortedPheromones = new ArrayList<Pair<BigDecimal, Pair<Integer, Integer>>>();
        for (var entry : pheromone.entrySet()) {
            if (entry.getValue().compareTo(pheromoneMean) >= 0) {
                sortedPheromones.add(new Pair<>(entry.getValue(), entry.getKey()));
            }
        }
        sortedPheromones.sort(Comparator.comparing(Pair::getKey));

        var bound = Math.max(1, (int) Math.ceil(sortedPheromones.size() * 0.5));
        var skippedEdges = current().nextInt(bound);
        if (skippedEdges > 0) {
            sortedPheromones.subList(0, skippedEdges).clear();
        }

        var edges = new HashMap<Integer, List<Integer>>();
        for (var edge : sortedPheromones) {
            var x = edge.getValue().getKey();
            var y = edge.getValue().getValue();
            edges.computeIfAbsent(x, k -> new ArrayList<>());
            edges.computeIfAbsent(y, k -> new ArrayList<>());
            edges.get(x).add(y);
            edges.get(y).add(x);
        }

        int nextClusterId = 0;
        var documentsClusterIds = new ArrayList<>(Collections.nCopies(documentsNumber, -1));
        var centroidsPerCluster = new ArrayList<Document>();
        var documentNumberPerCluster = new ArrayList<Integer>();

        for (int documentId = 0; documentId < documentsNumber; documentId++) {
            if (documentsClusterIds.get(documentId) == -1) {
                centroidsPerCluster.add(new Document(-1, null, null, Collections.nCopies(alphabetSize, BigDecimal.ZERO)));
                documentNumberPerCluster.add(0);
                dfs(documentId, edges, nextClusterId, documentsClusterIds, centroidsPerCluster, documentNumberPerCluster, documentsModel);
                centroidsPerCluster.get(nextClusterId).divWordAppearances(documentNumberPerCluster.get(nextClusterId));
                nextClusterId++;
            }
        }

        var avrDocumentDistToCentroid = BigDecimal.ZERO;
        for (int clusterId = 0; clusterId < nextClusterId; clusterId++) {
            var accumulator = BigDecimal.ZERO;
            for (int documentId = 0; documentId < documentsClusterIds.size(); documentId++) {
                if (documentsClusterIds.get(documentId) == clusterId) {
                    var documentFrom = documentsModel.get(documentId);
                    var documentTo = centroidsPerCluster.get(clusterId);
                    if (DISTANCE_FUNCTION == EUCLIDEAN) {
                        accumulator = accumulator.add(documentFrom.distance(documentTo));
                    } else {
                        accumulator = accumulator.add(documentFrom.cosine(documentTo));
                    }
                }
            }
            accumulator = accumulator.divide(BigDecimal.valueOf(documentNumberPerCluster.get(clusterId)), 128, RoundingMode.HALF_UP);
            avrDocumentDistToCentroid = avrDocumentDistToCentroid.add(accumulator);
        }
        avrDocumentDistToCentroid = avrDocumentDistToCentroid.divide(BigDecimal.valueOf(nextClusterId), 128, RoundingMode.HALF_UP);

        return new IterationResult(
                avrDocumentDistToCentroid, skippedEdges, documentsClusterIds
        );
    }

    private void dfs(
            int documentId,
            HashMap<Integer, List<Integer>> edges,
            int clusterId,
            ArrayList<Integer> documentsClusterIds,
            ArrayList<Document> centroidsPerCluster,
            ArrayList<Integer> documentNumberPerCluster,
            List<Document> documentsModel
    ) {
        documentsClusterIds.set(documentId, clusterId);
        documentNumberPerCluster.set(clusterId, documentNumberPerCluster.get(clusterId) + 1);
        centroidsPerCluster.get(clusterId).addWordAppearances(documentsModel.get(documentId));

        for (var neighbourDocId : edges.getOrDefault(documentId, List.of())) {
            if (documentsClusterIds.get(neighbourDocId) == -1) {
                dfs(neighbourDocId, edges, clusterId, documentsClusterIds, centroidsPerCluster, documentNumberPerCluster, documentsModel);
            }
        }
    }

    private void checkStoppingCriteria(IterationResult iterationResult, int iteration) {
        boolean resultImproved = false;
        if (bestResult == null)
            resultImproved = true;
        else if (DISTANCE_FUNCTION == COSINE) {
            if (iterationResult.result().compareTo(bestResult) > 0)
                resultImproved = true;
        } else if (iterationResult.result().compareTo(bestResult) < 0)
            resultImproved = true;

        if (resultImproved) {
            BigDecimal prevFitness = null;
            if (bestResult != null)
                prevFitness = bestResult.setScale(4, RoundingMode.HALF_UP);
            Logger.info("Best result improved from " + prevFitness + " to " + iterationResult.result().setScale(4, RoundingMode.HALF_UP) + " at iteration " + iteration);
            bestResult = iterationResult.result();
            skippedEdgesNumber = iterationResult.skippedEdgesNumber();
            bestDocumentClusterIds = iterationResult.documentsClusterIds();
            iterationsWithoutImprovement = 0;
        } else {
            iterationsWithoutImprovement++;
        }
    }

    private ClusteringResult getClusteringResult(
            List<Document> dataset,
            int iteration,
            long duration
    ) {
        Logger.info("Finishing at iteration = " + iteration + " after " + iterationsWithoutImprovement + " iterations without improvement");
        Logger.info("Algorithm execution time is " + duration + " milliseconds\n");
        Logger.info("Final objective function is " + bestResult);
        Logger.info("Skipping " + skippedEdgesNumber + " edges with least pheromone");

        var clusterToDocumentIds = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < bestDocumentClusterIds.size(); i++) {
            var clusterId = bestDocumentClusterIds.get(i);
            clusterToDocumentIds.computeIfAbsent(clusterId, k -> new ArrayList<>());
            clusterToDocumentIds.get(clusterId).add(i);
        }

        var clusteringResult = new ClusteringResult(new ArrayList<>());
        int clusterId = 0;
        for (var clusterDocIds : clusterToDocumentIds.values()) {
            var cluster = new Cluster(clusterId++, new HashMap<>());
            for (var docId : clusterDocIds) {
                cluster.addDocument(dataset.get(docId).clusterName, docId);
            }
            clusteringResult.addCluster(cluster);
        }

        Logger.info(clusteringResult.toString(dataset));
        return clusteringResult;
    }
}
