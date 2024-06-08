package org.hse.aco.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.hse.aco.algorithm.fuzzy.FuzzyAlgorithm;
import org.hse.aco.algorithm.graph.GraphAlgorithm;
import org.hse.aco.model.*;
import org.tinylog.Logger;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS;
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
    public static File DATASET_FILE = null;
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
    private Label datasetLabel;
    @FXML
    private TableView<TableClusterResult> clusteringResultTable;
    @FXML
    private Button runClusteringButton;
    @FXML
    private Button datasetChoserButton;

    @FXML
    private void initialize() {
        distanceFunctionBox.setItems(FXCollections.observableArrayList(COSINE, EUCLIDEAN));
        distanceFunctionBox.setValue(COSINE);

        algorithmBox.setItems(FXCollections.observableArrayList(FUZZY, GRAPH));
        algorithmBox.setValue(FUZZY);

        disableDistanceNumeratorIfNeeded();
        disableClustersAmountIfNeeded();
        disablePheromoneNumeratorIfNeeded();

        datasetLabel.setText("Dataset file: \"svd_TFIDF2by50_1.csv\"");

        TableColumn<TableClusterResult, Integer> idColumn = new TableColumn<>("Cluster id");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        clusteringResultTable.getColumns().add(idColumn);

        TableColumn<TableClusterResult, String> dominantColumn = new TableColumn<>("Dominant class");
        dominantColumn.setCellValueFactory(new PropertyValueFactory<>("dominantClass"));
        clusteringResultTable.getColumns().add(dominantColumn);

        TableColumn<TableClusterResult, Double> precisionColumn = new TableColumn<>("Precision");
        precisionColumn.setCellValueFactory(new PropertyValueFactory<>("precision"));
        clusteringResultTable.getColumns().add(precisionColumn);

        TableColumn<TableClusterResult, Double> recallColumn = new TableColumn<>("Recall");
        recallColumn.setCellValueFactory(new PropertyValueFactory<>("recall"));
        clusteringResultTable.getColumns().add(recallColumn);

        TableColumn<TableClusterResult, Double> fMeasureColumn = new TableColumn<>("F-measure");
        fMeasureColumn.setCellValueFactory(new PropertyValueFactory<>("fMeasure"));
        clusteringResultTable.getColumns().add(fMeasureColumn);

        TableColumn<TableClusterResult, String> purityColumn = new TableColumn<>("Entropy");
        purityColumn.setCellValueFactory(new PropertyValueFactory<>("entropy"));
        clusteringResultTable.getColumns().add(purityColumn);

        clusteringResultTable.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    @FXML
    protected void onRunClusteringClick() {
        disableAllInputs(true);
        CompletableFuture<ClusteringResult> clusteringResultFuture;
        if (ALGORITHM_TYPE == FUZZY) {
            clusteringResultText.textProperty().bind(FuzzyAlgorithm.iterationResultObservable);
            clusteringResultFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return fuzzyAlgorithm.clusterDocuments();
                } catch (Exception e) {
                    Logger.warn(e);
                    return null;
                }
            }, executor);
        } else {
            clusteringResultText.textProperty().bind(GraphAlgorithm.iterationResultObservable);
            clusteringResultFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return graphAlgorithm.clusterDocuments();
                } catch (Exception e) {
                    Logger.warn(e);
                    return null;
                }
            }, executor);
        }

        clusteringResultFuture.thenAccept(this::displayClusteringTable);
    }

    void displayClusteringTable(ClusteringResult clusteringResult) {
        clusteringResultTable.setVisible(true);

        ObservableList<TableClusterResult> tableClusterResults = FXCollections.observableList(
                clusteringResult.getClusters().stream().map(
                        (Cluster cluster) -> new TableClusterResult(cluster, clusteringResult.getDocCntOfDominantClass(cluster))
                ).toList()
        );
        clusteringResultTable.setItems(tableClusterResults);
        disableAllInputs(false);
    }

    private void disableAllInputs(boolean disable) {
        runClusteringButton.setDisable(disable);
        iterationsLimitField.setDisable(disable);
        iterationsImprovementLimitField.setDisable(disable);
        antsQuantityField.setDisable(disable);
        pheromoneEvaporationCoefField.setDisable(disable);
        pheromonePowerField.setDisable(disable);
        distancePowerField.setDisable(disable);
        pheromoneInitField.setDisable(disable);
        distanceFunctionBox.setDisable(disable);
        algorithmBox.setDisable(disable);
        datasetChoserButton.setDisable(disable);

        if (disable) {
            distanceNumeratorField.setDisable(disable);
            clustersQuantityField.setDisable(disable);
            pheromoneNumeratorField.setDisable(disable);
        } else {
            disableDistanceNumeratorIfNeeded();
            disableClustersAmountIfNeeded();
            disablePheromoneNumeratorIfNeeded();
        }
    }

    @FXML
    void antsQuantityInserted(KeyEvent event) {
        runCatching(antsQuantityField, () -> {
            var value = Integer.parseInt(antsQuantityField.getText());
            validate(value > 0 && value <= 10000);
            ANTS_NUMBER = value;
        });
    }

    @FXML
    void iterationsImprovementLimitFieldInserted(KeyEvent event) {
        runCatching(iterationsImprovementLimitField, () -> {
            var value = Integer.parseInt(iterationsImprovementLimitField.getText());
            validate(value > 0 && value <= 100000);
            ITERATIONS_WITHOUT_IMPROVEMENT_LIMIT = value;
        });
    }

    @FXML
    void iterationsLimitInserted(KeyEvent event) {
        runCatching(iterationsLimitField, () -> {
            var value = Integer.parseInt(iterationsLimitField.getText());
            validate(value > 0 && value <= 100000);
            ITERATIONS_LIMIT = value;
        });
    }

    @FXML
    void pheromoneEvaporationCoefInserted(KeyEvent event) {
        runCatching(pheromoneEvaporationCoefField, () -> {
            var value = Double.parseDouble(pheromoneEvaporationCoefField.getText());
            validate(value >= 0 && value <= 1);
            EVAPORATION_COEF = value;
        });
    }

    @FXML
    void pheromonePowerInserted(KeyEvent event) {
        runCatching(pheromonePowerField, () -> {
            var value = Integer.parseInt(pheromonePowerField.getText());
            validate(value >= 0 & value <= 1000);
            PHEROMONE_POWER = value;
        });
    }

    @FXML
    void distancePowerInserted(KeyEvent event) {
        runCatching(distancePowerField, () -> {
            var value = Integer.parseInt(distancePowerField.getText());
            validate(value >= 0 & value <= 1000);
            DISTANCE_POWER = value;
        });
    }

    @FXML
    void distanceNumeratorInserted(KeyEvent event) {
        runCatching(distanceNumeratorField, () -> {
            var value = Integer.parseInt(distanceNumeratorField.getText());
            validate(value > 0 & value <= 100000);
            DISTANCE_NUMERATOR = value;
        });
    }

    @FXML
    void pheromoneNumeratorInserted(KeyEvent event) {
        runCatching(pheromoneNumeratorField, () -> {
            var value = Integer.parseInt(pheromoneNumeratorField.getText());
            validate(value > 0 & value <= 100000);
            PHEROMONE_DELTA_NUMERATOR = value;
        });
    }

    @FXML
    void pheromoneInitInserted(KeyEvent event) {
        runCatching(pheromoneInitField, () -> {
            var value = Double.parseDouble(pheromoneInitField.getText());
            validate(value > 0 & value <= 100000);
            PHEROMONE_INIT = value;
        });
    }

    @FXML
    void clustersQuantityInserted(KeyEvent event) {
        runCatching(clustersQuantityField, () -> {
            var value = Integer.parseInt(clustersQuantityField.getText());
            validate(value > 0 & value <= 100);
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

    @FXML
    void onChooseDatasetClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose dataset file");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("CSV files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            DATASET_FILE = selectedFile;
            datasetLabel.setText("Dataset file: \"" + selectedFile.getName() + "\"");
        }
    }

    private void disableDistanceNumeratorIfNeeded() {
        distanceNumeratorField.setDisable(DISTANCE_FUNCTION == COSINE);
        if (DISTANCE_FUNCTION == COSINE) {
            distanceNumeratorField.setText("");
            distanceNumeratorField.setPromptText("Only for EUCLIDEAN distance function!");
            distanceNumeratorField.setStyle("-fx-background-color: #404040; -fx-background-radius: 10;");
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
            clustersQuantityField.setStyle("-fx-background-color: #404040; -fx-background-radius: 10;");
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
            pheromoneNumeratorField.setStyle("-fx-background-color: #404040; -fx-background-radius: 10;");
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
