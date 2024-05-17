package org.hse.aco.algorithm.fuzzy.dto;

import javafx.util.Pair;
import org.hse.aco.algorithm.fuzzy.dto.IterationSolutions.Solution;
import org.hse.aco.component.ProbabilityCollection;
import org.hse.aco.model.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.concurrent.ThreadLocalRandom.current;
import static org.hse.aco.controller.StartAlgorithmController.*;
import static org.hse.aco.model.DistanceFunction.EUCLIDEAN;

public class Ant {
    private final Solution solution;

    public Ant(int documentsNumber) {
        solution = new Solution(null, IntStream.generate(() -> -1).limit(documentsNumber).toArray());
    }

    public Solution getSolution() {
        return solution;
    }

    public void buildSolution(
            List<Document> dataset,
            Map<Pair<Integer, Integer>, BigDecimal> calculatedDistances,
            Map<Pair<Integer, Integer>, BigDecimal> pheromone
    ) {
        while (true) {
            Integer documentToAssign = getDocumentToAssign();
            if (documentToAssign == null) {
                return;
            }

            var allProbabilitiesToAssignCluster = IntStream.range(0, CLUSTERS_AMOUNT).mapToObj(clusterId -> {
                var documentToClusterPair = new Pair<>(documentToAssign, clusterId);
                var closenessToClusterDocuments = getClosenessToClusterDocuments(dataset, calculatedDistances, clusterId, documentToAssign);
                var documentToClusterPheromone = pheromone.get(documentToClusterPair);
                var probabilityToAssign = documentToClusterPheromone.pow(PHEROMONE_POWER).multiply(
                        closenessToClusterDocuments.pow(DISTANCE_POWER)
                );
                return new Pair<>(clusterId, probabilityToAssign);
            }).toList();

            BigDecimal probabilitiesSum = getProbabilitiesSum(allProbabilitiesToAssignCluster);

            ProbabilityCollection<Integer> clusterAssignmentProbabilityDistribution = new ProbabilityCollection<>();
            allProbabilitiesToAssignCluster.forEach(clusterIdToProbability -> {
                var probability = clusterIdToProbability.getValue().multiply(BigDecimal.valueOf(100))
                        .divide(probabilitiesSum, 128, RoundingMode.HALF_UP);

                clusterAssignmentProbabilityDistribution.add(clusterIdToProbability.getKey(), Math.max(probability.longValue(), 1));
            });

            solution.assignedClusters[documentToAssign] = clusterAssignmentProbabilityDistribution.get();
        }
    }

    public void evaluateSolution(List<Document> dataset, int alphabetSize) {
        var clustersCentroidAndSize = getClustersCentroidAndSize(dataset, alphabetSize);
        var centroidsPerCluster = clustersCentroidAndSize.getKey();
        var documentNumberPerCluster = clustersCentroidAndSize.getValue();

        var avrDocumentDistToCentroid = BigDecimal.ZERO;
        for (int clusterId = 0; clusterId < CLUSTERS_AMOUNT; clusterId++) {
            if (documentNumberPerCluster.get(clusterId) == null) {
                continue;
            }

            var accumulator = BigDecimal.ZERO;
            for (int documentId = 0; documentId < dataset.size(); documentId++) {
                if (solution.assignedClusters[documentId] == clusterId) {
                    if (DISTANCE_FUNCTION == EUCLIDEAN) {
                        accumulator = accumulator.add(dataset.get(documentId).distance(centroidsPerCluster.get(clusterId)));
                    } else {
                        accumulator = accumulator.add(dataset.get(documentId).cosine(centroidsPerCluster.get(clusterId)));
                    }
                }
            }
            accumulator = accumulator.divide(BigDecimal.valueOf(documentNumberPerCluster.get(clusterId)), 128, RoundingMode.HALF_UP);
            avrDocumentDistToCentroid = avrDocumentDistToCentroid.add(accumulator);
        }
        solution.fitness = avrDocumentDistToCentroid.divide(BigDecimal.valueOf(CLUSTERS_AMOUNT), 128, RoundingMode.HALF_UP);
    }

