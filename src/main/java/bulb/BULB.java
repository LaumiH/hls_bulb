package bulb;

import java.util.*;

import scheduler.*;

public class BULB extends Scheduler {

    private final ResourceConstraint resourceConstraint;

    private Graph dfg;

    private Map<Node, Interval> asapValues;

    private final Schedule asapSchedule;
    private final Schedule alapSchedule;

    private int bestLatency;
    private Schedule bestSchedule;

    // resource usage: map time stamps to the nr of resources used per type
    private Map<Integer, List<Resource>> resourceUsage;
    // real resources allocated to an operation in a step, must fulfill res constraint
    private Map<Integer, Set<String>> allocation;

    private final BulbGraph bulbGraph;

    public BULB(final ResourceConstraint rc, final Schedule asap, final Schedule alap) {
        this.resourceConstraint = rc;

        this.asapSchedule = asap;
        this.alapSchedule = alap;

        this.asapValues = new HashMap<>();
        for (Node n : asap.nodes()) {
            asapValues.put(n, asap.slot(n));
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
        this.dfg = sg;

        ListScheduler listScheduler = new ListScheduler();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Node> nodesDFG = this.alapSchedule.orderNodes("asc");

        List<Node> cloneThisGodDammit = new ArrayList<>(nodesDFG.size());

        for (Node node : nodesDFG) {
            cloneThisGodDammit.add(node.clone());
        }

        Schedule listSchedule = listScheduler.schedule(cloneThisGodDammit, new Schedule(), resourceConstraint, allocation);
        this.bestLatency = listSchedule.length();
        System.out.println(bestLatency);

        //add first node with empty schedule
        BulbNode root = new BulbNode(new HashSet<>(), new Schedule(), bestLatency, asapSchedule.length());
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

        System.out.printf("%nBULB partial%n%s%n", partial.diagnose());

        List<Node> nodesDFG = new ArrayList<>(nodes);

        //get current operation to schedule
        Node currentOperation = nodesDFG.get(i);
        System.out.printf("%nCurrent operation: %s%n", currentOperation);

        // end recursion condition
        if (i == dfg.size()) {
            if (bestLatency > partial.length()) {
                bestSchedule = partial;
                this.bestLatency = partial.length();
            }
        }

        // check all possible time slots for current operation
        //from lower bound of asap to upper bound of alap
        for (int step = asapSchedule.slot(currentOperation).lbound;
             step < alapSchedule.slot(currentOperation).ubound;
             step++) {

            Schedule updatedPartial = partial.clone();
            partial.getResources().putAll(updatedPartial.getResources());

            Interval duration = new Interval(step, step + currentOperation.getDelay() - 1);
            updatedPartial.add(currentOperation, duration);

            //add new node to BULB tree
            BulbNode currentBulbNode = new BulbNode(new HashSet<>(), updatedPartial, -1, -1);
            bulbGraph.addNode(parent, currentBulbNode);

            // save asap values for later reset
            Map<Node, Interval> saveAsap = new HashMap<>(asapValues);

            //ResourceUsed(step,res type) < M_type
            boolean canBeScheduled = checkResConstraint(currentOperation.getResourceType(), duration);
            if (canBeScheduled) {
                // operation can be scheduled in this time step
                // find out free resource name
                String resName = findAllocation(currentOperation.getResourceType(), duration);

                //schedule operation in this interval on the returned real resource
                updatedPartial.remove(currentOperation);

                updatedPartial.add(currentOperation, duration, resName);

                int l_bound = calculateBound("lower", partial, currentBulbNode, currentOperation, i, duration, resName, nodesDFG.subList(i + 1, nodesDFG.size()));
                System.out.println("Lower bound is: " + l_bound);
                int u_bound = calculateBound("upper", partial, currentBulbNode, currentOperation, i, duration, resName, nodesDFG.subList(i + 1, nodesDFG.size()));
                System.out.println("Upper bound is: " + u_bound);

                currentBulbNode.setL_bound(l_bound);
                currentBulbNode.setU_bound(u_bound);

                if (u_bound < this.bestLatency) {
                    this.bestLatency = u_bound;
                    incrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                    this.bestSchedule = upperBoundSchedule(updatedPartial, i, nodesDFG.subList(i + 1, nodesDFG.size()));
                    decrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                }
                if (l_bound < this.bestLatency) {
                    incrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                    updateAsap(step, i, nodesDFG);
                    enumerate(updatedPartial, i++, currentBulbNode, nodesDFG);
                    decrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                }
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
    private int calculateBound(String kind, Schedule sched, BulbNode currentBulbNode, Node currentOperation, int i,
                               Interval duration, String resName, List<Node> unschedNodes) {

        Map<Integer, List<Resource>> saveResourceUsage = new HashMap<>(this.resourceUsage); //store old resource usage
        Map<Integer, Set<String>> saveAllocation = new HashMap<>(this.allocation); //store old allocation

        //latency estimation based on critical path
        int latencyEstimate = 0;
        for (Node scheduled : sched.nodes()) {
            int Xi = sched.slot(scheduled).ubound;  //include duration
            int delayCP = criticalPath(scheduled);
            latencyEstimate = Math.max(latencyEstimate, Xi + delayCP);
        }

        //to calculate the dependent nodes, the current operation has to be added to the res constraints!
        incrementResourceUsed(duration, currentOperation.getResourceType(), resName);

        //move dependent nodes to later steps to fulfill constraint
        //estimate constraint dependent latency
        for (Node unscheduledNode : unschedNodes) {
            int k = asapValues.get(unscheduledNode).lbound;

            if ("upper".equals(kind)) {
                //get predecessors of bulb node in bulb tree
                HashSet<BulbNode> predecessors = currentBulbNode.predecessors();

                //while there is a member of T_pred (predecessors of the current BulbNode) scheduled on or after step k
                //get latest slot of predecessors
                int latestSlotInSchedule = 0;
                for (BulbNode bn : predecessors) {
                    for (Node temp : bn.getSchedule().nodes()) {
                        latestSlotInSchedule = Math.max(bn.getSchedule().slot(temp).ubound, latestSlotInSchedule);
                    }
                }
                k = latestSlotInSchedule + 1;
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
                latencyEstimate = Math.max(latencyEstimate, k);
            } else {
                latencyEstimate = Math.max(latencyEstimate, k + criticalPath(unscheduledNode));
            }
        }

        this.resourceUsage = saveResourceUsage;
        this.allocation = saveAllocation;

        return latencyEstimate;
    }

    private int criticalPath(Node parent) {
        //find critical path in DFG for nodes successors and add up delay
        //critical path -> mobility==0
        //mobility: (ALAP - ASAP) != 0
        int delayCP = 0;
        for (Node sgn : dfg.get(parent).allSuccessors().keySet()) {
            int asap = asapSchedule.slot(sgn).lbound;
            int alap = alapSchedule.slot(sgn).lbound;
            if (asap - alap != 0) delayCP += sgn.getDelay();
        }
        return delayCP;
    }

    private void incrementResourceUsed(Interval interval, ResourceType rt, String resName) {
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
    }

    private void decrementResourceUsed(Interval interval, ResourceType rt, String resName) {
        for (int step = interval.lbound; step <= interval.ubound; step++) {
            List<Resource> usedInStep = this.resourceUsage.get(step);
            for (Resource r : usedInStep) {
                if (r.rt == rt) {
                    r.dec();  //can set to 0
                    break;
                }
            }
            this.allocation.get(step).remove(resName);
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
        Set<String> compatible = this.resourceConstraint.getCompatibleRes(resourceToCheck);

        for (int step = interval.lbound; step <= interval.ubound; step++) {
            //get resources used in this step
            if (null == this.resourceUsage.get(step)) {
                if (step == interval.ubound) return true;
                continue;
            }
            //check if there are enough compatible real resources for the operation
            //AND they are not allocated for another operation!!
            //e.g. there is 1 div, and 2 compatible MUL real resources -> go ahead!
            Set<String> allocatedInStep = this.allocation.get(step);
            if (allocatedInStep.containsAll(compatible)) {
                return false;
            } else if (step == interval.ubound) return true;
        }
        System.out.printf("%nThere are not enough resources available to allocate the operation %s in interval %s%n",
                resourceToCheck, interval);
        return false;
    }

    private String findAllocation(ResourceType resourceToCheck, Interval interval) {
        //return name of any compatible, free resource for allocation (do not allocate yet)

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
                return compatibleClone.iterator().next();
            }
        }
        return null;
    }

    private Schedule upperBoundSchedule(Schedule partial, int node, List<Node> unschedNodes) {
        // take existing partial schedule and schedule all the missing nodes according to rc
        // do not update resUsage and allocation Map
        ListScheduler listScheduler = new ListScheduler();
        return listScheduler.schedule(unschedNodes, partial, this.resourceConstraint, this.allocation);
    }

    private void updateAsap(int step, int i, List<Node> nodes) {
        List<Node> nodesDFG = new ArrayList<>(nodes);
        for (int j = i + 1; j < nodesDFG.size(); j++) {
            Node succ = nodesDFG.get(j);
            if (nodesDFG.get(i).allSuccessors().containsKey(succ)) {
                Interval asapJ = asapValues.get(succ);
                //longest path from i to j
                int longestPath = nodesDFG.get(i).allSuccessors().get(succ);
                Interval shifted = asapValues.get(succ).shift(Math.max(0, (step + longestPath) - asapJ.lbound));
                asapValues.replace(succ, asapJ, shifted);
            }
        }
    }
}
