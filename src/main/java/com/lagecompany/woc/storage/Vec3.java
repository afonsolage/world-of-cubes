package com.lagecompany.woc.storage;

import com.lagecompany.woc.util.MathUtils;

/**
 * This class is used to store a grid position or a direction on Chunks world space. This class is optimized to work
 * with Voxel engine. Also this class assumes a RHS (Right-Handed System).
 * 
 * @author Afonso Lage
 *
 */
public class Vec3 {

	/**
	 * Represents the right direction (1, 0, 0)
	 */
	public static final Vec3 RIGHT = new Vec3(1, 0, 0);
	/**
	 * Represents the left direction (-1, 0, 0)
	 */
	public static final Vec3 LEFT = new Vec3(-1, 0, 0);
	/**
	 * Represents the up direction (0, 1, 0)
	 */
	public static final Vec3 UP = new Vec3(0, 1, 0);
	/**
	 * Represents the down direction (0, 1, 0)
	 */
	public static final Vec3 DOWN = new Vec3(0, -1, 0);
	/**
	 * Represents the forwards direction (0, 0, 1)
	 */
	public static final Vec3 FORWARD = new Vec3(0, 0, 1);
	/**
	 * Represents the backwards direction (0, 0, -1)
	 */
	public static final Vec3 BACKWARD = new Vec3(0, 0, -1);
	/**
	 * Represents a zero position (0, 0, 0)
	 */
	public static final Vec3 ZERO = new Vec3(0, 0, 0);
	/**
	 * Represents and static array containing all directions on the following order: UP, RIGHT, DOWN, LEFT, FORWARD,
	 * BACKWARD
	 */
	public static final Vec3[] ALL_DIRECTIONS = { UP, RIGHT, DOWN, LEFT, FORWARD, BACKWARD };

	/**
	 * x value of 3D position.
	 */
	public int x;
	/**
	 * y value of 3D position.
	 */
	public int y;
	/**
	 * z value of 3D position.
	 */
	public int z;

	/**
	 * Constructs a zero Vec3 (0, 0, 0)
	 */
	public Vec3() {
	}

	/**
	 * Constructs a Vec3 by coping the values from another Vec3
	 * 
	 * @param v
	 *            Vec3 object to be copied from
	 */
	public Vec3(Vec3 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	/**
	 * Constructs a Vec3 and initialize x, y, z with the given values
	 * 
	 * @param x
	 *            Value to initialize x member.
	 * @param y
	 *            Value to initialize y member.
	 * @param z
	 *            Value to initialize z member.
	 */
	public Vec3(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Constructs a Vec3 and initialize x, y, z with the given float values. Those values will be rounded using a
	 * special floor function.
	 * 
	 * @param x
	 *            Value to initialize x member.
	 * @param y
	 *            Value to initialize y member.
	 * @param z
	 *            Value to initialize z member.
	 */
	public Vec3(float x, float y, float z) {
		this.x = MathUtils.floorRound(x);
		this.y = MathUtils.floorRound(y);
		this.z = MathUtils.floorRound(z);
	}

	/**
	 * Sets x, y, z with the given float values. Those values will be rounded using a special floor function.
	 * 
	 * @param x
	 *            Value to initialize x member.
	 * @param y
	 *            Value to initialize y member.
	 * @param z
	 *            Value to initialize z member.
	 * @return A reference to this object.
	 */
	public Vec3 set(float x, float y, float z) {
		this.x = MathUtils.floorRound(x);
		this.y = MathUtils.floorRound(y);
		this.z = MathUtils.floorRound(z);

		return this;
	}

	/**
	 * Sets x, y, z with the given int values.
	 * 
	 * @param x
	 *            Value to initialize x member.
	 * @param y
	 *            Value to initialize y member.
	 * @param z
	 *            Value to initialize z member.
	 * @return A reference to this object.
	 */
	public Vec3 set(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;

		return this;
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

	/**
	 * Add the value of given Vec3 on this one
	 * 
	 * @param v
	 *            The given Vec3 to be added to this one
	 * @return Self reference to enable a chain method call.
	 */
	public Vec3 add(Vec3 v) {
		this.x += v.x;
		this.y += v.y;
		this.z += v.z;

		return this;
	}

	/**
	 * Add the given value on this Vec3
	 * 
	 * @param x
	 *            Value to be added on x member.
	 * @param y
	 *            Value to be added on x member.
	 * @param z
	 *            Value to be added on x member.
	 * @return Self reference to enable a chain method call.
	 */
	public Vec3 add(int x, int y, int z) {
		this.x += x;
		this.y += y;
		this.z += z;

		return this;
	}

	/**
	 * Subtract the given value on this Vec3
	 * 
	 * @param x
	 *            Value to be subtracted on x member.
	 * @param y
	 *            Value to be subtracted on x member.
	 * @param z
	 *            Value to be subtracted on x member.
	 * @return Self reference to enable a chain method call.
	 */
	public Vec3 subtract(int x, int y, int z) {
		this.x -= x;
		this.y -= y;
		this.z -= z;

		return this;
	}

	/**
	 * Subtract the value of given Vec3 on this one
	 * 
	 * @param v
	 *            The given Vec3 to be subtracted to this one
	 * @return Self reference to enable a chain method call.
	 */
	public Vec3 subtract(Vec3 v) {
		return subtract(v.x, v.y, v.z);
	}

	/**
	 * Create and returns a new Vec3 with a zero default value.
	 * 
	 * @return
	 */
	public Vec3 copy() {
		return new Vec3(this);
	}

	/**
	 * Create and returns a new Vec3 with a copy of current values.
	 * 
	 * @return
	 */
	public Vec3 copy(Vec3 other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;

		return this;
	}

	public Vec3 mod(int value) {
		this.x = MathUtils.absMod(this.x, value);
		this.y = MathUtils.absMod(this.y, value);
		this.z = MathUtils.absMod(this.z, value);

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