package bulb;

import java.util.*;

import scheduler.*;

public class BULB extends Scheduler {

    private final ResourceConstraint resourceConstraint;

    private Graph dfg;

    private Map<Node, Interval> asapValues;
    private Map<Node, Interval> alapValues;

    private final Schedule asapSchedule;
    private final Schedule alapSchedule;

    private int bestLatency;
    private Schedule bestSchedule;

    // resource usage: map time stamps to the nr of resources used per type
    private Map<Integer, List<Resource>> resourceUsage;
    // real resources allocated to an operation in a step, must fulfill res constraint
    private Map<Integer, Set<String>> allocation;

    private final BulbGraph bulbGraph;

    public BulbGraph getBulbGraph() {
        return bulbGraph;
    }

    public BULB(final ResourceConstraint rc, final Schedule asap, final Schedule alap) {
        this.resourceConstraint = rc;

        this.asapSchedule = asap;
        this.alapSchedule = alap;

        this.asapValues = new HashMap<>();
        for (Node n : asap.nodes()) {
            asapValues.put(n, asap.slot(n));
        }

        this.alapValues = new HashMap<>();
        for (Node n : alap.nodes()) {
            alapValues.put(n, alap.slot(n));
        }

        this.resourceUsage = new HashMap<>();
        this.allocation = new HashMap<>();
        this.bulbGraph = new BulbGraph(new HashSet<>());
    }

    /**
     * Use the graph given to create a schedule with the BULB algorithm.
     *
     * @param sg - the dependency graph
     * @return a schedule for the given graph
     */
    @Override
    public Schedule schedule(final Graph sg) {
        System.out.println("Start BULB");
        this.dfg = sg;
        List<Node> nodesDFG = this.alapSchedule.orderNodes("asc");

        ListScheduler listScheduler = new ListScheduler();
        Schedule listSchedule = listScheduler.schedule(nodesDFG, new Schedule(), resourceConstraint, allocation);
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " END OF LIST SCHEDULER FROM BULB " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        this.bestLatency = listSchedule.length();
        this.bestSchedule = listSchedule;
        System.out.printf("Best schedule length at beginning: %d%n", bestLatency);

        //add first node with empty schedule
        BulbNode root = new BulbNode(new HashSet<>(), new Schedule(), asapSchedule.length(), bestLatency);
        root.setValid(true);
        bulbGraph.addNode(null, root);

        enumerate(new Schedule(), 0, root, nodesDFG);

        return bestSchedule;
    }

    /**
     * begins with schedule <>
     *
     * @param partial - the (partial) schedule, which is a BulbNode in the BulbGraph
     * @param i       - the current node in the DFG
     */
    private void enumerate(final Schedule partial, int i, BulbNode parent, final List<Node> nodes) {
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " START OF BULB ENUMERATE " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.printf("%nBULB partial%n%s%n", partial.diagnose());

        List<Node> nodesDFG = new ArrayList<>(nodes);

        //get current operation to schedule
        Node currentOperation = nodesDFG.get(i);

        // end recursion condition
        if (i == dfg.size()) {
            if (bestLatency > partial.length()) {
                bestSchedule = partial;
                this.bestLatency = partial.length();
            }
        }

        // check all possible time slots for current operation
        //from lower bound of asap to upper bound of alap
        for (int step = asapSchedule.slot(currentOperation).lbound; step < alapSchedule.slot(currentOperation).ubound; step++) {

            System.out.printf("Current BULB step of operation %s: %d%n", currentOperation, step);

            //do not change partial schedule during loop
            Schedule updatedPartial = partial.clone();
            partial.getResources().putAll(updatedPartial.getResources());

            //get interval for current operation
            Interval duration = new Interval(step, step + currentOperation.getDelay() - 1);

            //add current operation to schedule
            updatedPartial.add(currentOperation, duration);

            //add new node to BULB tree with -1 as bounds
            //allows to later see which options were checked but could not be scheduled
            BulbNode currentBulbNode = new BulbNode(new HashSet<>(), updatedPartial, -1, -1);
            bulbGraph.addNode(parent, currentBulbNode);

            // save asap values for later reset
            Map<Node, Interval> saveAsap = new HashMap<>(asapValues);

            //ResourceUsed(step,res type) < M_type
            boolean canBeScheduled = checkResConstraint(currentOperation.getResourceType(), duration);
            if (canBeScheduled) {
                // operation can be scheduled in this time step following res constraints
                System.out.printf("Operation %s can be scheduled in step %d%n", currentOperation, step);

                // find out free resource name
                String resName = findAllocation(currentOperation.getResourceType(), duration);

                //schedule operation in this interval on the returned real resource
                updatedPartial.remove(currentOperation);
                updatedPartial.add(currentOperation, duration, resName);

                int l_bound = calculateBound("lower", updatedPartial, currentBulbNode, currentOperation, duration, resName, nodesDFG.subList(i + 1, nodesDFG.size()));
                System.out.printf("Lower bound is: %s%n", l_bound);
                int u_bound = calculateBound("upper", updatedPartial, currentBulbNode, currentOperation, duration, resName, nodesDFG.subList(i + 1, nodesDFG.size()));
                System.out.printf("Upper bound is: %s%n", u_bound);

                //update lower and upper bounds in BULB tree
                currentBulbNode.setL_bound(l_bound);
                currentBulbNode.setU_bound(u_bound);
                currentBulbNode.setSchedule(updatedPartial);
                currentBulbNode.setValid(true);

                if (l_bound > u_bound) {
                    System.out.println("Something went terribly wrong, lower bound > upper bound!");
                    System.exit(-1);
                }

                if (u_bound < this.bestLatency) {
                    System.out.printf("Upper bound (%d) is better than best latency (%d)%n", u_bound, bestLatency);
                    this.bestLatency = u_bound;
                    incrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                    this.bestSchedule = upperBoundSchedule(updatedPartial, i, nodesDFG.subList(i + 1, nodesDFG.size()));
                    decrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                }

                //at this point the algo stops if u_bound == l_bound, and we have the best schedule
                if (l_bound < this.bestLatency) {
                    System.out.printf("Lower bound (%d) is still smaller than upper bound (%d)%n", l_bound, u_bound);
                    incrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                    updateAsap(step, i, nodesDFG);
                    enumerate(updatedPartial, i++, currentBulbNode, nodesDFG);
                    decrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                }
                System.out.printf("Done with evaluating step %s because lower bound (%d) == upper bound (%d)%n", step, l_bound, u_bound);
            }
            //done with investigating this step
            asapValues = saveAsap;
        }
    }

