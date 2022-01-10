package bulb;

import scheduler.*;

import java.util.*;
import java.util.concurrent.Callable;

public class BULB extends Scheduler implements Callable<Schedule> {

    long startTime;
    long endTime;
    boolean timeoutReached = false;

    private final String lBoundEstimator;
    private final boolean updateAlapBound;

    private final ResourceConstraint resourceConstraint;
    private final Schedule asapSchedule;
    private final Schedule alapSchedule;
    private final boolean lazyALAP;
    private final BulbGraph bulbGraph;
    private final Graph dfg;
    private Map<Node, Interval> asapValues;
    private Map<Node, Interval> alapValues;
    private Map<Node, Interval> lazyAlapValues;
    private int bestLatency;
    private Schedule bestSchedule;
    // resource usage: map time stamps to the nr of resources used per type
    private Map<Integer, List<Resource>> resourceUsage;
    // real resources allocated to an operation in a step, must fulfill res constraint
    private Map<Integer, Set<String>> allocation;

    private int skippedNodes = 0;

    public BULB(Graph sourceGraph, String lBoundEstimator, final ResourceConstraint rc, final Schedule asap,
                final Schedule alap, final Schedule lazyAlap, boolean lazyALAP, boolean updateAlapBound) {
        this.dfg = sourceGraph;
        this.lBoundEstimator = lBoundEstimator;
        this.updateAlapBound = updateAlapBound;
        this.resourceConstraint = rc;
        this.lazyALAP = lazyALAP;

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

        this.lazyAlapValues = new HashMap<>();
        for (Node n : lazyAlap.nodes()) {
            lazyAlapValues.put(n, alap.slot(n));
        }

        this.resourceUsage = new HashMap<>();
        this.allocation = new HashMap<>();
        this.bulbGraph = new BulbGraph(new ArrayList<>());
    }

    public BulbGraph getBulbGraph() {
        return bulbGraph;
    }

    @Override
    public Schedule call() throws Exception {
        //System.out.println("Start BULB");
        startTime = System.currentTimeMillis();

        List<Node> nodesDFG = this.alapSchedule.orderNodes("asc");

        //System.out.printf("Nodes in ascending alap order: %s%n", nodesDFG);

        //get initial lower bound
        int l_bound;
        switch (this.lBoundEstimator) {
            case "ASAP":
                //find latest asap value, include updates on asap values through scheduling and res constraints
                //also including the scheduling of current node!
                l_bound = asapSchedule.length();
                break;
            case "PAPER":
                l_bound = calculateLowerBound(new Schedule(), /*null, null,*/nodesDFG.subList(0, nodesDFG.size())).length();
                break;
            default:
                throw new BULBException("Type of lower bound estimator not given, or not known, aborting");
        }

        //get initial best latency through list scheduler
        ListScheduler listScheduler = new ListScheduler();
        Schedule listSchedule = listScheduler.schedule(nodesDFG, new Schedule(), resourceConstraint, allocation);
        System.out.println("Finished first list scheduler run");
        this.bestLatency = listSchedule.length();
        this.bestSchedule = listSchedule;
        bulbGraph.setInitialLatency(bestLatency);
        bulbGraph.setBestLatency(bestLatency);
        bulbGraph.setDfgNodes(nodesDFG.size());
        //System.out.printf("Best schedule length at beginning: %d%n", bestLatency);

        //add first node with empty schedule and asal length as lower bound
        BulbNode root = new BulbNode(new HashSet<>(), new Schedule(), l_bound, bestLatency);
        root.setValid(true);
        bulbGraph.addNode(null, root);
        bulbGraph.incrementInspected();
        bulbGraph.setParameters("" +
                "lBoundEstimator: " + lBoundEstimator + ", " +
                "lazyALAP: " + lazyALAP + ", " +
                "updateAlapBound: " + updateAlapBound);

        if (Thread.interrupted()) timeoutReached = true;

        try {
            enumerate(new Schedule(), 0, root, nodesDFG);
        } catch (BulbTimeoutException bulbTimeoutException) {
            System.out.println("Received timeout, aborting");
            this.dfg.reset();
            endTime = System.currentTimeMillis();
            this.allocation.clear();
            this.resourceUsage.clear();
            bulbGraph.setExecutionTime(System.currentTimeMillis() - startTime);
            return this.bestSchedule;
        }

        this.dfg.reset();

        endTime = System.currentTimeMillis();
        this.allocation.clear();
        this.resourceUsage.clear();
        bulbGraph.setExecutionTime(System.currentTimeMillis() - startTime);
        bulbGraph.setReceivedTimeout(false);
        return this.bestSchedule;
    }

