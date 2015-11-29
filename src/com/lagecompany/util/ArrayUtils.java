package com.lagecompany.util;

/**
 *
 * @author afonsolage
 */
public class ArrayUtils {

    public static float[] append(float[] a, float[] b) {
        float[] result;

        if (a == null) {
            a = new float[]{};
        }

        if (b == null) {
            b = new float[]{};
        }

        result = new float[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);

        return result;
    }

    public static byte[] append(byte[] a, byte[] b) {
        byte[] result;

        if (a == null) {
            a = new byte[]{};
        }

        if (b == null) {
            b = new byte[]{};
        }

        result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);

        return result;
    }
}
