package scheduler;

import java.util.*;

public class ListScheduler {
    int test = 0;
    int runs = 0;

    public Schedule schedule(List<Node> nodesToSchedule, Schedule partial, ResourceConstraint alpha,
                             Map<Integer, List<Resource>> resUsage, Map<Integer, Set<String>> allocation) {
        int t = 0;
        boolean all_nodes_scheduled = false;

        Map<Integer, List<Node>> priority_sorted_list = new TreeMap<>();

        Schedule schedule = new Schedule();
        int lmax = 0; // need to set value?
        ResourceConstraint res_used = new ResourceConstraint(); // currently working

        List<Node> nodes;
        HashSet<Node> d;
        int nmbr_of_successors = 0;
        for (Node nd : nodesToSchedule) { // Sort the nodes after number of successors
            test = 0;
            amnt_of_successors(nd);
            nmbr_of_successors = test;
            nodes = priority_sorted_list.get(-nmbr_of_successors);

            if (nodes == null) {
                System.out.println("Empty List with priority : " + nmbr_of_successors + "    " + nd);
                nodes = new ArrayList<Node>();
            } else
                System.out.println("Added Node to key : " + nmbr_of_successors + "   " + nd);

            nodes.add(nd);

            priority_sorted_list.put(-nmbr_of_successors, nodes); // negative cause list only gives set in ascending order

        }

        ResourceType needed_res = null;
        Set<ResourceType> res_to_check = null;
        Set<String> constraint_res_types = alpha.getAllRes().keySet(); // all res constraint types
        Interval ii = null;
        Map<Node, String> curr_working_nodes = new HashMap<Node, String>();
        Map<Node, Interval> working_node_end_track = new HashMap<Node, Interval>();
        List<String> curr_free_res = new ArrayList<String>();
        Node curr_working_node_w_min_delay = null;

        if (constraint_res_types.isEmpty()) {
            System.out.println("empty");
            return null;
        }

        Set<ResourceType> p;
        for (String s : constraint_res_types) {
            System.out.println(s);
            curr_free_res.add(s);
            System.out.println(alpha.getAllRes().get(s));
            p = alpha.getAllRes().get(s);
            for (ResourceType res : p) {
                System.out.println(res);
            }
        }
        boolean res_scheduled = false;
        do {
             //  check which restype is free
            // check which node with the highest priority can use it
            for (String resource : curr_free_res) {
                res_to_check = alpha.getAllRes().get(resource);
                res_scheduled = false;
                //System.out.println("Res_cnst:" + resource);
                // check if res has free spot
                //loop until all res are used or all nodes are checked
                for (Integer key : priority_sorted_list.keySet()) { // loop through the all priorities
                    //System.out.println(key);
                    nodes = priority_sorted_list.get(key); // nodes with currently the highest priority
                    for (Node nd : nodes) {
                        System.out.println("Currently checked Node: " + nd);
                        needed_res = nd.getResourceType();
                        if (nd.top()) {

                            if (check_if_res_fits(res_to_check, needed_res)) {
                                // all predecessors of the node are finished
                                // schedule node
                                ii = new Interval(t, t + nd.getDelay());
                                schedule.add(nd, ii, resource);
                                res_scheduled = true;
                                curr_working_nodes.put(nd, resource);
                                priority_sorted_list.get(key).remove(nd); // remove object from list
                                System.out.println("Currently Working Nodes and Res: "+curr_working_nodes);
                                working_node_end_track.put(nd, ii);
                            }
                        } else {
                            System.out.println("Not all predecessors of Node: " + nd + " finished");
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
            System.out.println("");

            int min_delay = Integer.MAX_VALUE;
            System.out.println("Nodes in List with Intervall "+working_node_end_track);
            for (Node nd : working_node_end_track.keySet()) { // finding earliest end of a node
                if (working_node_end_track.get(nd).ubound < min_delay) {
                    min_delay = working_node_end_track.get(nd).ubound;
                }

            }




            Map<Node, Interval> copy_test = new HashMap<Node, Interval>(working_node_end_track);
            t =  min_delay+1;
            System.out.println("currently free Res "+curr_free_res);

            System.out.println("Next Time Step "+ t);
            for (Node nd :copy_test.keySet()){
                if(copy_test.get(nd).ubound < t){
                    System.out.println(nd);
                    if(!curr_free_res.contains(curr_working_nodes.get(nd))){
                        curr_free_res.add(curr_working_nodes.get(nd));
                    }
                    curr_working_nodes.remove(nd);
                    working_node_end_track.remove(nd);
                    for (Node n2 : nd.successors()) {
                        n2.handle(nd);
                    }
                }else {
                    if(curr_free_res.contains(curr_working_nodes.get(nd))){
                        curr_free_res.remove(curr_working_nodes.get(nd));
                    }
                }

            }
            System.out.println("Nodes in List with Intervall "+working_node_end_track);
            System.out.println("currently free Res "+curr_free_res);
            System.out.println();
            copy_test.clear();


            for (Integer key : priority_sorted_list.keySet()) { // check if all nodes have been scheduled;
                nodes = priority_sorted_list.get(key);
                if (nodes.isEmpty()) {
                    System.out.println("Nodes with priority " + key + " is empty");
                    all_nodes_scheduled = true;
                } else {
                    all_nodes_scheduled = false;
                    System.out.println("Nodes with priority " + key + " is not empty");
                    break;
                }
            }


            System.out.println(schedule.diagnose());
            runs++;
           /* if (runs == 4) {
                break;// check if min delay is minimum after second round
            }*/
            System.out.println("Finished one Iteration");

        } while (!all_nodes_scheduled);
        // ready list
        // evaluate set of nodes that are ready to be executed
        // Set of candidates are the nodes which predecessor finished

        // which resources are unused?
        //which resources are currently working

        // select node out of resources that can be executed with max prio with number of successors
        // s + G < alpha(r) (number of schedulable operations) + (busy resources) <= available resources
        // set time step for all scheduled nodes
        System.out.println("END OF LIST SCHEDULING !!!!!");
        return schedule;
    }

    void amnt_of_successors(Node nd) {
        if (nd != null) {
            test = test + 1;
            for (Node nd2 : nd.allSuccessors().keySet()) {
                amnt_of_successors(nd2);
            }
        }


    }

    boolean check_if_res_fits(Set<ResourceType> fromRes, ResourceType from_Node) {
        //System.out.println("check_res_type of " + from_Node);
        for (ResourceType type : fromRes) {
            //System.out.println("Restypes : " +type);
            if (type == from_Node) {
                //System.out.println("Res can be used");
                return true;
            }
        }
        //System.out.println("Res does not fit for node");
        return false;

    }
}
