/*
 * Copyright (c) 2015 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */

package com.windriver.projectiondemo;

public class Util {
    private Util() {}
    public static String dump(byte[] b, int n) {
        final int N = n > 0 ? n : b.length;
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < N; i++)
            s.append(String.format("%02X ", b[i] & 0xff));
        return s.toString();
    }
}