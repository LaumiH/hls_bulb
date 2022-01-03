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
    System.out.println("START OF PROGRAM");

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
    System.out.println("PARSING INPUT GRAPH");
    Graph g = dr.parse(args[0]);
    System.out.printf("Input graph:%n%n%s%n", g.diagnose());

    System.out.println("DOING ASAP SCHEDULE");
    Scheduler s = new ASAP();
    Schedule asap = s.schedule(g);
    System.out.printf("%nASAP%n%s%n", asap.diagnose());
    //System.out.printf("cost = %s%n", asap.cost());
    asap.draw("schedules/ASAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
    System.out.println("FINISHED ASAP SCHEDULE");
    System.out.println("");

    System.out.println("DOING ALAP SCHEDULE");
    s = new ALAP();
    Schedule alap = s.schedule(g);
    System.out.printf("%nALAP%n%s%n", alap.diagnose());
    //System.out.printf("cost = %s%n", alap.cost());
    alap.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
    System.out.println("FINISHED ALAP SCHEDULE");
    System.out.println("");


    System.out.println("STARTING BULB SCHEDULE");
    s = new BULB("ASAP", rc, asap, alap);
    Schedule bulb = s.schedule(g);
    System.out.printf("%nBULB%n%s%n", bulb.diagnose());
    ((BULB) s).getBulbGraph().print();
    bulb.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
    System.out.println("END OF BULB SCHEDULE");


    System.out.println("STARTING BULB SCHEDULE");
    System.out.println("");
    s = new BULB("PAPER", rc, asap, alap);
    bulb = s.schedule(g);
    System.out.printf("%nBULB%n%s%n", bulb.diagnose());
    ((BULB) s).getBulbGraph().print();
    bulb.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
    System.out.println("END OF BULB SCHEDULE");

  }
}
