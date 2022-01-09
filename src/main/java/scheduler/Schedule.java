package scheduler;

import bulb.BULBException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * This class represents a single schedule.
 *
 * @author ruschke
 */
public class Schedule {

    /**
     * Map of resource types and a sorted mapping of resources and the time steps they are used in
     */
    private final Map<ResourceType, TreeMap<Resource, Integer>> sort_res;
    /**
     * Map of resource types and a sorted mapping of the time steps and resource that is used
     */
    private final Map<ResourceType, Map<Integer, Resource>> tsort_res;
    /**
     * Map of nodes and the real resource used for this node
     */
    private final Map<Node, String> resources = new HashMap<>();
    /**
     * Map of nodes and their intervals in the schedule
     */
    private Map<Node, Interval> nodes;
    /**
     * Map of time steps and a set of the currently scheduled nodes
     */
    private Map<Integer, Set<Node>> slots;

    public Schedule() {
        nodes = new HashMap<>();
        slots = new TreeMap<>();
        sort_res = new HashMap<>();
        tsort_res = new HashMap<>();
    }

    public Map<Node, String> getResources() {
        return this.resources;
    }

    public Map<Integer, Set<Node>> getSlots() {
        return slots;
    }

    /**
     * Add a node to the schedule during the given interval
     *
     * @param node     - the node to be scheduled
     * @param interval - the interval the node will be scheduled in
     */
    public void add(Node node, Interval interval) {
        if (nodes.containsKey(node)) remove(node);

        nodes.put(node, interval);

        for (int ii = interval.lbound; ii <= interval.ubound; ii++) {
            // get all nodes in the slot
            Set<Node> nodesInSlot = slots.get(ii);
            if (nodesInSlot == null) nodesInSlot = new HashSet<>();
            nodesInSlot.add(node);
            slots.put(ii, nodesInSlot);

            {
                TreeMap<Resource, Integer> rmm =
                        sort_res.computeIfAbsent(node.getResourceType(), k -> new TreeMap<>());
                Map<Integer, Resource> rtm =
                        tsort_res.computeIfAbsent(node.getResourceType(), k -> new HashMap<>());
                // get resource used in time slot ii
                Resource rss = rtm.get(ii);
                if (rss == null) {
                    rss = new Resource(node.getResourceType(), ii);
                    rss.inc();
                    rmm.put(rss, ii);
                    rtm.put(ii, rss);
                } else {
                    rmm.remove(rss);
                    rss.inc();
                    rmm.put(rss, ii);
                }
            }
        }
    }

    /**
     * Add a node to the schedule during the given interval and using the given real resource
     *
     * @param nd       - node to be scheduled
     * @param i        - interval to schedule it in
     * @param resource - resource to be used for the node
     */
    public void add(Node nd, Interval i, String resource) {
        resources.put(nd, resource);
        add(nd, i);
    }

    /**
     * Remove a node from the schedule
     *
     * @param nd - node to be removed
     */
    public void remove(Node nd) {
        Interval i = nodes.get(nd);

        nodes.remove(nd);
        resources.remove(nd);

        if (i == null) return;

        for (int ii = i.lbound; ii <= i.ubound; ii++) {
            slots.get(ii).remove(nd);

            {
                Map<Integer, Resource> rtm = tsort_res.get(nd.getResourceType());
                if (rtm == null) continue;
                Resource r = rtm.get(ii);
                if (r == null) continue;
                sort_res.get(nd.getResourceType()).remove(r);
                r.dec();
                sort_res.get(nd.getResourceType()).put(r, ii);
            }
        }
    }

    /**
     * Calculate this schedules cost
     *
     * @return cost of this schedule
     */
    public Double cost() {
        Double c = 0.0;
        for (ResourceType rt : sort_res.keySet()) c += sort_res.get(rt).firstKey().weight();

        return c;
    }

    /**
     * @return a map of resource types and their cost in the schedule
     */
    public Map<ResourceType, Double> costPerResource() {
        Map<ResourceType, Double> cm = new TreeMap<>();
        Double w;
        for (ResourceType rt : sort_res.keySet()) {
            w = sort_res.get(rt).firstKey().weight();
            if (w.compareTo(0.0) > 0) cm.put(rt, w);
        }
        return cm;
    }

    /**
     * Shift the schedule by the given amount. I.e. add the given amount to all intervals.
     *
     * @param shift the amount to shift the schedule by
     * @return this schedule after shifting
     */
    public Schedule shift(Integer shift) {
        TreeMap<Integer, Set<Node>> ns = new TreeMap<>();
        HashMap<Node, Interval> nds = new HashMap<>();
        for (Node nd : nodes.keySet()) nds.put(nd, nodes.get(nd).shift(shift));

        for (ResourceType rt : tsort_res.keySet()) {
            Map<Integer, Resource> rm = tsort_res.get(rt);
            Map<Integer, Resource> nrm = new HashMap<>();
            for (Integer i : rm.keySet()) nrm.put(i + shift, rm.get(i));
            tsort_res.put(rt, nrm);

            TreeMap<Resource, Integer> rtm = sort_res.get(rt);
            TreeMap<Resource, Integer> nrtm = new TreeMap<>();

            Set<Resource> rset;

            if (shift > 0) rset = rtm.keySet();
            else rset = rtm.descendingKeySet();

            for (Resource r : rset) {
                r.step(rtm.get(r) + shift);
                nrtm.put(r, rtm.get(r) + shift);
            }
            sort_res.put(rt, nrtm);
        }
        for (Integer i : slots.keySet()) {
            ns.put(i + shift, slots.get(i));
        }

        nodes = nds;
        slots = ns;
        return this;
    }

