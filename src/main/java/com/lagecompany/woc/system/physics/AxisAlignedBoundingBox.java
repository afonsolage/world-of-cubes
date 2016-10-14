package com.lagecompany.woc.system.physics;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;

public class AxisAlignedBoundingBox {

	private final Entity entity;
	private final Vector3f center;
	private final Vector3f extent;

	public AxisAlignedBoundingBox(Entity entity, Vector3f center, Vector3f extent) {
		this.entity = entity;
		this.center = center;
		this.extent = extent;
	}

	public boolean intersect(AxisAlignedBoundingBox other) {
		if (entity.getId() == other.getEntity().getId())
			return false;
		
		if (center.x + extent.x < other.center.x - other.extent.x
				|| center.x - extent.x > other.center.x + other.extent.x) {
			return false;
		} else if (center.y + extent.y < other.center.y - other.extent.y
				|| center.y - extent.y > other.center.y + other.extent.y) {
			return false;
		} else if (center.z + extent.z < other.center.z - other.extent.z
				|| center.z - extent.z > other.center.z + other.extent.z) {
			return false;
		} else {
			return true;
		}
	}

	public Entity getEntity() {
		return entity;
	}
}
