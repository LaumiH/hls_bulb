package bulb;

import java.util.*;

import scheduler.*;

public class BULB extends Scheduler {

  private final ResourceConstraint resourceConstraint;
  private List<Node> nodesDFG;
  private Schedule asapSchedule;
  private Schedule alapSchedule;
  private Graph dfg;

  private Map<Node, Integer> asapValues;

  private int bestLatency;
  private Schedule bestSchedule;

  // resource usage: map time stamps to the nr of resources used per type
  private Map<Integer, List<Resource>> resourceUsage;
  // real resources allocated to an operation in a step, must fulfill res constraint
  private Map<Integer, Set<String>> allocation;

  private final BulbGraph bulbGraph;

  public BULB(ResourceConstraint rc) {
    this.resourceConstraint = rc;
    asapValues = new HashMap<>();
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

    // get nodes in decreasing ASAP order
    Scheduler asap = new ASAP();
    this.asapSchedule = asap.schedule(sg);
    for (Node n : asapSchedule.nodes()) {
      asapValues.put(n, asapSchedule.slot(n).ubound);
    }

    Scheduler alap = new ALAP();
    this.alapSchedule = alap.schedule(sg);
    for (Node node : alapSchedule.nodes()) {
      System.out.printf("%s : %s%n", node, asapSchedule.slot(node));
    }
    nodesDFG = alapSchedule.orderNodes("asc");

    Schedule schedule = new Schedule();
    this.bestLatency = alapSchedule.length(); // TODO: replace with list scheduler length
    System.out.println(bestLatency);

    //add first node with empty schedule
    BulbNode root = new BulbNode(new HashSet<>(), schedule, asapSchedule.length(), alapSchedule.length());
    bulbGraph.addNode(null, root);

    enumerate(schedule, 0, root);

    return bestSchedule;
  }

  /**
   * begins with schedule <>
   *
   * @param partial - the (partial) schedule, which is a BulbNode in the BulbGraph
   * @param i - the current node in the DFG
   */
  private void enumerate(final Schedule partial, int i, BulbNode parent) {

    Schedule updatedPartial = partial.clone();

    //get current operation to schedule
    Node currentOperation = nodesDFG.get(i);

    // end recursion condition
    if (i > dfg.size()) {
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

      //add new node to BULB tree
      BulbNode currentBulbNode = new BulbNode(new HashSet<>(), partial, step, alapSchedule.length()); //TODO: replace with list scheduler
      bulbGraph.addNode(parent, currentBulbNode);

      // save asap values for later reset
      Map<Node,Integer> saveAsap = asapValues;

      //ResourceUsed(step,res type) < M_type
      boolean canBeScheduled = checkResConstraint(currentOperation.getResourceType(), i);
      if (canBeScheduled) {
        // operation can be scheduled in this time step
        // find out free resource name
        String resName = findAllocation(currentOperation.getResourceType(), step);
        int l_bound = lowerBound(partial,currentBulbNode, currentOperation, i, resName);
        int u_bound = upperBound(partial,currentBulbNode, currentOperation, i, resName);
        if (u_bound < this.bestLatency) {
          this.bestLatency = u_bound ;
          this.bestSchedule = upperBoundSchedule(partial, resName);
        }
        if (l_bound < this.bestLatency) {
          //schedule operation in this step on the returned real resource
          updatedPartial.add(currentOperation, new Interval(step, currentOperation.getDelay()-1), resName);
          incrementResourceUsed(step, currentOperation.getResourceType(), resName);
          updateAsap(step, i);
          enumerate(updatedPartial, i++, currentBulbNode);
          decrementResourceUsed(step, currentOperation.getResourceType(), resName);
        }
      }
      //done with investigating this node of the BULB tree
      asapValues = saveAsap;
    }
  }

  /**
   * Calculates the upper bound for a given graph using list scheduling.
   *
   * @param sched - the partial schedule to be estimated
   * @return the upper bound for needed clock cycles
   */
  private int upperBound(Schedule sched, BulbNode currentBulbNode, Node currentOperation, int i, String resName) {
    // must work on partially scheduled graphs
    // must yield a legal schedule
    Map<Integer, List<Resource>> saveResourceUsage = this.resourceUsage; //store old resource usage
    Map<Integer, Set<String>> saveAllocation = this.allocation; //store old allocation

    //latency estimation based on critical path
    int latencyEstimate = 0;
    for (Node scheduled : sched.nodes()) {
      int Xi = sched.slot(scheduled).ubound;  //include duration
      int delayCP = criticalPath(scheduled);
      latencyEstimate = Math.max(latencyEstimate, Xi+delayCP);
    }

    //move dependent nodes to later steps to fulfill constraint
    //estimate constraint dependent latency
    for (Node n : nodesDFG.subList(i+1, nodesDFG.size()-1)) {
      int k = asapValues.get(n);
      //get predecessors of bulb node in bulb tree
      HashSet<BulbNode> predecessors = currentBulbNode.predecessors();
      //while there is a member of T_pred (predecessors of the current BulbNode) scheduled on or step k
      for (BulbNode bn : predecessors) {
        int latestSlotInSchedule = i+1;
        for (Node temp : bn.getSchedule().nodes()) {
          latestSlotInSchedule = Math.max(bn.getSchedule().slot(temp).ubound, latestSlotInSchedule);
        }
        while(k <= latestSlotInSchedule) k++;
      }
      while(!checkResConstraint(currentOperation.getResourceType(), i)) {
        k++;
      }
      //finally found a slot to schedule operation
      //add resource of operation type to used resources in this step
      //AND all the following steps for the duration of the operation!!
      incrementResourceUsed(i, currentOperation.getResourceType(), resName);
      latencyEstimate = Math.max(latencyEstimate, k);
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
      if (asap-alap != 0) delayCP += sgn.getDelay();
    }
    return delayCP;
  }

