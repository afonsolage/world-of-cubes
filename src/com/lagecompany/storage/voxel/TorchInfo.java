package com.lagecompany.storage.voxel;

/**
 *
 * @author Afonso Lage
 */
public class TorchInfo extends SpecialVoxelInfo {

    //TODO: Add as static final.
    private final float x1 = .4376f;
    private final float x2 = x1 + 0.095f;
    private final float y1 = .0f;
    private final float y2 = .625f;
    private final float z1 = x1;
    private final float z2 = x2;
    private final float unit = 1f / 128f;

    protected TorchInfo() {
    }

    @Override
    public short getCode() {
	return Voxel.VT_TORCH;
    }

    @Override
    public float[] getTile(short side) {
	return new float[]{
	    //Front
	    2, 0,
	    2, 0,
	    2, 0,
	    2, 0,
	    //Right
	    2, 0,
	    2, 0,
	    2, 0,
	    2, 0,
	    //Back
	    2, 0,
	    2, 0,
	    2, 0,
	    2, 0,
	    //Left
	    2, 0,
	    2, 0,
	    2, 0,
	    2, 0,
	    //Top
	    2, 2,
	    2, 2,
	    2, 2,
	    2, 2,
	    //Down
	    2, 1,
	    2, 1,
	    2, 1,
	    2, 1,};
    }

    @Override
    public float[] getColor(short side) {
	return new float[]{
	    //Front
	    3, 3, 3, 3,
	    3, 3, 3, 3,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    //Right
	    3, 3, 3, 3,
	    3, 3, 3, 3,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    //Back
	    3, 3, 3, 3,
	    3, 3, 3, 3,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    //Left
	    3, 3, 3, 3,
	    3, 3, 3, 3,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    //Top
	    3, 3, 3, 3,
	    3, 3, 3, 3,
	    3, 3, 3, 3,
	    3, 3, 3, 3,
	    //Down
	    0.1f, 0.1f, 0.1f, 0.1f,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    0.1f, 0.1f, 0.1f, 0.1f,
	    0.1f, 0.1f, 0.1f, 0.1f,};
    }

    //The front face is composed of v0, v1, v2 and v3.
    //The back face is composed of v5, v4, v7 and v6.
    //The top face is composed of v3, v2, v6 and v7.
    //The down face is composed of v4, v5, v1 and v0.
    //The right face is composed of v1, v5, v6 and v2.
    //The left face is composed of v4, v0, v3 and v7.
    @Override
    public float[] getVertices() {
	return new float[]{
	    //Front
	    x1, y2, z1, //v3
	    x2, y2, z1, //v2
	    x2, y1, z1, //v1
	    x1, y1, z1, //v0

	    //Right
	    x2, y2, z1, //v2
	    x2, y2, z2, //v6
	    x2, y1, z2, //v5
	    x2, y1, z1, //v1

	    //Back
	    x2, y2, z2, //v6
	    x1, y2, z2, //v7
	    x1, y1, z2, //v4
	    x2, y1, z2, //v5

	    //Left
	    x1, y2, z2, //v7
	    x1, y2, z1, //v3
	    x1, y1, z1, //v0
	    x1, y1, z2, //v4

	    //Top
	    x1, y2, z2, //v7
	    x2, y2, z2, //v6
	    x2, y2, z1, //v2
	    x1, y2, z1, //v3

	    //Down
	    x1, y1, z1, //v0
	    x2, y1, z1, //v1
	    x2, y1, z2, //v5
	    x1, y1, z2, //v4
	};
    }

    @Override
    public int[] getIndexes() {
	return new int[]{
	    //Front
	    0, 1, 2,
	    2, 3, 0,
	    //Right
	    4, 5, 6,
	    6, 7, 4,
	    //Back
	    8, 9, 10,
	    10, 11, 8,
	    //Left
	    12, 13, 14,
	    14, 15, 12,
	    //Top
	    16, 17, 18,
	    18, 19, 16,
	    //Down
	    20, 21, 22,
	    22, 23, 20,};
    }

    @Override
    public float[] getTextCoord() {
	return new float[]{
	    //Front
	    57 * unit, 80 * unit,
	    72 * unit, 80 * unit,
	    72 * unit, 0,
	    57 * unit, 0,
	    //Right
	    57 * unit, 80 * unit,
	    72 * unit, 80 * unit,
	    72 * unit, 0,
	    57 * unit, 0,
	    //Back
	    57 * unit, 80 * unit,
	    72 * unit, 80 * unit,
	    72 * unit, 0,
	    57 * unit, 0,
	    //Left
	    57 * unit, 80 * unit,
	    72 * unit, 80 * unit,
	    72 * unit, 0,
	    57 * unit, 0,
	    //Top
	    57 * unit, 72 * unit,
	    72 * unit, 72 * unit,
	    72 * unit, 57 * unit,
	    57 * unit, 57 * unit,
	    //Down
	    57 * unit, 72 * unit,
	    72 * unit, 72 * unit,
	    72 * unit, 57 * unit,
	    57 * unit, 57 * unit,};
    }

    @Override
    public float[] getNormals() {
	return new float[]{
	    //Front
	    0, 0, 1,
	    0, 0, 1,
	    0, 0, 1,
	    0, 0, 1,
	    //Right
	    1, 0, 0,
	    1, 0, 0,
	    1, 0, 0,
	    1, 0, 0,
	    //Back
	    0, 0, -1,
	    0, 0, -1,
	    0, 0, -1,
	    0, 0, -1,
	    //Left
	    -1, 0, 0,
	    -1, 0, 0,
	    -1, 0, 0,
	    -1, 0, 0,
	    //Top
	    0, 1, 0,
	    0, 1, 0,
	    0, 1, 0,
	    0, 1, 0,
	    //Down
	    0, -1, 0,
	    0, -1, 0,
	    0, -1, 0,
	    0, -1, 0
	};
    }
}
