package scheduler;

import java.net.SocketOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ALAP extends Scheduler {

    /**
     * Maximum schedule length
     */
    private final int lmax;
    private final String mode;
    public ALAP() {
        lmax = 0;
        mode = "l";
    }

    public ALAP(int lmax,String mode) {
        this.mode = mode;
        this.lmax = lmax - 1;
    }

    public Schedule schedule(final Graph sg) {
        boolean lazyAlap ;
        Map<Node, Interval> queue = new HashMap<>();
        Map<Node, Interval> qq;
        Map<Node, Interval> min_queue = new HashMap<>();
        Schedule schedule = new Schedule();
        int min = lmax;
        int i = 0;

        if(mode.toLowerCase().equals("lazy")){
            lazyAlap = true;
        }else{
            lazyAlap = false;
        }
        System.out.println("lazy" + lazyAlap);
        for (Node nd : sg)
            if (nd.isLeaf()) {
                queue.put(nd, new Interval(lmax + 1 - nd.getDelay() +i, lmax+i ));
                if(lazyAlap){
                    i+= nd.getDelay();
                }



            }
        if (queue.size() == 0)
            System.out.println("No leaf in Graph found. Empty or cyclic graph");
        System.out.println("queue :"+queue);

        Interval last_slot = null;
        while (queue.size() > 0) {
            qq = new HashMap<>();

            for (Node nd : queue.keySet()) {
                System.out.println("Max of schedule "+schedule.max());
                System.out.println("Node out of Queue set "+nd);
                Interval slot = queue.get(nd);

                System.out.println("Slot :" +slot );
                if (slot.lbound < min)
                    min = slot.lbound;

                schedule.add(nd, slot);
                System.out.println(schedule.diagnose());
                System.out.println("All predecessors of node "+nd);
                System.out.println("min_queue "+min_queue);
                for (Node l : nd.predecessors()) {
                    System.out.println(l);
                    sg.handle(l, nd);
                    Interval ii = min_queue.get(l);

                    if (ii == null || slot.lbound <= ii.ubound) {
                        System.out.println("last_interval " + last_slot);
                        ii = new Interval(slot.lbound - l.getDelay(), slot.lbound - 1);
                        if(last_slot == null || !lazyAlap ){
                            ii = new Interval(slot.lbound - l.getDelay(), slot.lbound - 1);
                            System.out.println();
                        }else{
                            ii = new Interval(last_slot.lbound - l.getDelay(), last_slot.lbound - 1);
                            System.out.println("testIntervall" + new Interval(last_slot.lbound - l.getDelay(), last_slot.ubound - 1));

                        }

                        System.out.println("min queue int " +ii);
                        last_slot = new Interval(ii.lbound,ii.ubound);

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
