package scheduler;

import java.util.*;

public class ListScheduler {
    int runs = 0;

    public Schedule schedule(final List<Node> nodesToSchedule, Schedule partial, ResourceConstraint alpha, Map<Integer, Set<String>> allocation) {
        System.out.println("\n\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.println("START OF LIST SCHEDULING");
        System.out.println(partial.diagnose());
        System.out.println("");
        System.out.println("List of Nodes to Schedule" + nodesToSchedule);
        for (Integer i : allocation.keySet()) {
            System.out.printf("allocated in step %d: %s%n", i, allocation.get(i));
        }

        if (nodesToSchedule.isEmpty()) {
            System.out.println("\n\nUmmmm, actually all nodes have been scheduled already. What exactly is it you want from me?");
            System.exit(-1);
        }

        //do not overwrite nodes from BULB
        List<Node> clonedNodesToSchedule = new ArrayList<>(nodesToSchedule.size());
        for (Node node : nodesToSchedule) {
            clonedNodesToSchedule.add(node.clone());
        }

        //set correct reference of new nodes in clone and partial schedule
        //otherwise already scheduled predecessors will not be recognized

        for (Node pred : partial.nodes()) {
            Set<Node> successors = pred.allSuccessors().keySet();
            for (Node toSchedule : nodesToSchedule) {
                if (successors.contains(toSchedule)) {
                    for (Node x : clonedNodesToSchedule) {
                        if (x.equals(toSchedule)) {
                            x.removeById(pred.id);
                            break;
                        }
                    }
                }
            }
        }

        int t = 0;
        boolean all_nodes_scheduled = false;
        Map<Integer, Set<Node>> priority_sorted_list = new TreeMap<>();

        //clone partial schedule including resources
        Schedule scheduleToWorkWith = partial.clone();
        scheduleToWorkWith.getResources().putAll(partial.getResources());


        Set<String> constraint_res_types = alpha.getAllRes().keySet(); // all res constraint types
        Map<Node, String> curr_working_nodes = new HashMap<>(); // Node and real res
        Map<Node, Interval> working_node_end_track = new HashMap<>(); // Node and Interval
        Set<String> curr_free_res = new HashSet<>();

        Set<Node> nodes;
        for (Node nd : clonedNodesToSchedule) { // Sort the nodes after number of successors
            int succ = amnt_of_successors(nd) - 1;
            nodes = priority_sorted_list.get(-succ);

            if (nodes == null) {
                System.out.println("\t\tEmpty List with priority : " + succ);
                nodes = new HashSet<>();
            }

            nodes.add(nd);
            System.out.println("\t\tAdded Node to key : " + succ + "   " + nd);

            priority_sorted_list.put(-succ, nodes); // negative cause list only gives set in ascending order
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Priority sorted list: \n");
        for (Map.Entry<Integer, Set<Node>> entry : priority_sorted_list.entrySet()) {
            builder.append("\tPriority ").append(entry.getKey()).append(": ");
            builder.append(entry.getValue()).append("\n");
        }
        builder.append("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        System.out.println(builder);

        //find the earliest step with yet unallocated resource(s)
        for (int step = 0; step < scheduleToWorkWith.length(); step++) {
            Set<String> allocated = allocation.get(step);
            if (allocated != null && allocated.size() < constraint_res_types.size()) {
                t = step;
                System.out.println("First step with unallocated resources: " + t);
                break;
            }
        }

        //calculate initial free resources
        for (String s : constraint_res_types) {
            if (allocation.get(t) == null || !allocation.get(t).contains(s)) {
                curr_free_res.add(s);
            }
        }

        boolean res_scheduled;
        do {
            System.out.println("Current step: " + t);

            //get nodes and allocation in this step from partial schedule ()
            if (null != scheduleToWorkWith.nodes(t)) {
                for (Node node : scheduleToWorkWith.nodes(t)) {
                    curr_working_nodes.put(node, scheduleToWorkWith.getResources().get(node));
                    //be careful with inrterval, only add if it hasn't been there
                    //otherwise interval gets wrongly updated
                    if (!working_node_end_track.containsKey(node)) {
                        working_node_end_track.put(node, new Interval(t, t + node.getDelay() - 1));
                    }
                    curr_free_res.remove(scheduleToWorkWith.getResources().get(node));
                }
            }

            System.out.println("Free res" + curr_free_res);
            System.out.println("Working nodes" + curr_working_nodes);
            // check which restype is free
            // check which node with the highest priority can use it
            for (String resource : curr_free_res) {
                Set<ResourceType> res_to_check = alpha.getAllRes().get(resource);
                res_scheduled = false;

                // check if res has free spot
                //loop until all res are used or all nodes are checked
                for (Map.Entry<Integer, Set<Node>> entry : priority_sorted_list.entrySet()) { // loop through the all priorities
                    for (Node nd : entry.getValue()) {  // nodes with currently the highest priority
                        System.out.println("\t\tCurrently checked Node: " + nd);
                        ResourceType needed_res = nd.getResourceType();
                        if (nd.top()) {
                            if (check_if_res_fits(res_to_check, needed_res)) {
                                // all predecessors of the node are finished
                                Interval ii = new Interval(t, t + nd.getDelay() - 1);
                                scheduleToWorkWith.add(nd, ii, resource);
                                res_scheduled = true;
                                curr_working_nodes.put(nd, resource);
                                entry.getValue().remove(nd); // remove object from list
                                System.out.println("\t\tCurrently Working Nodes and Res: " + curr_working_nodes);
                                working_node_end_track.put(nd, ii);
                            }
                        } else {
                            System.out.println("\t\tNot all predecessors of Node: " + nd + " finished");
                        }
                        if (res_scheduled) {
                            break;
                        }
                    }
                    if (res_scheduled) {
                        break;
                    }
                }


            }
            System.out.println();

            int min_delay = Integer.MAX_VALUE;
            System.out.println("\t\tNodes in List with Interval " + working_node_end_track);
            for (Node nd : working_node_end_track.keySet()) { // finding earliest end of a node
                if (working_node_end_track.get(nd).ubound < min_delay) {
                    min_delay = working_node_end_track.get(nd).ubound;
                }
            }


            Map<Node, Interval> copy_working_node_end_track = new HashMap<>(working_node_end_track);
            t = min_delay + 1;
            //System.out.println("currently free Res " + curr_free_res);

            System.out.println("Next Time Step " + t);
            System.out.println("Checking which nodes still occupy resources");
            for (Node nd : copy_working_node_end_track.keySet()) {
                System.out.printf("Check if still running in step %d: %s with interval %s%n", t, nd, working_node_end_track.get(nd));
                if (copy_working_node_end_track.get(nd).ubound < t) {
                    curr_free_res.add(curr_working_nodes.get(nd));
                    curr_working_nodes.remove(nd);
                    working_node_end_track.remove(nd);
                    if (!clonedNodesToSchedule.contains(nd)) continue;
                    //meaning the node does not need to be removed from its successors' unhandled_pred list
                    for (Node n2 : nd.successors()) {
                        boolean success = n2.handle(nd);
                        if (!success) {
                            System.out.println("Could not perform handle on node " + n2);
                            System.exit(-1);
                        }
                    }
                } else {
                    curr_free_res.remove(curr_working_nodes.get(nd));
                    //can return false when res was removed in earlier step and duration > 1
                }

            }
            System.out.printf("\t\tNodes in step %d: %s%n", t, working_node_end_track);
            System.out.println("\t\tCurrently free resources " + curr_free_res);
            System.out.println();
            copy_working_node_end_track.clear();

            for (Map.Entry<Integer, Set<Node>> entry : priority_sorted_list.entrySet()) {
                // check if all nodes have been scheduled
                if (entry.getValue().isEmpty()) {
                    System.out.println("\t\tNodes with priority " + entry.getKey() + " is empty");
                    all_nodes_scheduled = true;
                } else {
                    all_nodes_scheduled = false;
                    System.out.println("\t\tNodes with priority " + entry.getKey() + " is not empty");
                    break;
                }
            }

            System.out.println("\nDISPLAY PARTIAL SCHEDULE");
            System.out.println(scheduleToWorkWith.diagnose());
            runs++;
           /* if (runs == 4) {
                break;// check if min delay is minimum after second round
            }*/
            System.out.printf("Finished Iteration %d%n", runs - 1);
            if (runs > 20) {
                System.out.println("Something went wrong in ListScheduler, taking way too many iterations!");
                System.exit(-1);
            }

        } while (!all_nodes_scheduled);
        System.out.println("END OF LIST SCHEDULING !!!!!");
        return scheduleToWorkWith;
    }

    int amnt_of_successors(Node nd) {
        int succ = 0;
        if (nd != null) {
            succ++;
            for (Node nd2 : nd.allSuccessors().keySet()) {
                succ += amnt_of_successors(nd2);
            }
        }
        return succ;
    }

    boolean check_if_res_fits(Set<ResourceType> fromRes, ResourceType from_Node) {
        System.out.println("check_res_type of " + from_Node);
        for (ResourceType type : fromRes) {
            //System.out.println("Restypes : " +type);
            if (type == from_Node) {
                //System.out.println("Res can be used");
                return true;
            }
        }
        System.out.println("Res does not fit for node");
        return false;

    }
}
