module org.hse.aco {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.tinylog.api;
    requires com.opencsv;
    requires java.desktop;
    requires com.google.common;

    opens org.hse.aco to javafx.fxml;
    exports org.hse.aco;
    exports org.hse.aco.controller;
    exports org.hse.aco.model;
    opens org.hse.aco.controller to javafx.fxml;
}