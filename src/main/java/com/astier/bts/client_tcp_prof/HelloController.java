package com.astier.bts.client_tcp_prof;

import com.astier.bts.client_tcp_prof.aes.Aes_cbc;
import com.astier.bts.client_tcp_prof.aes.Outils;
import com.astier.bts.client_tcp_prof.aes.Record;
import com.astier.bts.client_tcp_prof.model.Connexion;
import com.astier.bts.client_tcp_prof.model.Ipv4;
import com.astier.bts.client_tcp_prof.multicast.MulticastDiff;
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
    public ChoiceBox<String> choice;

    private TCPBin tcp;
    private boolean enRun = false;
    private Aes_cbc aes;
    private ArrayList<Ipv4> interfaces;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (voyant != null) voyant.setFill(RED);
        chargerConfig();

        if (button != null) button.setOnAction(e -> envoyer());
        if (deconnecter != null) deconnecter.setOnAction(e -> deconnecter());
        if (connecter != null) connecter.setOnAction(e -> connecter());

        try {
            interfaces = ScanInterfaces.getSystemIP();
            if (choice != null) {
                interfaces.forEach(ipv4 -> 
                    choice.getItems().add(ipv4.nomInterfaceName() + " (" + ipv4.ip() + " )")
                );

                choice.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && newVal.intValue() >= 0) {
                        decouvreServeur(newVal.intValue());
                    }
                });
            }
        } catch (Exception e) {
            log("Erreur lors du scan des interfaces");
        }
    }

    private void chargerConfig() {
        try {
            Path chemin = Path.of("src/main/java/com/astier/bts/client_tcp_prof/aes/configuration_json.json");
            String json = Files.readString(chemin, StandardCharsets.UTF_8);

            Record conf = new Record(lireChamp(json, "motDePasse"), lireChamp(json, "iv"));
            aes = new Aes_cbc(conf.getMDPAsByte(), conf.getIVAssByte());
            log("Config AES chargée");
        } catch (Exception e) {
            log("Erreur config: " + Outils.DiagnosticException.afficheException(e));
        }
    }

    private String lireChamp(String json, String cle) {
        Matcher m = Pattern.compile("\"" + cle + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Champ absent dans le JSON: " + cle);
        }
        return m.group(1);
    }

    private void decouvreServeur(int indexInterface) {
        if (indexInterface < 0 || indexInterface >= interfaces.size()) {
            log("Interface invalide");
            return;
        }

        Ipv4 interfaceSelectionnee = interfaces.get(indexInterface);
        log("Découverte du serveur sur " + interfaceSelectionnee.nomInterfaceName() + "...");

        // Thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                MulticastDiff multicastDiff = new MulticastDiff(interfaceSelectionnee.nomInterfaceName());
                Connexion connexion = multicastDiff.discover();

                if (connexion != null) {
                    // Exécuter sur le thread JavaFX
                    javafx.application.Platform.runLater(() -> {
                        if (TextFieldIP != null) TextFieldIP.setText(connexion.addressAsString());
                        if (TextFieldPort != null) TextFieldPort.setText(String.valueOf(connexion.portTCP()));
                        log("✓ Serveur trouvé: " + connexion.addressAsString() + " TCP:" + connexion.portTCP());
                        connecter();
                    });
                } else {
                    javafx.application.Platform.runLater(() -> 
                        log("✗ Aucun serveur trouvé")
                    );
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                    log("✗ Erreur: " + e.getMessage())
                );
            }
        }).start();
    }

    private void envoyer() {
        if (TextFieldRequette == null) return;
        String requette = TextFieldRequette.getText();
        if (requette == null || requette.isEmpty()) {
            log("Requete vide");
            return;
        }
        if (requette.equalsIgnoreCase("exit")) {
            deconnecter();
            return;
        }
        if (!enRun || tcp == null) {
            log("Non connecté");
            return;
        }
        try {
            tcp.requette(requette);
            TextFieldRequette.clear();
        } catch (Exception e) {
            log("Erreur envoi: " + e.getMessage());
        }
    }

    private void deconnecter() {
        try {
            if (tcp != null && enRun) {
                tcp.deconnection();
            }
        } catch (Exception e) {
            log("Erreur déconnexion: " + e.getMessage());
        }
        serveurDeconnecte();
    }

    public void serveurDeconnecte() {
        if (enRun) {
            enRun = false;
            if (voyant != null) voyant.setFill(RED);
            log("Déconnecté");
        }
    }

    public void fermerConnexion() throws IOException {
        if (tcp != null && enRun) {
            tcp.deconnection();
        }
    }

    private void connecter() {
        if (aes == null) {
            log("Config AES absente");
            return;
        }
        if (enRun) return;
        if (TextFieldIP == null || TextFieldPort == null) return;

        String adresse = TextFieldIP.getText();
        String port = TextFieldPort.getText();

        if (adresse == null || adresse.isEmpty() || port == null || port.isEmpty()) {
            log("Erreur Connection - champs vides");
            return;
        }

        try {
            InetAddress addr = InetAddress.getByName(adresse);
            tcp = new TCPBin(addr, Integer.parseInt(port), this, aes);

            if (tcp.connection()) {
                tcp.start();
                enRun = true;
                if (voyant != null) voyant.setFill(GREEN);
                log("✓ Connecté au serveur");
            } else {
                log("Connexion impossible");
            }
        } catch (Exception e) {
            log("Erreur connexion: " + e.getMessage());
        }
    }

    private void log(String message) {
        if (TextAreaReponses != null) {
            TextAreaReponses.appendText(message + "\n");
            System.out.println("[UI] " + message);
        } else {
            System.out.println(message);
        }
    }
}
