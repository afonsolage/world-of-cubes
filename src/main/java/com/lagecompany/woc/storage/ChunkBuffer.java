package com.lagecompany.woc.storage;

import java.util.Arrays;

import com.lagecompany.woc.storage.voxel.Voxel;
import com.lagecompany.woc.storage.voxel.VoxelReference;
import com.lagecompany.woc.util.CacheUtils;
import com.lagecompany.woc.util.MathUtils;

/**
 * A buffer class that holds the Chunk data buffer and has some utility methods to read and write on this buffer.
 * 
 * @author Afonso Lage
 */
public class ChunkBuffer {

	/**
	 * The buffer byte array that stores the voxel data.
	 */
	private byte[] buffer;

	/**
	 * Default constructor that initialize the buffer array based on Voxel size and Chunk size.
	 */
	public ChunkBuffer() {
		buffer = new byte[Chunk.DATA_LENGTH * Voxel.SIZE];
	}

	/**
	 * Clears the internal buffer.
	 */
	public void free() {
		buffer = null;
	}

	/**
	 * Resets the merged and visible info of all voxels on this buffer.
	 */
	void resetMergedVisible() {
		VoxelReference voxel;
		for (int i = 0; i < Chunk.DATA_LENGTH; i++) {
			voxel = CacheUtils.getVoxelReference(this, i * Voxel.SIZE);

			for (int side : Voxel.ALL_SIDES) {
				voxel.resetSideMerged(side);
				voxel.resetSideVisible(side);
			}
		}
	}

	/**
	 * Resets to zero (clear) all info of the voxel on the given offset.
	 * 
	 * @param offset
	 */
	public void reset(int offset) {
		Arrays.fill(buffer, offset, offset + Voxel.SIZE, (byte) 0);
	}

	/**
	 * Copy a voxel info from one offset to another
	 * 
	 * @param srcOffset
	 *            The source offset to copy data from
	 * @param dstOffset
	 *            The destination offset to received the data copied.
	 */
	public void copy(int srcOffset, int dstOffset) {
		System.arraycopy(buffer, srcOffset, buffer, dstOffset, Voxel.SIZE);
	}

	/**
	 * Get the voxel data onto given voxel reference by using it's position to determine the offset onto this buffer.
	 * 
	 * @param voxel
	 *            The Voxel Reference to be used and which has a position set to calculate the voxel offset.
	 * @return True if the VoxelReference contains a valid position of a voxel. False otherwise.
	 */
	public boolean get(VoxelReference voxel) {
		voxel.offset = MathUtils.toVoxelIndex(voxel.position.x, voxel.position.y, voxel.position.z) * Voxel.SIZE;
		voxel.buffer = this;
		return voxel.offset > -1 && voxel.offset < buffer.length;
	}

	/**
	 * Sets the type of a voxel using the given VoxelReference
	 * 
	 * @param voxel
	 *            The Voxel Reference to be used and which has a position set to calculate the voxel offset.
	 * @param type
	 *            The new voxel type
	 */
	void set(VoxelReference voxel, short type) {
		if (get(voxel)) {
			voxel.setType(type);
		}
	}

	// WAT??
	// void set(VoxelReference newVoxel) {
	// if (get(newVoxel)) {
	// newVoxel.copy(newVoxel);
	// }
	// }

	/**
	 * Returns the raw byte at a given offset.
	 * 
	 * @param i
	 *            The offset (index) of the byte to be retrieved.
	 * @return The raw byte.
	 */
	public byte getByte(int i) {
		return buffer[i];
	}

	/**
	 * Sets the given raw byte on the specified offset (index)
	 * 
	 * @param i
	 *            The offset (index) to set the raw byte
	 * @param value
	 *            The raw byte to be set.
	 */
	public void setByte(int i, byte value) {
		if (i < 0 || i >= buffer.length) {
			System.out.println("Ooops...");
		}
		buffer[i] = value;
	}

	/**
	 * Returns a short on the given offset
	 * 
	 * @param i
	 *            The offset to retrieve the short.
	 * @return The short at the given offset
	 */
	public short getShort(int i) {
		return (short) ((buffer[i] << 8) | buffer[i + 1]);
	}

	/**
	 * Sets a short value at the given offset.
	 * 
	 * @param i
	 *            The offset to set the short.
	 * @param type
	 *            The short value to be set.
	 */
	public void setShort(int i, short type) {
		buffer[i] = (byte) ((type) >>> 8);
		buffer[i + 1] = (byte) (type & 0xFF);
	}
}