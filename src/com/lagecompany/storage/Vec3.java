package com.lagecompany.storage;

import com.lagecompany.util.MathUtils;

public class Vec3 {

    public static final Vec3 RIGHT = new Vec3(1, 0, 0);
    public static final Vec3 LEFT = new Vec3(-1, 0, 0);
    public static final Vec3 UP = new Vec3(0, 1, 0);
    public static final Vec3 DOWN = new Vec3(0, -1, 0);
    public static final Vec3 FRONT = new Vec3(0, 0, 1);
    public static final Vec3 BACK = new Vec3(0, 0, -1);
    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    public static final Vec3[] ALL_DIRECTIONS = {UP, RIGHT, DOWN, LEFT, FRONT, BACK};
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

    public Vec3(float x, float y, float z) {
	this.x = MathUtils.floorRound(x);
	this.y = MathUtils.floorRound(y);
	this.z = MathUtils.floorRound(z);

	updateHashCode();
    }

    public int getX() {
	return x;
    }

    public void set(float x, float y, float z) {
	this.x = (int) x;
	this.y = (int) y;
	this.z = (int) z;

	updateHashCode();
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
	return x + ", " + y + ", " + z;
    }

    public Vec3 add(Vec3 v) {
	this.x += v.x;
	this.y += v.y;
	this.z += v.z;

	updateHashCode();
	return this;
    }

    public Vec3 add(int x, int y, int z) {
	this.x += x;
	this.y += y;
	this.z += z;

	updateHashCode();
	return this;
    }

    public Vec3 subtract(int x, int y, int z) {
	this.x -= x;
	this.y -= y;
	this.z -= z;

	updateHashCode();
	return this;
    }

    public Vec3 subtract(Vec3 v) {
	return subtract(v.getX(), v.getY(), v.getZ());
    }

    public Vec3 copy() {
	return new Vec3(this);
    }

    public static Vec3 copyAdd(Vec3 position, int x, int y, int z) {
	return new Vec3(position.getX() + x, position.getY() + y, position.getZ() + z);
    }

    public static Vec3 copyAdd(Vec3 a, Vec3 b) {
	return new Vec3(a.getX() + b.getX(), a.getY() + b.getY(), a.getZ() + b.getZ());
    }

    public Vec3 addNew(Vec3 v) {
	return new Vec3(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    public Vec3 subtractNew(Vec3 v) {
	return new Vec3(this.x - v.x, this.y - v.y, this.z - v.z);
    }
}
