package com.astier.bts.client_tcp_prof;

import com.astier.bts.client_tcp_prof.tcp.TCP;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.shape.Circle;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import static javafx.scene.paint.Color.*;

public class HelloController implements Initializable {
    public Button button;
    public Button connecter;
    public Button deconnecter;
    public TextField TextFieldIP;
    public TextField TextFieldPort;
    public TextField TextFieldRequette;
    public Circle voyant;
    public TextArea TextAreaReponses;
    static public TCP tcp;
    static boolean enRun = false;
    String adresse,port;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        voyant.setFill(RED);
        //todo
    }


    private void envoyer() throws InterruptedException {
       //todo
    }

    private void deconnecter() throws InterruptedException {
        //todo
    }

    private void connecter() throws UnknownHostException {
        //todo
    }

}