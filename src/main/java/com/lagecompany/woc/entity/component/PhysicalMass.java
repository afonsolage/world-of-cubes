package com.lagecompany.woc.entity.component;

import com.simsilica.es.EntityComponent;

public class PhysicalMass implements EntityComponent {

	private final float mass;

	public PhysicalMass(float mass) {
		this(mass, 0.0f);
	}

	public PhysicalMass(float mass, float gravityForce) {
		this.mass = mass;
	}

	public float getMass() {
		return mass;
	}

	@Override
	public String toString() {
		return "PhysicalBody [mass=" + mass + "]";
	}
}
