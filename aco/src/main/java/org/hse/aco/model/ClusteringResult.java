package org.hse.aco.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ClusteringResult {
    private final List<Cluster> clusters;
    private int totalDocuments = 0;

    public ClusteringResult(List<Cluster> clusters) {
        this.clusters = clusters;
        for (var cluster : clusters) {
            totalDocuments += cluster.getSize();
        }
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void addCluster(Cluster cluster) {
        clusters.add(cluster);
        totalDocuments += cluster.getSize();
    }

    public BigDecimal getAccuracy() {
        long clusteredCorrectly = 0;
        for (var cluster : clusters) {
            clusteredCorrectly += cluster.getClassToDocumentIds().get(cluster.getDominantClass()).size();
        }
        return BigDecimal.valueOf(clusteredCorrectly * 100).divide(BigDecimal.valueOf(totalDocuments), 128, RoundingMode.HALF_UP);
    }

    public BigDecimal getFMeasure() {
        var fMeasure = BigDecimal.ZERO;
        for (var cluster : clusters) {
            fMeasure = fMeasure.add(cluster.getFMeasure(getDocCntOfDominantClass(cluster)));
        }
        return fMeasure.divide(BigDecimal.valueOf(clusters.size()), 128, RoundingMode.HALF_UP);
    }

    public BigDecimal getEntropy() {
        var entropy = BigDecimal.ZERO;
        for (var cluster : clusters) {
            entropy = entropy.add(cluster.getEntropy().multiply(BigDecimal.valueOf(cluster.getSize())));
        }
        return entropy.divide(BigDecimal.valueOf(totalDocuments), 128, RoundingMode.HALF_UP);
    }

    public int getDocCntOfDominantClass(Cluster cluster) {
        int cnt = 0;
        for (var clusterI : clusters) {
            cnt += clusterI.getClassToDocumentIds().getOrDefault(cluster.getDominantClass(), List.of()).size();
        }
        return cnt;
    }

    public String toString(List<Document> dataset) {
        StringBuilder string = new StringBuilder("Clustering result:\n");
        for (var cluster : clusters) {
            string.append(cluster.toString(getDocCntOfDominantClass(cluster), dataset)).append("\n\n");
        }
        string.append("Accuracy = ").append(getAccuracy().setScale(2, RoundingMode.HALF_UP)).append("%")
                .append(", F-measure = ").append(getFMeasure().setScale(2, RoundingMode.HALF_UP))
                .append(", Entropy = ").append(getEntropy().setScale(4, RoundingMode.HALF_UP));
        return string.toString();
    }
}
