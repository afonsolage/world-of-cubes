package test;

import com.lagecompany.storage.voxel.Voxel;

public class MemoryTest {

    public static void main(String[] args) {
	test();
	test();
	arrayEmpty();
	arrayEmpty();
	byteArrayAllocation();
	shortArrayAllocation();
	intArrayAllocation();

	long a = byteArrayAllocation();
	long b = shortArrayAllocation();
	long c = intArrayAllocation();

	System.out.println(String.format("%d, %d, %d", a, b, c));

	/*
	 * Conclusion:
	 *  An object has a minimum memory usage of 16 bytes: 8 bytes for object header, 4 bytes for reference and 
	 *  4 bytes just for padding.
	 * 
	 *  Out Voxel object has 4 bytes of data, so it will use 16 bytes anyway, becouse JVM doesnt need to pad it.
	 * 
	 *  An empty array uses 16 bytes also, but when data is added to it, there is an additional of 4 bytes to 
	 *  store array index and such and the data size.
	 * 
	 *  Our Voxel array uses 16 bytes when is empty and 40 bytes when has one object, 56 bytes when has two and
	 *  so on.
	 */
    }

    private static void test() {
	Runtime.getRuntime().gc();
	long a = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	Object dummy = new Object();

	Runtime.getRuntime().gc();
	long b = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    }


    private static long variables() {
	Runtime.getRuntime().gc();
	long a = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	Voxel v = null;

	Runtime.getRuntime().gc();
	long b = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	return b - a;
    }

    private static long arrayEmpty() {
	Runtime.getRuntime().gc();
	long a = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	Voxel[] v = new Voxel[]{};

	Runtime.getRuntime().gc();
	long b = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	return b - a;
    }

    private static long byteArrayAllocation() {
	Runtime.getRuntime().gc();
	long a = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	byte[] v = new byte[9];

	Runtime.getRuntime().gc();
	long b = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	return b - a;
    }

    private static long shortArrayAllocation() {
	Runtime.getRuntime().gc();
	long a = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	short[] v = new short[5];

	Runtime.getRuntime().gc();
	long b = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	return b - a;
    }

    private static long intArrayAllocation() {
	Runtime.getRuntime().gc();
	long a = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	int[] v = new int[5];

	Runtime.getRuntime().gc();
	long b = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

	return b - a;
    }
}