    private Pair<HashMap<Integer, Document>, HashMap<Integer, Integer>> getClustersCentroidAndSize(
            List<Document> dataset,
            int alphabetSize
    ) {
        var centroidsPerCluster = new HashMap<Integer, Document>();
        var documentNumberPerCluster = new HashMap<Integer, Integer>();
        for (int documentId = 0; documentId < dataset.size(); documentId++) {
            var clusterId = solution.assignedClusters[documentId];
            documentNumberPerCluster.put(clusterId, documentNumberPerCluster.getOrDefault(clusterId, 0) + 1);
            centroidsPerCluster.putIfAbsent(clusterId, new Document(-1, null, null, Collections.nCopies(alphabetSize, BigDecimal.ZERO)));
            centroidsPerCluster.get(clusterId).addWordAppearances(dataset.get(documentId));
        }
        centroidsPerCluster.forEach((clusterId, document) -> document.divWordAppearances(documentNumberPerCluster.get(clusterId)));
        return new Pair<>(centroidsPerCluster, documentNumberPerCluster);
    }

    private BigDecimal getProbabilitiesSum(List<Pair<Integer, BigDecimal>> allProbabilitiesToAssignCluster) {
        BigDecimal probabilitiesSum = BigDecimal.ZERO;
        for (Pair<Integer, BigDecimal> clusterIdToProbability : allProbabilitiesToAssignCluster) {
            probabilitiesSum = probabilitiesSum.add(clusterIdToProbability.getValue());
        }
        return probabilitiesSum;
    }

    /**
     * Returns mean similarity of document to assign with the documents that are already assigned to cluster
     */
    private BigDecimal getClosenessToClusterDocuments(
            List<Document> dataset,
            Map<Pair<Integer, Integer>, BigDecimal> calculatedDistances,
            int clusterId,
            Integer documentToAssign
    ) {
        BigDecimal closenessToClusterDocuments = BigDecimal.ZERO;
        int clusterDocumentsCnt = 0;
        for (int i = 0; i < solution.assignedClusters.length; i++) {
            if (solution.assignedClusters[i] == clusterId) {
                clusterDocumentsCnt++;
                var clusterDocument = dataset.get(i);
                var documentCandidate = dataset.get(documentToAssign);
                var distance = BigDecimal.ZERO;
                if (DISTANCE_FUNCTION == EUCLIDEAN) {
                    distance = clusterDocument.distance(documentCandidate);
                } else {
                    distance = clusterDocument.cosine(documentCandidate);
                }
                BigDecimal finalDistance = distance;
                calculatedDistances.computeIfAbsent(new Pair<>(documentToAssign, i), k -> finalDistance);
                calculatedDistances.computeIfAbsent(new Pair<>(i, documentToAssign), k -> finalDistance);
                closenessToClusterDocuments = closenessToClusterDocuments.add(calculatedDistances.get(new Pair<>(documentToAssign, i)));
            }
        }
        if (clusterDocumentsCnt == 0) {
            closenessToClusterDocuments = BigDecimal.valueOf(1000000000000L);
        } else {
            closenessToClusterDocuments = closenessToClusterDocuments.divide(new BigDecimal(clusterDocumentsCnt), 128, RoundingMode.HALF_UP);
            if (DISTANCE_FUNCTION == EUCLIDEAN) {
                closenessToClusterDocuments = BigDecimal.valueOf(DISTANCE_NUMERATOR).divide(closenessToClusterDocuments, 128, RoundingMode.HALF_UP);
            }
        }
        return closenessToClusterDocuments;
    }

    /**
     * Random selection of document with unassigned cluster
     */
    private Integer getDocumentToAssign() {
        List<Integer> unassignedDocumentIds = new ArrayList<>();
        for (int i = 0; i < solution.assignedClusters.length; i++) {
            if (solution.assignedClusters[i] == -1) {
                unassignedDocumentIds.add(i);
            }
        }

        if (unassignedDocumentIds.isEmpty()) {
            return null;
        }

        return unassignedDocumentIds.get(current().nextInt(unassignedDocumentIds.size()));
    }
}
