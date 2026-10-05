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
    public ChoiceBox choice;

    private TCPBin tcp;
    private boolean enRun = false;
    private Aes_cbc aes;
    private ArrayList<Ipv4> interfaces;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        voyant.setFill(RED);
        chargerConfig();

        button.setOnAction(e -> envoyer());
        deconnecter.setOnAction(e -> deconnecter());
        connecter.setOnAction(e -> connecter());

        try {
            interfaces = ScanInterfaces.getSystemIP();
            interfaces.forEach(ipv4 -> {
                choice.getItems().add(ipv4.nomInterfaceName() + " (" + ipv4.ip() + " )");
            });

            choice.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.intValue() >= 0) {
                    decouvreServeur(newVal.intValue());
                }
            });

        } catch (Exception e) {
            TextAreaReponses.appendText("Erreur lors du scan des interfaces\n");
        }
    }

    private void chargerConfig() {
        try {
            Path chemin = Path.of("src/main/java/com/astier/bts/client_tcp_prof/aes/configuration_json.json");
            String json = Files.readString(chemin, StandardCharsets.UTF_8);

            Record conf = new Record(lireChamp(json, "motDePasse"), lireChamp(json, "iv"));
            aes = new Aes_cbc(conf.getMDPAsByte(), conf.getIVAssByte());
            TextAreaReponses.appendText("Config AES chargée\n");
        } catch (Exception e) {
            e.printStackTrace();
            TextAreaReponses.appendText("Erreur config : "
                    + Outils.DiagnosticException.afficheException(e) + "\n");
        }
    }

    private String lireChamp(String json, String cle) {
        Matcher m = Pattern.compile("\"" + cle + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Champ absent dans le JSON : " + cle);
        }
        return m.group(1);
    }

    /**
     * Découvre le serveur via multicast quand une interface est sélectionnée
     */
    private void decouvreServeur(int indexInterface) {
        if (indexInterface < 0 || indexInterface >= interfaces.size()) {
            TextAreaReponses.appendText("Interface invalide\n");
            return;
        }

        Ipv4 interfaceSelectionnee = interfaces.get(indexInterface);
        TextAreaReponses.appendText("Découverte du serveur sur " 
                + interfaceSelectionnee.nomInterfaceName() + "...\n");

        // Lancer la découverte dans un thread séparé
        new Thread(() -> {
            try {
                MulticastDiff multicastDiff = new MulticastDiff(interfaceSelectionnee.nomInterfaceName());
                Connexion connexion = multicastDiff.discover();

                if (connexion != null) {
                    // Mettre à jour l'UI depuis le thread JavaFX
                    javafx.application.Platform.runLater(() -> {
                        TextFieldIP.setText(connexion.addressAsString());
                        TextFieldPort.setText(String.valueOf(connexion.portTCP()));
                        TextAreaReponses.appendText("✓ Serveur trouvé : "
                                + connexion.addressAsString()
                                + " TCP:" + connexion.portTCP()
                                + " UDP:" + connexion.portUDP() + "\n");
                        // Connexion automatique
                        connecter();
                    });
                } else {
                    javafx.application.Platform.runLater(() ->
                            TextAreaReponses.appendText("✗ Aucun serveur trouvé sur cette interface\n"));
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        TextAreaReponses.appendText("✗ Erreur lors de la découverte : "
                                + Outils.DiagnosticException.afficheException(e) + "\n"));
            }
        }).start();
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
            tcp.requette(requette);
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
                TextAreaReponses.appendText("✓ Connecté au serveur\n");
            } else {
                TextAreaReponses.appendText("Connexion impossible\n");
            }
        } catch (Exception e) {
            TextAreaReponses.appendText(Outils.DiagnosticException.afficheException(e) + "\n");
        }
    }
}
