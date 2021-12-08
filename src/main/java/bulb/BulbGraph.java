package bulb;

import java.util.Set;

public class BulbGraph {

    private final Set<BulbNode> nodes;

    private int inspectedNodes = 0;

    public BulbGraph(Set<BulbNode> nodes) {
        this.nodes = nodes;
    }

    public void addNode(BulbNode parent, BulbNode node) {
        if (parent != null) parent.getChildren().add(node);
        node.setParent(parent);
        inspectedNodes++;
        nodes.add(node);
    }
}
