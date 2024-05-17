package org.hse.aco.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Cluster {
    public final int id;
    private final Map<String, List<Integer>> classToDocumentIds;
    private final List<Integer> documentIds = new ArrayList<>();
    private String dominantClass = null;

    public Cluster(int id, Map<String, List<Integer>> classToDocumentIds) {
        this.id = id;
        this.classToDocumentIds = classToDocumentIds;

        for (var docIds : classToDocumentIds.values()) {
            documentIds.addAll(docIds);
        }
    }

    public void addDocument(String documentClass, int documentId) {
        classToDocumentIds.putIfAbsent(documentClass, new ArrayList<>());
        classToDocumentIds.get(documentClass).add(documentId);
        documentIds.add(documentId);
    }

    public String getDominantClass() {
        if (dominantClass == null) {
            int maxDocsInClass = 0;
            for (var entry : classToDocumentIds.entrySet()) {
                if (entry.getValue().size() > maxDocsInClass) {
                    maxDocsInClass = entry.getValue().size();
                    dominantClass = entry.getKey();
                }
            }
        }
        return dominantClass;
    }

    public BigDecimal getPrecision() {
        return BigDecimal.valueOf(classToDocumentIds.get(getDominantClass()).size())
                .divide(BigDecimal.valueOf(getSize()), 128, RoundingMode.HALF_UP);
    }

    public BigDecimal getRecall(int totalDocsOfDominantClass) {
        return BigDecimal.valueOf(classToDocumentIds.get(getDominantClass()).size())
                .divide(BigDecimal.valueOf(totalDocsOfDominantClass), 128, RoundingMode.HALF_UP);
    }

    public BigDecimal getFMeasure(int totalDocsOfDominantClass) {
        var precision = getPrecision();
        var recall = getRecall(totalDocsOfDominantClass);
        return BigDecimal.valueOf(2).multiply(precision).multiply(recall).divide(precision.add(recall), 128, RoundingMode.HALF_UP);
    }

    public BigDecimal getEntropy() {
        return BigDecimal.ZERO;
    }

    public BigDecimal getPurity(int totalDocsOfDominantClass) {
        return BigDecimal.valueOf(totalDocsOfDominantClass).multiply(getPrecision());
    }

    public int getSize() {
        return documentIds.size();
    }

    public Map<String, List<Integer>> getClassToDocumentIds() {
        return classToDocumentIds;
    }

    public String toString(int totalDocsOfDominantClass, List<Document> dataset) {
        List<String> documentNames = new ArrayList<>();
        for (var documentId : documentIds) {
            documentNames.add(dataset.get(documentId).name);
        }

        StringBuilder string = new StringBuilder("Cluster " + id + " contains " + getSize() + " documents: " + documentNames + "\n");
        string.append("Dominant class is ").append(getDominantClass())
                .append(", Precision = ").append(getPrecision().setScale(2, RoundingMode.HALF_UP))
                .append(", Recall = ").append(getRecall(totalDocsOfDominantClass).setScale(2, RoundingMode.HALF_UP))
                .append(", F-measure = ").append(getFMeasure(totalDocsOfDominantClass).setScale(2, RoundingMode.HALF_UP))
//                .append(", Entropy = ").append(getEntropy().setScale(2, RoundingMode.HALF_UP))
                .append(", Purity = ").append(getPurity(totalDocsOfDominantClass).setScale(2, RoundingMode.HALF_UP)).append("%");
        return string.toString();
    }
}
