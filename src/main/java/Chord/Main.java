package Chord;

        import java.math.BigInteger;
        import java.util.LinkedList;
        import java.util.ListIterator;
        import java.util.Scanner;

public class Main {


    public static void main(String[] args) {
        final int numNodes = 36;
        LinkedList<Node> nodes = new LinkedList<>();
        ChordController controller = new ChordController(new Server());

        Node a = new Node();
        Node b = new Node();

        System.out.println(a.getIp());
        System.out.println(a.getPublicIp());

        Scanner scanner = new Scanner(System.in);

        while(true){
            String arg = scanner.nextLine();
            if(arg.equals("add")){
                Node node = new Node();
                controller.join(node);
            }
        }





    }
}
