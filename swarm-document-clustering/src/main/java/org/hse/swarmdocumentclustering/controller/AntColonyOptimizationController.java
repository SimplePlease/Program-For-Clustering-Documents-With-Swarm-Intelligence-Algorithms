package org.hse.swarmdocumentclustering.controller;

import com.opencsv.CSVReader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.util.Pair;
import org.hse.swarmdocumentclustering.component.ProbabilityCollection;
import org.hse.swarmdocumentclustering.dto.IterationResult;
import org.hse.swarmdocumentclustering.model.Ant;
import org.hse.swarmdocumentclustering.model.Document;
import org.tinylog.Logger;

import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.ThreadLocalRandom.current;

public class AntColonyOptimizationController {
    @FXML
    public Label clusteringResultText;

    public static int ITERATIONS_LIMIT = 5000;

    public static int ITERATIONS_WITHOUT_IMPROVEMENT_LIMIT = 1000;

    public static int ANTS_NUMBER = 30;

    public static double EVAPORATION_COEF = 0.6;

    public static int PHEROMONE_POWER = 1;

    public static int DISTANCE_POWER = 5;

    public static int DISTANCE_NUMERATOR = 10;

    public static int PHEROMONE_DELTA_NUMERATOR = 40;

    private Map<Pair<Integer, Integer>, BigDecimal> calculatedDistances;

    private BigDecimal bestResult;

    private List<Integer> bestDocumentClusterIds;

    private int iterationsWithoutImprovement;

    private int skippedEdgesNumber;

    private int alphabetSize;

    @FXML
    protected void onRunClusteringClick() throws Exception {
        List<Document> documentsModel = initDocumentsModel();
        int documentsNumber = documentsModel.size();
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
            antsVisitAllDocuments(ants, documentsNumber, documentsModel, pheromone);
            evaporatePheromone(documentsNumber, pheromone);
            increasePheromone(ants, pheromone);
            var iterationResult = calculateResult(pheromone, documentsNumber, documentsModel);

            var iterationResultText = "After iteration " + iteration + " average distance to cluster centroid is " + iterationResult.result();
//            clusteringResultText.setText(iterationResultText);
            ExecutorService executor = Executors.newFixedThreadPool(1);

            Logger.debug("After iteration " + iteration + " average distance to cluster centroid is " + iterationResult.result() + ", skipping " + iterationResult.skippedEdgesNumber() + " edges");
            checkStoppingCriteria(iterationResult);
        }
        var endTime = Instant.now();
        Logger.debug("Algorithm execution time is " + Duration.between(startTime, endTime).toMillis() + " milliseconds");

        splitDocumentsIntoClusters(pheromone, iteration);
    }

    private IterationResult calculateResult(
            Map<Pair<Integer, Integer>, BigDecimal> pheromone,
            int documentsNumber,
            List<Document> documentsModel
    ) {
        var pheromoneMean = pheromone.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(pheromone.size()), 8, RoundingMode.HALF_UP);
        var sortedPheromones = new ArrayList<Pair<BigDecimal, Pair<Integer, Integer>>>();
        for (var entry : pheromone.entrySet()) {
            if (entry.getValue().compareTo(pheromoneMean) >= 0) {
                sortedPheromones.add(new Pair<>(entry.getValue(), entry.getKey()));
            }
        }
        sortedPheromones.sort(Comparator.comparing(Pair::getKey));

        var bound = Math.max(1, (int) Math.ceil(sortedPheromones.size() * 0.60));
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
                centroidsPerCluster.add(new Document(-1, Collections.nCopies(alphabetSize, BigDecimal.ZERO)));
                documentNumberPerCluster.add(0);
                dfs(documentId, edges, nextClusterId, documentsClusterIds, centroidsPerCluster, documentNumberPerCluster, documentsModel);
                centroidsPerCluster.get(nextClusterId).divWordAppearances(documentNumberPerCluster.get(nextClusterId));
                nextClusterId++;
            }
        }

