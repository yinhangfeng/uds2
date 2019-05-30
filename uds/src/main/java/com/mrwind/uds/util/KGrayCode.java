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
    private Element e;

    /**
     * @param n 位数
     * @param m 进制
     */
    public KGrayCode(int n, int m) {
        if (m > Short.MAX_VALUE + 1 || m < 1) {
            throw new IllegalArgumentException("1 <= m <= " + (Short.MAX_VALUE + 1));
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

        e = new Element();
        e.nm = nm;
    }

    public void reset(int n) {
        if (n > this.nm.length) {
            throw new IllegalArgumentException();
        }
        this.n = n;
        Arrays.fill(nm, (short) 0);
        Arrays.fill(nmd, (byte) 1);
        this.max = (int) (Math.pow(m, n) - 1);

        e.index = 0;
        e.value = 0;
        e.oldValue = -1;
        current = -1;
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
            return e;
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

            e.oldValue = nm[i];
            nm[i] += d;
            e.index = i;
            e.value = nm[i];
            break;
        }


        return e;
    }

    public static class Element {
        public short[] nm;
        public int index;
        public int value;
        public int oldValue = -1;

        @Override
        public String toString() {
            return "Element{" +
                    "nm=" + Arrays.toString(nm) +
                    ", index=" + index +
                    ", value=" + value +
                    ", oldValue=" + oldValue +
                    '}';
        }
    }
}
