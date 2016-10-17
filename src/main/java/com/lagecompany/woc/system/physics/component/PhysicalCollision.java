package com.lagecompany.woc.system.physics.component;

import java.util.HashMap;
import java.util.Map;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;

public class PhysicalCollision implements EntityComponent {

	private final Map<EntityId, Vector3f> collisions;

	public PhysicalCollision() {
		collisions = null;
	}

	public PhysicalCollision(Map<EntityId, Vector3f> collisions) {
		if (!collisions.isEmpty()) {
			this.collisions = new HashMap<>(collisions);
		} else {
			this.collisions = null;
		}
	}

	public boolean isCollided() {
		return this.collisions != null;
	}

	public Map<EntityId, Vector3f> getCollisions() {
		return collisions;
	}

	public Vector3f[] getCollisionNormals() {
		return collisions.values().toArray(new Vector3f[collisions.size()]);
	}

	@Override
	public String toString() {
		return "PhysicalCollision [collisions=" + collisions + "]";
	}
}