//        if (nextClusterId != 2) {
//            return new IterationResult(BigDecimal.valueOf(Long.MAX_VALUE), -1, List.of());
//        }

        var avrDocumentDistToCentroid = BigDecimal.ZERO;
        for (int clusterId = 0; clusterId < nextClusterId; clusterId++) {
            var accumulator = BigDecimal.ZERO;
            for (int documentId = 0; documentId < documentsClusterIds.size(); documentId++) {
                if (documentsClusterIds.get(documentId) == clusterId) {
                    accumulator = accumulator.add(documentsModel.get(documentId).distance(centroidsPerCluster.get(clusterId)));
                }
            }
            accumulator = accumulator.divide(BigDecimal.valueOf(documentNumberPerCluster.get(clusterId)), 8, RoundingMode.HALF_UP);
            avrDocumentDistToCentroid = avrDocumentDistToCentroid.add(accumulator);
        }
        avrDocumentDistToCentroid = avrDocumentDistToCentroid.divide(BigDecimal.valueOf(nextClusterId), 8, RoundingMode.HALF_UP);

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

    private void checkStoppingCriteria(IterationResult iterationResult) {
        if (bestResult == null || iterationResult.result().compareTo(bestResult) < 0) {
            Logger.debug("Best result improved from " + bestResult + " to " + iterationResult.result() + ", now skipping " + iterationResult.skippedEdgesNumber() + " edges instead of " + skippedEdgesNumber);
            bestResult = iterationResult.result();
            skippedEdgesNumber = iterationResult.skippedEdgesNumber();
            bestDocumentClusterIds = iterationResult.documentsClusterIds();
            iterationsWithoutImprovement = 0;
        } else {
            iterationsWithoutImprovement++;
        }
    }

    private void splitDocumentsIntoClusters(Map<Pair<Integer, Integer>, BigDecimal> pheromone, int iteration) {
        var clusteringResult = new StringBuilder("Finishing at iteration = " + iteration + " after " + iterationsWithoutImprovement + " iterations without improvement");
        clusteringResult.append("\nBest average distance to cluster centroid is ").append(bestResult);

        Logger.info("Finishing at iteration = " + iteration + " after " + iterationsWithoutImprovement + " iterations without improvement");
        Logger.info("Best average distance to cluster centroid is " + bestResult);
        Logger.info("Skipping " + skippedEdgesNumber + " edges with least pheromone");

        var clusterToDocumentIds = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < bestDocumentClusterIds.size(); i++) {
            var clusterId = bestDocumentClusterIds.get(i);
            clusterToDocumentIds.computeIfAbsent(clusterId, k -> new ArrayList<>());
            clusterToDocumentIds.get(clusterId).add(i);
        }

        var yPos = 100;
        clusteringResult.append("\nBest result achieved by splitting into ").append(clusterToDocumentIds.size()).append(" clusters");
        Logger.info("Best result achieved by splitting into " + clusterToDocumentIds.size() + " clusters");
        for (var clusterToDocs : clusterToDocumentIds.entrySet()) {
            Logger.info("Cluster " + clusterToDocs.getKey() + " contains documents: " + clusterToDocs.getValue());
            clusteringResult.append("\nCluster ").append(clusterToDocs.getKey()).append(" contains documents: ").append(clusterToDocs.getValue());
//            clusterTexts.add(new Text(20, yPos, "Cluster " + clusterToDocs.getKey() + " contains documents: " + clusterToDocs.getValue()));
            yPos += 20;
        }