    /**
     * Calculates the upper or lower bound for a given graph using list scheduling.
     *
     * @param sched - the partial schedule to be estimated
     * @return the upper bound for needed clock cycles
     */
    private int calculateBound(String kind, Schedule sched, BulbNode currentBulbNode, Node currentOperation, Interval duration, String resName, List<Node> unschedNodes) {
        Map<Integer, List<Resource>> saveResourceUsage = new HashMap<>(this.resourceUsage); //store old resource usage
        Map<Integer, Set<String>> saveAllocation = new HashMap<>(this.allocation); //store old allocation

        //latency estimation based on critical path
        //estimate is the step it will be ready in
        int latencyEstimate = 0;
        for (Node scheduled : sched.nodes()) {
            int Xi = sched.slot(scheduled).ubound;  //current step (ready)
            //int delayCP = criticalPath(scheduled) - scheduled.getDelay();
            int delayCP = CP(scheduled, duration);
            System.out.printf("%n\tcritical path for %s: %d%n", scheduled, delayCP);
            latencyEstimate = Math.max(latencyEstimate, Xi + delayCP + 1);  //+1 to get latency, not end step
        }
        System.out.printf("\tcalculateBound: %s latency estimate based on CP of scheduled nodes: %d%n", kind, latencyEstimate);

        //to calculate the dependent nodes, the current operation has to be added to the res constraints!
        incrementResourceUsed(duration, currentOperation.getResourceType(), resName);

        //move dependent nodes to later steps to fulfill constraint
        //estimate constraint dependent finish step
        for (Node unscheduledNode : unschedNodes) {
            int k = asapValues.get(unscheduledNode).lbound;

            if ("upper".equals(kind)) {
                //get predecessors of bulb node in bulb tree
                HashSet<BulbNode> predecessors = currentBulbNode.predecessors();

                //while there is a member of T_pred (predecessors of the current BulbNode) scheduled on or after step k
                //get latest slot of predecessors
                int latestSlotInSchedule = 0;
                if (!predecessors.isEmpty()) {
                    for (BulbNode bn : predecessors) {
                        for (Node temp : bn.getSchedule().nodes()) {
                            latestSlotInSchedule = Math.max(bn.getSchedule().slot(temp).ubound, latestSlotInSchedule);
                        }
                    }
                    k = latestSlotInSchedule + 1;
                }
            }

            duration = new Interval(k, k + unscheduledNode.getDelay() - 1);
            while (!checkResConstraint(unscheduledNode.getResourceType(), duration)) {
                k++;
                duration = duration.shift(1);
            }

            //finally found a slot to schedule operation
            //add resource of operation type to used resources in this step
            //AND all the following steps for the duration of the operation!!
            resName = findAllocation(unscheduledNode.getResourceType(), duration);
            incrementResourceUsed(duration, unscheduledNode.getResourceType(), resName);

            if ("upper".equals(kind)) {
                int criticalPath = CP(unscheduledNode, duration);
                System.out.printf("calculateBound: critical path for %s: %d%n", unscheduledNode, criticalPath);
                latencyEstimate = Math.max(latencyEstimate, k + unscheduledNode.getDelay()-1 + criticalPath + 1);
            } else {
                int criticalPath = CP(unscheduledNode, duration);
                System.out.printf("calculateBound: critical path for %s: %d%n", unscheduledNode, criticalPath);
                latencyEstimate = Math.max(latencyEstimate, k + unscheduledNode.getDelay()-1 + criticalPath + 1);

            }
            System.out.printf("calculateBound: new %s latency estimate after node %s: %d%n", kind, unscheduledNode, latencyEstimate);
        }

        this.resourceUsage = saveResourceUsage;
        this.allocation = saveAllocation;
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " END OF BULB CALCULATE_BOUND " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        return latencyEstimate;
    }

