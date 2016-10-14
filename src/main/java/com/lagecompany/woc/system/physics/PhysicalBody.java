package com.lagecompany.woc.system.physics;

import com.simsilica.es.Entity;

public class PhysicalBody {

	private Entity entity;
	private float mass;
	private float velocity;

	public PhysicalBody(Entity entity, float mass, float velocity) {
		this.entity = entity;
		this.mass = mass;
		this.velocity = velocity;
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

	public float getVelocity() {
		return velocity;
	}

	public void setVelocity(float velocity) {
		this.velocity = velocity;
	}
}
