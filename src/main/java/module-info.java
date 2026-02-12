module com.tlcsdm.pasori {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires com.dlsc.preferencesfx;
    requires atlantafx.base;
    requires java.prefs;
    requires com.google.gson;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires reactfx;

    opens com.tlcsdm.pasori to javafx.fxml;
    opens com.tlcsdm.pasori.controller to javafx.fxml;
    opens com.tlcsdm.pasori.config to com.dlsc.preferencesfx;
    opens com.tlcsdm.pasori.model to com.google.gson;
    
    exports com.tlcsdm.pasori;
    exports com.tlcsdm.pasori.controller;
    exports com.tlcsdm.pasori.model;
    exports com.tlcsdm.pasori.service;
    exports com.tlcsdm.pasori.config;
    exports com.tlcsdm.pasori.sdk.felica;
}
