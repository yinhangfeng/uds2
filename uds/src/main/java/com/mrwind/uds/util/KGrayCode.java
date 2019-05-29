package com.mrwind.uds.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * n 位 m 进制数格雷码迭代
 */
public class KGrayCode implements Iterable<KGrayCode.Element>, Iterator<KGrayCode.Element> {

    private int n;
    private int m;
    private int current = -1;
    private int max;
    private short[] nm;
    private byte[] nmd;
    private Element y;

    /**
     * @param n 位数
     * @param m 进制
     */
    public KGrayCode(int n, int m) {
        if (m > Short.MAX_VALUE + 1 || m < 2) {
            throw new IllegalArgumentException("2 <= m <= " + (Short.MAX_VALUE + 1));
        }
        this.n = n;
        this.m = m;
        nm = new short[n];
        nmd = new byte[n];
        Arrays.fill(nmd, (byte) 1);
        double max = Math.pow(m, n) - 1;
        if (max > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("number overflow " + max);
        }

        this.max = (int) max;

        y = new Element();
        y.nm = nm;
    }

    @Override
    public Iterator<Element> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return current < max;
    }

    @Override
    public Element next() {
        if (current >= max) {
            throw new NoSuchElementException();
        }
        ++current;
        if (current == 0) {
            return y;
        }

        byte d;
        int i = 0;
        for (; ; ++i) {
            d = nmd[i];
            if (d > 0) {
                if (nm[i] == m - 1) {
                    nmd[i] = -1;
                    continue;
                }
            } else {
                if (nm[i] == 0) {
                    nmd[i] = 1;
                    continue;
                }
            }

            nm[i] += d;
            y.index = i;
            y.value = nm[i];
            break;
        }


        return y;
    }

    public static class Element {
        public short[] nm;
        public int index;
        public int value;

        @Override
        public String toString() {
            return "Element{" +
                    "nm=" + Arrays.toString(nm) +
                    ", index=" + index +
                    ", value=" + value +
                    '}';
        }
    }
}
