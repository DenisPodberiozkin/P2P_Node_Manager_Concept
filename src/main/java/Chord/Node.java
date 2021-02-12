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
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

public class Node {
    //    private boolean isMax;
//    private boolean isMin;
    private int keySize;
    private String id;
    private SortedMap<String, Node> nodes;
    private String ip;
    private String publicIp;
    private Node predecessor;
    private Thread updateThread;

    public Node() {
//        isMax = false;
//        isMax = false;
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

    public void setPredecessor(Node predecessor) {
        this.predecessor = predecessor;
    }

    public Node getSuccessor() {
        return nodes.get(nodes.firstKey());
    }

    public String getPublicIp() {
        return publicIp;
    }

//    public boolean isMax() {
//        return isMax;
//    }
//
//    public void setMax(boolean max) {
//        isMax = max;
//    }
//
//    public boolean isMin() {
//        return isMin;
//    }
//
//    public void setMin(boolean min) {
//        isMin = min;
//    }

    public String getIp() {
        return ip;
    }

    public String getId() {
        return id;
    }

    public void addNode(Node node) {
        System.out.println("Node " + node.getId() + "was added to node " + id);
        nodes.put(node.getId(), node);
    }

    public Node getNode(String id) {
        return nodes.get(id);
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
        return Util.byteToHex(hashBytes);

    }
/*
    checks if candidate node is our predecessor and changes the current predecessor to the candidate if it is the case.
    Called by candidate node
 */

    public void join(Node successor) {
        System.out.println("Node with ID " + id + "started JOIN to successor " + successor.getId());
        addNode(successor);
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
            final String firstKey = nodes.firstKey();
            final Node successor = nodes.get(firstKey);
            final Node x = successor.getPredecessor();
            if (x == null) {
                if (isBigger(successor, this)) {
                    successor.joinNotify(this);
                    return false;
                }
                return false;
            }


            if (!this.equals(x)) {



                if (isBigger(x, this) && isBigger(successor, x)) { // Normal Case
                    System.out.println("Node " + x.getId() + " is successor of node " + id);
                    nodes.remove(firstKey);
                    addNode(x);
                    x.joinNotify(this);
                } else if (isBigger(this, successor)) { //verifies than this node is current max
                    // MIN or MAX case
                    if (isBigger(successor, x) /*then x is the new min */ ||
                            isBigger(x, this) /*then this node is max */) {
                        System.out.println("Node " + x.getId() + " is successor of node " + id);
                        nodes.remove(firstKey);
                        addNode(x);
                        x.joinNotify(this);
                    }
                }
            }
        }

        return true;

    }

    public void joinNotify(Node candidateNode) {
        System.out.println("Node " + id + "recieved joinNotify from " + candidateNode.getId());

        if (predecessor == null ||
                (isBigger(candidateNode, predecessor)) && isBigger(this, candidateNode) ||
                (isBigger(predecessor, this) && (isBigger(this, candidateNode) || isBigger(candidateNode, predecessor)))) {
            System.out.println("The new predecessor of node " + id + "is node " + candidateNode.getId());
            predecessor = candidateNode;
        }

        if (!hasNeighbours()) {
            addNode(candidateNode);
            candidateNode.joinNotify(this);
        }
    }

    public void checkConnections() {
        System.out.println("Node " + id + " started checking connections");
//        if (hasNeighbours()) {
        BigInteger id = Util.hexToInt(this.id);
        String hex;
        for (int i = 0; i < keySize; i++) {
            BigInteger base = BigInteger.valueOf(2);
            BigInteger offset = base.pow(i);
            BigInteger candidateId = id.add(offset);
            BigInteger maxId = base.pow(keySize);
            BigInteger result = candidateId.mod(maxId);
            hex = Util.byteToHex(result.toByteArray());
            //System.out.println("Node " + id + " looking for node " + hex);
            Node candidate = lookUp(hex);
            if (candidate == null) {
                System.err.println("Lookup error");
                break;
            }
            if (!candidate.equals(this)) {
                addNode(candidate);
            }
        }
//        }

    }

    public void ckeckPredecessor() {
        //TODO
    }

    private boolean isOtherBigger(String id) {
        return isBigger(id, nodes.lastKey());
    }

    private boolean isOtherSmaller(String id) {
        return !isBigger(id, nodes.firstKey());
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
            Therefore, the ID of the candidate is either the new MAX or MIN node
             */if (isBigger(this.getSuccessor(), this)) {
                // if current node is not max or min then go to the max node in the finger table.
                return nodes.get(arr[size - 1]);
            }
            // if the current node is the max node then return current node.
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
            String ip = in.readLine();
            return ip;
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
        if (r >= l) {
            int mid = l + (r - l) / 2;
            if (mid != r) {
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

    /*
    if looking node exists in the network then it will be returned.
    if looking node does not exist then its predecessor will be returned
     */

//    public Node lookUp(String id){
//
//        if(nodes.containsKey(id)){
//            return nodes.get(id);
//        }
//
//        if(isBigger(id, this.id)){
//            if(isOtherBigger(id)){
//                return this;
//            }
//            return getNode(id, isMax);
//
//        }else {
//            if(isOtherBigger(id)){
//                return nodes.get(nodes.lastKey()).lookUp(id);
//            }
//            return getNode(id, isMin);
//        }
//}
//
//    private Node getNode(String id, boolean isEdge) {
//        if(isOtherSmaller(id)){
//            if(isEdge){
//                return this;
//            }
//            return nodes.get(nodes.lastKey()).lookUp(id);
//        }
//
//        if(isBigger(nodes.firstKey(), id)){
//            return this;
//        }else {
//            return nodes.get(findLastMin(id)).lookUp(id);
//        }
//    }


    public synchronized Node lookUp(String id) {
        //System.out.println("Node " + this.id + " is looking for node " + id);
        try {
            if (isBigger(id, this.getId()) && (isBigger(getSuccessor().id, id)) || this.id.equals(id)) {
//            System.out.println("node " + this.id + "returns its successor" + getSuccessor().getId());
                return getSuccessor();
            } else {
                final Node highestPredecessor = findHighestPredecessor(id);
//            System.out.println("highest successor is " + highestPredecessor.getId());
                //if highestPredecessor is the MAX node then return its successor (min node) as successor of ID
                if (highestPredecessor.equals(this)) {
//                System.out.println("node " + this.id + " is a MAX node, returning its successor - min node " + highestPredecessor.getSuccessor().getId());
                    return highestPredecessor.getSuccessor();
                }
                //if highest predecessor is the normal node then continues look up.
//            System.out.println("node " + this.getId() + "sending lookup of node " + id + " to highest predecessor " + highestPredecessor.getId());
                return highestPredecessor.lookUp(id);
            }
        } catch (Exception e) {
            System.err.println("Lookup not found");
            return null;
        }

    }

    @Override
    public boolean equals(Object obj) {
        Node n = (Node) obj;
        return n.getId().compareTo(this.getId()) == 0;
    }


}
