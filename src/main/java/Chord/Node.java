package Chord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;

public class Node {
    private int keySize;
    private String id;
    private final SortedMap<String, Node> nodes;
    private String ip;
    private String publicIp;
    private Node predecessor;
    private Node successor;
    private final Thread updateThread;

    public Node() {

        nodes = Collections.synchronizedSortedMap(new TreeMap<String, Node>(Comparator.naturalOrder()));
        try {
            id = hash(getKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
            publicIp = publicIp();
        } catch (Exception e) {
            e.printStackTrace();
        }


        Updater updater = new Updater(this);
        updateThread = new Thread(updater);
        updateThread.setName("Node " + id);

        System.out.println("Node with ID " + id + " is created");

    }

    public static boolean isBigger(Node n1, Node n2) {
        return n1.getId().compareTo(n2.getId()) > 0;
    }

    public static boolean isBigger(String id1, String id2) {
        return id1.compareTo(id2) > 0;
    }

    public Node getPredecessor() {
        return predecessor;
    }

    public Node getSuccessor() {
        return successor;
    }


    public String getId() {
        return id;
    }

    public void addNode(Node node) {
        System.out.println("Node " + node.getId() + "was added to node " + id);
        nodes.put(node.getId(), node);
    }

    public boolean hasNeighbours() {
        return !nodes.isEmpty();
    }

    private PublicKey getKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        return keyGen.generateKeyPair().getPublic();

    }

    private String hash(byte[] msg) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        this.keySize = digest.getDigestLength() * 8;
        byte[] hashBytes = digest.digest(msg);
        return Util.byteToHex(new BigInteger(hashBytes));

    }
