package com.astier.bts.client_tcp_prof.tcp;

import com.astier.bts.client_tcp_prof.HelloController;
import com.astier.bts.client_tcp_prof.aes.Aes_cbc;
import javafx.application.Platform;

import java.io.ByteArrayOutputStream;
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
                ByteArrayOutputStream recu = new ByteArrayOutputStream();

                int lus = in.read(buffer); // bloque jusqu'à la première donnée
                if (lus == -1) {
                    marche = false;
                    break;
                }
                recu.write(buffer, 0, lus);

                // courte fenêtre pour récupérer la suite des données
                Thread.sleep(50);
                while (in.available() > 0) {
                    lus = in.read(buffer, 0, Math.min(buffer.length, in.available()));
                    if (lus == -1) break;
                    recu.write(buffer, 0, lus);
                }

                updateMessage(recu.toByteArray());
            } catch (Exception e) {
                if (marche) System.out.println("Déconnexion ou erreur de lecture");
                marche = false;
            }
        }
        Platform.runLater(fxmlCont::serveurDeconnecte);
    }

    /**
     * Les données reçues peuvent contenir plusieurs messages chiffrés collés.
     * On cherche la fin de chaque message : premier découpage (multiple de 16 octets)
     * dont le bourrage est valide et dont le contenu est du texte lisible.
     */
    protected void updateMessage(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int debut = 0;

        while (debut < data.length) {
            byte[] clair = null;
            int fin = -1;

            for (int e = debut + 16; e <= data.length; e += 16) {
                byte[] essai = aes.decryptage(Arrays.copyOfRange(data, debut, e));
                if (essai != null && estTexte(essai)) {
                    clair = essai;
                    fin = e;
                    break;
                }
            }

            if (clair == null) { // reste non déchiffrable : affichage en hexadécimal
                StringBuilder hex = new StringBuilder();
                for (int i = debut; i < data.length; i++) {
                    hex.append(String.format("%02X ", data[i]));
                }
                sb.append("SERVEUR (non déchiffrable) > ").append(hex).append("\n");
                break;
            }

            sb.append("SERVEUR > ").append(new String(clair, StandardCharsets.UTF_8)).append("\n");
            debut = fin;
        }

        String affichage = sb.toString();
        Platform.runLater(() -> fxmlCont.TextAreaReponses.appendText(affichage));
    }

    /** Vrai si les octets forment un texte UTF-8 lisible (pas de caractères de contrôle ni de �). */
    private boolean estTexte(byte[] b) {
        String s = new String(b, StandardCharsets.UTF_8);
        for (char c : s.toCharArray()) {
            if (c == '\uFFFD') return false;
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') return false;
        }
        return true;
    }
}