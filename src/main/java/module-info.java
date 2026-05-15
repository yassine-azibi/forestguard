module gestion.des.interventions {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires javafx.web;
    requires org.json;
    requires java.desktop;
    requires com.github.librepdf.openpdf;
    requires java.net.http;
    requires jdk.jsobject;
    requires itextpdf;
    requires jdk.httpserver;

    // ── Module gestion-utilisateur ──
    requires jbcrypt;
    requires jakarta.mail;
    requires jakarta.activation;
    requires twilio;

    // ── Module gestion-des-donnees ──
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.pdfbox;
    requires javafx.swing;

    // ── Module gestion-des-capteurs ──
    requires com.fazecast.jSerialComm;

    // Package app (point d'entrée principal)
    opens app to javafx.graphics, javafx.fxml;
    exports app;

    opens controller to javafx.fxml;
    opens tests to javafx.fxml;
    opens model to javafx.base;

    // Packages du module gestion-utilisateur
    opens com.forestguard.controllers to javafx.fxml;
    opens com.forestguard.entities to javafx.base;

    // Packages du module gestion-capteurs
    opens edu.capteur.controllers to javafx.fxml;
    opens edu.capteur.entities to javafx.base;

    // Packages du module gestion-données
    opens ForestGuard.controllers to javafx.fxml;
    opens ForestGuard.entities to javafx.base;

    // Packages du module pompier (Login pompier)
    opens edu.pompier.controllers to javafx.fxml;
    opens edu.pompier.entities to javafx.base;

    // Packages du module gestionforet
    // Note: Les fichiers de gestionforet sont dans le package 'controller' (sans s) déjà ouvert ligne 29

    exports tests;
    exports controller;
    exports model;
    exports dao;
    exports utils;
    exports service;
    exports com.forestguard.controllers;
    exports com.forestguard.entities;
    exports com.forestguard.services;
    exports com.forestguard.utils;
    exports com.forestguard.interfaces;
    exports edu.pompier.controllers;
    exports edu.pompier.entities;
}
