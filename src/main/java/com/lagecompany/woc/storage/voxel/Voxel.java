package com.lagecompany.woc.storage.voxel;

import java.util.ArrayList;
import java.util.List;

import com.lagecompany.woc.storage.Vec3;
import com.lagecompany.woc.util.CacheUtils;

/**
 * This class stores all static info needed by a voxel. This class doesn't hold and data direct, but instead, it
 * contains meta info about the Voxel.
 */
public abstract class Voxel {

	// VS = Visible Sides
	/**
	 * The bit flag of the front side of a voxel.
	 */
	public static final byte VS_FRONT = 0x01;
	/**
	 * The bit flag of the right side of a voxel.
	 */
	public static final byte VS_RIGHT = 0x02;
	/**
	 * The bit flag of the back side of a voxel.
	 */
	public static final byte VS_BACK = 0x04;
	/**
	 * The bit flag of the left side of a voxel.
	 */
	public static final byte VS_LEFT = 0x08;
	/**
	 * The bit flag of the top side of a voxel.
	 */
	public static final byte VS_TOP = 0x10;
	/**
	 * The bit flag of the down side of a voxel.
	 */
	public static final byte VS_DOWN = 0x20;
	/**
	 * The bit flag of all sides of a voxel.
	 */
	public static final byte VS_ALL = 0x3F;
	/**
	 * The bit flag of none sides of a voxel.
	 */
	public static final byte VS_NONE = 0x00;

	// VF = Voxel Flag
	/**
	 * The bit flag of the voxel data indicating this voxel is opaque.
	 */
	public static final short VF_OPAQUE = 0x4000; // 0 100 0000 0000 0000
	/**
	 * The bit flag of the voxel data indicating this voxel is special.
	 */
	public static final short VF_SPECIAL = 0x2000; // 0 010 0000 0000 0000

	// VT = Voxel Type
	/**
	 * The bit flag of the voxel data indicating this voxel is of type NONE (AIR).
	 */
	public static final short VT_NONE = 0x0000;
	/**
	 * The bit flag of the voxel data indicating this voxel is of type DIRT.
	 */
	public static final short VT_DIRT = 0x0001 | VF_OPAQUE;
	/**
	 * The bit flag of the voxel data indicating this voxel is of type GRASS.
	 */
	public static final short VT_GRASS = 0x0002 | VF_OPAQUE;
	/**
	 * The bit flag of the voxel data indicating this voxel is of type STONE.
	 */
	public static final short VT_STONE = 0x0003 | VF_OPAQUE;
	/**
	 * The bit flag of the voxel data indicating this voxel is of type TORCH.
	 */
	public static final short VT_TORCH = 0x0004 | VF_SPECIAL;

	/**
	 * The bit flag indicating the sun light value of a voxel.
	 */
	public static final byte SUN_LIGHT = 0x0F;

	protected static final List<VoxelInfo> infoList;

	/**
	 * Front side byte position on Voxel data structure.
	 */
	public static final int FRONT = 0;
	/**
	 * Right side byte position on Voxel data structure.
	 */
	public static final int RIGHT = 1;
	/**
	 * Back side byte position on Voxel data structure.
	 */
	public static final int BACK = 2;
	/**
	 * Left side byte position on Voxel data structure.
	 */
	public static final int LEFT = 3;
	/**
	 * Top side byte position on Voxel data structure.
	 */
	public static final int TOP = 4;
	/**
	 * Down side byte position on Voxel data structure.
	 */
	public static final int DOWN = 5;
	/**
	 * Type data byte position on Voxel data structure.
	 */
	public static final int TYPE = 6;
	/**
	 * Light data byte position on Voxel data structure.
	 */
	public static final int LIGHT = 8;

	/**
	 * An array containing all sides positions on a voxel data structure.
	 */
	public static final int[] ALL_SIDES = { FRONT, RIGHT, BACK, LEFT, TOP, DOWN };

	/**
	 * Size in bytes of Voxel data struct.
	 */
	public static final int SIZE = LIGHT + 1;

	/**
	 * BitWise check to be applied on a voxel side to check if it is visible.
	 */
	public static final byte SIDE_VISIBLE = 0x01; // 0000 0001
	/**
	 * BitWise check to be applied on a voxel side to check if it is already merged.
	 */
	public static final byte SIDE_MERGED = 0x02; // 0000 0010
	/**
	 * BitWise get light value of a voxel side.
	 */
	public static final byte SIDE_LIGHT = 0x3C; // 0011 1100

	/**
	 * Number of bits to be shifted when reading merged value on a voxel side.
	 */
	public static final int SIDE_MERGED_SHIFT = 1;
	/**
	 * Number of bits to be shifted when reading light value on a voxel side.
	 */
	public static final int SIDE_LIGHT_SHIFT = 2;

	/**
	 * BitWise get normal light value on a voxel.
	 */
	public static final short LIGHT_NORMAL = 0xF; // 0000 1111
	/**
	 * BitWise get normal light value on a voxel.
	 */
	public static final short LIGHT_SUN = 0xF0; // 1111 0000
	/**
	 * Number of bits to be shifted when reading sunlight value on a voxel.
	 */
	public static final int LIGHT_SUN_SHIFT = 4;

	static {
		infoList = new ArrayList<>();
		loadInfo();
	}

	private static void loadInfo() {
		infoList.add(new DirtVoxelInfo());
		infoList.add(new StoneVoxelInfo());
		infoList.add(new TorchInfo());
	}

