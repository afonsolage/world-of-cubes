package com.lagecompany.storage.voxel;

import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;

/**
 *
 * @author Afonso Lage
 */
public class VoxelNode {

    public byte x;
    public byte y;
    public byte z;
    public byte light;
    public Chunk chunk;

    public VoxelNode(Chunk chunk, Vec3 position, int light) {
	this(chunk, position.getX(), position.getY(), position.getZ(), light);
    }

    public VoxelNode(Chunk chunk, int x, int y, int z, int light) {
	this.x = (byte) x;
	this.y = (byte) y;
	this.z = (byte) z;
	this.chunk = chunk;
	this.light = (byte) light;
    }

    public VoxelNode copy() {
	return new VoxelNode(chunk, x, y, z, light);
    }
}
