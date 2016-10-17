package com.lagecompany.woc.system.physics;

import java.util.HashMap;
import java.util.Map;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.lagecompany.woc.system.physics.component.PhysicalCollision;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityId;

public class AABBCollider {

	private final Entity entity;
	private final Vector3f center;
	private final Vector3f extent;

	private Map<EntityId, Vector3f> collisions;

	public AABBCollider(Entity entity, Vector3f center, Vector3f extent) {
		this.entity = entity;
		this.center = center;
		this.extent = extent;
		this.collisions = new HashMap<>();
	}

	public Vector3f intersect(AABBCollider other) {
		if (entity.getId() == other.getEntity().getId())
			return null;

		if (center.x + extent.x < other.center.x - other.extent.x
				|| center.x - extent.x > other.center.x + other.extent.x) {
			return null;
		}

		if (center.y + extent.y < other.center.y - other.extent.y
				|| center.y - extent.y > other.center.y + other.extent.y) {
			return null;
		}

		if (center.z + extent.z < other.center.z - other.extent.z
				|| center.z - extent.z > other.center.z + other.extent.z) {
			return null;
		}

		Vector3f direction = other.center.subtract(center).normalize();

		Vector3f collision = direction.mult(extent).addLocal(center);
		Vector3f otherCollision = direction.negateLocal().mult(other.extent).addLocal(other.center);

		collision.subtractLocal(otherCollision);

		if (FastMath.abs(collision.x) > FastMath.abs(collision.y)
				&& FastMath.abs(collision.x) > FastMath.abs(collision.z)) {
			return new Vector3f(direction.x, 0, 0);
		} else if (FastMath.abs(collision.y) > FastMath.abs(collision.x)
				&& FastMath.abs(collision.y) > FastMath.abs(collision.z)) {
			return new Vector3f(0, collision.y, 0);
		} else {
			return new Vector3f(0, 0, collision.z);
		}
	}

	public Map<EntityId, Vector3f> getCollisions() {
		return collisions;
	}

	public void addCollision(EntityId entity, Vector3f collisionNormal) {
		collisions.put(entity, collisionNormal);
	}

	public Entity getEntity() {
		return entity;
	}

	public void updateEntity() {
		PhysicalCollision previousCollision = entity.get(PhysicalCollision.class);

		Map<EntityId, Vector3f> previousCollisions = previousCollision.getCollisions();

		// If there is no collision before and there is no collision now, then there is nothing to update.
		if (previousCollisions == null && collisions.isEmpty()) {
			return;
		} else if (previousCollisions != null && previousCollision.equals(collisions)) {
			return;
		}

		entity.set(new PhysicalCollision(collisions));
	}
}
