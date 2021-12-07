package scheduler;

import java.util.*;

public class ListScheduler  {

    public Schedule schedule(final Graph sourceGraph, ResourceConstraint alpha){
        int t = 0;
        boolean all_nodes_scheduled = false;
        boolean free_resources =  true; // all resources used
        Map<Integer,List<Node>> priority_sorted_list = new TreeMap<>();
        Map<Node,Integer> scheduled_list = new HashMap<>();
        Schedule schedule = new Schedule();
        int lmax = 0; // need to set value?
        ResourceConstraint res_used = new ResourceConstraint(); // currently working

        List<Node> nodes ;
        int nmbr_of_successors = 0;
        for(Node nd : sourceGraph){ // Sort the nodes after number of successors

            nmbr_of_successors = nd.allSuccessors().size();
            nodes = priority_sorted_list.get(nmbr_of_successors);

            if(nodes == null){
               System.out.println("Empty List with priority : "+nmbr_of_successors);
               nodes = new ArrayList<Node>();
            }else
            {
                System.out.println("Added Node to key : "+nmbr_of_successors);
            }
            nodes.add(nd);

            priority_sorted_list.put(-nmbr_of_successors,nodes); // negative cause list only gives set in ascending order

        }

        ResourceType needed_res = null;
        ResourceType res_to_check = null;
        Set<String> constraint_res_types = alpha.getAllRes().keySet(); // all res constraint types
        Interval  ii = null;
        do {
            //  check which restype is free
            // check which node with the highest priority can use it
            for(String type : constraint_res_types) {
                res_to_check = ResourceType.valueOf(type);

                // check if res has free spot
                if ((res_used.getRes(res_to_check).size() + 1) <= alpha.getRes(res_to_check).size()) { // free resource

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
                                    priority_sorted_list.get(key).remove(nd); // remove object from list
                                }
                        }
                    }
                }
            }
            // checked all resources to schedule





            t = t + 1; // increase the time step counter
            // and waiting until one res is free
            for(Integer key : priority_sorted_list.keySet()){ // check if all nodes have been scheduled;
                nodes = priority_sorted_list.get(key);
                if(nodes.isEmpty()){
                    all_nodes_scheduled = true;
                }else{
                    all_nodes_scheduled =  false;
                    break;
                }
            }
        }while(!all_nodes_scheduled);
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
