package Chord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

class UtilTest {

    @Test
    void hexToInt() {


    }

    @Test
    void byteToHex() {
        final String initialHex = "9FA50FC8F5BE7E3E9A8EA6F0244D6B60F21A435F";
        BigInteger bigInteger = new BigInteger(initialHex, 16);
        String result = Util.byteToHex(bigInteger);
        Assertions.assertEquals(initialHex, result);
        Assertions.assertEquals(initialHex, bigInteger.toString(16).toUpperCase());

    }
}