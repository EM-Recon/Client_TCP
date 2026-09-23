package com.astier.bts.client_tcp_prof.aes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

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
}
