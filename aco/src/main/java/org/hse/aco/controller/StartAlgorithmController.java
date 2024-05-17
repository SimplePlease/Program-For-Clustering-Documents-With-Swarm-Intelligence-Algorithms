package org.hse.aco.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.hse.aco.algorithm.fuzzy.FuzzyAlgorithm;
import org.hse.aco.algorithm.graph.GraphAlgorithm;
import org.hse.aco.model.AlgorithmType;
import org.hse.aco.model.ClusteringResult;
import org.hse.aco.model.DistanceFunction;
import org.tinylog.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hse.aco.model.AlgorithmType.FUZZY;
import static org.hse.aco.model.AlgorithmType.GRAPH;
import static org.hse.aco.model.DistanceFunction.COSINE;
import static org.hse.aco.model.DistanceFunction.EUCLIDEAN;

public class StartAlgorithmController {
    public static int ITERATIONS_LIMIT = 100;
    public static int ITERATIONS_WITHOUT_IMPROVEMENT_LIMIT = 25;
    public static int CLUSTERS_AMOUNT = 2;
    public static int ANTS_NUMBER = 30;
    public static int PHEROMONE_POWER = 1;
    public static int DISTANCE_POWER = 2;
    public static int DISTANCE_NUMERATOR = 10;
    public static double EVAPORATION_COEF = 0.9;
    public static int PHEROMONE_DELTA_NUMERATOR = 40;
    public static double PHEROMONE_INIT = 0.20000000;
    public static DistanceFunction DISTANCE_FUNCTION = COSINE;
    private static AlgorithmType ALGORITHM_TYPE = FUZZY;

    private final FuzzyAlgorithm fuzzyAlgorithm = new FuzzyAlgorithm();
    private final GraphAlgorithm graphAlgorithm = new GraphAlgorithm();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    public Label clusteringResultText;
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
    private TextField pheromoneInitField;
    @FXML
    private TextField clustersQuantityField;
    @FXML
    private ComboBox<DistanceFunction> distanceFunctionBox;
    @FXML
    private ComboBox<AlgorithmType> algorithmBox;

    @FXML
    private void initialize() {
        distanceFunctionBox.setItems(FXCollections.observableArrayList(COSINE, EUCLIDEAN));
        distanceFunctionBox.setValue(COSINE);

        algorithmBox.setItems(FXCollections.observableArrayList(FUZZY, GRAPH));
        algorithmBox.setValue(FUZZY);

        disableDistanceNumeratorIfNeeded();
        disableClustersAmountIfNeeded();
        disablePheromoneNumeratorIfNeeded();
    }

    @FXML
    protected void onRunClusteringClick() {
        ClusteringResult result = null;
        if (ALGORITHM_TYPE == FUZZY) {
            executor.submit(fuzzyAlgorithm::clusterDocuments);
        } else {
            executor.submit(graphAlgorithm::clusterDocuments);
        }
    }

    @FXML
    void antsQuantityInserted(ActionEvent event) {
        runCatching(antsQuantityField, () -> {
            var value = Integer.parseInt(antsQuantityField.getText());
            validate(value > 0);
            ANTS_NUMBER = value;
        });
    }

    @FXML
    void iterationsImprovementLimitFieldInserted(ActionEvent event) {
        runCatching(iterationsImprovementLimitField, () -> {
            var value = Integer.parseInt(iterationsImprovementLimitField.getText());
            validate(value > 0);
            ITERATIONS_WITHOUT_IMPROVEMENT_LIMIT = value;
        });
    }

    @FXML
    void iterationsLimitInserted(ActionEvent event) {
        runCatching(iterationsLimitField, () -> {
            var value = Integer.parseInt(iterationsLimitField.getText());
            validate(value > 0);
            ITERATIONS_LIMIT = value;
        });
    }

    @FXML
    void pheromoneEvaporationCoefInserted(ActionEvent event) {
        runCatching(pheromoneEvaporationCoefField, () -> {
            var value = Double.parseDouble(pheromoneEvaporationCoefField.getText());
            validate(value >= 0 && value <= 1);
            EVAPORATION_COEF = value;
        });
    }

    @FXML
    void pheromonePowerInserted(ActionEvent event) {
        runCatching(pheromonePowerField, () -> {
            var value = Integer.parseInt(pheromonePowerField.getText());
            validate(value >= 0);
            PHEROMONE_POWER = value;
        });
    }

