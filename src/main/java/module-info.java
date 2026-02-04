module com.tlcsdm.pasori {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;

    opens com.tlcsdm.pasori to javafx.fxml;
    opens com.tlcsdm.pasori.controller to javafx.fxml;
    
    exports com.tlcsdm.pasori;
    exports com.tlcsdm.pasori.controller;
    exports com.tlcsdm.pasori.model;
    exports com.tlcsdm.pasori.service;
}
