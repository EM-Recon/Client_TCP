package com.astier.bts.client_tcp_prof.tcp;

import com.astier.bts.client_tcp_prof.HelloController;
import javafx.application.Platform;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * @author Michael
 */
public class TCPBin extends Thread {
    int port;
    InetAddress serveur;
    Socket socket;
    boolean marche = false;
    boolean connection = false;

    DataOutputStream out;
    InputStream in;

    HelloController fxmlCont;

    public TCPBin() {
    }

    public TCPBin(InetAddress serveur, int port, HelloController fxmlCont) {
        this.port = port;
        this.serveur = serveur;
        this.fxmlCont = fxmlCont;
        System.out.println("@ serveur: " + serveur + " port: " + port);
    }

    public void connection() {
        try {
            socket = new Socket(serveur, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = socket.getInputStream();
            connection = true;
            marche = true;
            System.out.println("Connexion ok");

        } catch (IOException e) {
            System.out.println("Erreur de connexion");
        }
    }

    public void deconnection() throws IOException {
        marche = false;
        if (socket != null && !socket.isClosed()){
            socket.close();
        }
    }


    public void requette(byte[] laRequette) throws IOException {
        if (out != null) {
            out.write(laRequette);
            out.flush();
            System.out.println("Requête binaire envoyée (" + laRequette.length + " octets)");
        }
    }

    @Override
    public void run() {
        while (marche) {
            byte[] buffer = new byte[65535];
            int oclus = 0;
            try {
                oclus = in.read(buffer);
                if (oclus == -1) {
                    marche = false;
                    break;
                }
                if (oclus > 0 ) {
                    byte[] dataRecue = new byte[oclus];
                    System.arraycopy(buffer, 0, dataRecue, 0, oclus);
                    updateMessage(dataRecue);
                }
            } catch (Exception e){
                System.out.println("Déconnexion ou erreur de lecture");
                marche = false;
            }
        }
    }

    protected void updateMessage(byte[] data) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : data) {
            hexString.append(String.format("%02X ", b));
        }

        Platform.runLater(() ->
                fxmlCont.TextAreaReponses.appendText("    MESSAGE SERVEUR (HEX) >  \n      " + hexString.toString() + "\n")
        );
    }
}