    /**
     * begins with schedule <>
     *
     * @param partial - the (partial) schedule, which is a BulbNode in the BulbGraph
     * @param i       - the current node in the DFG
     */
    private void enumerate(final Schedule partial, int i, BulbNode parent, final List<Node> nodes) throws BulbTimeoutException, BULBException {
        if (Thread.interrupted()) timeoutReached = true;
        if (timeoutReached) {
            throw new BulbTimeoutException("");
        }

        //System.out.println(i);

        // end recursion condition
        if (i == dfg.size()-1) {
            System.out.println(i);
            System.out.println("investigated last node in DFG");
            if (bestLatency > partial.length()) {
                bestSchedule = partial;
                this.bestLatency = partial.length();
            }
            return;
        }

        //System.out.printf("%n%n%ni=%d; START OF BULB ENUMERATE%n", i);
        //System.out.printf("Current schedule: %s%n", partial.diagnose(resourceConstraint, dfg.size()));
        partial.validate(resourceConstraint, i);

        List<Node> nodesDFG = new ArrayList<>(nodes);

        //get current operation to schedule
        Node currentOperation = nodesDFG.get(i);

        int alapUpperBound = lazyALAP ? lazyAlapValues.get(currentOperation).ubound : alapValues.get(currentOperation).ubound;

        //System.out.printf("i=%d; Current operation to schedule: %s; " +
        //                "asap - alap range: step %d to %d; duration: %d%n", i, currentOperation,
        //        asapValues.get(currentOperation).lbound, alapUpperBound,
        //        currentOperation.getDelay());

        if (updateAlapBound) {
            //check if operation fits in asap - alap interval, otherwise it would not be scheduled!
            if ((alapUpperBound - asapValues.get(currentOperation).lbound) < (currentOperation.getDelay() - 1)) {
                //System.out.printf("Operation %s cannot get scheduled because alap (%s) - " +
                //                "asap (%s) does not leave enough space for its duration (%d)%n",
                //        currentOperation, alapUpperBound,
                //        asapValues.get(currentOperation).lbound,
                //        currentOperation.getDelay());
                //System.out.println("Replacing alap with asap.u_bound + the delay of the operation");
                if (lazyALAP) {
                    //System.out.println(lazyAlapSchedule.diagnose(null, dfg.size()));
                    Interval asap = asapValues.get(currentOperation);
                    lazyAlapValues.replace(currentOperation, asap.shift(currentOperation.getDelay()));
                    alapUpperBound = alapValues.get(currentOperation).ubound;
                } else {
                    Interval asap = asapValues.get(currentOperation);
                    alapValues.replace(currentOperation, asap.shift(currentOperation.getDelay()));
                    alapUpperBound = alapValues.get(currentOperation).ubound;
                }
            }
        }

        // check all possible time slots for current operation
        // from lower bound of asap to upper bound of alap
        for (int step = asapValues.get(currentOperation).lbound; step <= alapUpperBound - currentOperation.getDelay() + 1; step++) {

            // save asap and alap values for later reset
            Map<Node, Interval> saveAsap = new HashMap<>(asapValues);
            Map<Node, Interval> saveAlap = new HashMap<>(lazyALAP ? lazyAlapValues : alapValues);

            //need to update asap for the operation itself
            int diffStepAsap = step - asapValues.get(currentOperation).lbound;
            asapValues.replace(currentOperation, asapValues.get(currentOperation).shift(diffStepAsap));

            //get interval for current operation
            Interval duration = new Interval(step, step + currentOperation.getDelay() - 1);

            //System.out.printf("%ni=%d; BULB is trying %s in interval %s%n", i, currentOperation, duration);

            //if (duration.ubound > 200) {
            //    throw new BULBException("Suspicious: duration of interval > 200");
            //}
            if (duration.ubound > bestLatency) {
                System.out.println("Can skip this one, would be scheduled later than best latency");
            }

            //do not change partial schedule during loop
            Schedule updatedPartial = partial.clone();

            //add new node to BULB tree with -1 as bounds
            //allows to later see which options were checked but could not be scheduled
            //System.out.printf("i=%d; Add bulb node%n", i);
            BulbNode currentBulbNode = new BulbNode(new HashSet<>(), null, -1, -1);
            bulbGraph.addNode(parent, currentBulbNode);

            //System.out.printf("i=%d; Checking if %s can be scheduled in interval %s%n", i, currentOperation, duration);
            //ResourceUsed(step,res type) < M_type
            String resName = checkResConstraint(currentOperation.getResourceType(), duration);
            if (!resName.isBlank()) {
                bulbGraph.incrementInspected();
                // operation can be scheduled in this time step following res constraints

                //System.out.printf("i=%d; %s can be scheduled in %s on %s%n", i, currentOperation, duration, resName);

                //add current operation to schedule
                //System.out.printf("\nAdding %s to schedule in interval %s on resource %s%n", currentOperation, duration, resName);
                updatedPartial.add(currentOperation, duration, resName);
                updatedPartial.validate(resourceConstraint, i + 1);

                //update allocation and resUsed
                //System.out.println("Incrementing resource usage");
                incrementResourceUsed(duration, currentOperation.getResourceType(), resName);

                //System.out.printf("i=%d; Calculating lower bound for %s in interval %s%n", i, currentOperation, duration);
                int l_bound = 0;
                switch (this.lBoundEstimator) {
                    case "ASAP":
                        //find latest asap value, include updates on asap values through scheduling and res constraints
                        //also including the scheduling of current node!
                        for (Interval max : this.asapValues.values()) {
                            l_bound = Math.max(max.ubound + 1, l_bound);
                        }
                        break;
                    case "PAPER":
                        l_bound = calculateLowerBound(updatedPartial, /*currentOperation, duration,*/
                                nodesDFG.subList(i + 1, nodesDFG.size())).length();
                        break;
                    default:
                        throw new BULBException("Type of lower bound estimator not given, or not known, aborting");
                }

                //System.out.printf("Res usage before bounds: %s%n", this.resourceUsage);
                //System.out.printf("Allocation before bounds: %s%n", this.allocation);

                //System.out.printf("%ni=%d; Lower bound is: %s%n%n", i, l_bound);
                //System.out.printf("i=%d; Calculating upper bound for %s in interval %s%n", i, currentOperation, duration);

                List<Node> nodesToSchedule = nodesDFG.subList(i + 1, nodesDFG.size());
                Schedule potentialBestSchedule;
                //check if last node is scheduled
                if (nodesToSchedule.isEmpty()) {
                    System.out.println("Last node is scheduled, do not run list scheduler");
                    potentialBestSchedule = updatedPartial;
                } else {
                    //do list scheduling
                    potentialBestSchedule = upperBoundSchedule(updatedPartial, nodesToSchedule);
                }
                potentialBestSchedule.validate(resourceConstraint, dfg.size());
                int u_bound = potentialBestSchedule.length();

                //System.out.printf("%ni=%d; Upper bound is: %s%n%n", i, u_bound);

                //System.out.printf("Res usage after bounds: %s%n", this.resourceUsage);
                //System.out.printf("Allocation after bounds: %s%n%n", this.allocation);

                //update lower and upper bounds in BULB tree
                //System.out.printf("i=%d; Add update lower and upper bounds in BULB node, set to valid%n", i);
                currentBulbNode.setL_bound(l_bound);
                currentBulbNode.setU_bound(u_bound);
                currentBulbNode.setSchedule(updatedPartial);
                currentBulbNode.setInvestigatedNode(currentOperation);
                currentBulbNode.setInvestigatedInterval(duration);
                currentBulbNode.setValid(true);

                if (l_bound > u_bound) {
                    System.out.printf("i=%d; lower bound (%d) > upper bound (%d), " +
                                    "the lower bound estimator was too conservative%n",
                            i, l_bound, u_bound);
                } else if (u_bound <= this.bestLatency) {
                    if (u_bound < this.bestLatency) {
                        //System.out.printf("i=%d; Upper bound (%d) is better than best latency (%d)%n", i, u_bound, bestLatency);
                        this.bestLatency = u_bound;
                        this.bestSchedule = potentialBestSchedule;
                        bulbGraph.setBestLatency(bestLatency);
                    } //else {
                        //System.out.printf("i=%d; Upper bound (%d) cannot top best latency (%d)%n", i, u_bound, bestLatency);
                    //}

                    //stop investigating further down
                    if (l_bound == u_bound) {
                        bulbGraph.setConvergenceTime(System.currentTimeMillis() - startTime, u_bound);
                        bulbGraph.incrementNumberOfConvergences();

                        //System.out.println("Found best schedule with lower == upper");
                        //System.out.printf("Can skip scheduling %d nodes%n%n%n", bestSchedule.nodes().size() - i - 1);
                        skippedNodes = Math.max(skippedNodes, bestSchedule.nodes().size() - i - 1);
                        bulbGraph.setMaxSkippedNodes(skippedNodes);
                        //System.out.println(bestSchedule.diagnose(resourceConstraint, dfg.size()));
                        bestSchedule.validate(resourceConstraint, dfg.size());
                    } else {
                        //lower bound smaller than upper bound
                        //System.out.printf("i=%d; Lower bound (%d) is still smaller than upper bound (%d)%n", i, l_bound, u_bound);
                        updateAsap(duration, i, nodesDFG);
                        i = i + 1;
                        enumerate(updatedPartial, i, currentBulbNode, nodesDFG);
                        i = i - 1;
                    }

                } else {
                    //u_bound > this.bestLatency, cannot be a better schedule

                    //System.out.printf("i=%d; Upper bound (%d) is worse than best latency (%d), stop here%n", i, u_bound, bestLatency);
                    //System.out.printf("i=%d; Restoring asap values%n", i);
                    //System.out.println(bestSchedule.diagnose(resourceConstraint, dfg.size()));
                    bestSchedule.validate(resourceConstraint, dfg.size());
                }

                //make preparations for next step
                decrementResourceUsed(duration, currentOperation.getResourceType(), resName);
                asapValues = saveAsap;
                if (lazyALAP) {
                    lazyAlapValues = saveAlap;
                } else {
                    alapValues = saveAlap;
                }
            } else {
                //System.out.println("###############################################################");
                //System.out.printf("i=%d; Resource constraints do not allow to schedule %s in %s%n", i, currentOperation, duration);
                //System.out.println(this.allocation);
                //System.out.println("###############################################################");
                if (updateAlapBound) {
                    //update asap and alap values of current operation to the earliest free step
                    //if steps are exceeded!
                    if (step == alapUpperBound - currentOperation.getDelay() + 1) {
                        //alap bound is reached
                        //System.out.println("###############################################################");
                        //System.out.printf("i=%d; alap bound reached -> updating asap for %s and successors %n", i, currentOperation);
                        //System.out.println("###############################################################");
                        int freeStep = 1;
                        Interval copyDuration = duration.copy().shift(1);
                        while ("".equals(checkResConstraint(currentOperation.getResourceType(), copyDuration))) {
                            freeStep++;
                            copyDuration = copyDuration.shift(1);
                        }
                        this.asapValues.replace(currentOperation, asapValues.get(currentOperation).shift(freeStep));
                        if (lazyALAP) {
                            //System.out.println("Need to update alap of lazy alap");
                            this.lazyAlapValues.replace(currentOperation, lazyAlapValues.get(currentOperation).shift(freeStep));
                            alapUpperBound = lazyAlapValues.get(currentOperation).ubound;
                        } else {
                            this.alapValues.replace(currentOperation, alapValues.get(currentOperation).shift(freeStep));
                            alapUpperBound = alapValues.get(currentOperation).ubound;
                        }
                        step = copyDuration.lbound - 1;   //will be incremented in loop head
                        updateAsap(copyDuration, i, nodesDFG);
                        //alap will be updated as well if asap > alap
                    }
                }
            }
            //done with investigating this step
        }
        //System.out.printf("i=%d; done with enumerate round %d%n", i, i);
    }

