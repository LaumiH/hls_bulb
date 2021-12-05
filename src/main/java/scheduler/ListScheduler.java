package scheduler;
import java.util.HashMap;
import java.util.Map;

public class ListScheduler  {

    public Schedule schedule(final Graph sourceGraph, ResourceConstraint alpha){
        int t = 0;
        boolean all_nodes_scheduled = false;
        boolean free_resources =  true; // all resources used?
        Map<Node,Integer> ready_list = new HashMap<>();
        Map<Node,Integer> priority_list = new HashMap<>();
        Map<Node,Integer> scheduled_list = new HashMap<>();
        Schedule schedule = new Schedule();
        int lmax = 0; // need to set value?
        // currently working
        do{
            // ready list
            // evaluate set of nodes that are ready to be executed
            // Set of candidates are the nodes which predecessor finished
            for(Node nd : sourceGraph){
                if(nd.top()){ // no unfinished Predecessor, so the node is ready to be scheduled
                    if(!scheduled_list.containsKey(nd))//node is not in scheduled list
                    ready_list.put(nd, nd.getDelay());

                }


            }
            // which resources are unused?

            //which resources are currently working


            // select node out of resources that can be executed with max prio
            // s + G < alpha(r) (number of schedulable operations) + (busy resources) <= available resources
            for (Node nd: priority_list.keySet()){
                scheduled_list.put(nd,nd.getDelay());

            }
            // set time step for all scheduled nodes
        t = t +1;
        all_nodes_scheduled = true;
        }while(all_nodes_scheduled); // until all nodes are scheduled

            return schedule;
        }


}
