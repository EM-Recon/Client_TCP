package com.astier.bts.client_tcp_prof;

import com.astier.bts.client_tcp_prof.tcp.TCP;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.InetAddress;
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
        button.setOnAction(event -> {
            try {
                envoyer();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        deconnecter.setOnAction(event -> {
            try {
                deconnecter();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        connecter.setOnAction(event -> {
            try {
                connecter();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        });
    }


    private void envoyer() throws InterruptedException {
       String requette = TextFieldRequette.getText();
       TextAreaReponses.clear();
       if (requette.isEmpty()){
           TextAreaReponses.appendText("Requete vide");
           return;
       }
       if (requette.equalsIgnoreCase("exit")){
           deconnecter.fire();
       }
       if (!enRun || tcp == null){
           TextAreaReponses.appendText("Non connecté");
           return;
       }
       try {
           tcp.requette(requette);
           TextAreaReponses.clear();
       } catch (Exception e) {
           TextAreaReponses.appendText("Erreur");
       }
    }

    private void deconnecter() throws IOException {
        try {
            if (tcp != null && enRun){
                tcp.deconnection();
                enRun = false;
                voyant.setFill(RED);
                TextAreaReponses.appendText("Deco");

            }
        }catch (Exception e){
            TextAreaReponses.appendText("Ta gueule ta une erreur");
        }
    }

    private void connecter() throws UnknownHostException {
        adresse = TextFieldIP.getText();
        port = TextFieldPort.getText();

        if (port.isEmpty() || adresse.isEmpty()){
            TextAreaReponses.appendText("Erreur Connection");
            return;
        }
        int portInt = Integer.parseInt(port);
        InetAddress addr = InetAddress.getByName(adresse);
        tcp = new TCP(addr,portInt,this);
        tcp.connection();
        tcp.start();
        enRun=true;
        voyant.setFill(GREEN);

    }

}
