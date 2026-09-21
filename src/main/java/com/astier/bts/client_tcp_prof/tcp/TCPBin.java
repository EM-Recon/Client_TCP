package com.astier.bts.client_tcp_prof.tcp;

import com.astier.bts.client_tcp_prof.HelloController;
import com.astier.bts.client_tcp_prof.aes.Aes_cbc;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Classe TCP Binaire et Chiffrée avec AES-CBC
 */
public class TCP extends Thread {
    int port;
    InetAddress serveur;
    Socket socket;
    boolean marche = false;
    boolean connection = false;

    // Flux binaires au lieu de textuels
    private OutputStream out;
    private DataInputStream in;

    HelloController fxmlCont;
    private Aes_cbc aes;

    public TCP() {
    }

    /**
     * Constructeur prenant en compte la clé AES et l'IV pour le chiffrement.
     */
    public TCP(InetAddress serveur, int port, HelloController fxmlCont, byte[] key, byte[] iv) {
        this.port = port;
        this.serveur = serveur;
        this.fxmlCont = fxmlCont;
        this.aes = new Aes_cbc(key, iv);
        System.out.println("@ serveur: " + serveur + " port: " + port + " [AES-CBC activé]");
    }

    public void connection() {
        try {
            socket = new Socket(serveur, port);
            // Récupération des flux binaires bruts
            out = socket.getOutputStream();
            in = new DataInputStream(socket.getInputStream());
            connection = true;
            marche = true;
            System.out.println("Connexion binaire sécurisée OK");

        } catch (IOException e) {
            System.out.println("Erreur de connexion");
        }
    }

    public void deconnection() throws IOException {
        marche = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Chiffre la requête textuelle en octets puis l'envoie sur le réseau.
     * Envoie la taille du message (4 octets) suivie des données chiffrées.
     */
    public void requette(String laRequette) throws IOException {
        if (out != null && aes != null) {
            // 1. Conversion de la chaîne en octets (UTF-8)
            byte[] plainTextBytes = laRequette.getBytes(StandardCharsets.UTF_8);

            // 2. Chiffrement AES-CBC
            byte[] encryptedBytes = aes.cryptage(plainTextBytes);

            if (encryptedBytes != null) {
                // 3. Envoi de la taille du bloc chiffré (évite les problèmes de découpage TCP)
                out.write(ByteBuffer.allocate(4).putInt(encryptedBytes.length).array());

                // 4. Envoi des données chiffrées
                out.write(encryptedBytes);
                out.flush();

                System.out.println("Requête chiffrée envoyée (" + encryptedBytes.length + " octets)");
            }
        }
    }

    /**
     * Boucle de lecture binaire : lit la taille attendue puis déchiffre les octets reçus.
     */
    @Override
    public void run() {
        while (marche) {
            try {
                // 1. Lecture de la taille des données chiffrées arrivantes (4 octets)
                int length = in.readInt();

                if (length > 0) {
                    // 2. Allocation du tampon exact et lecture complète du bloc
                    byte[] encryptedData = new byte[length];
                    in.readFully(encryptedData);

                    // 3. Déchiffrement AES-CBC
                    byte[] decryptedData = aes.decryptage(encryptedData);

                    if (decryptedData != null) {
                        // 4. Reconstitution de la chaîne de caractères
                        String messageClair = new String(decryptedData, StandardCharsets.UTF_8);
                        updateMessage(messageClair);
                    }
                }
            } catch (IOException e) {
                // Déconnexion ou fin de flux
                marche = false;
                System.out.println("Déconnexion du serveur ou erreur de lecture.");
            }
        }
    }

    protected void updateMessage(String message) {
        Platform.runLater(() -> fxmlCont.TextAreaReponses.appendText("   MESSAGE SERVEUR > \n      " + message + "\n"));
    }
}
