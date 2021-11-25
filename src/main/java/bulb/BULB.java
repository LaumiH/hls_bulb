package bulb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import scheduler.ALAP;
import scheduler.ASAP;
import scheduler.Graph;
import scheduler.Interval;
import scheduler.Node;
import scheduler.ResourceConstraint;
import scheduler.ResourceType;
import scheduler.Schedule;
import scheduler.Scheduler;

public class BULB extends Scheduler {

  private final ResourceConstraint resourceConstraint;
  private List<Node> nodes;
  private Schedule asapSchedule;
  private Schedule alapSchedule;
  private Graph sg;

  private Map<Node, Integer> asapValues;

  private int bestLatency;
  private Schedule bestSchedule;

  public BULB(ResourceConstraint rc) {
    this.resourceConstraint = rc;
    asapValues = new HashMap<>();
  }

  /**
   * Use the graph given to create a schedule with the BULB algorithm.
   *
   * @param sg - the dependency graph
   * @return a schedule for the given graph
   */
  @Override
  public Schedule schedule(Graph sg) {
    this.sg = sg;

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
    nodes = alapSchedule.orderNodes("asc");

    Schedule schedule = new Schedule();
    this.bestLatency = alapSchedule.length(); // TODO: replace with list scheduler length
    System.out.println(bestLatency);

    enumerate(schedule, 0);

    return bestSchedule;
  }

  /**
   * begins with schedule <>
   *
   * @param partial - the (partial) schedule, which is a BulbNode in the BulbGraph
   * @param i
   * @return
   */
  private void enumerate(final Schedule partial, int i) {

    Schedule updatedPartial = partial.clone();

    Node operation = nodes.get(i);

    // end recursion condition
    if (i > sg.size()) {
      if (bestLatency > partial.length()) {
        bestSchedule = partial;
        this.bestLatency = partial.length();
      }
    }

    // check all possible time slots for operation
    for (int step = asapSchedule.slot(operation).lbound;
        step < alapSchedule.slot(operation).ubound;
        step++) {
      // save asap values for later reset
      Map<Node,Integer> saveAsap = asapValues;

      String resName =
          partial.checkResourceConstraints(
              operation, new Interval(step, step + operation.getDelay() - 1), resourceConstraint);

      if ("".equals(resName)) {
        // operation can be scheduled in this time step on returned resource name

        int l_bound = lowerBound(partial);
        int u_bound = upperBound(partial);
        if (u_bound < this.bestLatency) {
          this.bestLatency = u_bound ;
          this.bestSchedule = upperBoundSchedule(partial);
        }
        if (l_bound < this.bestLatency) {
          updatedPartial.add(operation, new Interval(step, step), resName);
          updateAsap(step, i);
          enumerate(updatedPartial, i++);
          updatedPartial.remove(operation);
        }
      }
      asapValues = saveAsap;
    }
  }

  /**
   * Calculates the upper bound for a given graph using list scheduling.
   *
   * @param sched - the partial schedule to be estimated
   * @return the upper bound for needed clock cycles
   */
  private int upperBound(Schedule sched) {
    // must work on partially scheduled graphs
    // must yield a legal schedule
    int latencyEstimate = 0;
    for (Node n : sched.nodes()) {
      //find critical path in DFG for nodes successors and add up delay
      //critical path -> mobility==0
      //mobility: (ALAP - ASAP) != 0
      int delayCP = 0;
      for (Node sgn : sg.get(n).successors()) {
        int asap = asapSchedule.slot(sgn).lbound;
        int alap = alapSchedule.slot(sgn).lbound;
        if (asap-alap != 0) delayCP += sgn.getDelay();
      }

      int currentEstimate = sched.slot(n).lbound + delayCP;
      if (currentEstimate > latencyEstimate) latencyEstimate = currentEstimate;
    }

    for (int i=sched.size(); i<sg.size(); i++) {
      int k = asapValues.get(nodes.get(i));
    }

    return latencyEstimate;
  }

  /**
   * Calculates the lower bound for a given graph using ASAP.
   *
   * @param sched - the partial schedule to be estimated
   * @return the lower bound for needed clock cycles
   */
  private int lowerBound(Schedule sched) {
    // quality has huge influence on bulb runtime
    // can use resource numbers
    return 0;
  }

  private Schedule upperBoundSchedule(Schedule partial) {
    // take existing partial schedule and schedule all the missing nodes according to rc

    return new Schedule();
  }

  private void updateAsap(int step, int i) {
    for (int j = i+1; j<nodes.size(); j++) {
      Node succ = nodes.get(j);
      if (nodes.get(i).allSuccessors().containsKey(succ)) {
        int asapJ = asapValues.get(succ);
        asapValues.replace(succ, asapJ, Math.max(asapJ+1, step + Math.max(nodes.get(i).getDelay(), nodes.get(j).getDelay())));
      }
    }
  }
}
