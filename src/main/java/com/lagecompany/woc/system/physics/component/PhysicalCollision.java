package com.lagecompany.woc.system.physics.component;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;

public class PhysicalCollision implements EntityComponent {

	private final Vector3f collisionPoint;

	public PhysicalCollision() {
		this(null);
	}

	public PhysicalCollision(Vector3f collisionPoint) {
		this.collisionPoint = collisionPoint;
	}

	public boolean isCollided() {
		return this.collisionPoint != null;
	}

	public Vector3f getCollisionPoint() {
		return collisionPoint;
	}

	@Override
	public String toString() {
		return "PhysicalCollision [collisionPoint=" + collisionPoint + "]";
	}
}