    /**
     * Calculates the upper or lower bound for a given graph using list scheduling.
     *
     * @param sched - the partial schedule to be estimated
     * @return the upper bound for needed clock cycles
     */
    private Schedule calculateLowerBound(Schedule sched, /*Node currentOperation, Interval duration,*/ List<Node> unschedNodes) throws BULBException, BulbTimeoutException {
        if (Thread.interrupted()) timeoutReached = true;
        if (timeoutReached) {
            throw new BulbTimeoutException("");
        }
        //System.out.println("\tcalculateBound: Current schedule: " + sched.diagnose(resourceConstraint, dfg.size()));
        //System.out.println("\tcalculateBound: Allocation: " + this.allocation);
        //System.out.println("\tcalculateBound: Resource usage: " + this.resourceUsage);
        sched.validate(resourceConstraint, dfg.size() - unschedNodes.size());

        if (unschedNodes.isEmpty()) {
            ////System.out.println("No unscheduled nodes remaining, returning length of schedule");
            return sched;
        }

        //store old resource usage and allocation
        Map<Integer, List<Resource>> saveResourceUsage = new HashMap<>();
        for (Map.Entry<Integer, List<Resource>> entry : this.resourceUsage.entrySet()) {
            List<Resource> resCopy = new ArrayList<>();
            for (Resource resource : entry.getValue()) {
                resCopy.add(resource.clone());
            }
            saveResourceUsage.put(entry.getKey(), resCopy);
        }
        Map<Integer, Set<String>> saveAllocation = new HashMap<>();
        for (Map.Entry<Integer, Set<String>> entry : this.allocation.entrySet()) {
            HashSet<String> resCopy = new HashSet<>(entry.getValue());
            saveAllocation.put(entry.getKey(), resCopy);
        }

        Schedule copy = sched.clone();
        //copy.getResources().putAll(sched.getResources());

        //latency estimation based on critical path
        //estimate is the step it will be ready in
        int latencyEstimate = 0;
        for (Node scheduled : sched.nodes()) {
            int Xi = sched.slot(scheduled).ubound;  //current step (ready)
            //int delayCP = criticalPath(scheduled) - scheduled.getDelay();
            int delayCP = CP(scheduled);
            ////System.out.printf("%n\tcalculateBound: critical path for %s: %d%n", scheduled, delayCP);
            latencyEstimate = Math.max(latencyEstimate, Xi + delayCP + 1);  //+1 to get latency, not end step
        }
        //System.out.printf("\ncalculateBound: %s latency estimate based on CP of scheduled nodes: %d%n", kind, latencyEstimate);

        //System.out.println("calculateBound: estimate bound through scheduling remaining nodes");

        //move dependent nodes to later steps to fulfill constraint (if this should be wrong somehow)
        //estimate constraint dependent finish step
        for (Node unscheduledNode : unschedNodes) {
            int k = asapValues.get(unscheduledNode).lbound;
            //System.out.printf("\tcalculateBound: %s has asap %s%n", unscheduledNode, k);

            // a predecessor might have been scheduled later than asap due to resource constraints
            for (Node pred : unscheduledNode.predecessors()) {
                //System.out.println("\tpred " + pred);
                //System.out.println("COPY SCHEDULE " + copy.diagnose(resourceConstraint));
                int predFinished = copy.slot(pred).ubound;
                if (predFinished >= k) {
                    // unscheduledNode asap needs to be moved
                    k = predFinished + 1;
                    //System.out.printf("\tcalculateBound: Updated k to %d as predecessor %s of %s finishes on %d%n", k, pred, unscheduledNode, predFinished);
                }
            }

            Interval duration = new Interval(k, k + unscheduledNode.getDelay() - 1);
            String resName = checkResConstraint(unscheduledNode.getResourceType(), duration);
            while ("".equals(resName)) {
                //System.out.printf("calculateBound: %s cannot be scheduled on %s%n", unscheduledNode, duration);
                k++;
                duration = duration.shift(1);
                resName = checkResConstraint(unscheduledNode.getResourceType(), duration);
            }

            //System.out.printf("calculateBound: %s can be scheduled on %s%n", unscheduledNode, duration);

            //finally found a slot to schedule operation
            //add resource of operation type to used resources in this step
            //AND all the following steps for the duration of the operation!!
            incrementResourceUsed(duration, unscheduledNode.getResourceType(), resName);
            copy.add(unscheduledNode, duration, resName);

            //System.out.printf("calculateBound: calculate bound based on %s%n", unscheduledNode);

            int criticalPath = CP(unscheduledNode);
            //System.out.printf("\tcalculateBound: critical path for %s: %d%n", unscheduledNode, criticalPath);
            latencyEstimate = Math.max(latencyEstimate, k + unscheduledNode.getDelay() - 1 + criticalPath + 1);
            //System.out.printf("\tcalculateBound: %s latency estimate after node %s: %d%n", kind, unscheduledNode, latencyEstimate);
        }

        //System.out.print("Schedule calculated in upper bound: ");
        //System.out.println(copy.diagnose(resourceConstraint, dfg.size()));
        copy.validate(resourceConstraint, dfg.size());

        this.resourceUsage = saveResourceUsage;
        this.allocation = saveAllocation;
        //System.out.printf("Latency estimate of bound: %d%n", latencyEstimate);
        if (copy.length() != latencyEstimate) {
            throw new BULBException("Calculated different schedule length than latency estimate, hm.");
        }

        //System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " END OF BULB CALCULATE_BOUND " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        return copy;
    }

