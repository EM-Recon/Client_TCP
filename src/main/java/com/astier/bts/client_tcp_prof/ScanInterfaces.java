/*
 * Copyright (c) 2026 Michaël Moser.
 *
 * Auteur : Michaël Moser
 * Tous droits réservés.
 */

package com.astier.bts.client_tcp_prof;

import com.astier.bts.client_tcp_prof.model.Ipv4;
import com.astier.bts.client_tcp_prof.exceptions.DiagnosticException;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;



public class ScanInterfaces {

    public static ArrayList<Ipv4> getSystemIP() throws SocketException {
        ArrayList<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        ArrayList<Ipv4> ipv4s = new ArrayList<>();
        interfaces.forEach(networkInterface -> {
            try {
                String nomInterface = networkInterface.getName().toLowerCase();
                String nomAffiche = networkInterface.getDisplayName().toLowerCase();
                boolean estInterfaceVirtuelle =
                        nomInterface.contains("virtual")
                                || nomAffiche.contains("virtual")
                                || nomAffiche.contains("hyper-v")
                                || nomAffiche.contains("docker")
                                || nomAffiche.contains("wsl")
                                || nomAffiche.contains("vmware")
                                || nomAffiche.contains("virtualbox");
                if (networkInterface.isUp()
                        && !networkInterface.isLoopback()
                        && !networkInterface.isVirtual()
                        && !networkInterface.isPointToPoint()
                        && !estInterfaceVirtuelle) {
                    ArrayList<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
                    inetAddresses.forEach(inetAddress -> {
                        if (inetAddress instanceof Inet4Address) {
                            ipv4s.add(new Ipv4(
                                    networkInterface.getDisplayName(),
                                    networkInterface.getName(),
                                    inetAddress.getHostAddress()
                            ));
                        }
                    });
                }
            } catch (Exception e) {
                System.out.println(DiagnosticException.afficheException(e));
            }
        });
        return ipv4s;
    }
}