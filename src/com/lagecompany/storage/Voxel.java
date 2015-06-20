package com.lagecompany.storage;

/**
 * This class stores all info needed by a voxel. It is made of 4 bytes, plus 12 bytes from object header, so the memory
 * consumption from this class instanciated is 16 bytes.
 */
public class Voxel {

    public static final byte VS_FRONT = 0x01;
    public static final byte VS_RIGHT = 0x02;
    public static final byte VS_BACK = 0x04;
    public static final byte VS_LEFT = 0x08;
    public static final byte VS_TOP = 0x10;
    public static final byte VS_DOWN = 0x20;
    public static final byte VS_ALL = 0x3F;
    public static final byte VS_NONE = 0x00;
    public static final short VT_NONE = 0x0000;
    public static final short VT_DIRT = 0x0001;
    public static final short VT_GRASS = 0x0002;
    public static final short VT_ROCK = 0x0003;
    private byte visibleSides;
    private byte mergedSides;
    private short type;
    
    public Voxel() {
	this.visibleSides = VS_NONE;
	this.mergedSides = VS_NONE;
	this.type = VT_NONE;
    }

    public Voxel(short type) {
	this.type = type;
    }

    public byte getVisibleSides() {
	return visibleSides;
    }

    public void setVisibleSides(byte visibleSides) {
	this.visibleSides = visibleSides;
    }

    public byte getMergedSides() {
	return mergedSides;
    }

    public void setMergedSides(byte mergedSides) {
	this.mergedSides = mergedSides;
    }

    public short getType() {
	return type;
    }

    public void setType(short type) {
	this.type = type;
    }

    public void toggleVisibleSide(byte side) {
	this.visibleSides |= side;
    }

    void toggleMergedSide(byte side) {
	this.mergedSides |= side;
    }

    /*
     * 
     *      v7 +-------+ v6	y
     *      / |     /  |		| Z 
     *   v3 +-------+v2|		|/
     *      |v4+-------+ v5	+-- X
     *      | /     | /
     *      +-------+ 
     *     v0        v1
     */
    public static float[] v0(int x, int y, int z) {
	return new float[]{x, y, z + 1};
    }

    public static float[] v1(int x, int y, int z) {
	return new float[]{x + 1, y, z + 1};
    }

    public static float[] v2(int x, int y, int z) {
	return new float[]{x + 1, y + 1, z + 1};
    }

    public static float[] v3(int x, int y, int z) {
	return new float[]{x, y + 1, z + 1};
    }

    public static float[] v4(int x, int y, int z) {
	return new float[]{x, y, z};
    }

    public static float[] v5(int x, int y, int z) {
	return new float[]{x + 1, y, z};
    }

    public static float[] v6(int x, int y, int z) {
	return new float[]{x + 1, y + 1, z};
    }

    public static float[] v7(int x, int y, int z) {
	return new float[]{x, y + 1, z};
    }
}