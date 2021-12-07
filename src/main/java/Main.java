import bulb.BULB;
import scheduler.ALAP;
import scheduler.ASAP;
import scheduler.DotReader;
import scheduler.Graph;
import scheduler.ResourceConstraint;
import scheduler.Schedule;
import scheduler.Scheduler;

public class Main {

  public static void main(String[] args) {
    ResourceConstraint rc = new ResourceConstraint();
    if (args.length > 1) {
      System.out.println("Reading resource constraints from " + args[1] + "\n");
      rc.parse(args[1]);
    }

    DotReader dr = new DotReader(false);
    if (args.length < 1) {
      System.err.printf("Usage: scheduler dotfile%n");
      System.exit(-1);
    } else {
      System.out.printf("Scheduling %s%n", args[0]);
    }

    Graph g = dr.parse(args[0]);
    System.out.printf("Input graph:%n%n%s%n", g.diagnose());

    /*
        Scheduler s = new ASAP();
        Schedule sched = s.schedule(g);
        System.out.printf("%nASAP%n%s%n", sched.diagnose());
        System.out.printf("cost = %s%n", sched.cost());

        sched.draw("schedules/ASAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));

        s = new ALAP();
        sched = s.schedule(g);
        System.out.printf("%nALAP%n%s%n", sched.diagnose());
        System.out.printf("cost = %s%n", sched.cost());

        sched.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
    */
    Scheduler s = new BULB(rc);
    Schedule sched = s.schedule(g);

  }
}