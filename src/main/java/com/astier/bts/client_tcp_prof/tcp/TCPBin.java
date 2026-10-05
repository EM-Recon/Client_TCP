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
            System.out.println("Connexion TCP OK vers " + serveur + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("Erreur de connexion TCP vers " + serveur + ":" + port + " -> " + e.getMessage());
            return false;
        }
    }

    public void deconnection() throws IOException {
        marche = false;
        if (out != null) {
            out.close();
        }
        if (in != null) {
            in.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public void requette(String message) throws IOException {
        if (message == null) {
            throw new IOException("Message null");
        }
        byte[] chiffre = aes.cryptage(message.getBytes(StandardCharsets.UTF_8));
        if (chiffre == null) {
            throw new IOException("Échec du chiffrement AES");
        }
        requette(chiffre);
    }

    public void requette(byte[] data) throws IOException {
        if (out == null || socket == null || socket.isClosed()) {
            throw new IOException("Socket TCP non prête");
        }
        out.write(data);
        out.flush();
        System.out.println("[TCP] Données envoyées : " + data.length + " octets");
    }

    @Override
    public void run() {
        byte[] buffer = new byte[65535];
        while (marche) {
            try {
                if (in == null || socket == null || socket.isClosed()) {
                    marche = false;
                    break;
                }

                int lus = in.read(buffer);
                if (lus == -1) {
                    marche = false;
                    break;
                }

                ByteArrayOutputStream recu = new ByteArrayOutputStream();
                recu.write(buffer, 0, lus);

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }

                while (in.available() > 0) {
                    int dispo = Math.min(buffer.length, in.available());
                    int l = in.read(buffer, 0, dispo);
                    if (l == -1) {
                        break;
                    }
                    recu.write(buffer, 0, l);
                }

                updateMessage(recu.toByteArray());

            } catch (IOException e) {
                if (marche) {
                    System.err.println("[TCP] Déconnexion ou erreur de lecture : " + e.getMessage());
                }
                marche = false;
            }
        }

        Platform.runLater(() -> {
            if (fxmlCont != null) {
                fxmlCont.serveurDeconnecte();
            }
        });
    }

    protected void updateMessage(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        int debut = 0;

        while (debut < data.length) {
            byte[] clair = null;
            int fin = -1;

            for (int e = debut + 16; e <= data.length; e += 16) {
                byte[] essai = Arrays.copyOfRange(data, debut, e);
                byte[] tmp = aes.decryptage(essai);
                if (tmp != null && estTexte(tmp)) {
                    clair = tmp;
                    fin = e;
                    break;
                }
            }

            if (clair == null) {
                StringBuilder hex = new StringBuilder();
                for (int i = debut; i < data.length; i++) {
                    hex.append(String.format("%02X ", data[i]));
                }
                sb.append("SERVEUR > ").append(hex).append("\n");
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
        if (b == null || b.length == 0) {
            return false;
        }

        String s = new String(b, StandardCharsets.UTF_8);
        for (char c : s.toCharArray()) {
            if (c == '\ufffd') {
                return false;
            }
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                return false;
            }
        }
        return true;
    }
}
