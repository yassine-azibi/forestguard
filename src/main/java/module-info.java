module gestion.des.interventions {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires javafx.web;
    requires org.json;
    requires java.desktop;
    requires com.github.librepdf.openpdf;

    opens controller to javafx.fxml;
    opens tests to javafx.fxml;
    opens model to javafx.base;

    exports tests;
    exports controller;
    exports model;
    exports dao;
    exports utils;
}