import bulb.BULB;
import bulb.BulbGraph;
import bulb.BulbTimeoutException;
import bulb.Evaluator;
import scheduler.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    //this is to keep track of overall statistics in the benchmark
    static int completed = 0;
    static Map<String, String> timeouted = new TreeMap<>();     //filename, parameters -> treemap is sorted
    static Map<String, String> exception = new TreeMap<>();     //filename, parameters
    static List<String> notEqual = new ArrayList<>();    //filename, res and parameters of both
    static Map<String, List<Long>> executionTimes = new TreeMap<>();
    static Map<String, List<Long>> convergenceTimes = new TreeMap<>();

    public static void main(String[] args) throws Exception {

        System.out.println("START OF PROGRAM");

        // Store current System.out before assigning a new value
        PrintStream console = System.out;

        /*
        ###########################################################################################
        ###########################################################################################
        ###########################################################################################
         */

        var shutdownListener = new Thread() {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                PrintStream metrics = null;
                try {
                    metrics = new PrintStream("benchmark/metrics.txt.bak");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

                System.setOut(metrics);
                System.out.printf("%d tried combinations completed in given time%n", completed);
                System.out.printf("%d tried combinations timed out%n", timeouted.size());
                for (Map.Entry<String, String> t : timeouted.entrySet()) {
                    System.out.printf("\t- %s timed out with %s%n", t.getKey(), t.getValue());
                }
                System.out.printf("%d tried combinations threw an exception, please check!%n", exception.size());
                for (Map.Entry<String, String> t : exception.entrySet()) {
                    System.out.printf("\t- %s threw an exception with %s%n", t.getKey(), t.getValue());
                }
                System.out.printf("%n%n%d tried comparisons produced unequal schedules%n", notEqual.size());
                Collections.sort(notEqual);
                for (String s : notEqual) {
                    System.out.printf("%s produced unequal schedules%n", s);
                }

                Evaluator eval = new Evaluator();
                eval.evaluateLogs();

                System.out.println("\n" +
                        "\n##########################################\n" +
                        "\n##########################################\n" +
                        "\n##########################################\n\n");
                System.out.println("Execution times");

                for (Map.Entry<String, List<Long>> entry : executionTimes.entrySet()) {
                    System.out.printf("%s: ", entry.getKey());
                    for (Long time : entry.getValue()) {
                        System.out.printf("%d, ", time);
                    }
                    System.out.println();
                }

                System.out.println("\n" +
                        "\n##########################################\n" +
                        "\n##########################################\n" +
                        "\n##########################################\n\n");

                System.out.println("Convergence times");

                for (Map.Entry<String, List<Long>> entry : convergenceTimes.entrySet()) {
                    System.out.printf("%s: ", entry.getKey());
                    for (Long time : entry.getValue()) {
                        System.out.printf("%d, ", time);
                    }
                    System.out.println();
                }

                System.setOut(console);
                System.out.println("\n\n\nEND OF PROGRAM");
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownListener);

        /*
        ###########################################################################################
        ###########################################################################################
        ###########################################################################################
         */

        List<File> listOfGraphFiles = new ArrayList<>();
        List<File> listOfResFiles = new ArrayList<>();
        String folderGraphs = "graphs/timeout";
        //String folderResources = "BULB_resources/r";
        //String folderGraphs = "BULB_resources/g";
        String folderResources = "resources/vlsi";

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

        for (File dfg : listOfGraphFiles) {
            for (File res : listOfResFiles) {
                String dotFile = dfg.getName().substring(dfg.getName().lastIndexOf("/") + 1);
                String resFile = res.getName().substring(res.getName().lastIndexOf("/") + 1);

                String dotFilePath;
                String resFilePath;
                if (args.length > 0) {
                    dotFilePath = args[0];
                    resFilePath = args[1];
                } else {
                    dotFilePath = folderGraphs + "/" + dotFile;
                    resFilePath = folderResources + "/" + resFile;
                }

                String logDir = "benchmark/" + dotFile.replaceAll(".dot", "") + "/" + resFile;

                String preambleFile = logDir + "/preamble.txt";
                String logFile = logDir + "/logs.txt";
                String evalFile = logDir + "/eval.txt";

                //String preambleName = logDir  + "/" + dotFile.replaceAll(".dot", "") + "__" + resFile + ".txt";
                //String logName = logDir + "/" + dotFile.replaceAll(".dot", "") + "__" + resFile + ".txt";

                // Creating a File object that represents the log file
                File log = new File(logDir);
                if (!log.exists()) {
                    if (!log.mkdirs()) {
                        System.out.printf("Cannot create directory %s%n", logFile);
                        System.exit(-1);
                    }
                }

                PrintStream preamble = null;
                try {
                    preamble = new PrintStream(preambleFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

                System.setOut(console);
                System.out.printf("Scheduling %s on %s%n", dotFile, resFile);

                // Assign o to output stream
                System.setOut(preamble);

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

                System.out.println("DOING NORMAL ALAP SCHEDULE");
                Scheduler normalAlap = new ALAP("normal");
                Schedule normal_alap = normalAlap.schedule(g);
                System.out.println(normal_alap.diagnose(null, g.size()));
                //System.out.printf("cost = %s%n", alap.cost());
                normal_alap.draw("schedules/ALAP_" + dotFilePath.substring(dotFilePath.lastIndexOf("/") + 1));
                System.out.println("FINISHED ALAP SCHEDULE");
                System.out.println();

                preamble.flush();
                preamble.close();

                /*
                ###########################################################################################
                ###########################################################################################
                ###########################################################################################
                 */

                PrintStream logs = null;
                try {
                    logs = new PrintStream(logFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

                System.setOut(logs);

                Map<BulbGraph, Schedule> bulbSchedules = new HashMap<>();

                //List<String> lBoundEstimators = new ArrayList<>(Arrays.asList("ASAP", "OWN", "PAPER"));
                //List<String> ALAPBounds = new ArrayList<>(Arrays.asList("lazyALAP", "listSchedule"));

                List<String> lBoundEstimators = new ArrayList<>(Arrays.asList("PAPER", "OWN", "ASAP"));
                List<String> ALAPBounds = new ArrayList<>(Arrays.asList("listSchedule", "lazyALAP"));

                for (String lBoundEstimator : lBoundEstimators) {   //ASAP, OWN or PAPER
                    for (String ALAPBound : ALAPBounds) {
                        System.out.printf("%n########################################" +
                                        "########################################" +
                                        "%nSTARTING BULB SCHEDULE with %s estimator, %s alap bound%n%n",
                                lBoundEstimator, ALAPBound);

                        BULB bulbScheduler = new BULB(g, lBoundEstimator, ALAPBound, rc, asap, normal_alap);
                        ExecutorService exs = Executors.newSingleThreadExecutor();
                        //List<Future<Schedule>> bulb2F =
                        //exs.invokeAll(List.of(new BULB(g, "ASAP", "LIST", rc, asap, normal_alap, lazy_alap, false)), 2, TimeUnit.SECONDS);
                        //        exs.invokeAll(List.of(new BULB(g, "ASAP", "LIST", rc, asap, normal_alap, lazy_alap, false)), 2, TimeUnit.SECONDS);

                        Future<Schedule> future = exs.submit(bulbScheduler);
                        Schedule bulb;
                        try {
                            bulb = future.get(5, TimeUnit.MINUTES);
                            bulbSchedules.put(bulbScheduler.getBulbGraph(), bulb);
                            completed++;
                            List<Long> execTimes = executionTimes.get(bulbScheduler.getBulbGraph().getParameters());
                            if (execTimes != null) {
                                execTimes.add(bulbScheduler.getBulbGraph().getExecutionTime());
                                executionTimes.replace(bulbScheduler.getBulbGraph().getParameters(), execTimes);
                            } else {
                                execTimes = new ArrayList<>();
                                execTimes.add(bulbScheduler.getBulbGraph().getExecutionTime());
                                executionTimes.put(bulbScheduler.getBulbGraph().getParameters(), execTimes);
                            }
                            List<Long> convTimes = convergenceTimes.get(bulbScheduler.getBulbGraph().getParameters());
                            if (convTimes != null) {
                                convTimes.add(bulbScheduler.getBulbGraph().getConvergenceTime());
                                convergenceTimes.replace(bulbScheduler.getBulbGraph().getParameters(), convTimes);
                            } else {
                                convTimes = new ArrayList<>();
                                convTimes.add(bulbScheduler.getBulbGraph().getConvergenceTime());
                                convergenceTimes.put(bulbScheduler.getBulbGraph().getParameters(), convTimes);
                            }
                        } catch (Exception e) {
                            if (e instanceof TimeoutException) {
                                e.printStackTrace();
                                System.out.println("Timeout reached while computing " + dotFile + " with " + resFile);
                                timeouted.put(logDir, lBoundEstimator + " estimator, " + ALAPBound + " ALAPBound");
                            } else {
                                e.printStackTrace();
                                exception.put(logDir, lBoundEstimator + " estimator, " + ALAPBound + " ALAPBound");
                            }
                        } finally {
                            future.cancel(true); //this method will stop the running underlying task
                            while (!future.isDone()) {
                                System.out.println("Waiting for completion of thread");
                            }
                            exs.shutdownNow();
                            //bulb.diagnose(rc, g.size());
                            Thread.sleep(1000);
                            logs.flush();
                        }
                    }
                }

                logs.close();

                /*
                ###########################################################################################
                ###########################################################################################
                ###########################################################################################
                 */

                PrintStream eval = null;
                try {
                    eval = new PrintStream(evalFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

                System.setOut(eval);

                if (bulbSchedules.isEmpty()) {
                    System.out.printf("%s on %s: Timeout too low, list scheduler did not complete once%n", dotFile, resFile);
                    continue;
                }

                System.setOut(console);
                System.out.printf("%n%nFinished scheduling %s on %s, start comparison%n%n", dotFile, resFile);
                System.setOut(eval);

                System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
                System.out.println("Comparison of schedules");
                System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");

                Map<BulbGraph, Schedule> copy = new HashMap<>(bulbSchedules);
                Iterator<Map.Entry<BulbGraph, Schedule>> iterator = copy.entrySet().iterator();

                boolean allEqual = true;
                for (int x = 0; x < bulbSchedules.size() / 2; x++) {
                    while (iterator.hasNext()) {
                        Map.Entry<BulbGraph, Schedule> next = iterator.next();
                        iterator.remove();
                        for (Map.Entry<BulbGraph, Schedule> e : copy.entrySet()) {
                            System.out.printf("%nComparing %s %n\t\t with %s%n--->  ", next.getKey().getParameters(), e.getKey().getParameters());
                            if (!next.getValue().compare(e.getValue())) {
                                System.out.println(next.getValue().diagnose(rc, g.size()));
                                System.out.println(e.getValue().diagnose(rc, g.size()));
                                allEqual = false;
                                notEqual.add(logDir + ": " + next.getKey().getParameters() + " with " + e.getKey().getParameters());
                            }
                            next.getKey().compare(e.getKey());
                            x++;
                        }
                    }
                }

                if (allEqual) {
                    System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
                    System.out.println("Printing schedule");
                    System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
                    System.out.println(bulbSchedules.values().iterator().next().diagnose(rc, g.size()));
                }

                System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
                System.out.println("Printing BULB trees");
                System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");

                for (Map.Entry<BulbGraph, Schedule> e : bulbSchedules.entrySet()) {
                    System.out.println(e.getKey().printMetrics());
                    System.out.println(e.getKey().print());
                }

                System.setOut(console);
                System.out.printf("Completed %s on %s%n%n%n", dotFile, resFile);

                eval.flush();
                eval.close();
            }
        }

        System.setOut(console);

        System.out.println("\n\n\nEND OF PROGRAM");
    }
}
