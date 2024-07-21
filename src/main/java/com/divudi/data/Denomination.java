package com.divudi.data;

/**
 *
 * @author buddhika
 */
public class Denomination {
    private int value;
    private int count;

    public Denomination() {}

    public Denomination(int value, int count) {
        this.value = value;
        this.count = count;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
