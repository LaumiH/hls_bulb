import bulb.BULB;
import bulb.BulbGraph;
import bulb.BulbTimeoutException;
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

        //this is to keep track of overall statistics in the benchmark
        int completed = 0;
        Map<String, String> timeouted = new TreeMap<>();     //filename, parameters -> treemap is sorted
        Map<String, String> exception = new TreeMap<>();     //filename, parameters
        List<String> notEqual = new ArrayList<>();    //filename, res and parameters of both

        List<File> listOfGraphFiles = new ArrayList<>();
        List<File> listOfResFiles = new ArrayList<>();
        //String folderGraphs = "graphs";
        String folderResources = "BULB_resources/r";
        String folderGraphs = "BULB_resources/g";
        //String folderResources = "resources/vlsi";

        if (args.length > 0) {
            File dfg = new File(args[0]);
            File res = new File(args[1]);
            listOfGraphFiles.add(dfg);
            listOfResFiles.add(res);
        } else {
            File folder = new File(folderGraphs);
            File[] files = folder.listFiles();
            Arrays.sort(Objects.requireNonNull(files)); //sort alphabetically
            listOfGraphFiles = List.of(files);

            folder = new File(folderResources);
            listOfResFiles = List.of(Objects.requireNonNull(folder.listFiles()));
        }

        // Store current System.out before assigning a new value
        PrintStream console = System.out;

        for (File dfg : listOfGraphFiles) {
            for (File res : listOfResFiles) {
                String dotFile = dfg.getName().substring(dfg.getName().lastIndexOf("/") + 1);
                String resFile = res.getName().substring(res.getName().lastIndexOf("/") + 1);

                String dotFilePath = folderGraphs + "/" + dotFile;
                String resFilePath = folderResources + "/" + resFile;

                String logDir = "benchmark/" + dotFile.replaceAll(".dot", "");
                String logName = logDir + "/" + dotFile.replaceAll(".dot", "") + "__" + resFile + ".txt";

                // Creating a File object that represents the log file
                File logFile = new File(logDir);
                if (!logFile.exists()) {
                    if (!logFile.mkdirs()) {
                        System.out.printf("Cannot create directory %s%n", logFile);
                        System.exit(-1);
                    }
                    // If you require it to make the entire directory path including parents,
                    // use directory.mkdirs(); here instead.
                }
                PrintStream o = null;
                try {
                    o = new PrintStream(logName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
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
                //System.out.printf("Input graph:%n%n%s%n", g.diagnose());

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
                List<Boolean> updateAlapBound = new ArrayList<>(Arrays.asList(true, false));

                for (String lBoundEstimator : lBoundEstimators) {
                    for (boolean lazy : lazyALAP) {
                        for (boolean updateAlap : updateAlapBound) {
                            System.out.printf("########################################" +
                                            "########################################" +
                                            "%nSTARTING BULB SCHEDULE with %s estimator, %s alap, updateAlapBound=%s%n%n",
                                    lBoundEstimator, lazy ? "lazy" : "normal", updateAlap ? "true" : "false");
                            BULB bulbScheduler = new BULB(g, lBoundEstimator, rc, asap, normal_alap, lazy_alap, lazy, updateAlap);
                            ExecutorService exs = Executors.newSingleThreadExecutor();
                            //List<Future<Schedule>> bulb2F =
                            //exs.invokeAll(List.of(new BULB(g, "ASAP", "LIST", rc, asap, normal_alap, lazy_alap, false)), 2, TimeUnit.SECONDS);
                            //        exs.invokeAll(List.of(new BULB(g, "ASAP", "LIST", rc, asap, normal_alap, lazy_alap, false)), 2, TimeUnit.SECONDS);

                            Future<Schedule> future = exs.submit(bulbScheduler);
                            Schedule bulb;
                            try {
                                bulb = future.get(15, TimeUnit.MINUTES);
                                bulbSchedules.put(bulbScheduler.getBulbGraph(), bulb);
                                completed++;
                            } catch (Exception e) {
                                if (e.getCause() instanceof BulbTimeoutException) {
                                    System.out.println("Timeout reached while computing " + dotFile + " with " + resFile);
                                    String alap = lazy ? "lazy" : "normal";
                                    timeouted.put(logName, lBoundEstimator + " estimator, " + alap + " alap, updateAlapBound=" + updateAlap);
                                } else {
                                    e.printStackTrace();
                                    System.out.println(e.getMessage());
                                    String alap = lazy ? "lazy" : "normal";
                                    exception.put(logName, lBoundEstimator + " estimator, " + alap + " alap, updateAlapBound=" + updateAlap);
                                }
                            } finally {
                                future.cancel(true); //this method will stop the running underlying task
                                while (!future.isDone()) {
                                    System.out.println("Waiting for completion of thread");
                                }
                                exs.shutdownNow();
                                //bulb.diagnose(rc, g.size());
                                Thread.sleep(1000);
                            }
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
                boolean allEqual = true;
                while (iterator.hasNext()) {
                    Map.Entry<BulbGraph, Schedule> next = iterator.next();
                    iterator.remove();
                    for (Map.Entry<BulbGraph, Schedule> e : copy.entrySet()) {
                        System.out.printf("Comparing %s %n\t\t with %s%n%n--->  ", next.getKey().getParameters(), e.getKey().getParameters());
                        if (!next.getValue().compare(e.getValue())) {
                            System.out.println(next.getValue().diagnose(rc, g.size()));
                            System.out.println(e.getValue().diagnose(rc, g.size()));
                            allEqual = false;
                            notEqual.add(logName + ": " + next.getKey().getParameters() + " with " + e.getKey().getParameters());
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

                if (allEqual) {
                    System.out.println(bulbSchedules.values().iterator().next().diagnose(rc, g.size()));
                }

                for (Map.Entry<BulbGraph, Schedule> e : bulbSchedules.entrySet()) {
                    System.out.println(e.getKey().printMetrics());
                    System.out.println(e.getKey().print());
                }

                System.setOut(console);
                System.out.printf("Completed %s on %s%n%n%n", dotFile, resFile);

                o.flush();
                o.close();
            }
        }

        System.setOut(console);
        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");

        PrintStream o = null;
        try {
            o = new PrintStream("benchmark/metrics.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.setOut(o);

        System.out.printf("%d tried combinations completed in given time%n", completed);

        System.out.printf("%d tried combinations timed out%n", timeouted.size());
        for (Map.Entry<String,String> t : timeouted.entrySet()) {
            System.out.printf("\t- %s timed out with %s%n", t.getKey(), t.getValue());
        }

        System.out.printf("%d tried combinations threw an exception, please check!%n", exception.size());
        for (Map.Entry<String,String> t : exception.entrySet()) {
            System.out.printf("\t- %s threw an exception with %s%n", t.getKey(), t.getValue());
        }

        System.out.printf("%n%n%d tried comparisons produced unequal schedules%n", notEqual.size());
        Collections.sort(notEqual);
        for (String s : notEqual) {
            System.out.printf("%s produced unequal schedules%n", s);
        }

        System.setOut(console);

        System.out.println("\n\n\nEND OF PROGRAM");
    }
}
