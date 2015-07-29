package com.lagecompany.storage.voxel;

import java.util.ArrayList;
import java.util.List;

/**
 * This class stores all info needed by a voxel. It is made of 4 bytes, plus 12 bytes from object header, so the memory
 * consumption from this class instanciated is 16 bytes.
 */
public class Voxel {

    //VS = Visible Sides
    public static final byte VS_FRONT = 0x01;
    public static final byte VS_RIGHT = 0x02;
    public static final byte VS_BACK = 0x04;
    public static final byte VS_LEFT = 0x08;
    public static final byte VS_TOP = 0x10;
    public static final byte VS_DOWN = 0x20;
    public static final byte VS_ALL = 0x3F;
    public static final byte VS_NONE = 0x00;
    //VF = Voxel Flag
    public static final short VF_TRANSPARENT = 0x4000; //0 100 0000 0000 0000
    public static final short VF_SPECIAL = 0x2000; //0 010 0000 0000 0000
    //VT = Voxel Type
    @SuppressWarnings("PointlessBitwiseExpression")
    public static final short VT_NONE = 0x0000 | VF_TRANSPARENT;
    public static final short VT_DIRT = 0x0001;
    public static final short VT_GRASS = 0x0002;
    public static final short VT_STONE = 0x0003;
    public static final short VT_TORCH = 0x0004 | VF_TRANSPARENT | VF_SPECIAL;
    public static final byte LIGHT_SUN = 0x0F;
    protected static final List<VoxelInfo> infoList;
    private byte visibleSides;
    private byte mergedSides;
    private byte frontLight;
    private byte rightLight;
    private byte backLight;
    private byte leftLight;
    private byte topLight;
    private byte downLight;
    private short type;

    static {
	infoList = new ArrayList<>();
	loadInfo();
    }

    private static void loadInfo() {
	infoList.add(new DirtVoxelInfo());
	infoList.add(new StoneVoxelInfo());
	infoList.add(new TorchInfo());
    }

    public Voxel() {
    }

    public Voxel(short type) {
	this.type = type;
    }

    public void reset() {
	visibleSides = VS_NONE;
	mergedSides = VS_NONE;
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

    public byte getFrontLight() {
	return frontLight;
    }

    public void setFrontLight(byte frontLight) {
	this.frontLight = frontLight;
    }

    public byte getRightLight() {
	return rightLight;
    }

    public void setRightLight(byte rightLight) {
	this.rightLight = rightLight;
    }

    public byte getBackLight() {
	return backLight;
    }

    public void setBackLight(byte backLight) {
	this.backLight = backLight;
    }

    public byte getLeftLight() {
	return leftLight;
    }

    public void setLeftLight(byte leftLight) {
	this.leftLight = leftLight;
    }

    public byte getTopLight() {
	return topLight;
    }

    public void setTopLight(byte topLight) {
	this.topLight = topLight;
    }

    public byte getDownLight() {
	return downLight;
    }

    public void setDownLight(byte downLight) {
	this.downLight = downLight;
    }

    public void setAllSidesLight(byte light) {
	frontLight = light;
	rightLight = light;
	backLight = light;
	leftLight = light;
	topLight = light;
	downLight = light;
    }

    public void toggleVisibleSide(byte side) {
	this.visibleSides |= side;
    }

    public void toggleMergedSide(byte side) {
	this.mergedSides |= side;
    }

    //TODO: Remove array creation and change for global buffer.
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

    private static VoxelInfo getInfo(short type) {
	for (VoxelInfo vi : infoList) {
	    if (vi.getCode() == type) {
		return vi;
	    }
	}

	return null;
    }

    public static float[] getColor(short type, short side) {
	return getInfo(type).getColor(side);
    }

    public static float[] getTile(short type, short side) {
	return getInfo(type).getTile(side);
    }

    public static boolean isOpaque(Voxel voxel) {
	return (voxel.getType() >>> 14 & 0x01) == 0;
    }

    public static boolean isSpecial(Voxel voxel) {
	return isSpecial(voxel.getType());
    }

    public static boolean isSpecial(short type) {
	return (type >>> 13 & 0x01) == 1;
    }
}