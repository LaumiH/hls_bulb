package bulb;

import java.util.List;
import scheduler.Schedule;

public class BulbNode {

    private final BulbNode parent;
    private final List<BulbNode> children;
    private final Schedule schedule;
    private int u_bound;
    private int l_bound;

    public BulbNode(BulbNode parent, List<BulbNode> children, Schedule schedule, int u_bound,
        int l_bound) {
        this.parent = parent;
        this.children = children;
        this.schedule = schedule;
        this.u_bound = u_bound;
        this.l_bound = l_bound;
    }

    public BulbNode getParent() {
        return parent;
    }

    public List<BulbNode> getChildren() {
        return children;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public int getU_bound() {
        return u_bound;
    }

    public void setU_bound(int u_bound) {
        this.u_bound = u_bound;
    }

    public int getL_bound() {
        return l_bound;
    }

    public void setL_bound(int l_bound) {
        this.l_bound = l_bound;
    }
}
