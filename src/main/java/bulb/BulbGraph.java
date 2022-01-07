package bulb;

import java.util.List;
import java.util.Set;

public class BulbGraph {

    private final List<BulbNode> nodes;

    private BulbNode root;

    private int inspectedNodes = 0;

    private boolean lowerEqualsUpperReached = false;

    private String parameters;

    public BulbGraph(List<BulbNode> nodes) {
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
    }

    public void incrementInspected() {
        this.inspectedNodes++;
    }

    public void print() {
        System.out.printf("BULB tree contains %d nodes%n", this.inspectedNodes);
        System.out.printf("%d tried combinations violated resource constraints%n",
                this.nodes.size() - this.inspectedNodes);
        System.out.println("Lower == Upper reached: " + lowerEqualsUpperReached);
        System.out.println("Parameters: " + parameters);
        System.out.println("Print BULB tree:");
        System.out.println(this.root.toString());
    }

    public boolean isLowerEqualsUpperReached() {
        return lowerEqualsUpperReached;
    }

    public void setLowerEqualsUpperReached(boolean lowerEqualsUpperReached) {
        this.lowerEqualsUpperReached = lowerEqualsUpperReached;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}
