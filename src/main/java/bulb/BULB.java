package bulb;

import scheduler.Graph;
import scheduler.ResourceConstraint;
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
    return new Schedule();
  }
}
