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

}
