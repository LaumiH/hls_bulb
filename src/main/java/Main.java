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

    Scheduler s = new ASAP();
    Schedule asap = s.schedule(g);
    System.out.printf("%nASAP%n%s%n", asap.diagnose());
    System.out.printf("cost = %s%n", asap.cost());

    asap.draw("schedules/ASAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));

    s = new ALAP();
    Schedule alap = s.schedule(g);
    System.out.printf("%nALAP%n%s%n", alap.diagnose());
    System.out.printf("cost = %s%n", alap.cost());

    alap.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));

    s = new BULB(rc, asap, alap);
    Schedule bulb = s.schedule(g);
    System.out.printf("%nASAP%n%s%n", bulb.diagnose());
    System.out.printf("cost = %s%n", bulb.cost());

    bulb.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
  }
}
