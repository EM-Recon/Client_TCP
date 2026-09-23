package com.astier.bts.client_tcp_prof.aes;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLProtocolException;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.net.http.HttpConnectTimeoutException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class Outils {

    public static byte[] normalizeChaine(String chaine, int longeur) {
        Random r = new Random();
        ArrayList<Byte> ar_Chaine = new ArrayList<>();
        byte[] b_Chaine = chaine.getBytes(StandardCharsets.UTF_8);
        for (byte b : b_Chaine) {
            ar_Chaine.add(b);
        }
        if (ar_Chaine.size()<longeur) {
            while (ar_Chaine.size()<longeur) {
                ar_Chaine.add((byte)(r.nextInt(0xff - 0x00)));
            }
        }
        System.out.println("la chaine");
        System.out.println(new String(convertionEnBytes(ar_Chaine, longeur)));
        return convertionEnBytes(ar_Chaine,longeur);
    }

    public static byte[] convertionEnBytes(ArrayList<Byte> ar_Bytes, int longeur){
        byte[] byteArray = new byte[longeur];
        for (int i = 0; i < longeur; i++) {
            byteArray[i] = ar_Bytes.get(i);
        }
        return byteArray;
    }

    public static class DiagnosticException {

        public static String afficheException(Throwable ex) {
            String diagnostic = switch (ex) {
                case SocketTimeoutException e ->
                        "Timeout [Socket] : Délai d'attente dépassé lors de la lecture ou de la connexion TCP/UDP.";
                case HttpConnectTimeoutException e ->
                        "Timeout [HTTP Client] : Échec de connexion réseau dans le délai imparti.";
                case TimeoutException e ->
                        "Timeout [Asynchrone/Future] : Une tâche ou un appel asynchrone a expiré.";
                case InterruptedIOException e ->
                        "Timeout / Interruption : Opération E/S interrompue (souvent suite à un délai dépassé).";
                case ConnectException e ->
                        "TCP [Connexion refusée] : Aucun serveur n'écoute sur ce port ou un pare-feu bloque le passage.";
                case BindException e ->
                        "TCP/UDP [Port occupé] : Impossible d'attacher la socket (le port local est déjà utilisé).";
                case NoRouteToHostException e ->
                        "TCP/UDP [Hôte inaccessible] : Pas d'itinéraire réseau vers la cible (réseau coupé ou routeur en panne).";
                case ProtocolException e ->
                        "TCP/HTTP [Protocole] : Violation des règles du protocole sous-jacent.";
                case PortUnreachableException e ->
                        "UDP [Port inaccessible] : Un paquet ICMP indique que le port distant n'écoute pas.";
                case UnknownHostException e ->
                        "DNS [Hôte inconnu] : Impossible de résoudre l'adresse IP du domaine.";
                case MalformedURLException e ->
                        "URL [Format Invalide] : L'adresse URL saisie est mal formée.";
                case SSLHandshakeException e ->
                        "TLS/SSL [Handshake échec] : Certificat invalide, expiré ou auto-signé non approved.";
                case SSLPeerUnverifiedException e ->
                        "TLS/SSL [Identité] : Impossible de vérifier l'identité de l'hôte distant.";
                case SSLProtocolException e ->
                        "TLS/SSL [Protocole] : Incompatibilité de version SSL/TLS ou de ciphers.";
                case SSLException e ->
                        "TLS/SSL [Erreur Générale] : Échec de la couche de chiffrement.";
                case AsynchronousCloseException e ->
                        "NIO [Canal Fermé] : Le canal d'E/S a été fermé par un autre Thread pendant l'opération.";
                case ClosedChannelException e ->
                        "NIO [Canal Inactif] : Tentative d'écriture ou lecture sur un canal déjà fermé.";
                case EOFException e ->
                        "Flux [Fin Prématurée] : La connexion ou le flux s'est fermé avant la fin de la lecture.";
                case FileNotFoundException e ->
                        "Flux [Fichier Introuvable] : Impossible d'ouvrir le fichier source/destination.";
                case SocketException e ->
                        "Socket [Erreur Bas Niveau] : " + switch (e.getMessage() != null ? e.getMessage().toLowerCase() : "") {
                            case String s when s.contains("connection reset") ->
                                    "La connexion TCP a été réinitialisée brutalement par le serveur distant.";
                            case String s when s.contains("broken pipe") ->
                                    "Écriture impossible : le canal réseau distant est fermé (Broken Pipe).";
                            default -> "Erreur de socket (" + e.getMessage() + ").";
                        };
                case IOException e ->
                        "E/S [Flux Générique] : Erreur de lecture/écriture sur le flux (" + e.getMessage() + ").";
                case SecurityException e ->
                        "Sécurité : Opération réseau interdite par la politique de sécurité locale.";
                case IllegalArgumentException e ->
                        "Argument Invalide : Numéro de port hors limites (0-65535) ou adresse null.";
                case Exception e ->
                        "Erreur : " + e.getClass().getSimpleName() + " - " + e.getMessage();
                case Error e ->
                        "Erreur Système Critique : " + e.getMessage();
                default -> "Throwable inconnu";

            };

            System.out.println(diagnostic);
            return diagnostic;
        }
    }
}
