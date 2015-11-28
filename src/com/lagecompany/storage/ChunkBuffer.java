/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.storage;

import com.lagecompany.storage.voxel.Voxel;
import com.lagecompany.storage.voxel.VoxelReference;
import com.lagecompany.util.MathUtils;
import java.util.Arrays;

/**
 *
 * @author Afonso Lage
 */
public class ChunkBuffer {

    private byte[] buffer;

    public ChunkBuffer() {
        buffer = new byte[Chunk.DATA_LENGTH * Voxel.SIZE];
    }

    public void free() {
        buffer = null;
    }

    void resetMergedVisible() {
        VoxelReference voxel;
        for (int i = 0; i < Chunk.DATA_LENGTH; i++) {
            voxel = new VoxelReference(this, i * Voxel.SIZE, 0, 0, 0);
            
            for(int side = Voxel.FRONT; side <= Voxel.DOWN; side++) {
                voxel.resetSideMerged(side);
                voxel.resetSideVisible(side);
            }
        }
    }

    public void reset(int offset) {
        Arrays.fill(buffer, offset, offset + Voxel.SIZE, (byte) 0);
    }

    public void copy(int srcOffset, int dstOffset) {
        System.arraycopy(buffer, srcOffset, buffer, dstOffset, Voxel.SIZE);
    }

    public VoxelReference get(int x, int y, int z) {
        int index = MathUtils.toVoxelIndex(x, y, z);
        return new VoxelReference(this, index * Voxel.SIZE, x, y, z);
    }

    void set(int x, int y, int z, short type) {
        get(x, y, z).setType(type);
    }

    void set(VoxelReference newVoxel) {
        get(newVoxel.x, newVoxel.y, newVoxel.z).copy(newVoxel);
    }

    public byte getByte(int i) {
        if (i < 0 || i >= buffer.length) {
            System.out.println("Ooops...");
        }
        return buffer[i];
    }

    public void setByte(int i, byte value) {
        if (i < 0 || i >= buffer.length) {
            System.out.println("Ooops...");
        }
        buffer[i] = value;
    }

    public short getShort(int i) {
        return (short) ((buffer[i] << 8) | buffer[i + 1]);
    }

    public void setShort(int i, short type) {
        buffer[i] = (byte) ((type) >>> 8);
        buffer[i + 1] = (byte) (type & 0xFF);
    }
}
