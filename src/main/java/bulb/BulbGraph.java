package bulb;

import java.util.Set;

public class BulbGraph {

    private final Set<BulbNode> nodes;

    private BulbNode root;

    private int inspectedNodes = 0;

    public BulbGraph(Set<BulbNode> nodes) {
        this.nodes = nodes;
    }

    public void addNode(BulbNode parent, BulbNode node) {
        if (parent == null) {
            this.root = node;
        } else {
            parent.getChildren().add(node);
        }
        node.setParent(parent);
        nodes.add(node);
        incrementInspected();
    }

    public void incrementInspected() {
        this.inspectedNodes++;
    }

    public void print() {
        System.out.printf("BULB tree contains %d nodes, %d nodes were inspected.%n", this.nodes.size(), this.inspectedNodes);
        System.out.println("Print BULB tree:");
        System.out.println(this.root.toString());
    }
}
