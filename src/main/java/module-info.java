module com.astier.bts.client_tcp_prof {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.astier.bts.client_tcp_prof to javafx.fxml;
    exports com.astier.bts.client_tcp_prof;
}