    private int CP(Node node) throws BulbTimeoutException {
        if (Thread.interrupted()) timeoutReached = true;
        if (timeoutReached) {
            throw new BulbTimeoutException("");
        }
        //System.out.printf("\t\tCP: Calculating critical path for DFG node %s%n", node);
        //find the latest end of a successor of the node
        int latest = 0;
        Set<Node> successors = node.reallyAllSuccessors();
        //System.out.printf("\t\tCP: %s successors: %s%n", node, successors);
        for (Node succ : successors) {
            if (alapValues.get(succ).ubound > latest) {
                latest = alapValues.get(succ).ubound;
            }
        }

        //shift latest according to interval shift
        int cp = latest - alapValues.get(node).ubound;
        return Math.max(cp, 0);   // 0 when it is the last node
    }

    private void incrementResourceUsed(Interval interval, ResourceType rt, String resName) throws BulbTimeoutException {
        if (Thread.interrupted()) timeoutReached = true;
        if (timeoutReached) {
            throw new BulbTimeoutException("");
        }
        //System.out.printf("\t\tincrementResourceUsed: allocating operation %s on resource %s in interval %s%n", rt, resName, interval);
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
        //System.out.println("\t\t\tResource usage after: " + this.resourceUsage);
        //System.out.println("\t\t\tAllocation after" + this.allocation);
    }

