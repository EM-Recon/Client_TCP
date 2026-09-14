/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_tcp;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;

import static javafx.scene.paint.Color.LIME;
import static javafx.scene.paint.Color.RED;

/**
 * @author Michael
 */
public class TCP extends Thread {
    int port;
    InetAddress serveur;
    Socket socket;
    boolean marche = false;
    boolean connection = false;
    PrintStream out;
    BufferedReader in;

    FXMLDocumentController fxmlCont;

    public TCP() {
    }

    public TCP(InetAddress serveur, int port, FXMLDocumentController fxmlCont) {
        this.port = port;
        this.serveur = serveur;
        this.fxmlCont = fxmlCont;
        System.out.println("@ serveur: " + serveur + " port: " + port);
    }

    static public ArrayList<String> listeDesAdresses() throws UnknownHostException {
        ArrayList<String> laListeDesAdresses = new ArrayList<>();
        InetAddress adrLB = InetAddress.getLoopbackAddress();
        InetAddress adrLH = InetAddress.getLocalHost();
        laListeDesAdresses.add(adrLB.getHostAddress());
        laListeDesAdresses.add(adrLH.getHostAddress());
        return laListeDesAdresses;
    }

    public void connection() {
        if (!this.isAlive()) {
            try {
                System.out.println("état de marche= " + marche);
                try {
                    this.socket = new Socket(this.serveur, this.port);
                } catch (IOException ex) {
                    Logger.getLogger(TCP.class.getName()).log(Level.SEVERE, null, ex);
                }
                out = new PrintStream(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                this.start();    //lance un thread par la methode run qui est la methode du thread d'écoute
                this.marche = true;
            } catch (IOException ex) {
                Logger.getLogger(TCP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void deconnection() throws InterruptedException {
        if (this.isAlive()) {
            try {
                fxmlCont.voyant.setFill(RED);
                out.print("exit");
                marche = false;
                Thread.sleep(1000);
                out.close();
                in.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(TCP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void requette(String laRequette) throws IOException {
        out.println(laRequette);  // envoi reseau
        System.out.println("la requette " + laRequette);
    }

    public void run() {
        while (marche) {
            try {
                String message = null;
                char[] bufferEntree = new char[65535];
                int NbLus;
                NbLus = in.read(bufferEntree);
                message = new String(bufferEntree, 0, NbLus);
                if (message.length() != 0) {
                    System.out.println("    MESSAGE SERVEUR >  \n      " + message + "\n");
                    updateMessage(message);
                }
            } catch (IOException ex) {

            }
        }

    }


    /*
    Pour déclencher une opération graphique en dehors du thread graphique  utiliser
    javafx.application.Platform.runLater(java.lang.Runnable)
    Cette méthode permet d'éxécuter le code du runnable par le thread graphique de JavaFX.
    */
    protected void updateMessage(String message) {
        Platform.runLater(() -> fxmlCont.TextAreaReponses.appendText("    MESSAGE SERVEUR >  \n      " + message + "\n"));
    }
}