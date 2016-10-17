package com.lagecompany.woc.system.physics;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;

public class PhysicalBody {

	private Entity entity;
	private float bouncing;
	private float mass;
	private Vector3f force;
	private boolean collided;

	public PhysicalBody(Entity entity, float mass, float bouncing) {
		this.entity = entity;
		this.mass = mass;
		this.bouncing = bouncing;
		this.collided = false;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public float getMass() {
		return mass;
	}

	public void setMass(float mass) {
		this.mass = mass;
	}

	public float getBouncing() {
		return bouncing;
	}

	public void setBouncing(float boucing) {
		this.bouncing = boucing;
	}

	public Vector3f getForce() {
		return force;
	}

	public void setForce(Vector3f force) {
		assert Vector3f.isValidVector(force);
		this.force = force;
	}

	public boolean isCollided() {
		return collided;
	}

	public void setCollided(boolean collided) {
		this.collided = collided;
	}
}
