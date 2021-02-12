package Chord;

public class ChordController {
    private Server server;

    public ChordController(Server server) {
        this.server = server;
    }

    public boolean join(Node node) {
        final Node lastNode = server.getLastNode();


        if (lastNode != null) {
            final String nodeId = node.getId();
            final String lastNodeId = lastNode.getId();

            if (lastNode.hasNeighbours()) {
                Node successorNode = lastNode.lookUp(nodeId);
                node.join(successorNode);
                server.setLastNode(node);
            } else {
                node.join(lastNode);
                server.setLastNode(node);
//                System.out.println(nodeId.compareTo(lastNodeId));
//                if (Node.isBigger(node, lastNode)) {
//                    node.setMax(true);
//                    lastNode.setMin(true);
//                }else {
//                    node.setMin(true);
//                    lastNode.setMax(true);
//                }
                return true;
            }

        } else {
            node.join();
            server.setLastNode(node);
            return true;
        }

        return false;
    }

//    public Node lookUp(Node node , String id){
//
//    }
}
