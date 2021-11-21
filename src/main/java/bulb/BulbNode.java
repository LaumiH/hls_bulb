package bulb;

import java.util.List;
import scheduler.Schedule;

public class BulbNode {

    private final BulbNode parent;
    private final List<BulbNode> children;
    private final Schedule schedule;
    private int u_bound;
    private int l_bound;

    public BulbNode(BulbNode parent, List<BulbNode> children, Schedule schedule) {
        this.parent = parent;
        this.children = children;
        this.schedule = schedule;
    }
}
