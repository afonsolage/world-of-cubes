package com.lagecompany.woc.storage.voxel;

import com.lagecompany.woc.storage.ChunkBuffer;
import com.lagecompany.woc.storage.Vec3;

/**
 * This class represents a reference of a single Voxel inside a Chunk Buffer. This class is a utility to help in working
 * with one single voxel.
 * 
 * @author TI.Afonso
 */
public class VoxelReference {

	/**
	 * Check if the given type is a special one.
	 * 
	 * @param type
	 *            The type to be checked.
	 * @return True if this type is special, False otherwhise.
	 */
	public static boolean isSpecial(short type) {
		return (type & Voxel.VF_SPECIAL) == Voxel.VF_SPECIAL;
	}

	/**
	 * Check if the given type is opaque.
	 * 
	 * @param type
	 *            The type to be checked.
	 * @return True if this type is opaque, False otherwhise.
	 */
	public static boolean isOpaque(short type) {
		return (type & Voxel.VF_OPAQUE) == Voxel.VF_OPAQUE;
	}

	/**
	 * The position of vertex. This variable is used to compute the offset inside the chunk buffer.
	 */
	public final Vec3 position;

	/**
	 * The offset of the voxel represented by this class, inside the chunk buffer.
	 */
	public int offset;

	/**
	 * The Chunk Buffer in witch the voxel reference by this class is contained.
	 */
	public ChunkBuffer buffer;

	/**
	 * Constructs an empty Voxel Reference.
	 */
	public VoxelReference() {
		position = new Vec3();
	}

	/**
	 * Constructs a Voxel Reference pointing to the given ChunkBuffer and offset
	 * 
	 * @param buffer
	 *            The ChunkBuffer which contains the voxel.
	 * @param offset
	 *            The offset of the voxel inside the ChunkBuffer.
	 */
	public VoxelReference(ChunkBuffer buffer, int offset) {
		this.buffer = buffer;
		this.offset = offset;
		position = new Vec3();
	}

	/**
	 * Constructs a Voxel Reference pointing to the given ChunkBuffer and offset and sets it's position.
	 * 
	 * @param buffer
	 *            The ChunkBuffer which contains the voxel.
	 * @param offset
	 *            The offset of the voxel inside the ChunkBuffer.
	 * @param x
	 *            The X value to be set on position.
	 * @param y
	 *            The Y value to be set on position.
	 * @param z
	 *            The Z value to be set on position.
	 */
	public VoxelReference(ChunkBuffer buffer, int offset, int x, int y, int z) {
		this.buffer = buffer;
		this.offset = offset;
		this.position = new Vec3(x, y, z);
	}

	/**
	 * Resets the Voxel Reference using the given ChunkBuffer and offset and sets it's position.
	 * 
	 * @param buffer
	 *            The ChunkBuffer which contains the voxel.
	 * @param offset
	 *            The offset of the voxel inside the ChunkBuffer.
	 * @param x
	 *            The X value to be set on position.
	 * @param y
	 *            The Y value to be set on position.
	 * @param z
	 *            The Z value to be set on position.
	 */
	public VoxelReference reset(ChunkBuffer buffer, int offset, int x, int y, int z) {
		this.buffer = buffer;
		this.offset = offset;
		this.position.set(x, y, z);
		return this;
	}

	/**
	 * Sets the type of the Voxel referenced by this object.
	 * 
	 * @param type
	 *            The new voxel type.
	 */
	public void setType(short type) {
		buffer.setShort(offset + Voxel.TYPE, type);
	}

	/**
	 * Sets the sun light value on the voxel referenced by this object.
	 * 
	 * @param value
	 *            The new sun light value.
	 */
	public void setSunLight(byte value) {
		// First clear the existing sun light value by using an AND bitwise operation on LIGHT_NORMAL (0000 1111)
		// Then, lets ensure that only four bits is used to set light value (0000 1111), because light value must be
		// less then 15.
		value = (byte) ((buffer.getByte(offset + Voxel.LIGHT) & Voxel.LIGHT_NORMAL)
				| ((value & 0x0F) << Voxel.LIGHT_SUN_SHIFT));
		buffer.setByte(offset + Voxel.LIGHT, value);
	}

	/**
	 * Gets the sun light value on the voxel referenced by this object.
	 * 
	 * @return The sun light value of referenced voxel.
	 */
	public byte getSunLight() {
		return (byte) ((buffer.getByte(offset + Voxel.LIGHT) & Voxel.LIGHT_SUN) >>> Voxel.LIGHT_SUN_SHIFT);
	}

	/**
	 * Sets the normal light value on the voxel referenced by this object.
	 * 
	 * @param value
	 *            The new normal light value.
	 */
	public void setLight(byte value) {
		// First clear light existing value by doing an AND bitwise operation on LIGHT_SUN (1111 0000)
		// Then, lets ensure that only four bits is used to set light value (0000 1111), because light value must be
		// less then 15.
		value = (byte) ((buffer.getByte(offset + Voxel.LIGHT) & Voxel.LIGHT_SUN) | value & 0x0F);
		buffer.setByte(offset + Voxel.LIGHT, value);
	}

	/**
	 * Gets the normal light value on the voxel referenced by this object.
	 * 
	 * @return The normal light value of referenced voxel.
	 */
	public byte getLight() {
		return (byte) (buffer.getByte(offset + Voxel.LIGHT) & Voxel.LIGHT_NORMAL);
	}

