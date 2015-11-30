/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.storage.light;

import com.lagecompany.storage.Are;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;
import com.lagecompany.storage.voxel.Voxel;
import com.lagecompany.storage.voxel.VoxelReference;

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
        VoxelReference neighbor = new VoxelReference();
        Chunk tmpChunk;

        if (previousType == Voxel.VT_NONE) {
            //Block add

            c.addSunLightRemovalQueue(new LightRemovalNode(voxel.position.x, voxel.position.y, voxel.position.z, previousSunLight));
        } else {
            //Block remove

            for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
                neighbor.position.set(voxel.position.x + dir.x, voxel.position.y + dir.y, voxel.position.z + dir.z);
                tmpChunk = are.validateChunkAndVoxel(c, neighbor.position);

                if (tmpChunk == null) {
                    //Oopps, we hit the are bounds, this shouldn't happen outside development.
                    continue;
                }

                tmpChunk.addSunLightPropagationQueue(tmpChunk.cloneReference(neighbor));
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
    private static void getNeighborhoodLighting(Chunk chunk, final VoxelReference voxel, byte[] voxelBuffer) {
        VoxelReference tmpVoxel = new VoxelReference();

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
        tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y + 1, voxel.position.z + 1);
        voxelBuffer[0] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

        tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y + 1, voxel.position.z);
        voxelBuffer[1] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

        tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y + 1, voxel.position.z - 1);
        voxelBuffer[2] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

        //TOP MIDDLE NEIGHBORS
        tmpVoxel.position.set(voxel.position.x, voxel.position.y + 1, voxel.position.z + 1);
        voxelBuffer[3] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x, voxel.position.y + 1, voxel.position.z - 1);
        voxelBuffer[4] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

        //TOP LEFT NEIGHBORS
        tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y + 1, voxel.position.z + 1);
        voxelBuffer[5] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y + 1, voxel.position.z);
        voxelBuffer[6] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y + 1, voxel.position.z - 1);
        voxelBuffer[7] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

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
        tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y, voxel.position.z + 1);
        voxelBuffer[8] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y, voxel.position.z - 1);
        voxelBuffer[9] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

        //MID LEFT NEIGHBORS
        tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y, voxel.position.z + 1);
        voxelBuffer[10] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y, voxel.position.z - 1);
        voxelBuffer[11] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

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
        tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y - 1, voxel.position.z + 1);
        voxelBuffer[12] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y - 1, voxel.position.z);
        voxelBuffer[13] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y - 1, voxel.position.z - 1);
        voxelBuffer[14] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

        //DOWN MIDDLE NEIGHBORS
        tmpVoxel.position.set(voxel.position.x, voxel.position.y - 1, voxel.position.z + 1);
        voxelBuffer[15] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x, voxel.position.y - 1, voxel.position.z - 1);
        voxelBuffer[16] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;

        //DOWN LEFT NEIGHBORS
        tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y - 1, voxel.position.z + 1);
        voxelBuffer[17] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y - 1, voxel.position.z);
        voxelBuffer[18] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
        tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y - 1, voxel.position.z - 1);
        voxelBuffer[19] = (chunk.getAreVoxel(tmpVoxel)) ? tmpVoxel.getFinalLight() : 0;
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

    private static float computeSmoorthLightingVertex(int sideLighting, byte neighborhood, byte neighborhood0, byte neighborhood1) {
        return (sideLighting + neighborhood + neighborhood0 + neighborhood1) / 4f;
    }
}
