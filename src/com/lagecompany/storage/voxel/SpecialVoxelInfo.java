package com.lagecompany.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public abstract class SpecialVoxelInfo extends VoxelInfo {
    public abstract float[] getVertices(byte side, int x, int y, int z);
}
