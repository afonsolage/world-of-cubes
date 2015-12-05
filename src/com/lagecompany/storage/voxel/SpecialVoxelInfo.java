package com.lagecompany.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public abstract class SpecialVoxelInfo extends VoxelInfo {
    public abstract byte getLight();
    public abstract int[] getIndexes();
    public abstract float[] getVertices();
    public abstract float[] getTextCoord();
    public abstract float[] getNormals();
}
