package com.lagecompany.storage.voxel;

import com.lagecompany.storage.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * This class stores all info needed by a voxel. It is made of 4 bytes, plus 12
 * bytes from object header, so the memory consumption from this class
 * instanciated is 16 bytes.
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
    public static final short VF_OPAQUE = 0x4000; //0 100 0000 0000 0000
    public static final short VF_SPECIAL = 0x2000; //0 010 0000 0000 0000
    //VT = Voxel Type
    public static final short VT_NONE = 0x0000;
    public static final short VT_DIRT = 0x0001 | VF_OPAQUE;
    public static final short VT_GRASS = 0x0002 | VF_OPAQUE;
    public static final short VT_STONE = 0x0003 | VF_OPAQUE;
    public static final short VT_TORCH = 0x0004 | VF_SPECIAL;

    public static final byte SUN_LIGHT = 0x0F;
    protected static final List<VoxelInfo> infoList;

    /**
     * **********************************
     */
    /**
     * Size in bytes of Voxel data struct.
     */
    public static final int SIZE = 9;

    /**
     * Front side byte position on Voxel data struct.
     */
    public static final int FRONT = 0;
    /**
     * Right side byte position on Voxel data struct.
     */
    public static final int RIGHT = 1;
    /**
     * Back side byte position on Voxel data struct.
     */
    public static final int BACK = 2;
    /**
     * Left side byte position on Voxel data struct.
     */
    public static final int LEFT = 3;
    /**
     * Top side byte position on Voxel data struct.
     */
    public static final int TOP = 4;
    /**
     * Down side byte position on Voxel data struct.
     */
    public static final int DOWN = 5;
    /**
     * Type data byte position on Voxel data struct.
     */
    public static final int TYPE = 6;
    /**
     * Light data byte position on Voxel data struct.
     */
    public static final int LIGHT = 8;

    /**
     * BitWise check to be applied on a voxel side to check if it is visible.
     */
    public static final byte SIDE_VISIBLE = 0x01;      // 0000 0001
    /**
     * BitWise check to be applied on a voxel side to check if it is already
     * merged.
     */
    public static final byte SIDE_MERGED = 0x02;       // 0000 0010
    /**
     * BitWise get light value of a voxel side.
     */
    public static final byte SIDE_LIGHT = 0x3C;        // 0011 1100

    /**
     * Number of bits to be shifted when reading merged value on a voxel side.
     */
    public static final int SIDE_MERGED_SHIFT = 1;
    /**
     * Number of bits to be shifted when reading light value on a voxel side.
     */
    public static final int SIDE_LIGHT_SHIFT = 2;

    /**
     * BitWise get normal light value on a voxel.
     */
    public static final short LIGHT_NORMAL = 0xF;       // 0000 1111
    /**
     * BitWise get normal light value on a voxel.
     */
    public static final short LIGHT_SUN = 0xF0;         // 1111 0000
    /**
     * Number of bits to be shifted when reading sunlight value on a voxel.
     */
    public static final int LIGHT_SUN_SHIFT = 4;

    static {
        infoList = new ArrayList<>();
        loadInfo();
    }

    private static void loadInfo() {
        infoList.add(new DirtVoxelInfo());
        infoList.add(new StoneVoxelInfo());
        infoList.add(new TorchInfo());
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

    public static int directionToSide(Vec3 dir) {
        if (dir == Vec3.FRONT) {
            return Voxel.FRONT;
        } else if (dir == Vec3.RIGHT) {
            return Voxel.RIGHT;
        } else if (dir == Vec3.BACK) {
            return Voxel.BACK;
        } else if (dir == Vec3.LEFT) {
            return Voxel.LEFT;
        } else if (dir == Vec3.UP) {
            return Voxel.TOP;
        } else {
            return Voxel.DOWN;
        }
    }
}
