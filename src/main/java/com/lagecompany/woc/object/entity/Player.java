package com.lagecompany.woc.object.entity;

import com.lagecompany.woc.storage.voxel.Voxel;

public class Player {

    private short activeBlock = Voxel.VT_NONE;

    public short getActiveBlock() {
        return activeBlock;
    }

    public void setActiveBlock(short activeBlock) {
        this.activeBlock = activeBlock;
    }
}