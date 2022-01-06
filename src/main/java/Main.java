import bulb.BULB;
import scheduler.*;

import java.util.ArrayList;
import java.util.List;

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
        System.out.println();

        System.out.println("DOING LAZY ALAP SCHEDULE");
        Scheduler lazyAlap = new ALAP("lazy");
        Schedule lazy_alap = lazyAlap.schedule(g);
        System.out.printf("%nALAP%n%s%n", lazy_alap.diagnose());
        //System.out.printf("cost = %s%n", alap.cost());
        lazy_alap.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("FINISHED ALAP SCHEDULE");
        System.out.println();

        System.out.println("DOING NORMAL ALAP SCHEDULE");
        Scheduler normalAlap = new ALAP("normal");
        Schedule normal_alap = normalAlap.schedule(g);
        System.out.printf("%nALAP%n%s%n", normal_alap.diagnose());
        //System.out.printf("cost = %s%n", alap.cost());
        normal_alap.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("FINISHED ALAP SCHEDULE");
        System.out.println();





        System.out.println("STARTING BULB SCHEDULE with \n" +
                "\tasap lower bound estimator,\n" +
                "\tupper bound estimator from paper,\n" +
                "\tnormal alap schedule in enumerate for loop");
        BULB bulbS1 = new BULB("ASAP", "PAPER", rc, asap, normal_alap);
        Schedule bulb1 = bulbS1.schedule(g);
        System.out.printf("%nBULB%n%s%n", bulb1.diagnose());
        bulbS1.getBulbGraph().print();
        bulb1.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("END OF BULB SCHEDULE with ASAP PAPER");



        System.out.println("STARTING BULB SCHEDULE with \n" +
                "\tlower bound estimator from paper,\n" +
                "\tupper bound estimator from paper,\n" +
                "\tlazy alap schedule in enumerate for loop");
        System.out.println();
        BULB bulbS3 = new BULB("PAPER", "PAPER", rc, asap, lazy_alap);
        Schedule bulb3 = bulbS3.schedule(g);
        System.out.printf("%nBULB%n%s%n", bulb3.diagnose());
        bulbS3.getBulbGraph().print();
        bulb3.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("END OF BULB SCHEDULE with PAPER PAPER");

        System.out.println("STARTING BULB SCHEDULE with \n" +
                "\tasap lower bound estimator,\n" +
                "\tlist scheduler upper bound estimator,\n" +
                "\tnormal alap schedule in enumerate for loop");
        BULB bulbS2 = new BULB("ASAP", "LIST", rc, asap, normal_alap);
        Schedule bulb2 = bulbS2.schedule(g);
        System.out.printf("%nBULB%n%s%n", bulb2.diagnose());
        bulbS2.getBulbGraph().print();
        bulb2.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("END OF BULB SCHEDULE with ASAP LIST");



        System.out.println("\n\n\nComparing schedules:");
        System.out.println(
                "\tasap lower bound estimator,\n" +
                "\tupper bound estimator from paper,\n" +
                "\tnormal alap schedule in enumerate for loop\n\n" +
                "with\n" +
                "\tasap lower bound estimator,\n" +
                "\tlist scheduler upper bound estimator,\n" +
                "\tnormal alap schedule in enumerate for loop");
        bulb1.compare(bulb2);

        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        System.out.println(
                "\tasap lower bound estimator,\n" +
                "\tupper bound estimator from paper,\n" +
                "\tnormal alap schedule in enumerate for loop\n\n" +
                "with\n" +
                "\tlower bound estimator from paper,\n" +
                "\tupper bound estimator from paper,\n" +
                "\tlazy alap schedule in enumerate for loop");
        bulb1.compare(bulb3);

        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        System.out.println(
                "\tasap lower bound estimator,\n" +
                "\tlist scheduler upper bound estimator,\n" +
                "\tnormal alap schedule in enumerate for loop\n\n" +
                "with\n" +
                "\tlower bound estimator from paper,\n" +
                "\tupper bound estimator from paper,\n" +
                "\tlazy alap schedule in enumerate for loop");
        bulb2.compare(bulb3);

        System.out.println();
        System.out.println();
        bulbS1.getBulbGraph().print();
        System.out.println();
        bulbS2.getBulbGraph().print();
        System.out.println();
        bulbS3.getBulbGraph().print();


    }

}
