package bulb;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Evaluator {

    List<String> noConvergence = new ArrayList<>();
    Map<String, Integer> faster = new TreeMap<>();

    public void evaluateLogs() {

        //read in logfiles of graphs
        String benchmarks = "benchmark";
        File folder = new File(benchmarks);
        List<File> logsPerGraph = List.of(Objects.requireNonNull(folder.listFiles()));

        for (File graphName : logsPerGraph) {
            if (graphName.getName().contains("metrics.txt.bak")) continue;
            List<File> logsPerRes = List.of(Objects.requireNonNull(graphName.listFiles()));
            for (File resName : logsPerRes) {
                for (File fileName : List.of(Objects.requireNonNull(resName.listFiles()))) {
                    String name = fileName.getName();
                    if (name.contains("eval.txt")) {
                        try (Scanner s = new Scanner(fileName)) {
                            while (s.hasNextLine()) {
                                String line = s.nextLine();
                                if (line.startsWith("Lower == Upper reached: false")) {
                                    noConvergence.add(graphName + "-" + resName);
                                } else if (line.startsWith("faster: ")) {
                                    String comparison = line.substring(7, line.indexOf("->")-1) + " faster than " +
                                            line.substring(line.indexOf("vs.")+4, line.lastIndexOf("->")-1);
                                    addFaster(comparison);
                                }
                            }
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }

        System.out.println("\n\n");

        for (String s : noConvergence) {
            System.out.println("no convergence in " + s);
        }

        System.out.println("\n\n");

        for (Map.Entry<String, Integer> entry : faster.entrySet()) {
            System.out.printf("%s %d times%n", entry.getKey(), entry.getValue());
        }
    }

    private void addFaster(String params) {
        if (faster.containsKey(params)) {
            faster.replace(params, faster.get(params)+1);
        } else {
            faster.put(params, 1);
        }
    }
}
