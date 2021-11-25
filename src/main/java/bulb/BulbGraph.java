package bulb;

import java.util.ArrayList;
import java.util.Set;
import scheduler.Schedule;

public class BulbGraph {

    private final Set<BulbNode> nodes;

    private int inspectedNodes = 0;

    public BulbGraph(Set<BulbNode> nodes) {
        this.nodes = nodes;
    }

    public void addNode(BulbNode parent, Schedule schedule, int u_bound, int l_bound) {
        BulbNode newNode = new BulbNode(parent, new ArrayList<>(), schedule, u_bound, l_bound);
        parent.getChildren().add(newNode);
        inspectedNodes++;
        nodes.add(newNode);
    }
}
