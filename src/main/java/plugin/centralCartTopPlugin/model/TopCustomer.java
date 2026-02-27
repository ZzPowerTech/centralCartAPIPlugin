package plugin.centralCartTopPlugin.model;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Representa um cliente top doador da CentralCart
 */
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopCustomer that = (TopCustomer) o;
        return Double.compare(that.total, total) == 0 &&
                position == that.position &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, total, position);
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

