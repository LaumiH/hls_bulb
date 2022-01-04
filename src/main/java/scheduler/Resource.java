package scheduler;

public class Resource implements Comparable<Resource> {
    /**
     * Resource type
     */
    public final ResourceType rt;
    /**
     * time step for this resource
     */
    private Integer tstep;
    /**
     * number of required resources
     */
    private Integer num;

    public Resource(ResourceType rt, Integer step) {
        this.rt = rt;
        this.tstep = step;
        num = 0;
    }

    public Resource(ResourceType rt, Integer step, Integer num) {
        this.rt = rt;
        this.tstep = step;
        this.num = num;
    }

    public Resource clone() {
        return new Resource(this.rt, this.step(), this.num);
    }

    /**
     * Set the number of resources
     *
     * @param num - new resource count
     * @return null iff input was null, the num otherwise
     */
    public Integer set(Integer num) {
        if (num == null) return null;
        this.num = num;
        return this.num;
    }

    /**
     * Increase the count of this resource
     *
     * @return the new resource count
     */
    public Integer inc() {
        return ++num;
    }

    /**
     * Decrease the count of this resource
     *
     * @return the new resource count
     */
    public Integer dec() {
        if (--num < 0) num = 0;
        return num;
    }

    /**
     * @return the current resource count
     */
    public Integer getResourceCount() {
        return num;
    }

    /**
     * Set time step
     *
     * @param s - new time step
     * @return old time step
     */
    public Integer step(Integer s) {
        if (s == null) return null;
        Integer res = tstep;
        this.tstep = s;
        return res;
    }

    /**
     * @return the time step of this resource
     */
    public Integer step() {
        return tstep;
    }

    /**
     * @return weight (cost) of this resource
     */
    public Double weight() {
        return rt.weight * num;
    }

    //	public boolean equals(Object o) {
    //		Resource r = (Resource) o;
    //		if (r.rt.equals(rt))
    //			return weight().equals(r.weight());
    //		else
    //			return false;
    //	}

    public boolean equals(Object i) {
        if (this == i) return true;
        if (i == null) return false;
        if (i.getClass() != this.getClass()) return false;
        return this.hashCode() == i.hashCode();
    }

    public int compareTo(Resource r) {
        if (this == r) return 0;

        int res;

        res = r.weight().compareTo(weight());
        if (res != 0) return res;
        res = r.step().compareTo(tstep);
        if (res == 0) return 1;
        return res;
    }

    public String toString() {
        return "" + tstep + ":" + rt + ":" + weight();
    }
}
