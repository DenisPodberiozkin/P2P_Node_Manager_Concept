package Chord;

public class Updater implements Runnable {


    private final Node node;

    public Updater(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("Runner Started");
                Thread.sleep(1000);
                System.out.println("Runner run!");
                node.checkConnections();
//                if (!node.stabilize()) {
//                    node.stabilize();
//                }
                boolean status;
                do {
                    status = node.stabilize();
                } while (!status);
            } catch (InterruptedException e) {
                e.printStackTrace();
//                System.out.println(node.toString());
            }

        }
    }
}