    /**
     * Get the slot (interval) for the given node
     *
     * @param nd - the node of interest
     * @return the interval it is scheduled in
     */
    public Interval slot(Node nd) {
        return nodes.get(nd);
    }

    /**
     * Get the nodes currently scheduled in the given time step. Note that if a node is started at an
     * earlier time step but overlaps the given, such a node is also part of the returned set.
     *
     * @param slot - time step of interest
     * @return set of nodes scheduled at the given time
     */
    public Set<Node> nodes(int slot) {
        return slots.get(slot);
    }

    /**
     * @return a set of all currently scheduled nodes
     */
    public Set<Node> nodes() {
        return nodes.keySet();
    }

    public List<Node> orderNodes(String order) {
        List<Node> nodesOrder = new ArrayList<>(size());

        for (Map.Entry<Integer, Set<Node>> entry : slots.entrySet()) {
            //operations longer than 1 clock cycle are in multiple slots
            //therefore only add the first occurrence
            for (Node node : entry.getValue()) {
                if (!nodesOrder.contains(node)) nodesOrder.add(node);
            }
        }
        if (order.equals("desc")) Collections.reverse(nodesOrder);
        return nodesOrder;
    }

    @Override
    public Schedule clone() {
        Schedule sched = new Schedule();

        for (Node nd : nodes.keySet()) {
            sched.add(nd, new Interval(nodes.get(nd).lbound, nodes.get(nd).ubound));
        }

        sched.getResources().clear();
        sched.getResources().putAll(this.getResources());

        return sched;
    }

    /**
     * Calculate the length of the given schedule. I.e. the span from the first to the last scheduled
     * node.
     *
     * @return this schedule's length
     */
    public int length() {
        int min, max;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
        for (Integer ii : slots.keySet()) {
            if (ii < min) min = ii;
            if (ii > max) max = ii;
        }
        return 1 + max - min;
    }

    /**
     * Find the earliest lower bound of a node. Commonly this is a non-negative number.
     *
     * @return the earliest lower bound of the currently scheduled nodes
     */
    public int min() {
        int min;
        min = Integer.MAX_VALUE;
        for (Integer ii : slots.keySet()) {
            if (ii < min) min = ii;
        }
        return min;
    }

    /**
     * Find the latest upper bound of a node. Commonly this is a non-negative number.
     *
     * @return the latest upper bound of the currently scheduled nodes
     */
    public int max() {
        int max;
        max = Integer.MIN_VALUE;
        for (Integer ii : slots.keySet()) {
            if (ii > max) max = ii;
        }
        return max;
    }

    /**
     * Check the schedule for simple conflicts. If any node overlaps its successors the first
     * conflicting node is returned.
     *
     * @return null iff the schedule has no illegal overlaps, a conflicting node otherwise
     */
    public Node validate(ResourceConstraint resourceConstraint, int nrOfNodes) throws BULBException {
        //System.out.println("########\nValidating schedule\n########");
        for (Node node : nodes.keySet()) {
            for (Node successor : node.successors()) {
                if (slot(successor) == null) continue;
                if (slot(node).ubound.compareTo(slot(successor).lbound) >= 0) {
                    //dependency violated
                    return node;
                }
            }
        }
        if (nodes().size() > nrOfNodes) {
            throw new BULBException("There are more nodes scheduled than in the source graph");
        }
        if (resourceConstraint != null) {
            if (resources.keySet().size() != nodes().size()) {
                throw new BULBException("Resource size does not match nodes size of schedule!");
            }
            Set<String> resNames = resourceConstraint.getAllRes().keySet();
            List<String> resUsed = new ArrayList<>();
            for (int step = 0; step < this.slots.size(); step++) {
                Set<Node> nodesInStep = this.slots.get(step);
                if (nodesInStep == null) {
                    continue;
                }

                for (Node n : nodesInStep) {
                    String resName = resources.get(n);
                    if (null == resName) {
                        throw new BULBException("Node is scheduled in step, but does not have resource!");
                    }
                    if (resUsed.contains(resName)) {
                        throw new BULBException("Resource " + resName + " is allocated twice in step " + step);
                    }
                    resUsed.add(resources.get(n));
                }
                if (resNames.size() < resUsed.size()) {
                    throw new BULBException("resNames.size() < resUsed.size()");
                }
                resUsed.clear();
            }
        }
        return null;
    }

    /**
     * @return number of currently scheduled nodes
     */
    public Integer size() {
        return nodes.keySet().size();
    }

