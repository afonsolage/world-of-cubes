package com.lagecompany.util;

import static com.lagecompany.storage.Chunk.SIZE_SHIFT_X;
import static com.lagecompany.storage.Chunk.SIZE_SHIFT_Y;

/**
 *
 * @author Afonso Lage
 */
public abstract class MathUtils {

    public static int floorRound(float value) {
	if (value < 0) {
	    return (int) (value - 1);
	} else {
	    return (int) value;
	}
    }

    public static int floorDiv(int x, int y) {
	int mod = x / y;
	
	//Check if x and y have different signs by using a XOR.
	//Also if mod not equals to zero, round down.
	
	if ((x ^ y) < 0 && (mod * y != x)) {
	    mod--;
	}
	
	return mod;
    }

    public static int absMod(int operand, int operator) {
	if (operand < 0) {
	    return (operand % operator + operator) % operator;
	} else {
	    return operand % operator;
	}
    }
    
    public static int toVoxelIndex(int x, int y, int z) {
        return (x << SIZE_SHIFT_X) + (y << SIZE_SHIFT_Y) + z;
    }
}
