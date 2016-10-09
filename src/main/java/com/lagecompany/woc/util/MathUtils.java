package com.lagecompany.woc.util;

import static com.lagecompany.woc.storage.Chunk.SIZE_SHIFT_X;
import static com.lagecompany.woc.storage.Chunk.SIZE_SHIFT_Y;

/**
 * Static class used to do some math utilities that is used across program.
 * 
 * @author Afonso Lage
 */
public abstract class MathUtils {

	/**
	 * Round down to int the given value, this method is necessary because the natural cast float to int doesn't round
	 * down using the zero as guide. So if you got a value 2.5f, it'll be rounded fine to 2, but if you got -2.5f, it'll
	 * be rounded to -3. This method will round 2.5f to 2 and -2.5f to -2.
	 * 
	 * @param value
	 *            Value to be rounded.
	 * @return An int value rounded.
	 */
	public static int floorRound(float value) {
		if (value < 0) {
			return (int) (value - 1);
		} else {
			return (int) value;
		}
	}

	/**
	 * Divide and round up if x or y is negative. Does a normal (/) division if both a positive. For example,
	 * floorDiv(4, 3) == 1 and (4 / 3) == 1. For example, floorDiv(-4, 3) == -2, whereas (-4 / 3) == -1. This method was
	 * borrowed from Java 8 Math.floorDiv, since jME3 still uses Java 7.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static int floorDiv(int x, int y) {
		int mod = x / y;

		// Check if x and y have different signs by using a XOR.
		// Also if mod not equals to zero, round down.

		if ((x ^ y) < 0 && (mod * y != x)) {
			mod--;
		}

		return mod;
	}

	/**
	 * Compute the absolute module of a given operand and operator.
	 * 
	 * @param operand
	 *            Operand number
	 * @param operator
	 *            Operator number
	 * @return the absolute module
	 */
	public static int absMod(int operand, int operator) {
		if (operand < 0) {
			return (operand % operator + operator) % operator;
		} else {
			return operand % operator;
		}
	}

	/**
	 * Converts a given 3D position (x, y, z) into a 1D position (index) to be used in a contiguous array.
	 * 
	 * @param x
	 *            The X value on 3D position.
	 * @param y
	 *            The X value on 3D position.
	 * @param z
	 *            The X value on 3D position.
	 * @return The 1D index value.
	 */
	public static int toVoxelIndex(int x, int y, int z) {
		return (x << SIZE_SHIFT_X) + (y << SIZE_SHIFT_Y) + z;
	}
}