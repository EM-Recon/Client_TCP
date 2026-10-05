package com.astier.bts.client_tcp_prof.multicast;

import com.astier.bts.client_tcp_prof.model.Connexion;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class MulticastDiff {
    private static final String GROUPE_MULTICAST = "239.255.0.1";
    private static final int PORT_MULTICAST = 5555;
    private static final int PORT_REPONSE = 5556;
    private static final int TIMEOUT_MS = 5000;

    private final String nomInterface;

    public MulticastDiff(String nomInterface) {
        this.nomInterface = nomInterface;
    }

    /**
     * Lance la découverte du serveur via multicast.
     * La réponse attendue du serveur doit être au format : IP;PORT_TCP;PORT_UDP
     */
    public Connexion discover() throws IOException {
        NetworkInterface ni = NetworkInterface.getByName(nomInterface);
        if (ni == null) {
            throw new IOException("Interface réseau introuvable : " + nomInterface);
        }

        InetAddress groupe = InetAddress.getByName(GROUPE_MULTICAST);

        try (MulticastSocket ms = new MulticastSocket(PORT_MULTICAST);
             DatagramSocket dsReponse = new DatagramSocket(PORT_REPONSE)) {

            ms.setNetworkInterface(ni);
            ms.setTimeToLive((byte) 60);
            ms.joinGroup(groupe);
            dsReponse.setSoTimeout(TIMEOUT_MS);

            byte[] data = "Tu es qui?".getBytes(StandardCharsets.UTF_8);
            DatagramPacket demande = new DatagramPacket(data, data.length, groupe, PORT_MULTICAST);
            ms.send(demande);

            System.out.println("[Multicast] Requête envoyée sur " + nomInterface);

            byte[] buffer = new byte[256];
            DatagramPacket reponse = new DatagramPacket(buffer, buffer.length);

            try {
                dsReponse.receive(reponse);
            } catch (SocketTimeoutException e) {
                System.out.println("[Multicast] Timeout : aucune réponse du serveur");
                return null;
            }

            String message = new String(reponse.getData(), 0, reponse.getLength(), StandardCharsets.UTF_8);
            System.out.println("[Multicast] Réponse reçue : " + message);

            String[] parts = message.split(";");
            if (parts.length < 3) {
                System.out.println("[Multicast] Format de réponse invalide : " + message);
                return null;
            }

            try {
                InetAddress ipServeur = InetAddress.getByName(parts[0].trim());
                int portTCP = Integer.parseInt(parts[1].trim());
                int portUDP = Integer.parseInt(parts[2].trim());
                return new Connexion(ipServeur, portTCP, portUDP);
            } catch (Exception e) {
                System.err.println("[Multicast] Erreur parsing réponse : " + e.getMessage());
                return null;
            } finally {
                try {
                    ms.leaveGroup(groupe);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
