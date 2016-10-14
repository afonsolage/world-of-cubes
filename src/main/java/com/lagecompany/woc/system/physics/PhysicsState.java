package com.lagecompany.woc.system.physics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jme3.app.Application;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.lagecompany.woc.system.debug.component.Position;
import com.lagecompany.woc.system.entity.EntityDataState;
import com.lagecompany.woc.system.physics.component.PhysicalAABBCollider;
import com.lagecompany.woc.system.physics.component.PhysicalCollision;
import com.lagecompany.woc.system.physics.component.PhysicalMass;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.lemur.event.BaseAppState;

public class PhysicsState extends BaseAppState {

	private static final float GRAVITY_ACC = 9.780f;
	private static final float MAX_VELOCITY = 1000f; // Limit velocity at 1k units per second
	private static final Vector3f GRAVITY_DIR = new Vector3f(0.0f, -1.0f, 0.0f);

	private EntityData entityData;
	private EntitySet physicalBodyEntities;
	private EntitySet physicalAABBColliderEntities;
	
	private final Map<EntityId, PhysicalBody> physicalBodies;
	private final Map<EntityId, AxisAlignedBoundingBox> AABBColliders;
	private float unitStep;

	public PhysicsState() {
		physicalBodies = new HashMap<>();
		AABBColliders = new HashMap<>();
	}

	@Override
	protected void initialize(Application app) {
		entityData = getState(EntityDataState.class).getEntityData();
		physicalBodyEntities = entityData.getEntities(PhysicalMass.class, Position.class, PhysicalCollision.class);
		physicalAABBColliderEntities = entityData.getEntities(PhysicalAABBCollider.class, Position.class, PhysicalCollision.class);
	}

	@Override
	public void update(float tpf) {
		updatePhysicalBodyEntities(tpf);
		updateAABBColliderEntities(tpf);
	}

	private void updateAABBColliderEntities(float tpf) {
		if (physicalAABBColliderEntities.applyChanges()) {
			removeAABBColiderEntities(physicalAABBColliderEntities.getRemovedEntities());
			addAABBColiderEntities(physicalAABBColliderEntities.getAddedEntities());
			updateAABBColiderEntities(physicalAABBColliderEntities.getChangedEntities());
		}
	}

	private void updateAABBColiderEntities(Set<Entity> changedEntities) {
		for (Entity e : changedEntities) {
			AABBColliders.put(e.getId(), new AxisAlignedBoundingBox(e, e.get(Position.class).getPosition(),
					e.get(PhysicalAABBCollider.class).getBounds()));

			checkCollision(e);
		}
	}

	private void checkCollision(Entity entity) {
		AxisAlignedBoundingBox collider = AABBColliders.get(entity.getId());

		for (AxisAlignedBoundingBox otherCollider : AABBColliders.values()) {
			if (collider.intersect(otherCollider)) {
				collider.getEntity().set(new PhysicalCollision(new Vector3f(0.0f, 0.0f, 0.0f)));
				otherCollider.getEntity().set(new PhysicalCollision(new Vector3f(0.0f, 0.0f, 0.0f)));
			}
		}

	}

	private void addAABBColiderEntities(Set<Entity> addedEntities) {
		for (Entity e : addedEntities) {
			AABBColliders.put(e.getId(), new AxisAlignedBoundingBox(e, e.get(Position.class).getPosition(),
					e.get(PhysicalAABBCollider.class).getBounds()));
		}
	}

	private void removeAABBColiderEntities(Set<Entity> removedEntities) {
		for (Entity e : removedEntities) {
			AABBColliders.remove(e.getId());
		}
	}

	private void updatePhysicalBodyEntities(float tpf) {
		if (physicalBodyEntities.applyChanges()) {
			unitStep = tpf / 1000.0f;
			removePhysicalBodyEntities(physicalBodyEntities.getRemovedEntities());
			addPhysicalBodyEntities(physicalBodyEntities.getAddedEntities());
		}

		updatePhysicalBodies();
	}
	
	private void removePhysicalBodyEntities(Set<Entity> removedEntities) {
		for (Entity e : removedEntities) {
			physicalBodies.remove(e.getId());
		}
	}

	private void addPhysicalBodyEntities(Set<Entity> addedEntities) {
		for (Entity e : addedEntities) {
			physicalBodies.put(e.getId(), new PhysicalBody(e, e.get(PhysicalMass.class).getMass(), 0.0f));
		}
	}

	private void updatePhysicalBodies() {
		for (PhysicalBody body : physicalBodies.values()) {
			Entity entity = body.getEntity();
			PhysicalCollision collision = entity.get(PhysicalCollision.class);

			if (collision.isCollided()) {
				body.setVelocity(0);
			} else {
				body.setVelocity(FastMath.clamp(body.getVelocity() + (GRAVITY_ACC * unitStep * body.getMass()), 0f,
						MAX_VELOCITY));
			}

			Vector3f move = GRAVITY_DIR.mult(body.getVelocity());

			if (move.length() >= 0.0f) {
				Position pos = entity.get(Position.class);
				entity.set(new Position(pos.getPosition().addLocal(move)));
			}
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
