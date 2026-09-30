package com.astier.bts.client_tcp_prof;

import com.astier.bts.client_tcp_prof.aes.Aes_cbc;
import com.astier.bts.client_tcp_prof.aes.Outils;
import com.astier.bts.client_tcp_prof.aes.Record;
import com.astier.bts.client_tcp_prof.model.Ipv4;
import com.astier.bts.client_tcp_prof.tcp.TCPBin;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public ChoiceBox choice;

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
        try {
            ArrayList<Ipv4> interfaces = ScanInterfaces.getSystemIP();
            interfaces.forEach(ipv4 -> {
                choice.getItems().add(ipv4.nomInterfaceName()+ " (" + ipv4.ip() + " )");
            });
        } catch (Exception e){
            TextAreaReponses.appendText(("Erreur"));
        }
    }

    private void chargerConfig() {
        try {
            Path chemin = Path.of("src/main/java/com/astier/bts/client_tcp_prof/aes/configuration_json.json");
            String json = Files.readString(chemin, StandardCharsets.UTF_8);

            Record conf = new Record(lireChamp(json, "motDePasse"), lireChamp(json, "iv"));
            // Clé et IV construits une seule fois
            aes = new Aes_cbc(conf.getMDPAsByte(), conf.getIVAssByte());
            TextAreaReponses.appendText("Config AES chargée\n");
        } catch (Exception e) {
            e.printStackTrace();
            TextAreaReponses.appendText("Erreur config : "
                    + Outils.DiagnosticException.afficheException(e) + "\n");
        }
    }


    private String lireChamp(String json, String cle) {
        Matcher m = Pattern.compile("\"" + cle + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Champ absent dans le JSON : " + cle);
        }
        return m.group(1);
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


    public void serveurDeconnecte() {
        if (enRun) {
            enRun = false;
            voyant.setFill(RED);
            TextAreaReponses.appendText("Déconnecté\n");
        }
    }


    public void fermerConnexion() throws IOException {
        if (tcp != null && enRun) {
            tcp.deconnection();
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