package Chord;

public class ChordController {
	private final Server server;

	public ChordController(Server server) {
		this.server = server;
	}

	public void join(Node node) {
		final Node lastNode = server.getLastNode();


		if (lastNode != null) {
			final String nodeId = node.getId();

			if (lastNode.hasNeighbours()) {
				Node successorNode = lastNode.lookUp(nodeId);
				node.join(successorNode);
				server.setLastNode(node);
			} else {
				node.join(lastNode);
				server.setLastNode(node);

			}

		} else {
            node.join();
            server.setLastNode(node);
        }

    }

}
