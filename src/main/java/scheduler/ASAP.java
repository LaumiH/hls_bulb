package scheduler;

import java.util.HashMap;
import java.util.Map;

public class ASAP extends Scheduler {

    public Schedule schedule(final Graph sourceGraph) {
        Map<Node, Interval> queue = new HashMap<>();
        Map<Node, Interval> qq;
        Map<Node, Interval> minslot = new HashMap<>();
        Schedule schedule = new Schedule();

        Graph g = sourceGraph;

        for (Node node : g) {
            if (node.isRoot())
                // interval is as long as resource type duration
                queue.put(node, new Interval(0, node.getDelay() - 1));
        }
        if(queue.size() == 0)
            System.out.println("No root in Graph found. Empty or cyclic graph");

        while (queue.size() > 0) {
            qq = new HashMap<>();

            for (Node node : queue.keySet()) {
                Interval slot = queue.get(node);
                schedule.add(node, slot);

                for (Node successor : node.successors()) {
                    g.handle(node, successor);

                    Interval ii = minslot.get(successor);
                    if (ii == null)
                        minslot.put(successor, new Interval(slot.ubound + 1,
                            slot.ubound + successor.getDelay()));
                    else if (ii.lbound.compareTo(slot.ubound) <= 0)
                        minslot.put(successor, new Interval(slot.ubound + 1,
                            slot.ubound + successor.getDelay()));

                    if (!successor.top())
                        continue;
                    ii = minslot.get(successor);
                    if ((queue.get(successor) == null)) {
                        if (qq.get(successor) == null)
                            qq.put(successor, ii);
                        else if (qq.get(successor).lbound <= slot.ubound)
                            qq.put(successor, ii);
                    } else if (queue.get(successor).lbound <= slot.ubound)
                        qq.put(successor, ii);
                }
            }
            queue = qq;
        }
        g.reset();

        return schedule;
    }
}
