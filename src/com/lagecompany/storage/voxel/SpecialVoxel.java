package com.lagecompany.storage.voxel;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Afonso Lage
 */
public class SpecialVoxel {

    private static final List<SpecialVoxelInfo> infoList;

    static {
	infoList = new ArrayList<>();
	loadInfo();
    }

    private static void loadInfo() {
	for (VoxelInfo info : Voxel.infoList) {
	    if (info instanceof SpecialVoxelInfo) {
		infoList.add((SpecialVoxelInfo) info);
	    }
	}
    }

    private static SpecialVoxelInfo get(short specialType) {
	for (SpecialVoxelInfo info : infoList) {
	    if (info.getCode() == specialType) {
		return info;
	    }
	}
	return null;
    }

    public static float[] getVertices(short specialType) {
	return get(specialType).getVertices();

    }

    public static int[] getIndexes(short specialType) {
	return get(specialType).getIndexes();
    }

    public static float[] getNormals(short specialType) {
	return get(specialType).getNormals();
    }

    public static float[] getTextCoord(short specialType) {
	return get(specialType).getTextCoord();
    }

    public static float[] getTileCoord(short specialType) {
	return get(specialType).getTile(Voxel.VS_ALL);
    }

    public static float[] getTextColor(short specialType) {
	return get(specialType).getColor(Voxel.VS_ALL);
    }
}
