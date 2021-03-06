package scheduler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceConstraint {
    /**
     * Maps each resource to a set of supported types
     */
    private final Map<String, Set<ResourceType>> res;
    /**
     * Maps each resource type to a set of all resources that are compatible with this type
     */
    private final Map<ResourceType, Set<String>> operations;

    public ResourceConstraint() {
        res = new TreeMap<>();
        operations = new TreeMap<>();
        for (ResourceType op : ResourceType.values()) {
            operations.put(op, new TreeSet<>());
        }
    }

    /**
     * Reads the input and builds up the resource constraints
     *
     * @param input - Buffered reader pointing to the configuration file
     */
    private void lex(BufferedReader input) {
        String line;
        Pattern res_def = Pattern.compile("(\\w\\w*)\\s+(\\w\\w*).*");
        Matcher m;
        try {
            line = input.readLine();
            while (line != null) {
                if (line.startsWith("//")) { // Ignore comments
                    line = input.readLine();
                    continue;
                }
                m = res_def.matcher(line);
                if (m.matches()) {
                    HashSet<ResourceType> ops = new HashSet<>();
                    String[] opArray = line.split("\\s+");
                    for (int i = 1; i < opArray.length; i++) {
                        ResourceType currOp = ResourceType.getResourceType(opArray[i]);
                        ops.add(currOp);                            //Add operation to list of ops that this resource can execute
                        operations.get(currOp).add(opArray[0]);        //Add resource to list of res that can execute that op
                    }
                    res.put(opArray[0], ops);
                }

                line = input.readLine();
            }
        } catch (Throwable e) {
            System.err.printf("FATAL: Could not read from input%n");
            e.printStackTrace(System.err);
            System.exit(-1);
        }

        // Print ops
        System.out.println("Available resources:");
        for (String resName : res.keySet()) {
            System.out.print(resName + ":\t\t");
            for (ResourceType op : res.get(resName)) {
                System.out.print(op.name + ", ");
            }
            System.out.println();
        }
        System.out.println();

        // Print resS for each OP
        System.out.println("Available operations:");
        for (ResourceType op : operations.keySet()) {
            System.out.print(op.name + ":\t\t");
            for (String resName : operations.get(op)) {
                System.out.print(resName + ", ");
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Try to parse the file supplied and quit if anything goes wrong
     *
     * @param fn - Filename of the configuration file
     */
    public void parse(String fn) {
        BufferedReader file_reader;

        try {
            file_reader = new BufferedReader(new FileReader(fn));
            lex(file_reader);
        } catch (FileNotFoundException e) {
            System.err.printf("FATAL: File not found: %s%n", fn);
            System.exit(-1);
        }
    }

    /**
     * @return a map of resources and their supported resource set.
     */
    public Map<String, Set<ResourceType>> getAllRes() {
        return res;
    }

    /**
     * Get compatible resources for the supplied resource type.
     *
     * @param operation - Resource type of interest
     * @return a set of compatible resources
     */
    public Set<String> getCompatibleRes(ResourceType operation) {
        return operations.get(operation);
    }

}
