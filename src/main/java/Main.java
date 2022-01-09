import bulb.BULB;
import bulb.BULBException;
import bulb.BulbGraph;
import scheduler.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("START OF PROGRAM");

        String dotFile = args[0].substring(args[0].lastIndexOf("/") + 1);
        String resFile = args[1].substring(args[1].lastIndexOf("/") + 1);
        ;

        // Creating a File object that represents the disk file.
        PrintStream o = null;
        try {
            o = new PrintStream(new File(dotFile + "__" + resFile + ".txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Store current System.out before assigning a new value
        PrintStream console = System.out;

        // Assign o to output stream
        System.setOut(o);

        // Use stored value for output stream
        //System.setOut(console);

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
        System.out.println(asap.diagnose(null, g.size()));
        //System.out.printf("cost = %s%n", asap.cost());
        asap.draw("schedules/ASAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("FINISHED ASAP SCHEDULE");
        System.out.println();

        System.out.println("DOING LAZY ALAP SCHEDULE");
        Scheduler lazyAlap = new ALAP("lazy");
        Schedule lazy_alap = lazyAlap.schedule(g);
        System.out.println(lazy_alap.diagnose(null, g.size()));
        //System.out.printf("cost = %s%n", alap.cost());
        lazy_alap.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("FINISHED ALAP SCHEDULE");
        System.out.println();

        System.out.println("DOING NORMAL ALAP SCHEDULE");
        Scheduler normalAlap = new ALAP("normal");
        Schedule normal_alap = normalAlap.schedule(g);
        System.out.println(normal_alap.diagnose(null, g.size()));
        //System.out.printf("cost = %s%n", alap.cost());
        normal_alap.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("FINISHED ALAP SCHEDULE");
        System.out.println();

        System.setOut(console);

        /*
        ###########################################################################################
        ###########################################################################################
        ###########################################################################################
         */

        Map<BulbGraph, Schedule> bulbSchedules = new HashMap<>();

        List<String> lBoundEstimators = new ArrayList<>(Arrays.asList("ASAP", "PAPER"));
        List<Boolean> lazyALAP = new ArrayList<>(Arrays.asList(false, true));

        for (String lBoundEstimator : lBoundEstimators) {
            for (boolean lazy : lazyALAP) {
                System.out.printf("STARTING BULB SCHEDULE with %n" +
                                "\t%s lower bound estimator,%n" +
                                "\tlist scheduler upper bound estimator,%n" +
                                "\t%s alap schedule in enumerate for loop%n%n",
                        lBoundEstimator, lazy ? "lazy" : "normal");
                BULB bulbScheduler = new BULB(g, lBoundEstimator, "LIST", rc, asap, normal_alap, lazy_alap, lazy);
                ExecutorService exs = Executors.newSingleThreadExecutor();
                //List<Future<Schedule>> bulb2F =
                //exs.invokeAll(List.of(new BULB(g, "ASAP", "LIST", rc, asap, normal_alap, lazy_alap, false)), 2, TimeUnit.SECONDS);
                //        exs.invokeAll(List.of(new BULB(g, "ASAP", "LIST", rc, asap, normal_alap, lazy_alap, false)), 2, TimeUnit.SECONDS);

                Future<Schedule> future = exs.submit(bulbScheduler);
                Schedule bulb = new Schedule();
                try {
                    bulb = future.get(10, TimeUnit.SECONDS);
                    bulbSchedules.put(bulbScheduler.getBulbGraph(), bulb);
                    Thread.sleep(5*1000);
                } catch (Exception e) {
                    System.out.println("meow");
                } finally {
                    future.cancel(true); //this method will stop the running underlying task
                    exs.shutdownNow();
                    bulb.diagnose(rc, g.size());
                }
            }
        }

        System.setOut(o);


        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        System.out.println("Comparing schedules");
        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");

        Map<BulbGraph, Schedule> copy = new HashMap<>(bulbSchedules);

        Iterator<Map.Entry<BulbGraph, Schedule>> i = copy.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<BulbGraph, Schedule> next = i.next();
            i.remove();
            for (Map.Entry<BulbGraph, Schedule> e : copy.entrySet()) {
                System.out.printf("Comparing %s %n\t\t with %s%n%n--->  ", next.getKey().getParameters(), e.getKey().getParameters());
                if (!next.getValue().compare(e.getValue())) {
                    throw new BULBException("Schedules differ");
                }
                System.out.println();
                System.out.println(next.getKey().printMetrics());
                System.out.println();
                System.out.println(e.getKey().printMetrics());
                System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n\n");
            }
        }

        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        System.out.println("Printing schedules");
        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");

        for (Map.Entry<BulbGraph, Schedule> e : bulbSchedules.entrySet()) {
            System.setOut(o);
            System.out.println(e.getKey().printMetrics());
            System.out.println(e.getKey().print());
        }

        System.out.println(bulbSchedules.values().iterator().next().diagnose(rc, g.size()));

        System.setOut(console);
        System.out.println("\n\n\nEND OF PROGRAM");
    }
}