    /**
     * @return a string with a textual representation of the schedule and the resources
     */
    public String diagnose(ResourceConstraint resourceConstraint, int nrOfNodes) throws BULBException {
        if (nodes.keySet().size() <= 0) return "%n";

        Formatter f = new Formatter();
        f.format("Found schedule of length %d with %d nodes%n%n", length(), size());
        Set<Node> os = new HashSet<>();
        for (Integer ii : slots.keySet())
            for (Node nd : slots.get(ii)) {
                if (os.contains(nd)) continue;
                os.add(nd);
                f.format("%s : %s%n", nd, nodes.get(nd));
            }

        /*
        f.format("%nRegistered resources%n");
        for (ResourceType rt : sort_res.keySet()) {
          for (Resource r : sort_res.get(rt).keySet())
            f.format(" %s %s %s %n", rt, r.step(), r.weight());
        }
        */

        String str = f.toString();
        f.close();

        validate(resourceConstraint, nrOfNodes);

        return str;
    }

    /**
     * Write a dot-file of the schedule. If a resource is specified for each node each column of the
     * schedule represents one resource.
     *
     * @param dotFileName - the file to be written
     */
    public void draw(String dotFileName) {
        try {
            BufferedWriter dotFile = new BufferedWriter(new FileWriter(dotFileName));
            int scaleY = 2;
            int scaleX = 2;
            int maxY = length() * scaleY;

            int X;
            int Y;

            int min = min();

            dotFile.write("//do not use DOT to generate pdf use NEATO or FDP\n");
            dotFile.write("digraph{\n");
            dotFile.write("layout=\"neato\";\n");
            dotFile.write("splines=\"ortho\";\n");

            int maxNodes = 0;

            for (int i = 0; i <= max(); i++) {
                if (nodes(i) != null) maxNodes = Math.max(nodes(i).size(), maxNodes);
            }

            boolean allResourcesGiven = true;
            for (Node n : nodes()) {
                if (!resources.containsKey(n)) {
                    allResourcesGiven = false;
                    break;
                }
            }

            int[] slots = new int[maxNodes];

            Map<String, Integer> peSlots = new HashMap<>();
            if (allResourcesGiven) {
                int x = 0;
                for (String s : resources.values()) {
                    peSlots.put(s, x++);
                }
            } else {
                for (int i = 0; i < maxNodes; i++) {
                    slots[i] = 0;
                }
            }

            for (int i = 0; i <= max(); i++) {
                Y = maxY - i * scaleY;
                X = 0;

                if (!allResourcesGiven) {
                    for (int j = 0; j < slots.length; j++) {
                        if (slots[j] > 0) {
                            slots[j]--;
                        }
                    }
                }

                if (nodes(i) != null)
                    for (Node n : nodes(i)) {

                        if (i == min || (nodes(i - 1) != null && !nodes(i - 1).contains(n))) {

                            int slot = 0;

                            if (allResourcesGiven) {
                                slot = peSlots.get(resources.get(n));
                            } else {
                                while (slots[slot] > 0) {
                                    slot++;
                                }
                                slots[slot] += n.getDelay();
                            }
                            X = slot * scaleX;

                            int nodeHeight = n.getDelay() * scaleY - 1;
                            int nodeY = Y - nodeHeight / 2;
                            int nodeWidth = 1;
                            dotFile.write(
                                    n
                                            + "[shape=\"ellipse\", style=\"filled\", color=\"#004E8ABF\", pos=\""
                                            + X
                                            + ","
                                            + nodeY
                                            + "!\", height=\""
                                            + nodeHeight
                                            + "\", width=\""
                                            + nodeWidth
                                            + "\"];\n");
                            for (Node suc : n.successors()) {
                                dotFile.write(n + " -> " + suc + ";\n");
                            }
                        }
                    }
            }

            dotFile.write("}");
            dotFile.flush();
            dotFile.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean compare(Schedule cmpSched) {

        if (cmpSched == null || cmpSched.size() == 0) {
            System.out.println("Comparison Schedule is null or has no nodes!");
            return false;
        }
        if (this.length() != cmpSched.length()) {
            System.out.println("Length is not equal, so Schedules can't be equal");
            return false;
        }

        Map<Integer, Set<Node>> slots1 = this.slots;
        Map<Integer, Set<Node>> slots2 = cmpSched.slots;
        //System.out.println("this map " + slots1);
        //System.out.println("cmp map " + slots2);

        int length = this.length();
        Set<Node> cmp, cmp2;
        boolean foundSameNode = false;
        for (int i = 0; i < length; i++) {
            cmp = slots1.get(i);
            cmp2 = slots2.get(i);
            //System.out.println("Investigating Slot : " + i);
            if (cmp.size() != cmp2.size()) {
                return false;
            }
            for (Node nd : cmp) {
                foundSameNode = false;
                for (Node ndcp : cmp2) {
                    //System.out.println("Comparing " + ndcp + " to " + nd);
                    if (nd.equals(ndcp)) {
                        //System.out.println("Same Node found");
                        foundSameNode = true;
                        break;
                    }
                }
                if (!foundSameNode) {
                    return false;
                }
            }
        }
        System.out.println("Schedules are equal");
        return true;
    }
}
