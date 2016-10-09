package com.lagecompany.woc.storage.voxel;

import java.util.Objects;

import com.lagecompany.woc.storage.Chunk;
import com.lagecompany.woc.storage.Vec3;

/**
 *
 * @author Afonso Lage
 */
public class SpecialVoxelData {

    public Chunk chunk;
    public int x;
    public int y;
    public int z;
    public boolean active;

    public SpecialVoxelData(Chunk chunk, Vec3 position) {
	this(chunk, position.x, position.y, position.z);
    }

    public SpecialVoxelData(Chunk chunk, int x, int y, int z) {
	this.chunk = chunk;
	this.x = x;
	this.y = y;
	this.z = z;
	this.active = true;
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 83 * hash + Objects.hashCode(this.chunk);
	hash = 83 * hash + this.x;
	hash = 83 * hash + this.y;
	hash = 83 * hash + this.z;
	return hash;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final SpecialVoxelData other = (SpecialVoxelData) obj;
	if (this.x != other.x) {
	    return false;
	}
	if (this.y != other.y) {
	    return false;
	}
	if (this.z != other.z) {
	    return false;
	}
	if (!Objects.equals(this.chunk, other.chunk)) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "Special Voxel " + chunk.getName() + "[" + x + "," + y + "," + z + "]";
    }
}
