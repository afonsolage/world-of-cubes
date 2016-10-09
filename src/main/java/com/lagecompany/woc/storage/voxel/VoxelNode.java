package com.lagecompany.woc.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public class VoxelNode {

    public byte x;
    public byte y;
    public byte z;
    public VoxelReference voxel;

    public VoxelNode(int x, int y, int z, VoxelReference voxel) {
        this.x = (byte) x;
        this.y = (byte) y;
        this.z = (byte) z;
        this.voxel = voxel;
    }
}
