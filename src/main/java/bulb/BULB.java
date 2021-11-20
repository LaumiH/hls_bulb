package bulb;

import java.util.List;
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

  public BULB(ResourceConstraint rc) {
    this.resourceConstraint = rc;
  }

  /**
   * Use the graph given to create a schedule with the BULB algorithm.
   *
   * @param sg - the dependency graph
   * @return a schedule for the given graph
   */
  @Override
  public Schedule schedule(Graph sg) {
    // get nodes in decreasing ASAP order
    Scheduler asap = new ASAP();
    Schedule asapSchedule = asap.schedule(sg);
    List<Node> descAsap = asapSchedule.nodesDescOrder();
    for (Node node : descAsap) {
      System.out.printf("%s : %s%n", node, asapSchedule.slot(node));
    }

    Scheduler alap = new ALAP();
    Schedule alapSchedule = asap.schedule(sg);

    Schedule schedule = new Schedule();
    int bestLatency = alapSchedule.length();  //TODO: replace with list scheduler length
    System.out.println(bestLatency);

    Interval asapInterval;

    for (int node = 0; node < descAsap.size(); node++) {
      Node current = descAsap.get(node);
      for (int slot = asapSchedule.slot(current).lbound; slot < alapSchedule.slot(current).ubound; slot++) {
        asapInterval = asapSchedule.slot(current);
        //get resources used in this step and check if operation fits

        //TODO: check resource constraint
        ResourceType rt = current.getResourceType();
        Set<Node> inSlot = schedule.nodes(slot);
        int resources = 0;
        for (Node n : inSlot) {
          if (n.getResourceType().equals(rt)) resources++;
        }
        //if (this.resourceConstraint.)
      }
    }

    /*
    1. run ASAP
    2. run ALAP
    3. try all possible starting times for a node, adhere to resource constraints
    4. try to schedule all successors
    5. evaluate upper and lower bound for subtree
    6. if == -> do not traverse
    7. if != -> pick another schedulable node in subtree
     */

    return null;
  }

  /**
   * Calculates the upper bound for a given graph using list scheduling.
   *
   * @param sg the dependency graph
   * @return the upper bound for needed clock cycles
   */
  public int upperBound(Graph sg) {
    // must work on partially scheduled graphs
    // must yield a legal schedule
    return 0;
  }

  /**
   * Calculates the lower bound for a given graph using ASAP.
   *
   * @param sg the dependency graph
   * @return the lower bound for needed clock cycles
   */
  public int lowerBound(Graph sg) {
    // quality has huge influence on bulb runtime
    // can use resource numbers
    return 0;
  }
}
