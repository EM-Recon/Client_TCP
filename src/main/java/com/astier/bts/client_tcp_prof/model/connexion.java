package com.astier.bts.client_tcp_prof.model;

import java.net.InetAddress;

public record connexion(InetAddress addressServeur, int portTCP, int portUDP) {
    public String addressAsString(){
        return String.valueOf(addressServeur).replace("/","");
    }
}
