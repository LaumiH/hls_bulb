package scheduler;

import java.util.*;

public class ListScheduler {

    public Schedule schedule(List<Node> nodesToSchedule, Schedule partial, ResourceConstraint alpha, Map<Integer,
            List<Resource>> resUsage, Map<Integer, Set<String>> allocation) {
        int t = 0;
        boolean all_nodes_scheduled = false;
        boolean free_resources = true; // all resources used
        Map<Integer, List<Node>> priority_sorted_list = new TreeMap<>();
        // Map<Node,Integer> scheduled_list = new HashMap<>();
        Schedule schedule = new Schedule();
        int lmax = 0; // need to set value?
        ResourceConstraint res_used = new ResourceConstraint(); // currently working

        List<Node> nodes;
        int nmbr_of_successors = 0;
        for (Node nd : nodesToSchedule) { // Sort the nodes after number of successors

            nmbr_of_successors = nd.allSuccessors().size();
            nodes = priority_sorted_list.get(-nmbr_of_successors);

            if (nodes == null) {
                System.out.println("Empty List with priority : " + nmbr_of_successors);
                nodes = new ArrayList<Node>();
            } else {
                System.out.println("Added Node to key : " + nmbr_of_successors);
            }
            nodes.add(nd);

            priority_sorted_list.put(-nmbr_of_successors, nodes); // negative cause list only gives set in ascending order

        }

        ResourceType needed_res = null;
        ResourceType res_to_check = null;
        Set<String> constraint_res_types = alpha.getAllRes().keySet(); // all res constraint types
        Interval ii = null;
        List<Node> curr_working_nodes = new ArrayList<Node>();
        Node curr_working_node_w_min_delay = null;
        int min_delay = Integer.MAX_VALUE;
        do {
            //  check which restype is free
            // check which node with the highest priority can use it
            for (String type : constraint_res_types) {
                res_to_check = ResourceType.valueOf(type);

                // check if res has free spot
                if ((res_used.getCompatibleRes(res_to_check).size() + 1) <= alpha.getCompatibleRes(res_to_check).size()) { // free resource

                    //loop until all res are used or all nodes are checked
                    for (Integer key : priority_sorted_list.keySet()) { // loop through the all priorities
                        nodes = priority_sorted_list.get(key); // nodes with currently the highest priority

                        for (Node nd : nodes) {
                            needed_res = nd.getResourceType();
                            if (needed_res == res_to_check)
                                if (nd.top()) { // all predecessors of the node are finished
                                    // schedule node
                                    ii = new Interval(t, t + nd.getDelay());
                                    schedule.add(nd, ii, needed_res.name);

                                    if (nd.getDelay() < min_delay) { //keeping track of the node which finishes first
                                        min_delay = ii.length();
                                        curr_working_node_w_min_delay = nd;
                                    }

                                    curr_working_nodes.add(nd);

                                    priority_sorted_list.get(key).remove(nd); // remove object from list
                                }
                        }
                    }
                }
            }
            // checked all resources to schedule

            // check if min delay is minimum after second round
            for (Node nd : curr_working_nodes) {
                if (nd.getDelay() < min_delay) {
                    curr_working_node_w_min_delay = nd;
                }
            }


            t = t + min_delay;// increase the time step counter


            curr_working_nodes.remove(curr_working_node_w_min_delay); // removing node


            for (Integer key : priority_sorted_list.keySet()) { // check if all nodes have been scheduled;
                nodes = priority_sorted_list.get(key);
                if (nodes.isEmpty()) {
                    all_nodes_scheduled = true;
                } else {
                    all_nodes_scheduled = false;
                    break;
                }
            }
        } while (!all_nodes_scheduled);
        // ready list
        // evaluate set of nodes that are ready to be executed
        // Set of candidates are the nodes which predecessor finished

        // which resources are unused?
        //which resources are currently working

        // select node out of resources that can be executed with max prio with number of successors
        // s + G < alpha(r) (number of schedulable operations) + (busy resources) <= available resources
        // set time step for all scheduled nodes

        return schedule;
    }


}
