import bulb.BULB;
import scheduler.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

public class Main {

    public static void main(String[] args) throws IOException {

        System.out.println("START OF PROGRAM");
        boolean printSchedules = false;
        String folder_Graph = "C:/Users/Dirk/IdeaProjects/hls-bulb/graphs";
        File folder = new File(folder_Graph);
        File[] listOfGraphFiles = folder.listFiles();

        String folder_ressources = "C:/Users/Dirk/IdeaProjects/hls-bulb/resources/vlsi";
        File res_folder = new File(folder_ressources);
        File[] listOfResFiles = res_folder.listFiles();

        String logFilePlace =  "C:/Users/Dirk/IdeaProjects/hls-bulb/logFile.txt";
        File logFile = new File(logFilePlace);
        FileWriter fileWriter = new FileWriter(logFilePlace);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        ResourceConstraint rc = new ResourceConstraint();
        for(int i = 0; i < listOfGraphFiles.length ; i++) {
            System.out.println(listOfResFiles.length);
            for(int j = 0; j < listOfResFiles.length; j++){


            if (args.length > 1) {
                System.out.println("Reading resource constraints from " + listOfResFiles[j]+ "\n");
                rc.parse(listOfResFiles[j].getAbsolutePath());
            }

            DotReader dr = new DotReader(false);
            if (args.length < 1) {
                System.err.printf("Usage: scheduler dotfile%n");
                System.exit(-1);
            } else {
                System.out.printf("Scheduling %s%n", listOfGraphFiles[i].getAbsolutePath());
            }
            System.out.println("PARSING INPUT GRAPH");
            Graph g = dr.parse(listOfGraphFiles[i].getAbsolutePath());
            System.out.printf("Input graph:%n%n%s%n", g.diagnose());

            System.out.println("DOING ASAP SCHEDULE");
            Scheduler s = new ASAP();
            Schedule asap = s.schedule(g);
            System.out.printf("%nASAP%n%s%n", asap.diagnose(null, g.size()));
            //System.out.printf("cost = %s%n", asap.cost());
            asap.draw("schedules/ASAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
            System.out.println("FINISHED ASAP SCHEDULE");
            System.out.println();

            System.out.println("DOING LAZY ALAP SCHEDULE");
            Scheduler lazyAlap = new ALAP("lazy");
            Schedule lazy_alap = lazyAlap.schedule(g);
            System.out.printf("%nALAP%n%s%n", lazy_alap.diagnose(null, g.size()));
            //System.out.printf("cost = %s%n", alap.cost());
            lazy_alap.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
            System.out.println("FINISHED ALAP SCHEDULE");
            System.out.println();

            System.out.println("DOING NORMAL ALAP SCHEDULE");
            Scheduler normalAlap = new ALAP("normal");
            Schedule normal_alap = normalAlap.schedule(g);
            System.out.printf("%nALAP%n%s%n", normal_alap.diagnose(null, g.size()));
            //System.out.printf("cost = %s%n", alap.cost());
            normal_alap.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/") + 1));
            System.out.println("FINISHED ALAP SCHEDULE");
            System.out.println();



        /*
        ###########################################################################################
        ###########################################################################################
        ###########################################################################################
         */

            /*
             * 1
             */
//        long timestart = System.currentTimeMillis();
//
//        System.out.println("STARTING BULB SCHEDULE with \n" +
//                "\tasap lower bound estimator,\n" +
//                "\tupper bound estimator from paper,\n" +
//                "\tnormal alap schedule in enumerate for loop");
//        BULB bulbS1 = new BULB("ASAP", "PAPER", rc, asap, normal_alap, lazy_alap, false);
//        Schedule bulb1 = bulbS1.schedule(g);
//        System.out.printf("%nBULB%n%s%n", bulb1.diagnose(rc, g.size()));
//        bulbS1.getBulbGraph().print();
//        bulb1.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
//        System.out.println("END OF BULB SCHEDULE");
//
//        long timend = System.currentTimeMillis()-timestart;
//        System.out.println(timend);
//        System.out.println("Duration of Bulb 1:" +(timend/60) + " S" );
//        timend = timend % 60;
//        System.out.println("Duration of Bulb 1:" +(timend) + " mS" );

            /*
             * 2
             */
            //timestart = System.currentTimeMillis();
        /*
        System.out.println("STARTING BULB SCHEDULE with \n" +
                "\tasap lower bound estimator,\n" +
                "\tlist scheduler upper bound estimator,\n" +
                "\tnormal alap schedule in enumerate for loop");
        BULB bulbS2 = new BULB("ASAP", "LIST", rc, asap, normal_alap, lazy_alap, false);
        Schedule bulb2 = bulbS2.schedule(g);
        System.out.printf("%nBULB%n%s%n", bulb2.diagnose(rc, g.size()));
        bulbS2.getBulbGraph().print();
        bulb2.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
        System.out.println("END OF BULB SCHEDULE");

//        timend = System.currentTimeMillis()-timestart;
//        System.out.println(timend);
//        System.out.println("Duration of Bulb 2:" +(timend/60) + " S" );
//        timend = timend % 60;
//        System.out.println("Duration of Bulb 2:" +(timend) + " mS" );
//      */
//        /*
//         * 3
//         */
//        timestart = System.currentTimeMillis();
//
//        System.out.println("STARTING BULB SCHEDULE with \n" +
//                "\tlower bound estimator from paper,\n" +
//                "\tupper bound estimator from paper,\n" +
//                "\tlazy alap schedule in enumerate for loop");
//        BULB bulbS3 = new BULB("PAPER", "PAPER", rc, asap, normal_alap, lazy_alap, true);
//        Schedule bulb3 = bulbS3.schedule(g);
//        System.out.printf("%nBULB%n%s%n", bulb3.diagnose(rc, g.size()));
//        bulbS3.getBulbGraph().print();
//        bulb3.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
//        System.out.println("END OF BULB SCHEDULE");
//
//        timend = System.currentTimeMillis()-timestart;
//        System.out.println(timend);
//        System.out.println("Duration of Bulb 3:" +(timend/60) + " S" );
//        timend = timend % 60;
//        System.out.println("Duration of Bulb 3:" +(timend) + " mS" );
//
//        /*
//         * 4
//         */
//        timestart = System.currentTimeMillis();
//
//        System.out.println("STARTING BULB SCHEDULE with \n" +
//                "\tlower bound estimator from paper,\n" +
//                "\tupper bound estimator from paper,\n" +
//                "\nnormal alap schedule in enumerate for loop");
//        BULB bulbS4 = new BULB("PAPER", "PAPER", rc, asap, normal_alap, lazy_alap, false);
//        Schedule bulb4 = bulbS4.schedule(g);
//        System.out.printf("%nBULB%n%s%n", bulb4.diagnose(rc, g.size()));
//        bulbS4.getBulbGraph().print();
//        bulb4.draw("schedules/BULB_" + args[0].substring(args[0].lastIndexOf("/") + 1));
//        System.out.println("END OF BULB SCHEDULE");
//
//        timend = System.currentTimeMillis()-timestart;
//        System.out.println(timend);
//        System.out.println("Duration of Bulb 4:" +(timend/60) + " S" );
//        timend = timend % 60;
//        System.out.println("Duration of Bulb 4:" +(timend) + " mS" );
//
//        /*
//        ###########################################################################################
//        ###########################################################################################
//        ###########################################################################################
//         */
//        System.out.println("\n\n\nComparing schedules:");
//        System.out.println(
//                "\tasap lower bound estimator,\n" +
//                        "\tupper bound estimator from paper,\n" +
//                        "\tnormal alap schedule in enumerate for loop\n\n" +
//                        "with\n" +
//                        "\tasap lower bound estimator,\n" +
//                        "\tlist scheduler upper bound estimator,\n" +
//                        "\tnormal alap schedule in enumerate for loop\n");
//        bulb1.compare(bulb2);
//
//        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
//        System.out.println(
//                "\tasap lower bound estimator,\n" +
//                        "\tupper bound estimator from paper,\n" +
//                        "\tnormal alap schedule in enumerate for loop\n\n" +
//                        "with\n" +
//                        "\tlower bound estimator from paper,\n" +
//                        "\tupper bound estimator from paper,\n" +
//                        "\tlazy alap schedule in enumerate for loop\n");
//        bulb1.compare(bulb3);
//
//        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
//        System.out.println(
//                "\tasap lower bound estimator,\n" +
//                        "\tlist scheduler upper bound estimator,\n" +
//                        "\tnormal alap schedule in enumerate for loop\n\n" +
//                        "with\n" +
//                        "\tlower bound estimator from paper,\n" +
//                        "\tupper bound estimator from paper,\n" +
//                        "\tlazy alap schedule in enumerate for loop\n");
//        bulb2.compare(bulb3);
//
//        System.out.println("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
//        System.out.println(
//                "\tasap lower bound estimator,\n" +
//                        "\tlist scheduler upper bound estimator,\n" +
//                        "\tnormal alap schedule in enumerate for loop\n\n" +
//                        "with\n" +
//                        "\tlower bound estimator from paper,\n" +
//                        "\tupper bound estimator from paper,\n" +
//                        "\tnormal alap schedule in enumerate for loop\n");
//        bulb2.compare(bulb4);
//
//        System.out.println();
//        System.out.println();
//        bulbS1.getBulbGraph().print();
//        System.out.println(bulbS1.skippedNodes);
//        System.out.println(bulb1.diagnose(rc));
//
//        System.out.println();
//        bulbS2.getBulbGraph().print();
//        System.out.println(bulbS1.skippedNodes);
//        System.out.println(bulb2.diagnose(rc));
//
//        System.out.println();
//        bulbS3.getBulbGraph().print();
//        System.out.println(bulbS1.skippedNodes);
//        System.out.println(bulb3.diagnose(rc));
//
//        System.out.println();
//        bulbS4.getBulbGraph().print();
//        System.out.println(bulbS1.skippedNodes);
//        System.out.println(bulb4.diagnose(rc));
//
//        System.out.println("\n\n\nEND OF PROGRAM");
        }
        }
    }
}