//        Text iterationsAmountText = new Text(20, 40, "Finishing at iteration = " + iteration + " with " + iterationsWithoutImprovement + " iterations without improvement");
//        Text clustersAmountText = new Text(20, 60, "Best result achieved by splitting into " + clusterToDocumentIds.size() + " clusters");
//        Text bestResultText = new Text(20, 80, "Best average distance to cluster centroid is " + bestResult);
//        Group group = new Group(iterationsAmountText, clustersAmountText, bestResultText);
//        group.getChildren().addAll(clusterTexts);
//        Scene scene = clusteringRanText.getScene();
//        ((TilePane) (scene.getRoot())).getChildren().add(group);
//        Stage stage = (Stage) scene.getWindow();
//        stage.show();

        clusteringResultText.setText(clusteringResult.toString());
        clusteringResultText.setAlignment(Pos.CENTER_LEFT);
        clusteringResultText.setFont(Font.font(15));

//        pheromone.forEach((integerIntegerPair, bigDecimal) -> Logger.debug("Pair " + integerIntegerPair + " with pheromone = " + bigDecimal.setScale(4, RoundingMode.HALF_UP)));
//        var pheromoneMean = pheromone.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(pheromone.size()), 8, RoundingMode.HALF_UP);
//        Logger.debug("Pheromone mean = " + pheromoneMean);
//
//        var sortedPheromones = new ArrayList<Pair<BigDecimal, Pair<Integer, Integer>>>();
//        for (var entry : pheromone.entrySet()) {
//            if (entry.getValue().compareTo(pheromoneMean) >= 0) {
//                sortedPheromones.add(new Pair<>(entry.getValue(), entry.getKey()));
//            }
//        }
//        sortedPheromones.sort(Comparator.comparing(Pair::getKey));
//
//        var i = 1;
//        for (var entry : sortedPheromones) {
//            if (i > skippedEdgesNumber) {
//                Logger.debug("Cluster edge: " + entry.getValue());
//            } else {
//                Logger.debug("SKIPPED cluster edge: " + entry.getValue());
//            }
//            i++;
//        }
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
                            currentPheromone.add(BigDecimal.valueOf(PHEROMONE_DELTA_NUMERATOR).divide(distanceTravelledFinal, 8, RoundingMode.HALF_UP)));
                }
                prevDocumentId = documentId;
            }
        }
    }

    private void evaporatePheromone(int documentsNumber, Map<Pair<Integer, Integer>, BigDecimal> pheromone) {
        for (int documentMin = 0; documentMin < documentsNumber; documentMin++) {
            for (int documentMax = documentMin + 1; documentMax < documentsNumber; documentMax++) {
                var documentPair = new Pair<>(documentMin, documentMax);
                var currentPheromone = pheromone.getOrDefault(documentPair, BigDecimal.ONE.divide(BigDecimal.valueOf(documentsNumber), 8, RoundingMode.HALF_UP));
                pheromone.put(documentPair, currentPheromone.multiply(BigDecimal.valueOf(EVAPORATION_COEF)));
            }
        }
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
                            var closenessToDocument = BigDecimal.valueOf(DISTANCE_NUMERATOR).divide(distanceToDocument, 8, RoundingMode.HALF_UP);
                            var pheromoneBetweenDocuments = pheromone.getOrDefault(documentPair, BigDecimal.ONE.divide(BigDecimal.valueOf(documentsNumber), 8, RoundingMode.HALF_UP));
                            var probabilityToVisit = pheromoneBetweenDocuments.pow(PHEROMONE_POWER).multiply(
                                    closenessToDocument.pow(DISTANCE_POWER)
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
                            .divide(finalProbabilitiesSum, 16, RoundingMode.HALF_UP);
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
        calculatedDistances.computeIfAbsent(key, k -> documentFrom.distance(documentTo));
        return calculatedDistances.get(key);
    }

    private List<Ant> placeAnts(int documentsNumber) {
        return Stream.generate(() -> {
            var ant = new Ant(documentsNumber);
            Logger.debug("Ant placed: " + ant);
            return ant;
        }).limit(ANTS_NUMBER).toList();
    }

    private List<Document> initDocumentsModel() throws Exception {
        Path path = Paths.get(ClassLoader.getSystemResource("org/hse/swarmdocumentclustering/dataset/documents_10_vs_10_nonunique.csv").toURI());
        var documentId = -1;
        List<Document> documents = new ArrayList<>();
        for (var document : readAllCsvLines(path)) {
            if (documentId >= 0) {
                documents.add(
                        new Document(documentId, Arrays.stream(document).map(BigDecimal::new).toList())
                );
            }
            documentId++;
        }

        documents.addAll(List.of(
//                new Document(0, List.of(BigDecimal.valueOf(1))),
//                new Document(1, List.of(BigDecimal.valueOf(2))),
//                new Document(2, List.of(BigDecimal.valueOf(1000))),
//                new Document(3, List.of(BigDecimal.valueOf(1090)))

//                new Document(0, List.of(BigDecimal.valueOf(1))),
//                new Document(1, List.of(BigDecimal.valueOf(2))),
//                new Document(2, List.of(BigDecimal.valueOf(3))),
//                new Document(3, List.of(BigDecimal.valueOf(1000))),
//                new Document(4, List.of(BigDecimal.valueOf(1001))),
//                new Document(5, List.of(BigDecimal.valueOf(1002))),
//                new Document(6, List.of(BigDecimal.valueOf(1003))),
//                new Document(7, List.of(BigDecimal.valueOf(9999)))

//                new Document(0, List.of(BigDecimal.valueOf(5))),
//                new Document(1, List.of(BigDecimal.valueOf(6))),
//                new Document(2, List.of(BigDecimal.valueOf(7))),
//                new Document(3, List.of(BigDecimal.valueOf(8)))
        ));

        for (Document document : documents) {
            Logger.debug("Document is read: " + document);
        }
        alphabetSize = documents.get(0).getAlphabetSize();
        return documents;
    }

    public List<String[]> readAllCsvLines(Path filePath) throws Exception {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                return csvReader.readAll();
            }
        }
    }

    @FXML
    private TextField iterationsLimitField;

    @FXML
    private TextField iterationsImprovementLimitField;

    @FXML
    private TextField antsQuantityField;

    @FXML
    private TextField pheromoneEvaporationCoefField;

    @FXML
    private TextField pheromonePowerField;

    @FXML
    private TextField distancePowerField;

    @FXML
    private TextField distanceNumeratorField;

    @FXML
    private TextField pheromoneNumeratorField;

    @FXML
    void antsQuantityInserted(ActionEvent event) {
        ANTS_NUMBER = Integer.parseInt(antsQuantityField.getText());
    }

    @FXML
    void iterationsImprovementLimitFieldInserted(ActionEvent event) {
        ITERATIONS_WITHOUT_IMPROVEMENT_LIMIT = Integer.parseInt(iterationsImprovementLimitField.getText());
    }

    @FXML
    void iterationsLimitInserted(ActionEvent event) {
        ITERATIONS_LIMIT = Integer.parseInt(iterationsLimitField.getText());
    }

    @FXML
    void pheromoneEvaporationCoefInserted(ActionEvent event) {
        EVAPORATION_COEF = Integer.parseInt(pheromoneEvaporationCoefField.getText());
    }

    @FXML
    void pheromonePowerInserted(ActionEvent event) {
        PHEROMONE_POWER = Integer.parseInt(pheromonePowerField.getText());
    }

    @FXML
    void distancePowerInserted(ActionEvent event) {
        DISTANCE_POWER = Integer.parseInt(distancePowerField.getText());
    }

    @FXML
    void distanceNumeratorInserted(ActionEvent event) {
        DISTANCE_NUMERATOR = Integer.parseInt(distanceNumeratorField.getText());
    }

    @FXML
    void pheromoneNumeratorInserted(ActionEvent event) {
        PHEROMONE_DELTA_NUMERATOR = Integer.parseInt(pheromoneNumeratorField.getText());
    }
}