    @FXML
    void distancePowerInserted(ActionEvent event) {
        runCatching(distancePowerField, () -> {
            var value = Integer.parseInt(distancePowerField.getText());
            validate(value >= 0);
            DISTANCE_POWER = value;
        });
    }

    @FXML
    void distanceNumeratorInserted(ActionEvent event) {
        runCatching(distanceNumeratorField, () -> {
            var value = Integer.parseInt(distanceNumeratorField.getText());
            validate(value > 0);
            DISTANCE_NUMERATOR = value;
        });
    }

    @FXML
    void pheromoneNumeratorInserted(ActionEvent event) {
        runCatching(pheromoneNumeratorField, () -> {
            var value = Integer.parseInt(pheromoneNumeratorField.getText());
            validate(value > 0);
            PHEROMONE_DELTA_NUMERATOR = value;
        });
    }

    @FXML
    void pheromoneInitInserted(ActionEvent event) {
        runCatching(pheromoneInitField, () -> {
            var value = Double.parseDouble(pheromoneInitField.getText());
            validate(value > 0);
            PHEROMONE_INIT = value;
        });
    }

    @FXML
    void clustersQuantityInserted(ActionEvent event) {
        runCatching(clustersQuantityField, () -> {
            var value = Integer.parseInt(clustersQuantityField.getText());
            validate(value > 0);
            CLUSTERS_AMOUNT = value;
        });
    }

    @FXML
    void distanceFunctionSelected(ActionEvent event) {
        DISTANCE_FUNCTION = distanceFunctionBox.getValue();
        disableDistanceNumeratorIfNeeded();
    }

    @FXML
    void algorithmSelected(ActionEvent event) {
        ALGORITHM_TYPE = algorithmBox.getValue();
        disableClustersAmountIfNeeded();
        disablePheromoneNumeratorIfNeeded();
    }

    private void disableDistanceNumeratorIfNeeded() {
        distanceNumeratorField.setDisable(DISTANCE_FUNCTION == COSINE);
        if (DISTANCE_FUNCTION == COSINE) {
            distanceNumeratorField.setText("");
            distanceNumeratorField.setPromptText("Only for EUCLIDEAN distance function!");
            distanceNumeratorField.setStyle("-fx-background-color: #404040; -fx-font-weight: bold; -fx-background-radius: 10;");
        } else {
            DISTANCE_NUMERATOR = 10;
            distanceNumeratorField.setPromptText("10");
            distanceNumeratorField.setStyle("-fx-background-radius: 10;");
        }
    }

    private void disableClustersAmountIfNeeded() {
        clustersQuantityField.setDisable(ALGORITHM_TYPE == GRAPH);
        if (ALGORITHM_TYPE == GRAPH) {
            clustersQuantityField.setText("");
            clustersQuantityField.setPromptText("Only for FUZZY algorithm type!");
            clustersQuantityField.setStyle("-fx-background-color: #404040; -fx-font-weight: bold; -fx-background-radius: 10;");
        } else {
            CLUSTERS_AMOUNT = 2;
            clustersQuantityField.setPromptText("2");
            clustersQuantityField.setStyle("-fx-background-radius: 10;");
        }
    }

    private void disablePheromoneNumeratorIfNeeded() {
        pheromoneNumeratorField.setDisable(ALGORITHM_TYPE == FUZZY);
        if (ALGORITHM_TYPE == FUZZY) {
            pheromoneNumeratorField.setText("");
            pheromoneNumeratorField.setPromptText("Only for GRAPH algorithm type!");
            pheromoneNumeratorField.setStyle("-fx-background-color: #404040; -fx-font-weight: bold; -fx-background-radius: 10;");
        } else {
            PHEROMONE_DELTA_NUMERATOR = 40;
            pheromoneNumeratorField.setPromptText("40");
            pheromoneNumeratorField.setStyle("-fx-background-radius: 10;");
        }
    }

    private void runCatching(TextField textField, Runnable runnable) {
        try {
            runnable.run();
            textField.setStyle("-fx-background-radius: 10;");
        } catch (Exception e) {
            Logger.warn(e);
            textField.setStyle("-fx-border-color: RED; -fx-border-radius: 10; -fx-background-radius: 10;");
        }
    }

    private void validate(boolean condition) {
        if (!condition)
            throw new RuntimeException();
    }
}
