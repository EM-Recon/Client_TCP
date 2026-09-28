package com.astier.bts.client_tcp_prof;

import com.astier.bts.client_tcp_prof.aes.Aes_cbc;
import com.astier.bts.client_tcp_prof.aes.Outils;
import com.astier.bts.client_tcp_prof.aes.Record;
import com.astier.bts.client_tcp_prof.tcp.TCPBin;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
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

    private TCPBin tcp;
    private boolean enRun = false;
    private Aes_cbc aes;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        voyant.setFill(RED);
        chargerConfig();

        button.setOnAction(e -> envoyer());
        deconnecter.setOnAction(e -> deconnecter());
        connecter.setOnAction(e -> connecter());
    }

    private void chargerConfig() {
        try {
            Record conf = new ObjectMapper().readValue(new File("config.json"), Record.class);
            // Clé et IV construits une seule fois
            aes = new Aes_cbc(conf.getMDPAsByte(), conf.getIVAssByte());
            TextAreaReponses.appendText("Config AES chargée\n");
        } catch (Exception e) {
            TextAreaReponses.appendText("Erreur config.json : "
                    + Outils.DiagnosticException.afficheException(e) + "\n");
        }
    }

    private void envoyer() {
        String requette = TextFieldRequette.getText();
        if (requette.isEmpty()) {
            TextAreaReponses.appendText("Requete vide\n");
            return;
        }
        if (requette.equalsIgnoreCase("exit")) {
            deconnecter();
            return;
        }
        if (!enRun || tcp == null) {
            TextAreaReponses.appendText("Non connecté\n");
            return;
        }
        try {
            tcp.requette(requette); // chiffrée dans TCPBin
        } catch (Exception e) {
            TextAreaReponses.appendText(Outils.DiagnosticException.afficheException(e) + "\n");
        }
    }

    private void deconnecter() {
        try {
            if (tcp != null && enRun) {
                tcp.deconnection();
            }
        } catch (Exception e) {
            TextAreaReponses.appendText(Outils.DiagnosticException.afficheException(e) + "\n");
        }
        serveurDeconnecte();
    }

    /** Appelée aussi par TCPBin quand le serveur ferme la connexion. */
    public void serveurDeconnecte() {
        if (enRun) {
            enRun = false;
            voyant.setFill(RED);
            TextAreaReponses.appendText("Déconnecté\n");
        }
    }

    private void connecter() {
        if (aes == null) {
            TextAreaReponses.appendText("Config AES absente\n");
            return;
        }
        if (enRun) return;

        String adresse = TextFieldIP.getText();
        String port = TextFieldPort.getText();
        if (port.isEmpty() || adresse.isEmpty()) {
            TextAreaReponses.appendText("Erreur Connection\n");
            return;
        }
        try {
            InetAddress addr = InetAddress.getByName(adresse);
            tcp = new TCPBin(addr, Integer.parseInt(port), this, aes);
            if (tcp.connection()) {
                tcp.start();
                enRun = true;
                voyant.setFill(GREEN);
            } else {
                TextAreaReponses.appendText("Connexion impossible\n");
            }
        } catch (Exception e) {
            TextAreaReponses.appendText(Outils.DiagnosticException.afficheException(e) + "\n");
        }
    }
}
