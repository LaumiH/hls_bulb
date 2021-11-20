package scheduler;

import java.util.HashMap;
import java.util.Map;

public class ALAP extends Scheduler {

    /**
     * Maximum schedule length
     */
    private final int lmax;

    public ALAP() {
        lmax = 0;
    }
    public ALAP(int lmax) {
        this.lmax = lmax-1;
    }

    public Schedule schedule(final Graph sg) {
        Map<Node, Interval> queue = new HashMap<>();
        Map<Node, Interval> qq;
        Map<Node, Interval> min_queue = new HashMap<>();
        Schedule schedule = new Schedule();
        int min = lmax;

        for (Node nd : sg)
            if (nd.isLeaf())
                queue.put(nd, new Interval(lmax + 1 - nd.getDelay(), lmax));
        if(queue.size() == 0)
            System.out.println("No leaf in Graph found. Empty or cyclic graph");


        while (queue.size() > 0) {
            qq = new HashMap<>();

            for (Node nd : queue.keySet()) {
                Interval slot = queue.get(nd);
                if (slot.lbound < min)
                    min = slot.lbound;

                schedule.add(nd, slot);
                for (Node l : nd.predecessors()) {
                    sg.handle(l, nd);
                    Interval ii = min_queue.get(l);
                    if (ii == null || slot.lbound <= ii.ubound) {
                        ii = new Interval(slot.lbound-l.getDelay(), slot.lbound-1);
                        min_queue.put(l, ii);
                    }
                    if (!l.bottom())
                        continue;
                    if ((queue.get(l) == null)) {
                        if (qq.get(l) == null) {
                            qq.put(l, ii);
                        } else if (qq.get(l).ubound >= slot.lbound) {
                            qq.put(l, ii);
                        }
                    } else if (queue.get(l).ubound >= slot.lbound) {
                        qq.put(l, ii);
                    }
                }
            }
            queue = qq;
        }
        sg.reset();

        if (lmax == 0)
            return schedule.shift(-(min));
        return schedule;
    }
}
