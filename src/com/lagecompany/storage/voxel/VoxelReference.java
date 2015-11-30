/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.storage.voxel;

import com.lagecompany.storage.ChunkBuffer;
import com.lagecompany.storage.Vec3;

/**
 *
 * @author TI.Afonso
 */
public class VoxelReference {

    public final Vec3 position = new Vec3();

    public int offset;
    public ChunkBuffer buffer;
    

    public VoxelReference() {
    }

    public VoxelReference(ChunkBuffer buffer, int offset) {
        this.buffer = buffer;
        this.offset = offset;
    }

    public VoxelReference(ChunkBuffer buffer, int offset, int x, int y, int z) {
        this.buffer = buffer;
        this.offset = offset;
        this.position.set(x, y, z);
    }

    public void set(int offset, ChunkBuffer buffer) {
        this.offset = offset;
        this.buffer = buffer;
    }

    public void setType(short type) {
        buffer.setShort(offset + Voxel.TYPE, type);
    }

    public void setSunLight(byte value) {
        //First clear sun light existing value by doing an AND bitwise operation on LIGHT_NORMAL (0000 1111)
        //Then, lets ensure that only four bits is used to set light value (0000 1111), because light value must be less then 15.
        value = (byte) ((buffer.getByte(offset + Voxel.LIGHT) & Voxel.LIGHT_NORMAL) | ((value & 0x0F) << Voxel.LIGHT_SUN_SHIFT));
        buffer.setByte(offset + Voxel.LIGHT, value);
    }

    public byte getSunLight() {
        return (byte) ((buffer.getByte(offset + Voxel.LIGHT) & Voxel.LIGHT_SUN) >>> Voxel.LIGHT_SUN_SHIFT);
    }

    public void setLight(byte value) {
        //First clear light existing value by doing an AND bitwise operation on LIGHT_SUN (1111 0000)
        //Then, lets ensure that only four bits is used to set light value (0000 1111), because light value must be less then 15.
        value = (byte) ((buffer.getByte(offset + Voxel.LIGHT) & Voxel.LIGHT_SUN) | value & 0x0F);
        buffer.setByte(offset + Voxel.LIGHT, value);
    }

    public byte getLight() {
        return (byte) (buffer.getByte(offset + Voxel.LIGHT) & Voxel.LIGHT_NORMAL);
    }

    public boolean isTransparent() {
        return !isOpaque();
    }

    public boolean isOpaque() {
        return (buffer.getShort(offset + Voxel.TYPE) & Voxel.VF_OPAQUE) == Voxel.VF_OPAQUE;
    }

    public short getType() {
        return buffer.getShort(offset + Voxel.TYPE);
    }

    public void reset() {
        short type = getType();
        buffer.reset(offset);
        setType(type);
    }

    public byte getSideLight(int side) {
        assert side >= 0 && side <= Voxel.LIGHT;

        return (byte) ((buffer.getByte(offset + side) & Voxel.SIDE_LIGHT) >> Voxel.SIDE_LIGHT_SHIFT);
    }

    public void setSideLight(byte b, int side) {
        assert side >= 0 && side <= Voxel.LIGHT;

        byte sideValue = buffer.getByte(offset + side);
        buffer.setByte(offset + side, (byte) ((sideValue & ~Voxel.SIDE_LIGHT) | ((b & 0xF) << Voxel.SIDE_LIGHT_SHIFT)));
    }

    public boolean isSideVisible(int side) {
        assert side >= 0 && side <= Voxel.LIGHT;

        return (buffer.getByte(offset + side) & Voxel.SIDE_VISIBLE) == Voxel.SIDE_VISIBLE;
    }

    public void setSideVisible(int side) {
        assert side >= 0 && side <= Voxel.LIGHT;

        byte sideValue = buffer.getByte(offset + side);
        buffer.setByte(offset + side, (byte) (sideValue | Voxel.SIDE_VISIBLE));
    }

    public void resetSideVisible(int side) {
        assert side >= 0 && side <= Voxel.LIGHT;

        byte sideValue = buffer.getByte(offset + side);
        buffer.setByte(offset + side, (byte) (sideValue & ~Voxel.SIDE_VISIBLE));
    }

    public boolean isSideMerged(int side) {
        assert side >= 0 && side <= Voxel.LIGHT;

        return (buffer.getByte(offset + side) & Voxel.SIDE_MERGED) == Voxel.SIDE_MERGED;
    }

    public void setSideMerged(int side) {
        assert side >= 0 && side <= Voxel.LIGHT;

        byte sideValue = buffer.getByte(offset + side);
        buffer.setByte(offset + side, (byte) ((sideValue & ~Voxel.SIDE_MERGED) | (1 << Voxel.SIDE_MERGED_SHIFT)));
    }

    public void resetSideMerged(int side) {
        assert side >= 0 && side <= Voxel.LIGHT;

        byte sideValue = buffer.getByte(offset + side);
        buffer.setByte(offset + side, (byte) (sideValue & ~Voxel.SIDE_MERGED));
    }

    public void copy(VoxelReference newVoxel) {
        buffer.copy(newVoxel.offset, offset);
    }

    public byte getFinalLight() {
        byte value = buffer.getByte(offset + Voxel.LIGHT);

        byte sunLight = (byte) ((value & Voxel.LIGHT_SUN) >>> Voxel.LIGHT_SUN_SHIFT);
        byte light = (byte) (value & Voxel.LIGHT_NORMAL);
        return (sunLight > light) ? sunLight : light;
    }
}
