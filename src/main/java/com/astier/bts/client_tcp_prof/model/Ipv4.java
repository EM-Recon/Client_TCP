package com.astier.bts.client_tcp_prof.model;

public record Ipv4(String interfaceType,String nomInterfaceName,String ip) {
    @Override
    public String toString(){
        return nomInterfaceName;
    }
}