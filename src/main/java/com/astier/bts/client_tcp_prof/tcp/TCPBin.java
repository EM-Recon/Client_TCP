package com.astier.bts.client_tcp_prof.tcp;

import com.astier.bts.client_tcp_prof.HelloController;
import com.astier.bts.client_tcp_prof.aes.Aes_cbc;
import javafx.application.Platform;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TCPBin extends Thread {
    private final int port;
    private final InetAddress serveur;
    private Socket socket;
    private volatile boolean marche = false;

    private DataOutputStream out;
    private InputStream in;

    private final HelloController fxmlCont;
    private final Aes_cbc aes;

    public TCPBin(InetAddress serveur, int port, HelloController fxmlCont, Aes_cbc aes) {
        this.port = port;
        this.serveur = serveur;
        this.fxmlCont = fxmlCont;
        this.aes = aes;
        setDaemon(true);
    }

    public boolean connection() {
        try {
            socket = new Socket(serveur, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = socket.getInputStream();
            marche = true;
            System.out.println("Connexion ok");
            return true;
        } catch (IOException e) {
            System.out.println("Erreur de connexion");
            return false;
        }
    }

    public void deconnection() throws IOException {
        marche = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /** Chiffre le texte en AES-CBC puis l'envoie. */
    public void requette(String message) throws IOException {
        byte[] chiffre = aes.cryptage(message.getBytes(StandardCharsets.UTF_8));
        if (chiffre == null) {
            throw new IOException("Échec du chiffrement");
        }
        requette(chiffre);
    }

    /** Envoi brut (déjà chiffré). */
    public void requette(byte[] data) throws IOException {
        if (out != null) {
            out.write(data);
            out.flush();
            System.out.println("Requête binaire envoyée (" + data.length + " octets)");
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[65535];
        while (marche) {
            try {
                int lus = in.read(buffer);
                if (lus == -1) {
                    marche = false;
                    break;
                }
                if (lus > 0) {
                    updateMessage(Arrays.copyOf(buffer, lus));
                }
            } catch (Exception e) {
                if (marche) System.out.println("Déconnexion ou erreur de lecture");
                marche = false;
            }
        }
        Platform.runLater(fxmlCont::serveurDeconnecte);
    }

    protected void updateMessage(byte[] data) {
        StringBuilder hex = new StringBuilder();
        for (byte b : data) {
            hex.append(String.format("%02X ", b));
        }

        String affichage;
        byte[] clair = (data.length % 16 == 0) ? aes.decryptage(data) : null;
        if (clair != null) {
            affichage = "MESSAGE SERVEUR (déchiffré) > " + new String(clair, StandardCharsets.UTF_8)
                    + "\n   (HEX reçu) > " + hex;
        } else {
            affichage = "MESSAGE SERVEUR (non déchiffrable) > " + hex;
        }

        Platform.runLater(() -> fxmlCont.TextAreaReponses.appendText(affichage + "\n"));
    }
}
