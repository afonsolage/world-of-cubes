package com.lagecompany.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public class VoxelNode {

    public byte x;
    public byte y;
    public byte z;
    public byte light;
    public byte skipDir;
    
    public VoxelNode(int x, int y, int z, int light, byte skipDir) {
	this.x = (byte) x;
	this.y = (byte) y;
	this.z = (byte) z;
	this.light = (byte) light;
	this.skipDir = skipDir;
    }
}
