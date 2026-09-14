/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.astier.bts.client_tcp_prof.tcp;


import com.astier.bts.client_tcp_prof.HelloController;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


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

    HelloController fxmlCont;

    public TCP() {
    }

    public TCP(InetAddress serveur, int port, HelloController fxmlCont) {
        this.port = port;
        this.serveur = serveur;
        this.fxmlCont = fxmlCont;
        System.out.println("@ serveur: " + serveur + " port: " + port);
    }



    public void connection() {
       try {
           socket = new Socket(serveur,port);
           out = new PrintStream(socket.getOutputStream(), true);
           in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
           connection = true;
           marche = true;
           System.out.println("Connextion ok");

       } catch (IOException e) {
           System.out.println("Erreur de connection");
       }
    }

    public void deconnection() throws IOException {
        //todo
        in.close();
        socket.close();
        out.close();
    }

    public void requette(String laRequette) throws IOException {
        out.println(laRequette);  // envoi reseau
        System.out.println("la requette " + laRequette);


    }

    public void run() {
        while (marche) {
            String message = null;
            char[] chars = new char [65535];
            int oclus = 0;
            try {
                oclus = in.read(chars);
                if (oclus > 0 ){
                    message = new String(chars,0, oclus);
                    updateMessage(message);
                }
            } catch (Exception e){
                System.out.println("test");
                marche = false;
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
