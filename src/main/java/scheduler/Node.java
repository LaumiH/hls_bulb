package scheduler;

import java.util.*;

/**
 * This class represents a single node.
 */
public class Node {
    /**
     * ID for this node - unique name
     */
    public final String id;
    public final String label;

    // contains all successors as key - the value is the edge weight
    /**
     * Map of successor nodes and their edge weight
     */
    private final HashMap<Node, Integer> successors;
    // contains all predecessors as key - the value is the edge weight
    /**
     * Map of predecessors and their edge weight
     */
    private final HashMap<Node, Integer> predecessors;
    /**
     * Set of unhandled successors
     */
    private HashSet<Node> unhandled_succ;
    /**
     * Set of unhandled predecessors
     */
    private HashSet<Node> unhandled_pred;
    /**
     * Resource type of this node
     */
    private ResourceType rt;

    /**
     * @param id - ID of the new node
     * @param rt - resource type
     */
    public Node(String id, String label, ResourceType rt) {
        this.id = id;
        this.label = label;
        successors = new HashMap<>();
        predecessors = new HashMap<>();
        unhandled_succ = new HashSet<>();
        unhandled_pred = new HashSet<>();
        this.rt = rt;
    }

    /**
     * @param id - ID of the new node
     */
    public Node(String id, String label) {
        this.id = id;
        this.label = label;
        rt = ResourceType.getResourceType(id);
        successors = new HashMap<>();
        predecessors = new HashMap<>();
        unhandled_succ = new HashSet<>();
        unhandled_pred = new HashSet<>();
    }

    public Node clone() {
        Node clone = new Node(this.id, this.label, this.rt);
        HashMap<Node, Integer> predecessors = new HashMap<>(this.predecessors);
        HashMap<Node, Integer> successors = new HashMap<>(this.successors);
        for (Map.Entry<Node, Integer> entry : predecessors.entrySet()) {
            clone.prepend(entry.getKey(), entry.getValue());
            entry.getKey().remove(this);
            entry.getKey().append(clone, entry.getValue());
        }
        for (Map.Entry<Node, Integer> entry : successors.entrySet()) {
            clone.append(entry.getKey(), entry.getValue());
            entry.getKey().remove(this);
            entry.getKey().prepend(clone, entry.getValue());
        }
        for (Node node : this.unhandled_pred) {
            clone.unhandled_pred.add(node);
        }
        for (Node node : this.unhandled_succ) {
            clone.unhandled_succ.add(node);
        }
        return clone;
    }

    public HashSet<Node> getUnhandled_pred() {
        return unhandled_pred;
    }

    /**
     * Adds node n to the successors of this node. The edge weight is w.
     *
     * @param n - new successor
     * @param w - edge weight
     * @return null iff node was not appended, this node otherwise
     */
    public Node append(Node n, int w) {
        if (n == null) return null;
        successors.put(n, w);
        if (w == 0) unhandled_succ.add(n);
        return this;
    }

    /**
     * Adds node n to the predecessors of this node. The edge weight is w.
     *
     * @param n - new predecessor
     * @param w - edge weight
     * @return null iff node was not appended, this node otherwise
     */
    public Node prepend(Node n, int w) {
        if (n == null) return null;
        predecessors.put(n, w);
        if (w == 0) unhandled_pred.add(n);
        return this;
    }

    /**
     * Removes a node from successors and/or predecessors.
     *
     * @param n - Node to remove
     * @return true iff a node has been removed
     */
    public boolean remove(Node n) {
        unhandled_succ.remove(n);
        unhandled_pred.remove(n);
        return successors.remove(n) != null || predecessors.remove(n) != null;
    }

    public boolean removeById(String id) {
        for (Node n : unhandled_pred) {
            if (id.equals(n.id)) return unhandled_pred.remove(n);
        }

        for (Node n : unhandled_succ) {
            if (id.equals(n.id)) return unhandled_succ.remove(n);
        }
        return false;
    }

    /**
     * Mark a node as handled. Useful during scheduling.
     *
     * @param n - Node to mark handled.
     * @return true iff a node has been marked.
     */
    public boolean handle(Node n) {
        boolean success = unhandled_succ.remove(n) || unhandled_pred.remove(n);
        if (!success) {
            System.out.printf("%s does not contain %s in unhandled_pred or unhandled_succ. Try to remove by id%n", this, n);
            System.out.printf("unhandled_pred: %s; unhandled_succ: %s%n", this.unhandled_pred, unhandled_succ);
            //try to remove by id, as reference of nodes might just be wrong!
            success = this.removeById(n.id) || this.removeById(n.id);
        }
        return success;
    }

    /**
     * Check if this node is a root node. I.e. it has no predecessors with 0 edge weight
     *
     * @return true iff this node is a root
     */
    public boolean isRoot() {
        for (Node n : predecessors.keySet()) if (predecessors.get(n) == 0) return false;
        return true;
    }