  private void incrementResourceUsed(int step, ResourceType rt, String resName) {
    for (int i=step; i<step+rt.delay-1; i++) {
      List<Resource> usedInStep = this.resourceUsage.get(step);
      boolean resInUse = false;
      for (Resource r : usedInStep) {
        if (r.rt == rt) {
          r.inc();
          Set<String> allocatedInStep = allocation.computeIfAbsent(step, k -> new HashSet<>());
          allocatedInStep.add(resName);
          allocation.replace(step, allocatedInStep);
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
      }
    }
  }
  private void decrementResourceUsed(int step, ResourceType rt, String resName) {
    for (int i=step; i<step+rt.delay-1; i++) {
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
   * @param resourceToCheck - the operation / resource type to check
   * @param step - the target step (remind allocating the resource for all steps < duration of operation!)
   * @return the name of the real resource to allocate the operation to
   */
  private boolean checkResConstraint(ResourceType resourceToCheck, int step) {
    //get resources used in this step
    List<Resource> usedInStep = this.resourceUsage.get(step);
    boolean typeAlreadyInUse = false;
    for (Resource r : usedInStep) {
      if (r.rt == resourceToCheck) {
        //check if there are enough compatible real resources for the operation
        //e.g. there is 1 MUL, and 2 compatible MUL real resources -> go ahead!
        if (r.getResourceCount() < this.resourceConstraint.getCompatibleRes(resourceToCheck).size()) {
          return true;
        }
        typeAlreadyInUse = true;
      }
    }
    if (!typeAlreadyInUse && this.resourceConstraint.getCompatibleRes(resourceToCheck).size() > 0) {
      //there was no operation of this type yet, but there is an available resource for it
      return true;
    }

    System.out.println("There are not enough resources available to allocate the operation " + resourceToCheck);
    return false;
  }

  private String findAllocation(ResourceType resourceToCheck, int step) {
    //return name of any compatible, free resource for allocation (do not allocate yet)
    Set<String> compatible = this.resourceConstraint.getCompatibleRes(resourceToCheck);
    compatible.removeAll(this.allocation.get(step));
    if (compatible.size() > 0) {
      return compatible.iterator().next();
    }
    return null;
  }

  /**
   * Calculates the lower bound for a given graph using ASAP.
   *
   * @param sched - the partial schedule to be estimated
   * @return the lower bound for needed clock cycles
   */
  private int lowerBound(Schedule sched, BulbNode currentBulbNode, Node currentOperation, int i, String resName) {
    // quality has huge influence on bulb runtime
    // can use resource numbers
    Map<Integer, List<Resource>> saveResourceUsage = this.resourceUsage; //store old resource usage
    Map<Integer, Set<String>> saveAllocation = this.allocation; //store old allocation

    //latency estimation based on critical path
    int latencyEstimate = 0;
    for (Node scheduled : sched.nodes()) {
      int Xi = sched.slot(scheduled).ubound;  //include duration
      int delayCP = criticalPath(scheduled);
      latencyEstimate = Math.max(latencyEstimate, Xi+delayCP);
    }

    //move dependent nodes to later steps to fulfill constraint
    //estimate constraint dependent latency
    for (Node n : nodesDFG.subList(i+1, nodesDFG.size()-1)) {
      int k = asapValues.get(n);
      while(!checkResConstraint(currentOperation.getResourceType(), i)) {
        k++;
      }
      //finally found a slot to schedule operation
      //add resource of operation type to used resources in this step
      //AND all the following steps for the duration of the operation!!
      incrementResourceUsed(i, currentOperation.getResourceType(), resName);
      latencyEstimate = Math.max(latencyEstimate, k);
    }

    this.resourceUsage = saveResourceUsage;
    this.allocation = saveAllocation;
    return latencyEstimate;
  }

  private Schedule upperBoundSchedule(Schedule partial, String resName) {
    // take existing partial schedule and schedule all the missing nodes according to rc
    // do not update resUsage and allocation Maps
    // TODO: replace with list scheduler
    return alapSchedule;
  }

  private void updateAsap(int step, int i) {
    for (int j = i+1; j< nodesDFG.size(); j++) {
      Node succ = nodesDFG.get(j);
      if (nodesDFG.get(i).allSuccessors().containsKey(succ)) {
        int asapJ = asapValues.get(succ);
        //longest path from i to j
        asapValues.replace(succ, asapJ, Math.max(asapJ+succ.getDelay()-1, step + nodesDFG.get(i).allSuccessors().get(succ)));
      }
    }
  }
}
