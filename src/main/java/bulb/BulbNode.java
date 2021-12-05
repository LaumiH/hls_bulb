package bulb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import scheduler.Schedule;

public class BulbNode {

    private BulbNode parent;
    private HashSet<BulbNode> children;
    private Schedule schedule;
    private int u_bound;
    private int l_bound;

    public BulbNode(HashSet<BulbNode> children, Schedule schedule, int u_bound,
                    int l_bound) {
        this.children = children;
        this.schedule = schedule;
        this.u_bound = u_bound;
        this.l_bound = l_bound;
    }

    public HashSet<BulbNode> predecessors() {
        HashSet<BulbNode> predecessors = new HashSet<>();
        BulbNode parent = (this.parent);
        while(parent.getParent() != null) {
            predecessors.add(parent);
            parent = parent.getParent();
        }
        return predecessors;
    }

    public BulbNode getParent() {
        return parent;
    }

    public void setParent(BulbNode parent) {
        this.parent = parent;
    }

    public Set<BulbNode> getChildren() {
        return children;
    }

    public void setChildren(HashSet<BulbNode> children) {
        this.children = children;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
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