    private int CP(Node node, Interval duration) {
        //find the latest end of a successor of the node
        int latest = 0;
        for (Node succ : node.reallyAllSuccessors()) {
            if (alapValues.get(succ).ubound > latest) {
                latest = alapValues.get(succ).ubound;
            }
        }

        //shift latest according to interval shift
        int cp = latest - alapValues.get(node).ubound;
        return Math.max(cp, 0);   // 0 when it is the last node
    }

    /*
    private int criticalPath(Node nd) {
        int asap_nd = asapSchedule.slot(nd).lbound;
        int alap_nd = alapSchedule.slot(nd).lbound;
        int cp = 0;
        int max_cp_of_succ = 0;
        int delay = 0;
        if (nd != null && (asap_nd - alap_nd == 0)) {
            System.out.printf("\t\tcalculating CP for node %s%n", nd);
            cp = nd.getDelay();
            for (Node nd2 : nd.allSuccessors().keySet()) {
                System.out.printf("\t\tsuccessor node: %s%n", nd2);
                int asap = asapSchedule.slot(nd2).lbound;
                int alap = alapSchedule.slot(nd2).lbound;
                if (asap - alap == 0) {
                    delay = criticalPath(nd2);
                    System.out.printf("\t\tdelay for node %s: %d%n", nd2, delay);
                    if (max_cp_of_succ < delay) {
                        max_cp_of_succ = delay;
                    }
                }
            }
        } else {
            System.out.printf("\t\tNode %s is not on critical path%n", nd);
            return -1;
        }
        System.out.printf("\t\tmax_cp_of_succ: %d%n", max_cp_of_succ + cp);
        return cp + max_cp_of_succ;
    }
    */

    private void incrementResourceUsed(Interval interval, ResourceType rt, String resName) {
        System.out.printf("\t\tincrementResourceUsed: allocating operation %s on resource %s in interval %s%n", rt, resName, interval);
        System.out.println("\t\t\tResource usage before: " + this.resourceUsage);
        System.out.println("\t\t\tAllocation before" + this.allocation);
        for (int step = interval.lbound; step <= interval.ubound; step++) {
            List<Resource> usedInStep = this.resourceUsage.computeIfAbsent(step, k -> new ArrayList<>());
            boolean resInUse = false;
            for (Resource r : usedInStep) {
                if (r.rt == rt) {
                    r.inc();
                    Set<String> allocatedInStep = allocation.computeIfAbsent(step, k -> new HashSet<>());
                    allocatedInStep.add(resName);
                    //allocation.replace(step, allocatedInStep);
                    resInUse = true;
                    break;
                }
            }
            if (!resInUse) {
                //there is no such operation scheduled yet in this step
                Resource newResource = new Resource(rt, step);
                newResource.inc();
                usedInStep.add(newResource);
                this.resourceUsage.replace(step, usedInStep);
                Set<String> allocatedInStep = allocation.computeIfAbsent(step, k -> new HashSet<>());
                allocatedInStep.add(resName);
                //allocation.replace(step, allocatedInStep);
            }
        }
        System.out.println("\t\t\tResource usage after: " + this.resourceUsage);
        System.out.println("\t\t\tAllocation after" + this.allocation);
    }

    private void decrementResourceUsed(Interval interval, ResourceType rt, String resName) {
        System.out.printf("\t\tdecrementResourceUsed: removing operation %s from resource %s in interval %s%n", rt, resName, interval);
        System.out.println("\t\t\tResource usage before: " + this.resourceUsage);
        System.out.println("\t\t\tAllocation before" + this.allocation);
        for (int step = interval.lbound; step <= interval.ubound; step++) {
            List<Resource> usedInStep = this.resourceUsage.get(step);
            for (Resource r : usedInStep) {
                if (r.rt == rt) {
                    r.dec();  //can set to 0
                    break;
                }
            }
            this.allocation.get(step).remove(resName);
            System.out.println("\t\t\tResource usage after: " + this.resourceUsage);
            System.out.println("\t\t\tAllocation after" + this.allocation);
        }
    }

