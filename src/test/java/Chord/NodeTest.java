package Chord;

import org.junit.jupiter.api.Test;

class NodeTest {

    @Test
    void isBigger() {
        System.out.println(Node.isBigger("C6F48A3186B8EAD3B9CE208D00E99F00169D52E7", "06F48A3186B8EAD3B9CE208D00E99F00169D52E7"));
        System.out.println(Node.isBigger("C6F48A3186B8EAD3B9CE208D00E99F00169D52E7", "6F48A3186B8EAD3B9CE208D00E99F00169D52E7"));
    }

    @Test
    void isBigger1() {
    }
}