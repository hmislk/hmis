/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * @author safrin
 */
public enum MaxSize {

    _50,
    _100,
    _200,
    _300,
    _500,
    _1000,
    _1500,
    _2000;

    public int getValue() {
        switch (this) {
            case _50:
                return 50;
            case _100:
                return 100;
            case _200:
                return 200;
            case _300:
                return 300;
            case _500:
                return 500;
            case _1000:
                return 1000;
            case _1500:
                return 1500;
        }

        return 10;
    }
}
