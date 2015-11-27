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

    public int x;
    public int y;
    public int z;

    public Vec3() {
    }

    public Vec3(Vec3 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public Vec3(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(float x, float y, float z) {
        this.x = MathUtils.floorRound(x);
        this.y = MathUtils.floorRound(y);
        this.z = MathUtils.floorRound(z);
    }

    public void set(float x, float y, float z) {
        this.x = (int) x;
        this.y = (int) y;
        this.z = (int) z;
    }

    @Override
    public int hashCode() {
        int hashCode = 7;
        hashCode = 97 * hashCode + this.x;
        hashCode = 97 * hashCode + this.y;
        hashCode = 97 * hashCode + this.z;

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

        return this;
    }

    public Vec3 add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Vec3 subtract(int x, int y, int z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public Vec3 subtract(Vec3 v) {
        return subtract(v.x, v.y, v.z);
    }

    public Vec3 copy() {
        return new Vec3(this);
    }

    public Vec3 copy(Vec3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;

        return this;
    }

    public static Vec3 copyAdd(Vec3 position, int x, int y, int z) {
        return new Vec3(position.x + x, position.y + y, position.z + z);
    }

    public static Vec3 copyAdd(Vec3 a, Vec3 b) {
        return new Vec3(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public Vec3 addNew(Vec3 v) {
        return new Vec3(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    public Vec3 subtractNew(Vec3 v) {
        return new Vec3(this.x - v.x, this.y - v.y, this.z - v.z);
    }
}