	/**
	 * Check if the referenced voxel is transparent.
	 * 
	 * @return True if the referenced voxel is transparent. False otherwise.
	 */
	public boolean isTransparent() {
		return !isOpaque();
	}

	/**
	 * Check if the referenced voxel is opaque.
	 * 
	 * @return True if the referenced voxel is opaque. False otherwise.
	 */
	public boolean isOpaque() {
		return (buffer.getShort(offset + Voxel.TYPE) & Voxel.VF_OPAQUE) == Voxel.VF_OPAQUE;
	}

	/**
	 * Check if the referenced voxel is special.
	 * 
	 * @return True if the referenced voxel is special. False otherwise.
	 */
	public boolean isSpecial() {
		return (buffer.getShort(offset + Voxel.TYPE) & Voxel.VF_SPECIAL) == Voxel.VF_SPECIAL;
	}

	/**
	 * Gets the current type of the referenced voxel.
	 * 
	 * @return The current type of the referenced voxel.
	 */
	public short getType() {
		return buffer.getShort(offset + Voxel.TYPE);
	}

	/**
	 * Reset the value of the referenced voxel, but keeping it's type. All other values are cleared.
	 */
	public void reset() {
		short type = getType();
		buffer.reset(offset);
		setType(type);
	}

	/**
	 * Gets the light value of a side of referenced voxel.
	 * 
	 * @param side
	 *            Side of referenced voxel.
	 * @return The light value of given side.
	 */
	public byte getSideLight(int side) {
		assert side >= 0 && side <= Voxel.LIGHT;

		return (byte) ((buffer.getByte(offset + side) & Voxel.SIDE_LIGHT) >> Voxel.SIDE_LIGHT_SHIFT);
	}

	/**
	 * Sets the light value of a side of referenced voxel.
	 * 
	 * @param b
	 *            The new light value.
	 * @param side
	 *            Side of referenced voxel.
	 */
	public void setSideLight(byte b, int side) {
		assert side >= 0 && side <= Voxel.LIGHT;

		byte sideValue = buffer.getByte(offset + side);
		buffer.setByte(offset + side, (byte) ((sideValue & ~Voxel.SIDE_LIGHT) | ((b & 0xF) << Voxel.SIDE_LIGHT_SHIFT)));
	}

	/**
	 * Checks if the given side of referenced voxel is visible.
	 * 
	 * @param side
	 *            Side of referenced voxel to be checked.
	 * @return True if the given side is visible. False otherwise.
	 */
	public boolean isSideVisible(int side) {
		assert side >= 0 && side <= Voxel.LIGHT;

		return (buffer.getByte(offset + side) & Voxel.SIDE_VISIBLE) == Voxel.SIDE_VISIBLE;
	}

	/**
	 * Sets visible the given side of referenced voxel.
	 * 
	 * @param side
	 *            The side to be set as visible.
	 */
	public void setSideVisible(int side) {
		assert side >= 0 && side <= Voxel.LIGHT;

		byte sideValue = buffer.getByte(offset + side);
		buffer.setByte(offset + side, (byte) (sideValue | Voxel.SIDE_VISIBLE));
	}

	/**
	 * Sets invisible the given side of referenced voxel.
	 * 
	 * @param side
	 *            The side to be set as invisible.
	 */
	public void resetSideVisible(int side) {
		assert side >= 0 && side <= Voxel.LIGHT;

		byte sideValue = buffer.getByte(offset + side);
		buffer.setByte(offset + side, (byte) (sideValue & ~Voxel.SIDE_VISIBLE));
	}

	public boolean isSideMerged(int side) {
		assert side >= 0 && side <= Voxel.LIGHT;

		return (buffer.getByte(offset + side) & Voxel.SIDE_MERGED) == Voxel.SIDE_MERGED;
	}

	public void setSideMerged(int side) {
		assert side >= 0 && side <= Voxel.LIGHT;

		byte sideValue = buffer.getByte(offset + side);
		buffer.setByte(offset + side, (byte) ((sideValue & ~Voxel.SIDE_MERGED) | (1 << Voxel.SIDE_MERGED_SHIFT)));
	}

	public void resetSideMerged(int side) {
		assert side >= 0 && side <= Voxel.LIGHT;

		byte sideValue = buffer.getByte(offset + side);
		buffer.setByte(offset + side, (byte) (sideValue & ~Voxel.SIDE_MERGED));
	}

	/**
	 * Copy the content of the vixel referenced by given object on the voxel referenced by this object.
	 * 
	 * @param newVoxel
	 *            The voxel referenced to be copied data from.
	 */
	public void copy(VoxelReference newVoxel) {
		buffer.copy(newVoxel.offset, offset);
	}

	/**
	 * Compute the sun light and normal light into a final light value.
	 * 
	 * @return The computed light value.
	 */
	public byte getFinalLight() {
		byte value = buffer.getByte(offset + Voxel.LIGHT);

		byte sunLight = (byte) ((value & Voxel.LIGHT_SUN) >>> Voxel.LIGHT_SUN_SHIFT);
		byte light = (byte) (value & Voxel.LIGHT_NORMAL);
		return (sunLight > light) ? sunLight : light;
	}
}