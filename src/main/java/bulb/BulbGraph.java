package bulb;

import java.util.List;

public class BulbGraph {

    private final List<BulbNode> nodes;
    private BulbNode root;
    private int inspectedNodes = 0;
    private boolean lowerEqualsUpperReached = false;
    private String parameters;
    private int maxSkippedNodes;
    private long executionTime = -1;
    private long convergenceTime = -1;
    private int numberOfConvergences = 0;
    private int bestLatency;
    private int initialLatency;
    private int dfgNodes;
    private boolean receivedTimeout = true; //will be set to false when bulb completed

    public BulbGraph(List<BulbNode> nodes) {
        this.nodes = nodes;
    }

    public void addNode(BulbNode parent, BulbNode node) {
        if (parent == null) {
            this.root = node;
        } else {
            parent.getChildren().add(node);
        }
        node.setParent(parent);
        nodes.add(node);
    }

    public void incrementInspected() {
        this.inspectedNodes++;
    }

    public String print() {
        StringBuilder builder = new StringBuilder();
        if (this.nodes.size() > 3000) {
            builder.append("Buld tree is huge, will not print it");
            return builder.toString();
        } else {
            builder.append("Print BULB tree: ").append("\n")
                    .append(this.root.toString());
            return builder.toString();
        }
    }

    public String printMetrics() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n###########################################################\n");
        builder.append("Printing BULB metrics for ").append(parameters).append("\n");
        if (receivedTimeout) {
            builder.append("Received timeout while executing").append("\n");
        }
        builder.append("BULB tree contains ").append(inspectedNodes).append(" inspected nodes").append("\n")
                .append(this.nodes.size() - this.inspectedNodes)
                .append(" tried intervals violated resource constraints and were not further inspected").append("\n")
                .append("Lower == Upper reached: ").append(lowerEqualsUpperReached).append(", ")
                .append(numberOfConvergences).append(" times").append("\n")
                .append("Best latency found: ").append(bestLatency).append("\n")
                .append("Initial best latency: ").append(initialLatency).append("\n")
                .append(maxSkippedNodes).append(" out of ").append(dfgNodes).append(" DFG nodes could be skipped to find best schedule").append("\n")
                .append("It took ").append(convergenceTime).append(" milliseconds to converge").append("\n")
                .append("Scheduling took ").append(executionTime).append(" milliseconds").append("\n");
        return builder.toString();
    }

    public boolean isLowerEqualsUpperReached() {
        return lowerEqualsUpperReached;
    }

    public void setLowerEqualsUpperReached(boolean lowerEqualsUpperReached) {
        this.lowerEqualsUpperReached = lowerEqualsUpperReached;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public int getMaxSkippedNodes() {
        return maxSkippedNodes;
    }

    public void setMaxSkippedNodes(int number) {
        maxSkippedNodes = number;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public long getConvergenceTime() {
        return convergenceTime;
    }

    public void setConvergenceTime(long convergenceTime, int u_bound) {
        if (this.convergenceTime < 0) {
            System.out.printf("Setting initial convergence time at %s with latency %d%n", convergenceTime, u_bound);
            this.convergenceTime = convergenceTime;
        }
        if (u_bound < this.bestLatency) {
            System.out.printf("Schedule converged at %s with latency %d%n", convergenceTime, u_bound);
            this.convergenceTime = convergenceTime;
        } else {
            //System.out.println("Schedule converged with worse or equal latency");
        }
    }

    public int getNumberOfConvergences() {
        return numberOfConvergences;
    }

    public void incrementNumberOfConvergences() {
        numberOfConvergences++;
        lowerEqualsUpperReached = true;
    }

    public int getBestLatency() {
        return bestLatency;
    }

    public void setBestLatency(int bestLatency) {
        this.bestLatency = bestLatency;
    }

    public int getInitialLatency() {
        return initialLatency;
    }

    public void setInitialLatency(int initialLatency) {
        this.initialLatency = initialLatency;
    }

    public int getDfgNodes() {
        return dfgNodes;
    }

    public void setDfgNodes(int dfgNodes) {
        this.dfgNodes = dfgNodes;
    }

    public boolean isReceivedTimeout() {
        return receivedTimeout;
    }

    public void setReceivedTimeout(boolean receivedTimeout) {
        this.receivedTimeout = receivedTimeout;
    }

    public void compare(BulbGraph toCompare) {
        StringBuilder builder = new StringBuilder();
        long exTime = toCompare.executionTime-this.executionTime;
        if (exTime > 1) {
            builder.append("faster: ").append(this.parameters).append(" -> ").append(this.executionTime).append(", vs. ")
                    .append(toCompare.parameters).append(" -> ").append(toCompare.executionTime);
        } else if (exTime < -1) {
            builder.append("faster: ").append(toCompare.parameters).append(" -> ").append(toCompare.executionTime).append(", vs. ")
                    .append(this.parameters).append(" -> ").append(this.executionTime);
        } else {
            builder.append(this.parameters).append(" took the same time as ").append(toCompare.getParameters());
        }
        builder.append("\n");
        builder.append("\n");
        System.out.println(builder);
    }
}
