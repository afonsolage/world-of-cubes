/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.storage.light;

import com.lagecompany.storage.Are;
import com.lagecompany.storage.Chunk;
import static com.lagecompany.storage.Chunk.SIZE;
import com.lagecompany.storage.Vec3;
import com.lagecompany.storage.voxel.Voxel;
import com.lagecompany.storage.voxel.VoxelReference;
import com.lagecompany.util.MathUtils;

/**
 *
 * @author TI.Afonso
 */
public class LightManager {

    private static final Are are;

    static {
        are = Are.getInstance();
    }

    public static void updateVoxelLight(Chunk c, VoxelReference voxel, int previousSunLight, int previousLight, short previousType, short newType) {
        Vec3 tmpVec = new Vec3();
        Chunk tmpChunk;

        if (previousType == Voxel.VT_NONE) {
            //Block add

            c.addSunLightRemovalQueue(new LightRemovalNode(voxel.x, voxel.y, voxel.z, previousSunLight));
        } else {
            //Block remove

            for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
                tmpVec.set(voxel.x + dir.x, voxel.y + dir.y, voxel.z + dir.z);
                tmpChunk = are.validateChunkAndVoxel(c, tmpVec);

                if (tmpChunk == null) {
                    //Oopps, we hit the are bounds, this shouldn't happen outside development.
                    continue;
                }

                tmpChunk.addSunLightPropagationQueue(tmpChunk.get(tmpVec));
            }
        }
    }

    /**
     * Fills the voxelBuffer with neighbor information. If neighbor exists and
     * is opaque, voxelBuffer is set to true, else it is set to false.
     *
     * @param chunk Chunk where voxel is placed.
     * @param voxel
     * @param voxelBuffer
     */
    private static void getVoxelNeighborhood(Chunk chunk, VoxelReference voxel, boolean[] voxelBuffer) {
        VoxelReference v;

        /*
         -               +-------++-------++-----+
         -             /   7   //  4    //  2   /|
         -            +-------++-------++------+ |
         -          /   6   //||     //   1   /| +
         -         +-------++-------++-------+ |/+
         -       /   5   //    3   //   0   /| +/|
         -       +-------++-------++-------+ |/+ |
         -       |       ||       ||       | + | +
         -       |       ||       ||       |/+ |/+
         -       +-------++-------++-------+/| +/|
         -       +-------+  +      +-------+ | + |
         -       |       | /+------|       | +/| +
         -       |       |//       |       |/+ |/ 
         -       +-------+/        +-------+/| +
         -       +-------++-------++-------+ |/ 
         -       |       ||       ||       | +  
         -       |       ||       ||       |/ 
         -       +-------++-------++-------+
         - Y
         - | 
         - | 
         - |
         - +------ X
         -  \
         -   \
         -    Z
        
         */
        //TOP RIGHT NEIGHBORS
        voxelBuffer[0] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y + 1, voxel.z + 1)) != null) && v.isOpaque();
        voxelBuffer[1] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y + 1, voxel.z)) != null) && v.isOpaque();
        voxelBuffer[2] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y + 1, voxel.z - 1)) != null) && v.isOpaque();

        //TOP MIDDLE NEIGHBORS
        voxelBuffer[3] = ((v = chunk.getAreVoxel(voxel.x, voxel.y + 1, voxel.z + 1)) != null) && v.isOpaque();
        voxelBuffer[4] = ((v = chunk.getAreVoxel(voxel.x, voxel.y + 1, voxel.z - 1)) != null) && v.isOpaque();

        //TOP LEFT NEIGHBORS
        voxelBuffer[5] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y + 1, voxel.z + 1)) != null) && v.isOpaque();
        voxelBuffer[6] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y + 1, voxel.z)) != null) && v.isOpaque();
        voxelBuffer[7] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y + 1, voxel.z - 1)) != null) && v.isOpaque();

        /*
         -               +-------++-------++-----+
         -             /       //       //      /|
         -            +-------++-------++------+ |
         -          /       / ||      /       /| +
         -         +-------++-------++-------+ |/
         -       /       //        //       /| +
         -       +-------++-------++-------+ |/
         -       |       ||       ||       | +
         -       |       ||       ||       |/
         -       +-------++-------++-------+
        
         -               +-------          +-----+
         -             /  11   /|        /   9  /|
         -            +-------+ |       +------+ |
         -            |       | +-------|      | +
         -         +-------+  |/     +-------+ |/+
         -       /  10    /|--++----/   8   /|-+/|
         -       +-------+ |/ ||   +-------+ | + |
         -       |       | |  ||   |       | +/| +
         -       |       |/+-------|       |/+ |/
         -       +-------+/        +-------+/| +
         -       +-------++-------++-------+ |/ 
         -       |       ||       ||       | +  
         -       |       ||       ||       |/ 
         -       +-------++-------++-------+
         - Y
         - | 
         - | 
         - |
         - +------ X
         -  \
         -   \
         -    Z
        
         */
        //MID RIGHT NEIGHBORS
        voxelBuffer[8] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y, voxel.z + 1)) != null) && v.isOpaque();
        voxelBuffer[9] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y, voxel.z - 1)) != null) && v.isOpaque();

        //MID LEFT NEIGHBORS
        voxelBuffer[10] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y, voxel.z + 1)) != null) && v.isOpaque();
        voxelBuffer[11] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y, voxel.z - 1)) != null) && v.isOpaque();

        /*
        
         -               +-------++-------++-----+
         -             /       //       //      /|
         -            +-------++-------++------+ |
         -          /       //||     //       /| +
         -         +-------++-------++-------+ |/+
         -       /       //        //       /| +/|
         -       +-------++-------++-------+ |/+ |
         -       |       ||       ||       | +/| +
         -       |       ||       ||       |/+ |/
         -       +-------++-------++-------+/| +
         -       +-------+  +      +-------+ | 
         -       |       | /       |       | +  
         -       |       |/        |       |/ 
         -       +-------+         +-------+
        
         -               +-------++-------++-----+
         -             /  19   //  16   //  14  /|
         -            +-------++-------++------+ |
         -          /  18   //||     //  13   /| +
         -         +-------++-------++-------+ |/
         -       /   17  //   15   //  12   /| +
         -       +-------++-------++-------+ |/
         -       |       ||       ||       | +
         -       |       ||       ||       |/
         -       +-------++-------++-------+
         - Y
         - | 
         - | 
         - |
         - +------ X
         -  \
         -   \
         -    Z
        
         */
        //DOWN RIGHT NEIGHBORS
        voxelBuffer[12] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y - 1, voxel.z + 1)) != null) && v.isOpaque();
        voxelBuffer[13] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y - 1, voxel.z)) != null) && v.isOpaque();
        voxelBuffer[14] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y - 1, voxel.z - 1)) != null) && v.isOpaque();

        //DOWN MIDDLE NEIGHBORS
        voxelBuffer[15] = ((v = chunk.getAreVoxel(voxel.x, voxel.y - 1, voxel.z + 1)) != null) && v.isOpaque();
        voxelBuffer[16] = ((v = chunk.getAreVoxel(voxel.x, voxel.y - 1, voxel.z - 1)) != null) && v.isOpaque();

        //DOWN LEFT NEIGHBORS
        voxelBuffer[17] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y - 1, voxel.z + 1)) != null) && v.isOpaque();
        voxelBuffer[18] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y - 1, voxel.z)) != null) && v.isOpaque();
        voxelBuffer[19] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y - 1, voxel.z - 1)) != null) && v.isOpaque();
    }

    /**
     * Compute Ambiente Occlusion. This method was based on 0fps blog:
     * http://0fps.net/2013/07/03/ambient-occlusion-for-minecraft-like-worlds/
     *
     * @param side1 side one of current calculating vertex. Should be 0 or 1.
     * @param side2 side two of current calculating vertex. Should be 0 or 1.
     * @param corner corner of current calculating vertex. Should be 0 or 1.
     * @return The Ambient Occulsion factor, from 0 to 3.
     */
    private static byte computeAO(boolean side1, boolean side2, boolean corner) {
        if (side1 && side2) {
            return 0;
        }
        return (byte) (3 - ((side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0)));
    }

    public static byte[] computeAmbientOcclusion(Chunk chunk) {
        byte[] result = new byte[Chunk.DATA_LENGTH * 6]; //6 sides.

        VoxelReference voxel;
        boolean[] neighborhood = new boolean[20];
        int voxelIndex;
        byte aoV0 = 0, aoV1 = 0, aoV2 = 0, aoV3 = 0;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    voxel = chunk.get(x, y, z);

                    getVoxelNeighborhood(chunk, voxel, neighborhood);
                    voxelIndex = MathUtils.toVoxelIndex(x, y, z) * 6; //6 sides

                    /*
                     * 
                     *      v7 +-------+ v6         y
                     *      /  |      /|            |  
                     *   v3 +-------+v2|            |
                     *      |v4+-------+ v5         +-- X
                     *      | /     | /              \
                     *      +-------+                 Z 
                     *     v0        v1
                     */
                    for (int side = Voxel.FRONT; side <= Voxel.DOWN; side++) {

                        if (!voxel.isSideVisible(side)) {
                            continue;
                        }

                        switch (side) {
                            case Voxel.FRONT: {
                                //The front face is composed of v0, v1, v2 and v3.
                                aoV0 = computeAO(neighborhood[15], neighborhood[10], neighborhood[17]); //v0
                                aoV1 = computeAO(neighborhood[15], neighborhood[8], neighborhood[12]); //v1
                                aoV2 = computeAO(neighborhood[3], neighborhood[8], neighborhood[0]); //v2
                                aoV3 = computeAO(neighborhood[3], neighborhood[10], neighborhood[5]); //v3
                                break;
                            }
                            case Voxel.RIGHT: {
                                //The right face is composed of v1, v5, v6 and v2.
                                aoV0 = computeAO(neighborhood[13], neighborhood[8], neighborhood[12]); //v1
                                aoV1 = computeAO(neighborhood[13], neighborhood[9], neighborhood[14]); //v5
                                aoV2 = computeAO(neighborhood[1], neighborhood[9], neighborhood[2]); //v6
                                aoV3 = computeAO(neighborhood[1], neighborhood[8], neighborhood[0]); //v2
                                break;
                            }
                            case Voxel.BACK: {
                                //The back face is composed of v5, v4, v7 and v6.
                                aoV0 = computeAO(neighborhood[16], neighborhood[9], neighborhood[14]); //v5
                                aoV1 = computeAO(neighborhood[16], neighborhood[11], neighborhood[19]); //v4
                                aoV2 = computeAO(neighborhood[4], neighborhood[11], neighborhood[7]); //v7
                                aoV3 = computeAO(neighborhood[4], neighborhood[9], neighborhood[2]); //v6
                                break;
                            }
                            case Voxel.LEFT: {
                                //The left face is composed of v4, v0, v3 and v7.
                                aoV0 = computeAO(neighborhood[18], neighborhood[11], neighborhood[19]); //v4
                                aoV1 = computeAO(neighborhood[18], neighborhood[10], neighborhood[17]); //v0
                                aoV2 = computeAO(neighborhood[6], neighborhood[10], neighborhood[5]); //v3
                                aoV3 = computeAO(neighborhood[6], neighborhood[11], neighborhood[7]); //v7
                                break;
                            }
                            case Voxel.TOP: {
                                //The top face is composed of v3, v2, v6 and v7.
                                aoV0 = computeAO(neighborhood[3], neighborhood[6], neighborhood[5]); //v3
                                aoV1 = computeAO(neighborhood[3], neighborhood[1], neighborhood[0]); //v2
                                aoV2 = computeAO(neighborhood[4], neighborhood[1], neighborhood[2]); //v6
                                aoV3 = computeAO(neighborhood[4], neighborhood[6], neighborhood[7]); //v7
                                break;
                            }
                            case Voxel.DOWN: {
                                //The down face is composed of v4, v5, v1 and v0.
                                aoV0 = computeAO(neighborhood[16], neighborhood[18], neighborhood[19]); //v4
                                aoV1 = computeAO(neighborhood[16], neighborhood[13], neighborhood[14]); //v5
                                aoV2 = computeAO(neighborhood[15], neighborhood[13], neighborhood[12]); //v1
                                aoV3 = computeAO(neighborhood[15], neighborhood[18], neighborhood[17]); //v0
                                break;
                            }
                            default: {
                                System.out.println("Invalid side value!!!");
                            }
                        }
                        result[voxelIndex + side] = (byte) (aoV3 << 6 | aoV2 << 4 | aoV1 << 2 | aoV0);
                    }
                }
            }
        }
        return result;
    }

    public static int getAoBufferIndex(int x, int y, int z, int side) {
        return (MathUtils.toVoxelIndex(x, y, z) * 6) + side;
    }

}
