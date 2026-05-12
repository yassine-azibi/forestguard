module gestion.alertes {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.net.http;
    requires jdk.jsobject;
    requires java.desktop;
    requires itextpdf;

    opens controller to javafx.fxml;
    opens tests     to javafx.fxml;
    opens model     to javafx.base;

    exports tests;
    exports controller;
    exports model;
    exports dao;
    exports utils;
    exports service;
}

