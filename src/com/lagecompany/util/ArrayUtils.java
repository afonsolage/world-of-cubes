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
}
