/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.entity;

import com.lagecompany.storage.voxel.Voxel;

/**
 *
 * @author Afonso Lage
 */
public class Player {

    private short activeBlock = Voxel.VT_NONE;

    public short getActiveBlock() {
        return activeBlock;
    }

    public void setActiveBlock(short activeBlock) {
        this.activeBlock = activeBlock;
    }
}
