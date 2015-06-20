package com.lagecompany.storage;

public class Vec3 {

    private int x;
    private int y;
    private int z;
    private int hashCode;

    public Vec3() {
	updateHashCode();
    }

    public Vec3(Vec3 v) {
	this(v.getX(), v.getY(), v.getZ());
    }

    public Vec3(int x, int y, int z) {
	this.x = x;
	this.y = y;
	this.z = z;

	updateHashCode();
    }

    public int getX() {
	return x;
    }

    public void setX(int x) {
	this.x = x;
	updateHashCode();
    }

    public int getY() {
	return y;
    }

    public void setY(int y) {
	this.y = y;
	updateHashCode();
    }

    public int getZ() {
	return z;
    }

    public void setZ(int z) {
	this.z = z;
	updateHashCode();
    }

    private void updateHashCode() {
	hashCode = 7;
	hashCode = 97 * hashCode + this.x;
	hashCode = 97 * hashCode + this.y;
	hashCode = 97 * hashCode + this.z;
    }

    @Override
    public int hashCode() {
	return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final Vec3 other = (Vec3) obj;
	if (this.x != other.x) {
	    return false;
	}
	if (this.y != other.y) {
	    return false;
	}
	if (this.z != other.z) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return String.format("%d, %d, %d", x, y, z);
    }

    public Vec3 add(Vec3 v) {
	this.x += v.x;
	this.y += v.y;
	this.z += v.z;
	
	updateHashCode();
	return this;
    }

    public Vec3 copy() {
	return new Vec3(this);
    }
    
    public static Vec3 RIGHT() {
	return new Vec3(1, 0, 0);
    }

    public static Vec3 LEFT() {
	return new Vec3(-1, 0, 0);
    }

    public static Vec3 UP() {
	return new Vec3(0, 1, 0);
    }

    public static Vec3 DOWN() {
	return new Vec3(0, -1, 0);
    }

    public static Vec3 FRONT() {
	return new Vec3(0, 0, 1);
    }

    public static Vec3 BACK() {
	return new Vec3(0, 0, 1);
    }

    public static Vec3 ZERO() {
	return new Vec3(0, 0, 0);
    }

    public Vec3 add(int x, int y, int z) {
	this.x += x;
	this.y += y;
	this.z += z;
	
	updateHashCode();
	return this;
    }
}
