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

        //String folderGraphs = "graphs";
        String folderGraphs = "BULB_resources/g";
        File folder = new File(folderGraphs);
        List<File> listOfGraphFiles = List.of(Objects.requireNonNull(folder.listFiles()));

        //String folderResources = "resources/vlsi";
        String folderResources = "BULB_resources/r";
        folder = new File(folderResources);
        List<File> listOfResFiles = List.of(Objects.requireNonNull(folder.listFiles()));

        // Store current System.out before assigning a new value
        PrintStream console = System.out;

        for (File dfg : listOfGraphFiles) {
            for (File res : listOfResFiles) {
                String dotFile = dfg.getName().substring(dfg.getName().lastIndexOf("/") + 1);
                String resFile = res.getName().substring(res.getName().lastIndexOf("/") + 1);

                String dotFilePath = folderGraphs + "/" + dotFile;
                String resFilePath = folderResources + "/" + resFile;

                String logDir = "target/" + dotFile.replaceAll(".dot", "");
                String logName = logDir + "/" + dotFile.replaceAll(".dot", "") + "__" + resFile + ".txt";

                // Creating a File object that represents the log file
                File logFile = new File(logDir);
                if (! logFile.exists()){
                    if (!logFile.mkdirs()) {
                        System.out.printf("Cannot create directory %s%n", logFile);
                        System.exit(-1);
                    }
                    // If you require it to make the entire directory path including parents,
                    // use directory.mkdirs(); here instead.
                }
                logFile = new File(logName);
                PrintStream o = null;
                try {
                    o = new PrintStream(logFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    Objects.requireNonNull(o).close();
                }

                System.setOut(console);
                System.out.printf("Scheduling %s on %s%n", dotFile, resFile);

                // Assign o to output stream
                System.setOut(o);

                ResourceConstraint rc = new ResourceConstraint();
                System.out.println("Reading resource constraints from " + resFilePath + "\n");
                rc.parse(resFilePath);
                if (rc.getAllRes().size() < 1) {
                    System.out.println("Not able to read resources from res file!");
                    System.exit(-1);
                }

                DotReader dr = new DotReader(false);

                System.out.printf("PARSING INPUT GRAPH: %s%n", dotFilePath);
                Graph g = dr.parse(dotFilePath);
                System.out.printf("Input graph:%n%n%s%n", g.diagnose());

                System.out.println("DOING ASAP SCHEDULE");
                Scheduler s = new ASAP();
                Schedule asap = s.schedule(g);
                System.out.println(asap.diagnose(null, g.size()));
                //System.out.printf("cost = %s%n", asap.cost());
                asap.draw("schedules/ASAP_" + dotFilePath.substring(dotFilePath.lastIndexOf("/") + 1));
                System.out.println("FINISHED ASAP SCHEDULE");
                System.out.println();

                System.out.println("DOING LAZY ALAP SCHEDULE");
                Scheduler lazyAlap = new ALAP("lazy");
                Schedule lazy_alap = lazyAlap.schedule(g);
                System.out.println(lazy_alap.diagnose(null, g.size()));
                //System.out.printf("cost = %s%n", alap.cost());
                lazy_alap.draw("schedules/ALAP_" + dotFilePath.substring(dotFilePath.lastIndexOf("/") + 1));
                System.out.println("FINISHED ALAP SCHEDULE");
                System.out.println();

                System.out.println("DOING NORMAL ALAP SCHEDULE");
                Scheduler normalAlap = new ALAP("normal");
                Schedule normal_alap = normalAlap.schedule(g);
                System.out.println(normal_alap.diagnose(null, g.size()));
                //System.out.printf("cost = %s%n", alap.cost());
                normal_alap.draw("schedules/ALAP_" + dotFilePath.substring(dotFilePath.lastIndexOf("/") + 1));
                System.out.println("FINISHED ALAP SCHEDULE");
                System.out.println();

                /*
                ###########################################################################################
                ###########################################################################################
                ###########################################################################################
                 */

                Map<BulbGraph, Schedule> bulbSchedules = new HashMap<>();

                List<String> lBoundEstimators = new ArrayList<>(Arrays.asList("ASAP", "PAPER"));
                List<Boolean> lazyALAP = new ArrayList<>(Arrays.asList(false, true));

                System.setOut(console);
                for (String lBoundEstimator : lBoundEstimators) {
                    for (boolean lazy : lazyALAP) {
                        System.out.printf("%n%n%nSTARTING BULB SCHEDULE with %n" +
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
                            bulb = future.get(5, TimeUnit.SECONDS);
                            bulbSchedules.put(bulbScheduler.getBulbGraph(), bulb);
                        } catch (Exception e) {
                            System.out.println("meow");
                            e.printStackTrace();
                        } finally {
                            future.cancel(true); //this method will stop the running underlying task
                            while(!future.isDone()) {
                                System.out.println("Waiting for completion of thread");
                            }
                            exs.shutdownNow();
                            bulb.diagnose(rc, g.size());
                            Thread.sleep(5 * 1000);
                        }
                    }
                }

                if (bulbSchedules.isEmpty()) {
                    System.setOut(console);
                    System.out.printf("%s on %s: Timeout too low, list scheduler did not complete once%n", dotFile, resFile);
                    System.setOut(o);
                    continue;
                }

                System.setOut(console);
                System.out.printf("%n%nFinished scheduling %s on %s, start comparison%n%n", dotFile, resFile);
                System.setOut(o);

                System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
                System.out.println("Comparing schedules");
                System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");

                Map<BulbGraph, Schedule> copy = new HashMap<>(bulbSchedules);

                Iterator<Map.Entry<BulbGraph, Schedule>> iterator = copy.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<BulbGraph, Schedule> next = iterator.next();
                    iterator.remove();
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
                    System.out.println(e.getKey().printMetrics());
                    System.out.println(e.getKey().print());
                }

                System.out.println(bulbSchedules.values().iterator().next().diagnose(rc, g.size()));

                System.setOut(console);
                System.out.printf("Completed %s on %s%n%n%n", dotFile, resFile);

                o.flush();
                o.close();
            }
        }

        System.setOut(console);
        System.out.println("\n\n\nEND OF PROGRAM");
    }
}
