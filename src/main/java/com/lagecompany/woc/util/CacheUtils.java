package com.lagecompany.woc.util;

import com.lagecompany.woc.storage.voxel.VoxelReference;

/**
 * A class that just holds cached objects.
 * 
 * @author Afonso Lage
 *
 */
class CacheObject {
	/**
	 * A float array that represents a Vector3f.
	 */
	final float[] f3Array = { 0.0f, 0.0f, 0.0f };
	
	/**
	 * A Voxel Reference object.
	 */
	final VoxelReference voxelReference = new VoxelReference();
}

/**
 * A utility classe that holds a ThreadLocal reference to a CacheObject, which contains per-thread cache objects to be
 * used.
 * 
 * @author Afonso Lage
 *
 */
public abstract class CacheUtils {
	/**
	 * A ThreadLocal reference of a CacheObject, used on subsequent call of this class.
	 */
	private static final ThreadLocal<CacheObject> cacheObject = new ThreadLocal<CacheObject>();

	static {
		cacheObject.set(new CacheObject());
	}

	/**
	 * Get the cached float array that represents a Vector3f. This method clear the array with value 0.0f to avoid using
	 * previous cached values.
	 * 
	 * @return A cached float array object.
	 */
//	public static float[] get3fArray() {
//		return get3fArray(0.0f, 0.0f, 0.0f);
//	}

	/**
	 * Get the cached float array that represents a Vector3f. This method sets the array with the given values.
	 * 
	 * @param x
	 *            Sets the value on position 0 of array.
	 * @param y
	 *            Sets the value on position 1 of array.
	 * @param z
	 *            Sets the value on position 2 of array.
	 * @return A cached float array object.
	 */
//	public static float[] get3fArray(float x, float y, float z) {
//		CacheObject obj = cacheObject.get();
//		obj.f3Array[0] = x;
//		obj.f3Array[1] = y;
//		obj.f3Array[2] = z;
//		return obj.f3Array;
//	}
	
	/**
	 * Get the cached voxel reference and reset it's value. The position on Voxel Reference will be set to (0, 0, 0)
	 * @param buffer The new Chunk Buffer used by the cache reference.
	 * @param offset The new offset on Chunk Buffer used by the cache reference.
	 * @return The cached voxel reference.
	 */
//	public static VoxelReference getVoxelReference(ChunkBuffer buffer, int offset) {
//		return cacheObject.get().voxelReference.reset(buffer, offset, 0, 0, 0);
//	}
	
	/**
	 * Get the cached voxel reference and reset it's value.
	 * @param buffer The new Chunk Buffer used by the cache reference.
	 * @param offset The new offset on Chunk Buffer used by the cache reference.
	 * @param x The X position of Voxel Reference.
	 * @param y The Y position of Voxel Reference.
	 * @param z The Z position of Voxel Reference.
	 * @return The cached voxel reference.
	 */
//	public static VoxelReference getVoxelReference(ChunkBuffer buffer, int offset, int x, int y, int z) {
//		return cacheObject.get().voxelReference.reset(buffer, offset, x, y, z);
//	}
}
