package com.lagecompany.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public class TorchInfo extends SpecialVoxelInfo {

    private final float[] TILE_TEXTURE_TOP;
    private final float[] TILE_TEXTURE_SIDE;
    private final float[] TILE_TEXTURE_DOWN;
    private final float x1 = .45f;
    private final float x2 = .55f;
    private final float y1 = .0f;
    private final float y2 = .5f;
    private final float z1 = .45f;
    private final float z2 = .55f;

    protected TorchInfo() {
	TILE_TEXTURE_SIDE = new float[]{1f, 0f};
	TILE_TEXTURE_DOWN = new float[]{1f, 0f};
	TILE_TEXTURE_TOP = new float[]{1f, 0f};
    }

    @Override
    public short getCode() {
	return Voxel.VT_TORCH;

    }

    @Override
    public float[] getTile(short side) {
	if (side == Voxel.VS_TOP) {
	    return TILE_TEXTURE_TOP;
	} else if (side == Voxel.VS_DOWN) {
	    return TILE_TEXTURE_DOWN;
	} else {
	    return TILE_TEXTURE_SIDE;
	}
    }

    @Override
    public float[] getColor(short side) {
	return VoxelInfo.COLOR_NONE;
    }

    //The front face is composed of v0, v1, v2 and v3.
    //The back face is composed of v5, v4, v7 and v6.
    //The top face is composed of v3, v2, v6 and v7.
    //The down face is composed of v4, v5, v1 and v0.
    //The right face is composed of v1, v5, v6 and v2.
    //The left face is composed of v4, v0, v3 and v7.
    @Override
    public float[] getVertices(byte side, int x, int y, int z) {
	switch (side) {
	    case Voxel.VS_FRONT: {
		return new float[]{
		    x + x1, y + y2, z + z1, //v3
		    x + x2, y + y2, z + z1, //v2
		    x + x2, y + y1, z + z1, //v1
		    x + x1, y + y1, z + z1, //v0
		};
	    }
	    case Voxel.VS_BACK: {
		return new float[]{
		    x + x2, y + y2, z + z2, //v6
		    x + x1, y + y2, z + z2, //v7
		    x + x1, y + y1, z + z2, //v4
		    x + x2, y + y1, z + z2, //v5
		};
	    }
	    case Voxel.VS_TOP: {
		return new float[]{
		    x + x1, y + y2, z + z2, //v7
		    x + x2, y + y2, z + z2, //v6
		    x + x2, y + y2, z + z1, //v2
		    x + x1, y + y2, z + z1, //v3
		};
	    }
	    case Voxel.VS_DOWN: {
		return new float[]{
		    x + x1, y + y1, z + z1, //v0
		    x + x2, y + y1, z + z1, //v1
		    x + x2, y + y1, z + z2, //v5
		    x + x1, y + y1, z + z2, //v4
		};
	    }
	    case Voxel.VS_RIGHT: {
		return new float[]{
		    x + x2, y + y2, z + z1, //v2
		    x + x2, y + y2, z + z2, //v6
		    x + x2, y + y1, z + z2, //v5
		    x + x2, y + y1, z + z1, //v1
		};
	    }
	    case Voxel.VS_LEFT: {
		return new float[]{
		    x + x1, y + y2, z + z2, //v7
		    x + x1, y + y2, z + z1, //v3
		    x + x1, y + y1, z + z1, //v0
		    x + x1, y + y1, z + z2, //v4
		};
	    }
	    default: {
		return new float[]{};
	    }
	}
    }
}
