package com.lagecompany.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public abstract class VoxelInfo {
    
    public static final float[] COLOR_NONE = {1.0f, 1.0f, 1.0f, 1.0f};
    
    public abstract short getCode();
    public abstract float[] getTile(short side);
    public abstract float[] getColor(short side);
}
