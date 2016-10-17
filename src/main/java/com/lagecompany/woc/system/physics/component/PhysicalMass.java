package com.lagecompany.woc.system.physics.component;

import com.simsilica.es.EntityComponent;

public class PhysicalMass implements EntityComponent {

	private final float mass;
	private final float bouncing;

	public PhysicalMass(float mass) {
		this(mass, 0.0f);
	}

	public PhysicalMass(float mass, float bouncing) {
		this.mass = mass;
		this.bouncing = bouncing;
	}

	public float getMass() {
		return mass;
	}

	@Override
	public String toString() {
		return "PhysicalBody [mass=" + mass + "]";
	}

	public float getBouncing() {
		return bouncing;
	}
}