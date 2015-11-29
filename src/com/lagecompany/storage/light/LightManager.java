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
     * Fills the voxelBuffer with neighbor lighting information.
     *
     * @param chunk Chunk where voxel is placed.
     * @param voxel
     * @param voxelBuffer
     */
    private static void getNeighborhoodLighting(Chunk chunk, VoxelReference voxel, byte[] voxelBuffer) {
        VoxelReference v;

        /*
         -               +-------++-------++-----+
         -             /   7   //  4    //  2   /|
         -            +-------++-------++------+ |
         -          /   6   / ||      /   1   /| +
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
        voxelBuffer[0] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y + 1, voxel.z + 1)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[1] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y + 1, voxel.z)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[2] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y + 1, voxel.z - 1)) != null) ? v.getFinalLight() : 0;

        //TOP MIDDLE NEIGHBORS
        voxelBuffer[3] = ((v = chunk.getAreVoxel(voxel.x, voxel.y + 1, voxel.z + 1)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[4] = ((v = chunk.getAreVoxel(voxel.x, voxel.y + 1, voxel.z - 1)) != null) ? v.getFinalLight() : 0;

        //TOP LEFT NEIGHBORS
        voxelBuffer[5] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y + 1, voxel.z + 1)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[6] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y + 1, voxel.z)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[7] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y + 1, voxel.z - 1)) != null) ? v.getFinalLight() : 0;

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
        voxelBuffer[8] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y, voxel.z + 1)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[9] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y, voxel.z - 1)) != null) ? v.getFinalLight() : 0;

        //MID LEFT NEIGHBORS
        voxelBuffer[10] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y, voxel.z + 1)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[11] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y, voxel.z - 1)) != null) ? v.getFinalLight() : 0;

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
        voxelBuffer[12] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y - 1, voxel.z + 1)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[13] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y - 1, voxel.z)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[14] = ((v = chunk.getAreVoxel(voxel.x + 1, voxel.y - 1, voxel.z - 1)) != null) ? v.getFinalLight() : 0;

        //DOWN MIDDLE NEIGHBORS
        voxelBuffer[15] = ((v = chunk.getAreVoxel(voxel.x, voxel.y - 1, voxel.z + 1)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[16] = ((v = chunk.getAreVoxel(voxel.x, voxel.y - 1, voxel.z - 1)) != null) ? v.getFinalLight() : 0;

        //DOWN LEFT NEIGHBORS
        voxelBuffer[17] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y - 1, voxel.z + 1)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[18] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y - 1, voxel.z)) != null) ? v.getFinalLight() : 0;
        voxelBuffer[19] = ((v = chunk.getAreVoxel(voxel.x - 1, voxel.y - 1, voxel.z - 1)) != null) ? v.getFinalLight() : 0;
    }

    public static void computeSmoothLighting(Chunk chunk, VoxelReference voxel) {
        byte[] neighborhood = new byte[26];
        int sideLighting;
        getNeighborhoodLighting(chunk, voxel, neighborhood);

        LightData data = new LightData(voxel.offset);
        chunk.setLightData(data);

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

            sideLighting = voxel.getSideLight(side);

            switch (side) {
                case Voxel.FRONT: {
                    //The front face is composed of v0, v1, v2 and v3.
                    data.setVertexData(side, 0, computeSmoorthLightingVertex(sideLighting, neighborhood[15], neighborhood[10], neighborhood[17])); //v0
                    data.setVertexData(side, 1, computeSmoorthLightingVertex(sideLighting, neighborhood[15], neighborhood[8], neighborhood[12])); //v1
                    data.setVertexData(side, 2, computeSmoorthLightingVertex(sideLighting, neighborhood[3], neighborhood[8], neighborhood[0])); //v2
                    data.setVertexData(side, 3, computeSmoorthLightingVertex(sideLighting, neighborhood[3], neighborhood[10], neighborhood[5])); //v3
                    break;
                }
                case Voxel.RIGHT: {
                    //The right face is composed of v1, v5, v6 and v2.
                    data.setVertexData(side, 0, computeSmoorthLightingVertex(sideLighting, neighborhood[13], neighborhood[8], neighborhood[12])); //v1
                    data.setVertexData(side, 1, computeSmoorthLightingVertex(sideLighting, neighborhood[13], neighborhood[9], neighborhood[14])); //v5
                    data.setVertexData(side, 2, computeSmoorthLightingVertex(sideLighting, neighborhood[1], neighborhood[9], neighborhood[2])); //v6
                    data.setVertexData(side, 3, computeSmoorthLightingVertex(sideLighting, neighborhood[1], neighborhood[8], neighborhood[0])); //v2
                    break;
                }
                case Voxel.BACK: {
                    //The back face is composed of v5, v4, v7 and v6.
                    data.setVertexData(side, 0, computeSmoorthLightingVertex(sideLighting, neighborhood[16], neighborhood[9], neighborhood[14])); //v5
                    data.setVertexData(side, 1, computeSmoorthLightingVertex(sideLighting, neighborhood[16], neighborhood[11], neighborhood[19])); //v4
                    data.setVertexData(side, 2, computeSmoorthLightingVertex(sideLighting, neighborhood[4], neighborhood[11], neighborhood[7])); //v7
                    data.setVertexData(side, 3, computeSmoorthLightingVertex(sideLighting, neighborhood[4], neighborhood[9], neighborhood[2])); //v6
                    break;
                }
                case Voxel.LEFT: {
                    //The left face is composed of v4, v0, v3 and v7.
                    data.setVertexData(side, 0, computeSmoorthLightingVertex(sideLighting, neighborhood[18], neighborhood[11], neighborhood[19])); //v4
                    data.setVertexData(side, 1, computeSmoorthLightingVertex(sideLighting, neighborhood[18], neighborhood[10], neighborhood[17])); //v0
                    data.setVertexData(side, 2, computeSmoorthLightingVertex(sideLighting, neighborhood[6], neighborhood[10], neighborhood[5])); //v3
                    data.setVertexData(side, 3, computeSmoorthLightingVertex(sideLighting, neighborhood[6], neighborhood[11], neighborhood[7])); //v7
                    break;
                }
                case Voxel.TOP: {
                    //The top face is composed of v3, v2, v6 and v7.
                    data.setVertexData(side, 0, computeSmoorthLightingVertex(sideLighting, neighborhood[3], neighborhood[6], neighborhood[5])); //v3
                    data.setVertexData(side, 1, computeSmoorthLightingVertex(sideLighting, neighborhood[3], neighborhood[1], neighborhood[0])); //v2
                    data.setVertexData(side, 2, computeSmoorthLightingVertex(sideLighting, neighborhood[4], neighborhood[1], neighborhood[2])); //v6
                    data.setVertexData(side, 3, computeSmoorthLightingVertex(sideLighting, neighborhood[4], neighborhood[6], neighborhood[7])); //v7
                    break;
                }
                case Voxel.DOWN: {
                    //The down face is composed of v4, v5, v1 and v0.
                    data.setVertexData(side, 0, computeSmoorthLightingVertex(sideLighting, neighborhood[16], neighborhood[18], neighborhood[19])); //v4
                    data.setVertexData(side, 1, computeSmoorthLightingVertex(sideLighting, neighborhood[16], neighborhood[13], neighborhood[14])); //v5
                    data.setVertexData(side, 2, computeSmoorthLightingVertex(sideLighting, neighborhood[15], neighborhood[13], neighborhood[12])); //v1
                    data.setVertexData(side, 3, computeSmoorthLightingVertex(sideLighting, neighborhood[15], neighborhood[18], neighborhood[17])); //v0
                    break;
                }
                default: {
                    System.out.println("Invalid side value!!!");
                }
            }
        }
    }

    private static byte getNeighborLighting(Chunk chunk, int side, Vec3 position) {
        chunk = are.validateChunkAndVoxel(chunk, position);

        if (chunk == null) {
            return 0;
        }

        VoxelReference voxel = chunk.get(position);
        return (voxel == null) ? 0 : voxel.getSideLight(side);
    }

    public static byte[] getNeighborhoodLighting(Chunk chunk, int side, Vec3 vec0, Vec3 vec1, Vec3 vec2, Vec3 vec3) {
        Vec3 tmpVec = new Vec3();
        byte[] neighborhood = new byte[12];
        int i = 0;

        switch (side) {
            case Voxel.FRONT: {
                //The front face is composed of v0, v1, v2 and v3.

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y, vec0.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y - 1, vec0.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x, vec0.y - 1, vec0.z + 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x, vec1.y - 1, vec1.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y - 1, vec1.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y, vec1.z + 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y, vec2.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y + 1, vec2.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x, vec2.y + 1, vec2.z + 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x, vec3.y + 1, vec3.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y + 1, vec3.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y, vec3.z + 1));
                break;
            }
            case Voxel.RIGHT: {
                //The right face is composed of v1, v5, v6 and v2.

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x + 1, vec0.y, vec0.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x + 1, vec0.y - 1, vec0.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x + 1, vec0.y - 1, vec0.z));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y - 1, vec1.z));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y - 1, vec1.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y, vec1.z - 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y, vec2.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y + 1, vec2.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y + 1, vec2.z));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x + 1, vec3.y + 1, vec3.z));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x + 1, vec3.y + 1, vec3.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x + 1, vec3.y, vec3.z + 1));
                break;
            }
            case Voxel.BACK: {
                //The back face is composed of v5, v4, v7 and v6.

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x + 1, vec0.y, vec0.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x + 1, vec0.y - 1, vec0.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x, vec0.y - 1, vec0.z - 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x, vec1.y - 1, vec1.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x - 1, vec1.y - 1, vec1.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x - 1, vec1.y, vec1.z - 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x - 1, vec2.y, vec2.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x - 1, vec2.y + 1, vec2.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x, vec2.y + 1, vec2.z - 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x, vec3.y + 1, vec3.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x + 1, vec3.y + 1, vec3.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x + 1, vec3.y, vec3.z - 1));

                break;
            }
            case Voxel.LEFT: {
                //The left face is composed of v4, v0, v3 and v7.

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y, vec0.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y - 1, vec0.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y - 1, vec0.z));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x - 1, vec1.y - 1, vec1.z));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x - 1, vec1.y - 1, vec1.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x - 1, vec1.y, vec1.z + 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x - 1, vec2.y, vec2.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x - 1, vec2.y + 1, vec2.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x - 1, vec2.y + 1, vec2.z));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y + 1, vec3.z));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y + 1, vec3.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y, vec3.z - 1));

                break;
            }
            case Voxel.TOP: {
                //The top face is composed of v3, v2, v6 and v7.

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y + 1, vec0.z));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y + 1, vec0.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x, vec0.y + 1, vec0.z + 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x, vec1.y + 1, vec1.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y + 1, vec1.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y + 1, vec1.z));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y + 1, vec2.z));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y + 1, vec2.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x, vec2.y + 1, vec2.z - 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x, vec3.y + 1, vec3.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y + 1, vec3.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y + 1, vec3.z));

                break;
            }
            case Voxel.DOWN: {
                //The down face is composed of v4, v5, v1 and v0.

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y - 1, vec0.z));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x - 1, vec0.y - 1, vec0.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec0.x, vec0.y - 1, vec0.z - 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x, vec1.y - 1, vec1.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y - 1, vec1.z - 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec1.x + 1, vec1.y - 1, vec1.z));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y - 1, vec2.z));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x + 1, vec2.y - 1, vec2.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec2.x, vec2.y - 1, vec2.z + 1));

                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x, vec3.y - 1, vec3.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y - 1, vec3.z + 1));
                neighborhood[i++] = getNeighborLighting(chunk, side, tmpVec.set(vec3.x - 1, vec3.y - 1, vec3.z));

                break;
            }
            default: {
                System.out.println("Invalid side value!!!");
            }
        }

        return neighborhood;
    }

    private static float computeSmoorthLightingVertex(int sideLighting, byte neighborhood, byte neighborhood0, byte neighborhood1) {
        return (sideLighting + neighborhood + neighborhood0 + neighborhood1) / 4f;
    }
}
