package com.CJFI.ch01.sec01;

/**
 * @author : pgajjar
 * @since : 7/1/16
 */
public class Test {
    private final int x;
    private final int y;

    public Test(int a, int b) {
        x = a;
        y = b;
    }

    public static Test getTest(Integer a, Integer b) {
        return new Test(a, b);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private int sum() {return x + y; }

    public String toString() {
        return new String("[" + x + ", " + y + "]");
    }
}
