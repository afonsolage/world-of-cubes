package com.lagecompany.woc.entity.system;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jme3.app.Application;
import com.jme3.math.Vector3f;
import com.lagecompany.woc.entity.EntityDataState;
import com.lagecompany.woc.entity.component.PhysicalBoxCollider;
import com.lagecompany.woc.entity.component.PhysicalCollision;
import com.lagecompany.woc.entity.component.Position;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.lemur.event.BaseAppState;

public class PhysicalColliderState extends BaseAppState {

	private EntityData entityData;
	private EntitySet entities;

	private final Map<EntityId, PhysicalColliderData> colliders;

	private class PhysicalColliderData {
		final Entity entity;
		final Vector3f position;
		final Vector3f bounds;

		public PhysicalColliderData(Entity entity, Vector3f position, Vector3f bounds) {
			this.entity = entity;
			this.position = position;
			this.bounds = bounds;
		}

		boolean intersect(PhysicalColliderData other) {
			if (entity.getId() == other.entity.getId())
				return false;

			if (position.x + bounds.x < other.position.x - other.bounds.x
					|| position.x - bounds.x > other.position.x + other.bounds.x) {
				return false;
			} else if (position.y + bounds.y < other.position.y - other.bounds.y
					|| position.y - bounds.y > other.position.y + other.bounds.y) {
				return false;
			} else if (position.z + bounds.z < other.position.z - other.bounds.z
					|| position.z - bounds.z > other.position.z + other.bounds.z) {
				return false;
			} else {
				return true;
			}
		}
	}

	public PhysicalColliderState() {
		colliders = new HashMap<>();
	}

	@Override
	protected void initialize(Application app) {
		entityData = getState(EntityDataState.class).getEntityData();
		entities = entityData.getEntities(Position.class, PhysicalCollision.class, PhysicalBoxCollider.class);
	}

	@Override
	public void update(float tpf) {
		if (entities.applyChanges()) {
			removeEntities(entities.getRemovedEntities());
			addEntities(entities.getAddedEntities());
			updateEntities(entities.getChangedEntities());
		}

	}

	private void updateEntities(Set<Entity> changedEntities) {
		for (Entity e : changedEntities) {
			colliders.put(e.getId(), new PhysicalColliderData(e, e.get(Position.class).getPosition(),
					e.get(PhysicalBoxCollider.class).getBounds()));

			checkCollision(e);

		}
	}

	private void checkCollision(Entity entity) {
		PhysicalColliderData collider = colliders.get(entity.getId());

		for (PhysicalColliderData otherCollider : colliders.values()) {
			if (collider.intersect(otherCollider)) {
				collider.entity.set(new PhysicalCollision(new Vector3f(0.0f, 0.0f, 0.0f)));
				otherCollider.entity.set(new PhysicalCollision(new Vector3f(0.0f, 0.0f, 0.0f)));
			}
		}

	}

	private void addEntities(Set<Entity> addedEntities) {
		for (Entity e : addedEntities) {
			colliders.put(e.getId(), new PhysicalColliderData(e, e.get(Position.class).getPosition(),
					e.get(PhysicalBoxCollider.class).getBounds()));
		}
	}

	private void removeEntities(Set<Entity> removedEntities) {
		for (Entity e : removedEntities) {
			colliders.remove(e.getId());
		}
	}

	@Override
	protected void cleanup(Application app) {

	}

	@Override
	protected void enable() {

	}

	@Override
	protected void disable() {

	}

}
