package com.divudi.data;

/**
 *
 * @author buddhika
 */
public class Denomination {
    private double value;
    private int count;

    public Denomination() {}

    public Denomination(int value, int count) {
        this.value = value;
        this.count = count;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
