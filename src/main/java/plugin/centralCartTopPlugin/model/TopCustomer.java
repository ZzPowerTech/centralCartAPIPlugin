package plugin.centralCartTopPlugin.model;

import com.google.gson.annotations.SerializedName;

public class TopCustomer {

    @SerializedName("name")
    private String name;

    @SerializedName("total")
    private double total;

    @SerializedName("position")
    private int position;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "TopCustomer{" +
                "name='" + name + '\'' +
                ", total=" + total +
                ", position=" + position +
                '}';
    }
}

