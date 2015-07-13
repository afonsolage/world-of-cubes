package com.lagecompany.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public class DirtVoxelInfo extends VoxelInfo {

    private final float[] TILE_UP_GRASS;
    private final float[] TILE_DOWN_DIRT;

    protected DirtVoxelInfo() {
	TILE_UP_GRASS = new float[]{0f, 1f};
	TILE_DOWN_DIRT = new float[]{1f, 0f};
    }

    @Override
    public short getCode() {
	return Voxel.VT_DIRT;
    }

    @Override
    public float[] getTile(short side) {
	if (side == Voxel.VS_TOP) {
	    return TILE_UP_GRASS;
	} else {
	    return TILE_DOWN_DIRT;
	}
    }

    @Override
    public float[] getColor(short side) {
	return VoxelInfo.COLOR_NONE;
    }
}