    private void decrementResourceUsed(Interval interval, ResourceType rt, String resName) throws BulbTimeoutException {
        if (Thread.interrupted()) timeoutReached = true;
        if (timeoutReached) {
            throw new BulbTimeoutException("");
        }
        //System.out.printf("\t\tdecrementResourceUsed: removing operation %s from resource %s in interval %s%n", rt, resName, interval);
        for (int step = interval.lbound; step <= interval.ubound; step++) {
            List<Resource> usedInStep = this.resourceUsage.get(step);
            for (Resource r : usedInStep) {
                if (r.rt == rt) {
                    r.dec();  //can set to 0
                    break;
                }
            }
            this.allocation.get(step).remove(resName);
            //System.out.println("\t\t\tResource usage after: " + this.resourceUsage);
            //System.out.println("\t\t\tAllocation after" + this.allocation);
        }
    }

    /**
     * check if current operation (resourceToCheck) can be scheduled in accordance with res constraint
     *
     * @param resourceToCheck - the operation / resource type to check
     * @param interval        - the target interval (remind allocating the resource for all steps in the duration of operation!)
     * @return the name of the real resource to allocate the operation to
     */
    private String checkResConstraint(ResourceType resourceToCheck, Interval interval) throws BulbTimeoutException {
        if (Thread.interrupted()) timeoutReached = true;
        if (timeoutReached) {
            throw new BulbTimeoutException("");
        }
        //System.out.println("\t\tcheckResConstraint: Resource usage: " + this.resourceUsage);
        //System.out.println("\t\tcheckResConstraint: Allocation: " + this.allocation);
        //System.out.printf("\t\tcheckResConstraint: checking resource constraint for %s in interval %s: ", resourceToCheck, interval);

        Set<String> allocated = this.allocation.get(interval.lbound);
        Set<String> constraint = this.resourceConstraint.getCompatibleRes(resourceToCheck);

        //do not mutate resourceConstraint!
        Set<String> free = new HashSet<>(constraint);
        if (allocated != null) {
            free.removeAll(allocated);
        }

        if (this.allocation.isEmpty()) return free.iterator().next();

        boolean canAllocate;
        for (String res : free) {
            canAllocate = true;
            for (int step = interval.lbound + 1; step <= interval.ubound; step++) {
                Set<String> resInStep = this.allocation.get(step);
                if (resInStep == null) continue;
                if (resInStep.contains(res)) {
                    canAllocate = false;
                    break;
                }
            }
            if (canAllocate) {
                //System.out.println(res);
                return res;
            }
        }

        //System.out.printf("Did not find resource for %s in interval %s%n", resourceToCheck, interval);
        return "";
    }

