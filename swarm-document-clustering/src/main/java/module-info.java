module org.hse.swarmdocumentclustering {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.tinylog.api;
    requires com.opencsv;

    opens org.hse.swarmdocumentclustering to javafx.fxml;
    exports org.hse.swarmdocumentclustering;
    exports org.hse.swarmdocumentclustering.controller;
    opens org.hse.swarmdocumentclustering.controller to javafx.fxml;
    exports org.hse.swarmdocumentclustering.component;
    opens org.hse.swarmdocumentclustering.component to javafx.fxml;
}