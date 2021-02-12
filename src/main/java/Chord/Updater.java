package Chord;

public class Updater implements Runnable {


    private Node node;

    public Updater(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        while (true){
            try {
                System.out.println("Runner Started");
                Thread.sleep(10000);
                System.out.println("Runner run!");
                node.checkConnections();
                if(!node.stabilize()){
                    node.stabilize();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
