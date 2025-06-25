module com.dedeclothingstore {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires org.kordamp.ikonli.fontawesome5;
    requires eu.hansolo.tilesfx;
    requires java.sql;


    opens com.dedeclothingstore to javafx.fxml;
    opens com.dedeclothingstore.controller to javafx.fxml;
    opens com.dedeclothingstore.model to javafx.fxml;
    exports com.dedeclothingstore;
    exports com.dedeclothingstore.controller;
    exports com.dedeclothingstore.model;
}