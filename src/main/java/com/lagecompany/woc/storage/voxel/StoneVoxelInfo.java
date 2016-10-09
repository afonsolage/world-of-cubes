package com.lagecompany.woc.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public class StoneVoxelInfo extends VoxelInfo {

    private final float[] TILE;

    protected StoneVoxelInfo() {
	TILE = new float[]{0f, 0f};
    }

    @Override
    public short getCode() {
	return Voxel.VT_STONE;
    }

    @Override
    public float[] getTile(short side) {
	return TILE;
    }

    @Override
    public float[] getColor(short side) {
	return VoxelInfo.COLOR_NONE;
    }
}
