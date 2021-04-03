package Chord;

import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        final int numNodes = 100;
        ChordController controller = new ChordController(new Server());

        for (int i = 0; i < numNodes; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            controller.join(new Node());
        }

        Scanner scanner = new Scanner(System.in);

        while (true) {
            String arg = scanner.nextLine();
            String[] tokens = arg.split(" ");
            if (tokens[0].equals("add")) {
                Node node = new Node();
                controller.join(node);
            }
        }


    }
}
