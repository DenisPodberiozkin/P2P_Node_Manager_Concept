package Chord;

import java.math.BigInteger;

public class Util {

    public static BigInteger hexToInt(String hex){
        return new BigInteger(hex, 16);
    }

    public static String byteToHex(byte[] bytes){
        StringBuilder hexString = new StringBuilder(2 * bytes.length);

        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));

        }

        return hexString.toString();
    }

}
