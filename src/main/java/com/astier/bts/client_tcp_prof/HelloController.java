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
            } catch (InterruptedException e) {
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

    private void deconnecter() throws InterruptedException {
        //todo
    }

    private void connecter() throws UnknownHostException {
   
    adresse = TextFieldIP.getText();
    port = TextFieldPort.getText();
    
   
    if (adresse.isEmpty() || port.isEmpty()) {
        TextAreaReponses.appendText("Erreur: Veuillez remplir l'IP et le port!\n");
        return;
    }
    
    
    try {
        int portNum = Integer.parseInt(port);
        
        
        tcp = new TCP(InetAddress.getByName(adresse), portNum, this);
        
        
        tcp.start();
        
       
        enRun = true;
        
       
        voyant.setFill(GREEN);
        
       
        TextAreaReponses.appendText("Connexion établie avec " + adresse + ":" + port + "\n");
        
        
        connecter.setDisable(true);
        deconnecter.setDisable(false);
        button.setDisable(false);
        
    } catch (NumberFormatException e) {
        TextAreaReponses.appendText("Erreur: Le port doit être un nombre!\n");
    } catch (UnknownHostException e) {
        TextAreaReponses.appendText(" Erreur: Adresse IP invalide!\n");
    } catch (Exception e) {
        TextAreaReponses.appendText("Erreur de connexion: " + e.getMessage() + "\n");
    }
}

}
