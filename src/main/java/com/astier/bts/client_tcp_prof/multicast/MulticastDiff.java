package com.astier.bts.client_tcp_prof.multicast;
import com.astier.bts.client_tcp_prof.aes.Outils;
import com.astier.bts.client_tcp_prof.model.Connexion;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MulticastDiff {
    private final String MON_INTERFACE = "ethernet_32768";
    private InetAddress ip;
    private byte [] data = "Tu es qui?".getBytes(StandardCharsets.UTF_8);
    private int port = 5555;
    private int portReponse = 5556;
    private byte ttl = 60;
    private byte [] bufferReponse = new byte[27];
    private DatagramPacket dp;
    private MulticastSocket ms;
    private DatagramSocket dsReponse;


    public MulticastDiff() throws IOException {
        ms = new MulticastSocket();
        NetworkInterface ni = NetworkInterface.getByName(MON_INTERFACE);
        ms.setNetworkInterface(ni);
        ms.setTimeToLive(ttl);
        dp = new DatagramPacket(data, data.length, ip, port);
        dsReponse = new DatagramSocket(portReponse);
        ms.send(dp);

        new Thread(() -> {
            dp = new DatagramPacket(bufferReponse, bufferReponse.length);
            System.out.println("Attente");
            try {
                dsReponse.receive(dp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String reponseServeur = new String(dp.getData(),0,dp.getLength());
            String[] reponseSplitted = reponseServeur.split(";");
            try {
                Connexion connexion = new Connexion(
                        InetAddress.getByName(reponseSplitted[0]),
                        Integer.parseInt(reponseSplitted[1]),
                        Integer.parseInt(reponseSplitted[2])
                );
            } catch (UnknownHostException e){
                System.err.println(Outils.DiagnosticException.afficheException(e));
            }

            System.out.println("Reponse : " + new String(bufferReponse));
        }).start();

    }
}