    private Schedule upperBoundSchedule(Schedule partial, List<Node> unschedNodes) throws BULBException, BulbTimeoutException {
        if (Thread.interrupted()) timeoutReached = true;
        if (timeoutReached) {
            throw new BulbTimeoutException("");
        }
        //System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " CALL OF BULB UPPER_BOUND_SCHEDULE " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        // take existing partial schedule and schedule all the missing nodes according to rc
        // do not update resUsage and allocation Map
        ListScheduler listScheduler = new ListScheduler();
        return listScheduler.schedule(unschedNodes, partial, this.resourceConstraint, this.allocation);
        //System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " END OF BULB UPPER_BOUND_SCHEDULE " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
    }

    private void updateAsap(Interval duration, int i, List<Node> nodes) throws BulbTimeoutException {
        if (Thread.interrupted()) timeoutReached = true;
        if (timeoutReached) {
            throw new BulbTimeoutException("");
        }
        //System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " START OF BULB UPDATE_ASAP " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        Node iNode = nodes.get(i);
        Set<Node> successorsOfI = iNode.reallyAllSuccessors();
        if (duration.lbound.equals(asapSchedule.slot(iNode).lbound)) {
            //System.out.println("updateASAP not needed, as node is scheduled in asap interval");
            return;
        }
        //System.out.printf("Current node is scheduled in %s, ASAP was %s%n", duration, asapSchedule.slot(iNode));
        // //System.out.printf("\tupdateAsap: successors of current node %s: %s%n", iNode, successorsOfI);
        for (int j = i + 1; j < nodes.size(); j++) {
            Node succ = nodes.get(j);
            if (successorsOfI.contains(succ)) {
                Interval asapJ = asapValues.get(succ);
                //System.out.printf("\tupdateAsap: old interval of %s: %s%n", succ, asapJ);
                //longest path from i to j
                //int longestPath = this.dfg.distance(iNode, succ) - iNode.getDelay();
                int longestPath = this.dfg.distance(iNode, succ);
                //System.out.printf("\tupdateAsap: longest path (l_succ - u_pred) from %s to %s: %d%n", iNode, succ, longestPath);
                int l_asap = Math.max(asapJ.lbound, duration.lbound + longestPath);
                if (l_asap > asapJ.lbound) {
                    Interval shifted = asapValues.get(succ).shift(l_asap - asapJ.lbound);
                    //System.out.printf("\tupdateAsap: asap of %s will be updated to %s%n", succ, shifted);
                    asapValues.replace(succ, asapJ, shifted);
                } //else {
                //System.out.printf("\tupdateAsap: asap of %s does not need to be updated%n", succ);
                //}
                //check if asap shift caused asap > alap,
                //which would result in not trying to schedule resource in enumerate!
                //System.out.printf("Checking asap of %s, is %s%n", succ, alapValues.get(succ));
                if (lazyALAP) {
                    Interval alapJ = lazyAlapValues.get(succ);
                    if (asapJ.gt(alapJ)) {
                        //System.out.printf("\tupdateAsap: asap (%s) > alap (%s) for %s%n", asapJ, alapJ, succ);
                        Interval shifted = lazyAlapValues.get(succ).shift(l_asap - asapJ.lbound);
                        //System.out.printf("\tupdateAsap: alap of %s will be updated to %s%n", succ, shifted);
                        lazyAlapValues.replace(succ, alapJ, shifted);
                    }
                } else {
                    Interval alapJ = alapValues.get(succ);
                    if (asapJ.gt(alapJ)) {
                        //System.out.printf("\tupdateAsap: asap (%s) > alap (%s) for %s%n", asapJ, alapJ, succ);
                        Interval shifted = alapValues.get(succ).shift(l_asap - asapJ.lbound);
                        //System.out.printf("\tupdateAsap: alap of %s will be updated to %s%n", succ, shifted);
                        alapValues.replace(succ, alapJ, shifted);
                    }
                }
            }
        }
        //System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" + " END OF BULB UPDATE_ASAP " + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
    }

    @Override
    public Schedule schedule(Graph sg) {
        Schedule schedule = new Schedule();
        try {
            schedule = call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return schedule;
    }
}