/*
    checks if candidate node is our predecessor and changes the current predecessor to the candidate if it is the case.
    Called by candidate node
 */

    public void join(Node successor) {
        System.out.println("Node with ID " + id + "started JOIN to successor " + successor.getId());
        addNode(successor);
        this.successor = successor;
        successor.joinNotify(this);
        checkConnections();

        updateThread.start();
    }

    public void join() {
        System.out.println("Node with ID " + id + "started JOIN");
        updateThread.start();
    }

    /*
    verifies current successor if it was changed then tells the successor about its new predecessor (us)
    Called periodically
     */
    public boolean stabilize() {
        System.out.println("Node " + id + " started stabilization");
        if (hasNeighbours()) {
            final Node x = successor.getPredecessor();
            if (x == null) {

                successor.joinNotify(this);
                return false;
            }


            if (!this.equals(x)) {


                if (isBigger(x, this) && isBigger(successor, x)) { // Normal Case
                    executeChanges(x);
                } else if (isBigger(this, successor)) { //verifies than this node is current max
                    // MIN or MAX case
                    if (isBigger(successor, x) /*then x is the new min */ ||
                            isBigger(x, this) /*then this node is max */) {
                        executeChanges(x);
                    }
                }
            }
        }

        return true;

    }

    private void executeChanges(Node x) {
        System.out.println("Node " + x.getId() + " is successor of node " + id);
        nodes.remove(successor.getId());
        addNode(x);
        this.successor = x;
        x.joinNotify(this);
    }

    public void joinNotify(Node candidateNode) {
        System.out.println("Node " + id + "received joinNotify from " + candidateNode.getId());

        if (predecessor == null ||
                (isBigger(candidateNode, predecessor)) && isBigger(this, candidateNode) ||
                (isBigger(predecessor, this) && (isBigger(this, candidateNode) || isBigger(candidateNode, predecessor)))) {
            System.out.println("The new predecessor of node " + id + "is node " + candidateNode.getId());
            predecessor = candidateNode;
        }

        if (!hasNeighbours()) {
            addNode(candidateNode);
            successor = candidateNode;
            candidateNode.joinNotify(this);
        }
    }

    public void checkConnections() {
        System.out.println("Node " + id + " started checking connections");
        BigInteger id = Util.hexToInt(this.id);
        String hex;
        HashMap<String, Node> temp = new HashMap<>();
        for (int i = 0; i < keySize; i++) {
            BigInteger base = BigInteger.valueOf(2);
            BigInteger offset = base.pow(i);
            BigInteger candidateId = id.add(offset);
            BigInteger maxId = base.pow(keySize);
            BigInteger result = candidateId.mod(maxId);
            hex = Util.byteToHex(result);
//            System.out.println("Node " + id + " looking for node " + hex + " with initial int " + result);
            Node candidate = lookUp(hex);
            if (candidate == null) {
                System.err.println("Lookup error");
                break;
            }

            if (!candidate.equals(this)) {
                temp.put(candidate.getId(), candidate);
            }
            if (i == 0 && !successor.getId().equals(candidate.getId())) {
                successor = candidate;
                successor.joinNotify(this);
            }
        }

        nodes.clear();
        nodes.putAll(temp);


    }


    private Node findHighestPredecessor(String id) {
        final int size = nodes.keySet().size();
        final String[] arr = new String[size];
        nodes.keySet().toArray(arr);
        int resId = binarySearch(arr, 0, size - 1, id);
        if (resId < 0) { // < 0 if ID either bigger or smaller than every node in the finger table
            /*
            If successor of current node is bigger then we go to the maximum node in the finger table
            If successor of current node is less, it means that current node is the maximum node and successor of current node is the minimum node.
             */
            if (isBigger(this.getSuccessor(), this)) {
                // if current node is not max or min then go to the max node in the finger table.
                return nodes.get(arr[size - 1]);
            }

            // if the current node is the max node and ID is not new MAX or MIN then go to he highest node in the finger table
            if (isBigger(this.getId(), id) && isBigger(id, this.getSuccessor().getId())) {
                return nodes.get(arr[size - 1]);
            }

            // if the current node is the max node and ID is the new MIN or MAX node; then return current node.
            return this;
        }
        return nodes.get(arr[resId]);


    }

    private String publicIp() throws Exception {
        URL whatIsMyIp = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatIsMyIp.openStream()));
            return in.readLine();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int binarySearch(String[] arr, int l, int r, String x) {
//        System.out.println("Highest was chosen from");

        if (r >= l) {
            int mid = l + (r - l) / 2;
            if (mid != arr.length - 1) {
                if (isBigger(x, arr[mid]) && isBigger(arr[mid + 1], x)) {
                    return mid;
                }


                if (isBigger(arr[mid], x))
                    return binarySearch(arr, l, mid - 1, x);

                return binarySearch(arr, mid + 1, r, x);
            }

        }

        return -1;
    }


    public synchronized Node lookUp(String id) {
        //System.out.println("Node " + this.id + " is looking for node " + id);
        try {
//            System.out.println(this.id + " START LOOKUP id " + id);
            if (isBigger(id, this.getId()) && (isBigger(getSuccessor().id, id)) || this.id.equals(id)) {
//            System.out.println("node " + this.id + "returns its successor" + getSuccessor().getId());
                return getSuccessor();
            } else {
                final Node highestPredecessor = findHighestPredecessor(id);
//                System.out.println("Highest predecessor is node " + highestPredecessor.getId());
//            System.out.println("highest successor is " + highestPredecessor.getId());
                //if highestPredecessor is the MAX node
                if (highestPredecessor.equals(this)) {
//                System.out.println("node " + this.id + " is a MAX node, returning its successor - min node " + highestPredecessor.getSuccessor().getId());
                    //if ID is either new MIN or MAX node then return current MIN node as ID's successor
                    if (isBigger(id, this.id) || isBigger(getSuccessor().getId(), id)) {
                        return highestPredecessor.getSuccessor();
                    }
                    /* if we are here, then current node is a MAX node which has only one connection in its finger table(successor only),
                     and ID is a NORMAL node; then we go to current node's successor;
                     */
                    return highestPredecessor.getSuccessor().lookUp(id);
                }
                //if highest predecessor is the normal node then continues look up.
//            System.out.println("node " + this.getId() + "sending lookup of node " + id + " to highest predecessor " + highestPredecessor.getId());
                return highestPredecessor.lookUp(id);
            }
        } catch (Exception e) {
            System.err.println("Lookup not found");
            System.err.println(e.getLocalizedMessage());
            System.err.println(e.toString());
            return null;
        }

    }

    @Override
    public boolean equals(Object obj) {
        Node n = (Node) obj;
        return n.getId().compareTo(this.getId()) == 0;
    }

}
