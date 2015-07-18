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
	this.x = (byte) position.getX();
	this.y = (byte) position.getY();
	this.z = (byte) position.getZ();
	this.chunk = chunk;
	this.light = (byte) light;
    }
}
