package scheduler;

public enum ResourceType {

    MEM (2, 9.0, "Mem"),
    ADD (1, 1.0, "Add"),
    SUB (1, 1.4, "Sub"),
    MUL (4, 2.3, "Mul"),
    DIV (18, 4.3, "Div"),
    SH (1, 2.0, "Shift"),
    AND (1, 2.0, "And"),
    OR (1, 2.0, "Or"),
    CMP (1, 2.1, "Cmp"),
    OTHER (1, 1.0, "Other"),
    SLACK(1, 0.0, "Slack");

    /**
     * Delay (duration) of this resource type
     */
    public final Integer delay;

    /**
     * Weight of this resource type
     */
    public final Double weight;

    /**
     * Name of this resource type
     */
    public final String name;

    ResourceType(Integer delay, Double weight, String name) {
        this.delay = delay;
        this.weight = weight;
        this.name = name;
    }

    /**
     * Get the resource type for the given string.
     * @param id - the ID of the node to get the resource type from
     * @return the resource type. ResourceType.OTHER if none was found
     */
    public static ResourceType getResourceType(String id){
        if (id.contains("MUL")) {
            return ResourceType.MUL;
        } else if (id.contains("ADD")|| id.contains("INC")) {
            return ResourceType.ADD;
        } else if (id.contains("DIV")) {
            return ResourceType.DIV;
        } else if (id.contains("SUB")) {
            return ResourceType.SUB;
        } else if (id.contains("SH")) {
            return ResourceType.SH;
        } else if (id.contains("AND")) {
            return ResourceType.AND;
        } else if (id.contains("MEM")) {
            return ResourceType.MEM;
        } else if (id.contains("STORE") || id.contains("LOAD")) {
            return ResourceType.MEM;
        } else if (id.contains("OR")) {
            return ResourceType.OR;
        } else if (id.contains("IF") || id.contains("CMP")) {
            return ResourceType.CMP;
        } else {
            return ResourceType.OTHER;
        }
    }
}
