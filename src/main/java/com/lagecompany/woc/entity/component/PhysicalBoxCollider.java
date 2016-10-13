package com.lagecompany.woc.entity.component;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;

public class PhysicalBoxCollider implements EntityComponent {

	private final Vector3f bounds;

	public PhysicalBoxCollider(Vector3f bounds) {
		this.bounds = bounds;
	}

	public Vector3f getBounds() {
		return bounds;
	}

	@Override
	public String toString() {
		return "PhysicalBoxCollider [bounds=" + bounds + "]";
	}
}