    /**
     * check if current operation (resourceToCheck) can be scheduled in accordance with res constraint
     *
     * @param resourceToCheck - the operation / resource type to check
     * @param interval        - the target interval (remind allocating the resource for all steps in the duration of operation!)
     * @return the name of the real resource to allocate the operation to
     */
    private boolean checkResConstraint(ResourceType resourceToCheck, Interval interval) {
        System.out.println("\t\t\tResource usage: " + this.resourceUsage);
        System.out.println("\t\t\tAllocation: " + this.allocation);
        System.out.printf("\t\tcheckResConstraint: checking resource constraint for %s in interval %s: ", resourceToCheck, interval);

        Set<String> compatible = this.resourceConstraint.getCompatibleRes(resourceToCheck);

        for (int step = interval.lbound; step <= interval.ubound; step++) {
            //get resources used in this step
            if (null == this.resourceUsage.get(step)) {
                if (step == interval.ubound) {
                    System.out.printf("operation %s can be scheduled%n", resourceToCheck);
                    return true;
                }
                continue;
            }
            //check if there are enough compatible real resources for the operation
            //AND they are not allocated for another operation!!
            //e.g. there is 1 div, and 2 compatible MUL real resources -> go ahead!
            Set<String> allocatedInStep = this.allocation.get(step);
            if (allocatedInStep.containsAll(compatible)) {
                System.out.printf("there are not enough resources available to allocate operation %s%n", resourceToCheck);
                return false;
            } else if (step == interval.ubound) {
                System.out.printf("operation %s can be scheduled%n", resourceToCheck);
                return true;
            }
        }
        System.out.printf("there are not enough resources available to allocate operation %s%n", resourceToCheck);
        return false;
    }

    private String findAllocation(ResourceType resourceToCheck, Interval interval) {
        //return name of any compatible, free resource for allocation (do not allocate yet)v
        System.out.println("\t\t\tAllocation " + this.allocation);
        System.out.printf("\t\tFind allocation for operation %s in interval %s: ", resourceToCheck, interval);

        for (int step = interval.lbound; step <= interval.ubound; step++) {
            Set<String> allocated = this.allocation.get(step);
            Set<String> compatible = this.resourceConstraint.getCompatibleRes(resourceToCheck);
            //do not mutate resourceConstraint!
            Set<String> compatibleClone = new HashSet<>(compatible);
            if (allocated != null) {
                compatibleClone.removeAll(allocated);
            }
            if (compatibleClone.size() > 0) {
                //just return the first compatible real resource
                String resource = compatibleClone.iterator().next();
                System.out.printf("%s can be allocated on %s%n", resourceToCheck, resource);
                return resource;
            }
        }
        System.out.printf("%s cannot be allocated in interval %s%n", resourceToCheck, interval);
        return null;
    }

    private Schedule upperBoundSchedule(Schedule partial, int node, List<Node> unschedNodes) {
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " CALL OF BULB UPPER_BOUND_SCHEDULE " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        // take existing partial schedule and schedule all the missing nodes according to rc
        // do not update resUsage and allocation Map
        ListScheduler listScheduler = new ListScheduler();
        Schedule listSchedule = listScheduler.schedule(unschedNodes, partial, this.resourceConstraint, this.allocation);
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " END OF BULB UPPER_BOUND_SCHEDULE " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        return listSchedule;
    }

    private void updateAsap(int step, int i, List<Node> nodes) {
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " START OF BULB UPDATE_ASAP " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        Node iNode = nodes.get(i);
        Set<Node> successorsOfI = iNode.reallyAllSuccessors();
        for (int j = i + 1; j < nodes.size(); j++) {
            Node succ = nodes.get(j);
            if (successorsOfI.contains(succ)) {
                Interval asapJ = asapValues.get(succ);
                //longest path from i to j
                int longestPath = this.dfg.distance(iNode, succ) - iNode.getDelay();
                System.out.printf("\tLongest path (l_succ - u_pred) from %s to %s: %d%n", iNode, succ, longestPath);
                int l_asap = Math.max(asapJ.lbound, step + longestPath);
                if (l_asap > asapJ.lbound) {
                    Interval shifted = asapValues.get(succ).shift(l_asap - asapJ.lbound);
                    System.out.printf("\tAsap of %s will be updated to %s%n", succ, shifted);
                    asapValues.replace(succ, asapJ, shifted);
                }
            }
        }
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " END OF BULB UPDATE_ASAP " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
    }
}
