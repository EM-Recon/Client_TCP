package com.astier.bts.client_tcp_prof.aes;
import java.util.Objects;

public record Record(String motDePasse, String iv) {
    public Record {
        Objects.requireNonNull(motDePasse, "Rentrer un mdp");
        Objects.requireNonNull(iv,"besoin d'un iv");
    }
    public byte[] getIVAssByte(){
        return Outils.normalizeChaine(iv,16);
    }
    public byte[] getMDPAsByte(){
        return Outils.normalizeChaine(motDePasse,16);
    }
}
