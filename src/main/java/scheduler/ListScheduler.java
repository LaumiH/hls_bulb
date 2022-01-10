package scheduler;

import bulb.BULBException;
import bulb.BulbTimeoutException;

import java.util.*;

public class ListScheduler {
    int runs = 0;

    public Schedule schedule(final List<Node> nodesToSchedule, Schedule partial, ResourceConstraint alpha, Map<Integer, Set<String>> allocation) throws BULBException, BulbTimeoutException {
        //System.out.println("\n\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        //System.out.println("START OF LIST SCHEDULING");
        //partial.validate(alpha, nodesToSchedule.size());
        //System.out.println(partial.diagnose(alpha, nodesToSchedule.size()+partial.size()));
        //System.out.println("");
        //System.out.println("List of Nodes to Schedule" + nodesToSchedule);
        //for (Integer i : allocation.keySet()) {
            //System.out.printf("allocated in step %d: %s%n", i, allocation.get(i));
        //}

        if (nodesToSchedule.isEmpty()) {
            throw new BULBException("\n\nUmmmm, actually all nodes have been scheduled already. What exactly is it you want from me?");
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

        Set<String> constraint_res_types = alpha.getAllRes().keySet(); // all res constraint types
        Map<Node, String> curr_working_nodes = new HashMap<>(); // Node and real res
        Map<Node, Interval> working_node_end_track = new HashMap<>(); // Node and Interval
        Set<String> curr_free_res = new HashSet<>();

        Set<Node> nodes;
        for (Node nd : clonedNodesToSchedule) { // Sort the nodes after number of successors
            if (Thread.interrupted()) throw new BulbTimeoutException("");
            int succ = amnt_of_successors(nd) - 1;
            nodes = priority_sorted_list.get(-succ);

            if (nodes == null) {
                ////System.out.println("\t\tEmpty List with priority : " + succ);
                nodes = new HashSet<>();
            }

            nodes.add(nd);
            //System.out.println("\t\tAdded Node to key : " + succ + "   " + nd);

            priority_sorted_list.put(-succ, nodes); // negative cause list only gives set in ascending order
        }

        /*
        StringBuilder builder = new StringBuilder();
        builder.append("Priority sorted list: \n");
        for (Map.Entry<Integer, Set<Node>> entry : priority_sorted_list.entrySet()) {
            builder.append("\tPriority ").append(entry.getKey()).append(": ");
            builder.append(entry.getValue()).append("\n");
        }
        builder.append("\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
        System.out.println(builder);
        */

        //find the earliest step with yet unallocated resource(s)
        for (int step = 0; step < scheduleToWorkWith.length(); step++) {
            Set<String> allocated = allocation.get(step);
            if (allocated != null && allocated.size() < constraint_res_types.size()) {
                t = step;
                //System.out.println("First step with unallocated resources: " + t);
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
            //System.out.println("Current step: " + t);

            //get nodes and allocation in this step from partial schedule ()
            if (null != scheduleToWorkWith.nodes(t)) {
                for (Node node : scheduleToWorkWith.nodes(t)) {
                    curr_working_nodes.put(node, scheduleToWorkWith.getResources().get(node));
                    //be careful with interval, only add if it hasn't been there
                    //otherwise interval gets wrongly updated
                    if (!working_node_end_track.containsKey(node)) {
                        working_node_end_track.put(node, new Interval(t, t + node.getDelay() - 1));
                    }
                    curr_free_res.remove(scheduleToWorkWith.getResources().get(node));
                }
            }

            //System.out.println("Free res" + curr_free_res);
            //System.out.println("Working nodes" + curr_working_nodes);
            // check which restype is free
            // check which node with the highest priority can use it
            boolean removedInThisIteration = true;
            for (String resource : curr_free_res) {
                if (Thread.interrupted()) throw new BulbTimeoutException("");
                //if (!removedInThisIteration) {
                    //System.out.printf("Could not find any operation to schedule on resource %s in step %d%n", resource, t);
                //}
                removedInThisIteration = false;
                Set<ResourceType> res_to_check = alpha.getAllRes().get(resource);
                res_scheduled = false;

                // check if res has free spot
                //loop until all res are used or all nodes are checked
                for (Map.Entry<Integer, Set<Node>> entry : priority_sorted_list.entrySet()) { // loop through the all priorities
                    Set<Node> iteration = entry.getValue();
                    for (Node nd : iteration) {  // nodes with currently the highest priority
                        if (Thread.interrupted()) throw new BulbTimeoutException("");
                        //System.out.printf("step %d: Currently checked Node: %s%n", t, nd);
                        ResourceType needed_res = nd.getResourceType();
                        if (nd.top()) {
                            //check if in this step all predecessors from partial schedule are finished
                            if (!predFinishedInStep(t, nd, scheduleToWorkWith)) continue;
                            if (check_if_res_fits(resource, res_to_check, needed_res)) {
                                // all predecessors of the node are finished and res fits
                                //BUT a node in partial schedule might be scheduled on this res in duration of node!
                                Interval ii = new Interval(t, t + nd.getDelay() - 1);
                                boolean resOccupiedLater = false;
                                for (int x=ii.lbound; x<=Math.min(ii.ubound, partial.length()-1); x++) {
                                    if (partial.size() > 0) {
                                        Set<Node> nodesInStep = partial.nodes(x);
                                        if (nodesInStep == null) {
                                            continue;
                                        }
                                        for (Node inPartial : nodesInStep) {
                                            if (resource.equals(partial.getResources().get(inPartial))) {

                                                //System.out.printf("%s from partial schedule is occupying %s in step %d, " +
                                                //        "cannot allocate%n", inPartial, resource, x);
                                                resOccupiedLater = true;
                                                break;
                                            }
                                        }
                                        if (resOccupiedLater) break;
                                    }
                                }
                                if (resOccupiedLater) continue;

                                scheduleToWorkWith.add(nd, ii, resource);
                                res_scheduled = true;
                                curr_working_nodes.put(nd, resource);
                                entry.getValue().remove(nd); // remove node from priority list
                                removedInThisIteration = true;
                                //System.out.println("Currently Working Nodes and Res: " + curr_working_nodes);
                                working_node_end_track.put(nd, ii);
                            }
                        } else {
                            //check that there are really no predecessors left in priority sorted list
                            //references between nodes might be buggy -.-

                            Set<Node> unscheduledPred = new HashSet<>(nd.predecessors());
                            for (Node pred : nd.predecessors()) {
                                if (Thread.interrupted()) throw new BulbTimeoutException("");
                                if (scheduleToWorkWith.nodes().contains(pred)) {
                                    if (scheduleToWorkWith.slot(pred).ubound < t) {
                                        unscheduledPred.remove(pred);
                                    }
                                }
                            }
                            if (unscheduledPred.isEmpty()) {
                                throw new BULBException("top was wrong, there are all predecessors scheduled!");
                            }

                            //System.out.println("\tNot all predecessors of Node: " + nd + " finished");
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
            //System.out.println();

            //System.out.println("Preparing new step");

            if (working_node_end_track.isEmpty()) {
                //System.out.printf("No operation could be scheduled in %d, moving to next step%n", t);
                t++;
            } else {
                //System.out.println("\t\tNodes in List with Interval " + working_node_end_track);

                //get random end of a working node and then check if a node ends earlier
                int min_delay = working_node_end_track.values().iterator().next().ubound;
                for (Interval interval : working_node_end_track.values()) { // finding the earliest end of a node
                    min_delay = Math.min(min_delay, interval.ubound);
                }
                //System.out.printf("Minimal delay of nodes: %s%n", min_delay);
                t = min_delay + 1;
            }

            if (t<0) {
                throw new BULBException("t is somehow < 0?");
            }

            //System.out.println("currently free Res " + curr_free_res);

            //System.out.println("Next Time Step " + t);
            //System.out.println("Checking which nodes still occupy resources");
            Map<Node, Interval> copy_working_node_end_track = new HashMap<>(working_node_end_track);
            for (Node nd : copy_working_node_end_track.keySet()) {
                //System.out.printf("\tCheck if still running in step %d: %s with interval %s%n", t, nd, working_node_end_track.get(nd));
                if (copy_working_node_end_track.get(nd).ubound < t) {
                    curr_free_res.add(curr_working_nodes.get(nd));
                    curr_working_nodes.remove(nd);
                    working_node_end_track.remove(nd);
                    //handle successors of node, i.e. remove nd from unhandled_pred
                    //can only be done at end of step, otherwise dependencies will be disregarded ...
                    if (!clonedNodesToSchedule.contains(nd)) continue;
                    //meaning the node does not need to be removed from its successors' unhandled_pred list
                    for (Node n2 : nd.successors()) {
                        boolean success = n2.handle(nd);
                        if (!success) {
                            throw new BULBException("Could not perform handle on node " + n2);
                        }
                    }
                } else {
                    curr_free_res.remove(curr_working_nodes.get(nd));
                    //can return false when res was removed in earlier step and duration > 1
                }

            }
            //System.out.printf("Nodes in step %d (without partial): %s%n", t, working_node_end_track);
            //System.out.printf("Currently free resources: %s%n%n", curr_free_res);
            copy_working_node_end_track.clear();


            for (Map.Entry<Integer, Set<Node>> entry : priority_sorted_list.entrySet()) {
                // check if all nodes have been scheduled
                if (entry.getValue().isEmpty()) {
                    //System.out.println("\t\tNodes with priority " + entry.getKey() + " is empty");
                    all_nodes_scheduled = true;
                } else {
                    all_nodes_scheduled = false;
                    //System.out.printf("\t\tNodes with priority %s is not empty: %s%n", entry.getKey(), entry.getValue());
                    break;
                }
            }

            //System.out.println(scheduleToWorkWith.diagnose(alpha, nodesToSchedule.size()+partial.size()));
            scheduleToWorkWith.validate(alpha, nodesToSchedule.size()+ partial.size());
            runs++;

            //System.out.printf("Finished Iteration %d%n", runs - 1);
            if (runs > nodesToSchedule.size()*alpha.getAllRes().size()*1000) {
                throw new BULBException("Something is fishy with the ListScheduler, taking many iterations!");
            }

        } while (!all_nodes_scheduled);
        //System.out.println("END OF LIST SCHEDULING !!!!!");
        return scheduleToWorkWith;
    }

    int amnt_of_successors(Node nd) throws BulbTimeoutException {
        int succ = 0;
        if (nd != null) {
            succ++;
            for (Node nd2 : nd.allSuccessors().keySet()) {
                if (Thread.interrupted()) throw new BulbTimeoutException("");
                succ += amnt_of_successors(nd2);
            }
        }
        return succ;
    }

    boolean check_if_res_fits(String resName, Set<ResourceType> fromRes, ResourceType from_Node) {
        ////System.out.println("check_res_type of " + from_Node);
        for (ResourceType type : fromRes) {
            ////System.out.println("Restypes : " +type);
            if (type == from_Node) {
                //System.out.printf("\tRes %s can be used%n", resName);
                return true;
            }
        }
        //System.out.printf("\tRes %s does not fit for node%n", resName);
        return false;
    }

    boolean predFinishedInStep(int step, Node node, Schedule schedule) {
        for (Node scheduledPred : node.predecessors()) {
            if (schedule.slot(scheduledPred).ubound >= step) {
                //System.out.printf("\t\t%s cannot be scheduled because predecessor %s is not finished yet%n",
                //        node, scheduledPred);
                return false;
            }
        }
        return true;
    }

    Set<Node> getAllNodesFromPrioritySortedList(Map<Integer, Set<Node>> psl) {
        Set<Node> allNodes = new HashSet<>() {};
        for (Set<Node> nodes : psl.values()) {
            allNodes.addAll(nodes);
        }
        return allNodes;
    }
}