    /**
     * Check if this node is a leaf node. I.e. it has no successors with 0 edge weight
     *
     * @return true iff this node is a leaf
     */
    public boolean isLeaf() {
        for (Node n : successors.keySet()) if (successors.get(n) == 0) return false;
        return true;
    }

    /**
     * Checks if any unhandled predecessors are left. For scheduling only - use isRoot() to test for
     * graph properties!
     *
     * @return true if this node has no unhandled predecessors, false otherwise.
     */
    public boolean top() {
        return unhandled_pred.size() == 0;
    }

    /**
     * TRUE if no unhandled successors are left. For scheduling only! Use isLeaf() to test for graph
     * properties!
     *
     * @return TRUE if this node has no unhandled successors, FALSE otherwise.
     */
    public boolean bottom() {
        return unhandled_succ.size() == 0;
    }

    /**
     * Mark all nodes as unhandled again
     */
    public void reset() {
        unhandled_succ = successors();
        unhandled_pred = predecessors();
    }

    /**
     * Return all successors of this node within one Iteration. Means all successors with edge weight
     * 0
     *
     * @return A set of all successors of this iteration
     */
    public HashSet<Node> successors() {
        HashSet<Node> succ = new HashSet<>();
        for (Node n : successors.keySet()) {
            if (successors.get(n) == 0) succ.add(n);
        }
        return succ;
    }

    /**
     * Return all successors with arbitrary edge weight
     *
     * @return A map of all successors and their edge weight
     */
    @SuppressWarnings("unchecked")
    public HashMap<Node, Integer> allSuccessors() {
        return (HashMap<Node, Integer>) successors.clone();
    }

    public Set<Node> reallyAllSuccessors() {
        //System.out.printf("\t\t\treallyAllSuccessors: calculating successors for %s%n", this.id);
        List<Node> successors = new ArrayList<>(this.successors.keySet());
        //System.out.printf("Successors of %s: %s%n", this, successors);
        Set<Node> allSuccessors = new HashSet<>();
        if (successors.isEmpty()) return new HashSet<>(successors);
        for (int i = 0; i < successors.size(); i++) {
            Node node = successors.get(i);
            //System.out.printf("%s is a successor of %s%n", node, this);
            allSuccessors.add(node);
            allSuccessors.addAll(node.reallyAllSuccessors());
        }
        //System.out.printf("\t\tcurrently collected successors: %s%n", allSuccessors);
        return allSuccessors;
    }

    /**
     * Return all predecessors of this node within one Iteration. Means all predecessors with edge
     * weight 0
     *
     * @return A set of all predecessors of this iteration
     */
    public HashSet<Node> predecessors() {
        HashSet<Node> pred = new HashSet<>();
        for (Node n : predecessors.keySet()) {
            if (predecessors.get(n) == 0) pred.add(n);
        }
        return pred;
    }

    /**
     * Return all predecessors with arbitrary edge weight
     *
     * @return A map of all predecessors and their edge weight
     */
    @SuppressWarnings("unchecked")
    public HashMap<Node, Integer> allPredecessors() {
        return (HashMap<Node, Integer>) predecessors.clone();
    }

    public String toString() {
        return id + "--" + this.label;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public boolean equals(Object e) {
        try {
            Node nd = (Node) e;
            return nd.id.equals(this.id);
        } catch (Throwable err) {
            System.out.println("Two compared nodes do not match, error thrown");
            System.exit(-1);
        }
        return false;
    }

    /**
     * Prints a string for diagnosis.
     *
     * @return a string giving information about the node and its successors and predecessors
     */
    public String diagnose() {
        Formatter f = new Formatter();

        f.format("%s (%s):%n", id, rt.name);
        if (successors.size() > 0) {
            f.format("  successors%n");
            for (Node nd : successors.keySet())
                f.format(
                        "   %s \t%d%n", nd, getSuccWeight(nd)); // write the successors plus the edge weight
        }
        if (predecessors.size() > 0) {
            f.format("  predecessors%n");
            for (Node nd : predecessors.keySet())
                f.format(
                        "   %s \t%d%n",
                        nd, getPredWeight(nd)); // write the predecessors plus the edge weight
        }

        String ret = f.toString();
        f.close();

        return ret;
    }

    /**
     * Returns the edge weight for the given predecessor.
     *
     * @param nd - the predecessor to find the weight for
     * @return weight for given predecessor
     */
    public int getPredWeight(Node nd) {
        return predecessors.get(nd);
    }

    /**
     * Returns the edge weight for the given successor.
     *
     * @param nd - the successor to find the weight for
     * @return weight for given successor
     */
    public int getSuccWeight(Node nd) {
        return successors.get(nd);
    }

    /**
     * Get the resource type for this node
     *
     * @return this nodes resource type
     */
    public ResourceType getResourceType() {
        return this.rt;
    }

    /**
     * Set the resource type for this node
     *
     * @param rt - new resource type of this node
     */
    public void setResourceType(ResourceType rt) {
        this.rt = rt;
    }

    /**
     * Get the delay for this nodes operation
     *
     * @return the delay/duration of this operation
     */
    public int getDelay() {
        return rt.delay;
    }
}