	/*																							
	 * 																							
	 *      v7 +-------+ v6																			
	 *      / |     /  |	 																	
	 *   v3 +-------+v2|																		
	 *      |v4+-------+ v5																		
	 *      | /     | /																			
	 *      +-------+ 																			
	 *     v0        v1																			
	 *     																						
	 *     Y																					
	 *     |  Z																					
	 *     | /																					
	 *     +----x																				
	 */

	/**
	 * Compute the v0 of a 3D cube using the given center of cube. This function uses a cached float array to avoid
	 * creation of unnecessary objects on heap
	 * 
	 * @param x
	 *            X position of the center of cube.
	 * @param y
	 *            Y position of the center of cube.
	 * @param z
	 *            Z position of the center of cube.
	 * @return A 3d point representing the v0 of a 3D cube
	 */
	public static float[] v0(int x, int y, int z) {
		return CacheUtils.get3fArray(x, y, z + 1);
	}

	/**
	 * Compute the v1 of a 3D cube using the given center of cube. This function uses a cached float array to avoid
	 * creation of unnecessary objects on heap
	 * 
	 * @param x
	 *            X position of the center of cube.
	 * @param y
	 *            Y position of the center of cube.
	 * @param z
	 *            Z position of the center of cube.
	 * @return A 3d point representing the v1 of a 3D cube
	 */
	public static float[] v1(int x, int y, int z) {
		return CacheUtils.get3fArray(x + 1, y, z + 1);
	}

	/**
	 * Compute the v2 of a 3D cube using the given center of cube. This function uses a cached float array to avoid
	 * creation of unnecessary objects on heap
	 * 
	 * @param x
	 *            X position of the center of cube.
	 * @param y
	 *            Y position of the center of cube.
	 * @param z
	 *            Z position of the center of cube.
	 * @return A 3d point representing the v2 of a 3D cube
	 */
	public static float[] v2(int x, int y, int z) {
		return CacheUtils.get3fArray(x + 1, y + 1, z + 1);
	}

	/**
	 * Compute the v3 of a 3D cube using the given center of cube. This function uses a cached float array to avoid
	 * creation of unnecessary objects on heap
	 * 
	 * @param x
	 *            X position of the center of cube.
	 * @param y
	 *            Y position of the center of cube.
	 * @param z
	 *            Z position of the center of cube.
	 * @return A 3d point representing the v3 of a 3D cube
	 */
	public static float[] v3(int x, int y, int z) {
		return CacheUtils.get3fArray(x, y + 1, z + 1);
	}

	/**
	 * Compute the v4 of a 3D cube using the given center of cube. This function uses a cached float array to avoid
	 * creation of unnecessary objects on heap
	 * 
	 * @param x
	 *            X position of the center of cube.
	 * @param y
	 *            Y position of the center of cube.
	 * @param z
	 *            Z position of the center of cube.
	 * @return A 3d point representing the v4 of a 3D cube
	 */
	public static float[] v4(int x, int y, int z) {
		return CacheUtils.get3fArray(x, y, z);
	}

	/**
	 * Compute the v5 of a 3D cube using the given center of cube. This function uses a cached float array to avoid
	 * creation of unnecessary objects on heap
	 * 
	 * @param x
	 *            X position of the center of cube.
	 * @param y
	 *            Y position of the center of cube.
	 * @param z
	 *            Z position of the center of cube.
	 * @return A 3d point representing the v5 of a 3D cube
	 */
	public static float[] v5(int x, int y, int z) {
		return CacheUtils.get3fArray(x + 1, y, z);
	}

	/**
	 * Compute the v6 of a 3D cube using the given center of cube. This function uses a cached float array to avoid
	 * creation of unnecessary objects on heap
	 * 
	 * @param x
	 *            X position of the center of cube.
	 * @param y
	 *            Y position of the center of cube.
	 * @param z
	 *            Z position of the center of cube.
	 * @return A 3d point representing the v6 of a 3D cube
	 */
	public static float[] v6(int x, int y, int z) {
		return CacheUtils.get3fArray(x + 1, y + 1, z);
	}

	/**
	 * Compute the v7 of a 3D cube using the given center of cube. This function uses a cached float array to avoid
	 * creation of unnecessary objects on heap
	 * 
	 * @param x
	 *            X position of the center of cube.
	 * @param y
	 *            Y position of the center of cube.
	 * @param z
	 *            Z position of the center of cube.
	 * @return A 3d point representing the v7 of a 3D cube
	 */
	public static float[] v7(int x, int y, int z) {
		return CacheUtils.get3fArray(x, y + 1, z);
	}

	private static VoxelInfo getInfo(short type) {
		for (VoxelInfo vi : infoList) {
			if (vi.getCode() == type) {
				return vi;
			}
		}

		return null;
	}

	public static float[] getColor(short type, short side) {
		return getInfo(type).getColor(side);
	}

	public static float[] getTile(short type, short side) {
		return getInfo(type).getTile(side);
	}

	/**
	 * Converts a Vec3 direction to a Voxel Side represented by an int.
	 * 
	 * @param dir
	 *            The Vec3 direction to convert into Voxel Side
	 * @return An int representing the Voxel Side.
	 */
	public static int directionToSide(Vec3 dir) {
		if (dir == Vec3.FORWARD) {
			return Voxel.FRONT;
		} else if (dir == Vec3.RIGHT) {
			return Voxel.RIGHT;
		} else if (dir == Vec3.BACKWARD) {
			return Voxel.BACK;
		} else if (dir == Vec3.LEFT) {
			return Voxel.LEFT;
		} else if (dir == Vec3.UP) {
			return Voxel.TOP;
		} else {
			return Voxel.DOWN;
		}
	}
}