package bulb;

import scheduler.Schedule;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BulbNode {

    private BulbNode parent;
    private HashSet<BulbNode> children;
    private Schedule schedule;
    private int u_bound;
    private int l_bound;
    private boolean valid;

    public BulbNode(HashSet<BulbNode> children, Schedule schedule, int l_bound, int u_bound) {
        this.children = children;
        this.schedule = schedule;
        this.u_bound = u_bound;
        this.l_bound = l_bound;
        this.valid = false;
    }

    public HashSet<BulbNode> predecessors() {
        HashSet<BulbNode> predecessors = new HashSet<>();
        BulbNode parent = (this.parent);
        while (parent.getParent() != null) {
            predecessors.add(parent);
            parent = parent.getParent();
        }
        return predecessors;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
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

    public String toString() {
        StringBuilder buffer = new StringBuilder(50);
        print(buffer, "", "");
        return buffer.toString();
    }

    private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
        buffer.append(prefix);
        if (valid) {
            buffer.append("l_bound: ").append(l_bound).append(", ");
            buffer.append("u_bound: ").append(u_bound).append("; ");
            buffer.append("Investigated partial schedule: ").append(this.schedule.getSlots()).append("; ");
        } else {
            buffer.append("invalid").append("; ");
        }
        buffer.append('\n');
        for (Iterator<BulbNode> it = children.iterator(); it.hasNext(); ) {
            BulbNode next = it.next();
            if (it.hasNext()) {
                next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
            } else {
                next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
            }
        }
    }
}
