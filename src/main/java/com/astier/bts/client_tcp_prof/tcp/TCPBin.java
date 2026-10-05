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
            log("✓ Connexion TCP établie vers " + serveur.getHostAddress() + ":" + port);
            return true;
        } catch (IOException e) {
            log("✗ Erreur connexion TCP : " + e.getMessage());
            return false;
        }
    }

    public void deconnection() throws IOException {
        marche = false;
        if (out != null) out.close();
        if (in != null) in.close();
        if (socket != null && !socket.isClosed()) socket.close();
        log("Déconnexion TCP effectuée");
    }

    public void requette(String message) throws IOException {
        if (message == null) throw new IOException("Message null");
        
        byte[] chiffre = aes.cryptage(message.getBytes(StandardCharsets.UTF_8));
        if (chiffre == null) throw new IOException("Échec du chiffrement AES");
        
        requette(chiffre);
    }

    public void requette(byte[] data) throws IOException {
        if (out == null || socket == null || socket.isClosed()) {
            throw new IOException("Socket TCP non prête");
        }
        out.write(data);
        out.flush();
        log("[CLIENT] Envoyé " + data.length + " octets (chiffrés)");
    }

    @Override
    public void run() {
        log("[TCP] Boucle de réception démarrée");
        
        byte[] buffer = new byte[65535];
        
        while (marche) {
            try {
                if (in == null || socket == null || socket.isClosed()) {
                    log("[TCP] Socket fermée, arrêt de la lecture");
                    marche = false;
                    break;
                }

                int lus = in.read(buffer);
                if (lus == -1) {
                    log("[TCP] Fin de flux (connexion fermée par serveur)");
                    marche = false;
                    break;
                }

                log("[TCP] " + lus + " octets reçus");
                
                // Afficher les données brutes en hex
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < Math.min(lus, 32); i++) {
                    hex.append(String.format("%02X ", buffer[i]));
                }
                log("[RAW HEX] " + hex + (lus > 32 ? "..." : ""));

                // Essayer de déchiffrer
                byte[] data = Arrays.copyOf(buffer, lus);
                updateMessage(data);

            } catch (IOException e) {
                if (marche) {
                    log("[TCP] Erreur : " + e.getMessage());
                }
                marche = false;
            } catch (Exception e) {
                log("[TCP] Exception : " + e.getClass().getSimpleName() + " - " + e.getMessage());
                marche = false;
            }
        }

        log("[TCP] Fin de la boucle de réception");
        Platform.runLater(() -> {
            if (fxmlCont != null) {
                fxmlCont.serveurDeconnecte();
            }
        });
    }

    protected void updateMessage(byte[] data) {
        if (data == null || data.length == 0) {
            log("[PARSE] Données vides");
            return;
        }

        log("[PARSE] Tentative de déchiffrement de " + data.length + " octets");

        StringBuilder sb = new StringBuilder();
        int debut = 0;

        while (debut < data.length) {
            byte[] clair = null;
            int fin = -1;

            // Essayer les longueurs de 16 octets
            for (int e = debut + 16; e <= data.length; e += 16) {
                byte[] essai = Arrays.copyOfRange(data, debut, e);
                byte[] tmp = aes.decryptage(essai);
                
                if (tmp != null && estTexte(tmp)) {
                    clair = tmp;
                    fin = e;
                    log("[DECRYPT] ✓ Bloc déchiffré (" + essai.length + " octets) -> " + new String(tmp, StandardCharsets.UTF_8));
                    break;
                }
            }

            if (clair == null) {
                log("[DECRYPT] ✗ Bloc non déchiffrable à partir de l'offset " + debut);
                
                // Afficher les données brutes
                StringBuilder hex = new StringBuilder();
                int afficher = Math.min(32, data.length - debut);
                for (int i = 0; i < afficher; i++) {
                    hex.append(String.format("%02X ", data[debut + i]));
                }
                sb.append("SERVEUR (brut) > ").append(hex).append("\n");
                break;
            }

            sb.append("SERVEUR > ").append(new String(clair, StandardCharsets.UTF_8)).append("\n");
            debut = fin;
        }

        String affichage = sb.toString();
        Platform.runLater(() -> {
            if (fxmlCont != null && fxmlCont.TextAreaReponses != null) {
                fxmlCont.TextAreaReponses.appendText(affichage);
            }
        });
    }

    private boolean estTexte(byte[] b) {
        if (b == null || b.length == 0) return false;

        String s = new String(b, StandardCharsets.UTF_8);
        for (char c : s.toCharArray()) {
            if (c == '\ufffd') return false;
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') return false;
        }
        return true;
    }

    private void log(String msg) {
        System.out.println(msg);
        Platform.runLater(() -> {
            if (fxmlCont != null && fxmlCont.TextAreaReponses != null) {
                fxmlCont.TextAreaReponses.appendText(msg + "\n");
            }
        });
    }
}
