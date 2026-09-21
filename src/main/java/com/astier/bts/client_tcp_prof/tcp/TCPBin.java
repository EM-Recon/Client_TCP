package com.astier.bts.client_tcp_prof.tcp;

import com.astier.bts.client_tcp_prof.HelloController;
import com.astier.bts.client_tcp_prof.aes.Aes_cbc;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Client TCP binaire sécurisé par AES-CBC
 * @author Michael
 */
public class TcpBinaireAes extends Thread {
    private InetAddress adrServeur;
    private int portEcoute;
    private Socket chaussette;
    private boolean actif = false;
    private boolean estConnecte = false;

    private DataOutputStream fluxSortant;
    private DataInputStream fluxEntrant;

    private HelloController controleur;
    private Aes_cbc chiffreur;

    public TcpBinaireAes() {
    }

    public TcpBinaireAes(InetAddress adrServeur, int portEcoute, HelloController controleur, byte[] clef, byte[] vectorInit) {
        this.adrServeur = adrServeur;
        this.portEcoute = portEcoute;
        this.controleur = controleur;
        this.chiffreur = new Aes_cbc(clef, vectorInit);
        System.out.println("@ hôte: " + adrServeur + " port: " + portEcoute + " [AES activé]");
    }

    public void connection() {
        try {
            chaussette = new Socket(adrServeur, portEcoute);
            fluxSortant = new DataOutputStream(chaussette.getOutputStream());
            fluxEntrant = new DataInputStream(chaussette.getInputStream());
            estConnecte = true;
            actif = true;
            System.out.println("Connexion binaire établie");
        } catch (IOException err) {
            System.out.println("Échec de connexion : " + err.getMessage());
        }
    }

    public void deconnection() throws IOException {
        actif = false;
        if (chaussette != null && !chaussette.isClosed()) {
            chaussette.close();
        }
    }

    public void requette(String commandeTxt) throws IOException {
        if (fluxSortant != null && chiffreur != null) {
            // Conversion texte vers octets
            byte[] donneesClaires = commandeTxt.getBytes(StandardCharsets.UTF_8);

            // Chiffrement
            byte[] donneesChiffrees = chiffreur.cryptage(donneesClaires);

            if (donneesChiffrees != null) {
                // Envoi de la taille du paquet puis du paquet chiffré
                fluxSortant.writeInt(donneesChiffrees.length);
                fluxSortant.write(donneesChiffrees);
                fluxSortant.flush();
                System.out.println("Requête envoyée (" + donneesChiffrees.length + " octets chiffrés)");
            }
        }
    }

    @Override
    public void run() {
        while (actif) {
            try {
                // Lecture de la taille du bloc chiffré à recevoir
                int taillePaquet = fluxEntrant.readInt();

                if (taillePaquet > 0) {
                    byte[] tamponCrypt = new byte[taillePaquet];
                    fluxEntrant.readFully(tamponCrypt);

                    // Déchiffrement du bloc
                    byte[] tamponClair = chiffreur.decryptage(tamponCrypt);

                    if (tamponClair != null) {
                        String reponseServeur = new String(tamponClair, StandardCharsets.UTF_8);
                        updateMessage(reponseServeur);
                    }
                }
            } catch (IOException err) {
                actif = false;
                System.out.println("Flux binaire interrompu.");
            }
        }
    }

    protected void updateMessage(String msgRecu) {
        Platform.runLater(() -> controleur.TextAreaReponses.appendText("    SERVEUR SECURE > \n      " + msgRecu + "\n"));
    }
}
