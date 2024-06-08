package org.hse.aco.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import static java.math.RoundingMode.HALF_UP;

public class TableClusterResult {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty dominantClass;
    private final SimpleDoubleProperty precision;
    private final SimpleDoubleProperty recall;
    private final SimpleDoubleProperty fMeasure;
    private final SimpleDoubleProperty entropy;

    public TableClusterResult(Cluster cluster, int totalDocsOfDominantClass) {
        id = new SimpleIntegerProperty(cluster.id);
        dominantClass = new SimpleStringProperty(cluster.getDominantClass());
        precision = new SimpleDoubleProperty(cluster.getPrecision().setScale(2, HALF_UP).doubleValue());
        recall = new SimpleDoubleProperty(cluster.getRecall(totalDocsOfDominantClass).setScale(2, HALF_UP).doubleValue());
        fMeasure = new SimpleDoubleProperty(cluster.getFMeasure(totalDocsOfDominantClass).setScale(2, HALF_UP).doubleValue());
        entropy = new SimpleDoubleProperty(cluster.getEntropy().setScale(2, HALF_UP).doubleValue());
    }

    public int getId() {
        return id.get();
    }

    public String getDominantClass() {
        return dominantClass.get();
    }

    public double getPrecision() {
        return precision.get();
    }

    public double getRecall() {
        return recall.get();
    }

    public double getFMeasure() {
        return fMeasure.get();
    }

    public double getEntropy() {
        return entropy.get();
    }
}
