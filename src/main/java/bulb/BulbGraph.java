package bulb;

import java.util.List;

public class BulbGraph {

    private final List<BulbNode> nodes;
    private BulbNode root;
    private int inspectedNodes = 0;
    private boolean lowerEqualsUpperReached = false;
    private String parameters;
    private int maxSkippedNodes;
    private long executionTime;
    private long convergenceTime;
    private int numberOfConvergences = 0;
    private int bestLatency;
    private int initialLatency;

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
        if (this.nodes.size() > 2000) {
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
        builder.append("Printing BULB metrics for ").append(parameters).append("\n")
                .append("BULB tree contains ").append(inspectedNodes).append(" inspected nodes").append("\n")
                .append(this.nodes.size() - this.inspectedNodes)
                .append(" tried intervals violated resource constraints and were not further inspected").append("\n")
                .append("Lower == Upper reached: ").append(lowerEqualsUpperReached).append("\n")
                .append("Best latency found: ").append(bestLatency).append("\n")
                .append("Initial best latency: ").append(initialLatency).append("\n")
                .append(maxSkippedNodes).append(" nodes could be skipped to find best schedule").append("\n")
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
        if (bestLatency > 0 && u_bound > bestLatency) {
            System.out.println("Schedule converged later with better u_bound, is that plausible?");
            System.exit(-1);
        }

        this.convergenceTime = convergenceTime;
    }

    public int getNumberOfConvergences() {
        return numberOfConvergences;
    }

    public void incrementNumberOfConvergences() {
        numberOfConvergences++;
        setLowerEqualsUpperReached(true);
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
}
