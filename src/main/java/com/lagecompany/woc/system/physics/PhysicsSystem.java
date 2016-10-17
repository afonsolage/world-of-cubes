package com.lagecompany.woc.system.physics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.lagecompany.woc.system.debug.component.Position;
import com.lagecompany.woc.system.physics.component.PhysicalAABBCollider;
import com.lagecompany.woc.system.physics.component.PhysicalCollision;
import com.lagecompany.woc.system.physics.component.PhysicalMass;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;

public class PhysicsSystem implements Runnable {

	private static final float unitStep = 1 / 60.0f; // Physics System will run at 30 Ticks per Second.
	private static final long tickDelta = (long) (unitStep * 1000000000); // 1 Nanosecond = 1e+9;

	private static final float GRAVITY_ACC = 9.780f;
	private static final float MAX_VELOCITY = 1000f; // Limit velocity at 1k units per second
	private static final Vector3f GRAVITY_DIR = new Vector3f(0.0f, -1.0f, 0.0f);

	// private EntityData entityData;
	private EntitySet physicalBodyEntities;
	private EntitySet physicalAABBColliderEntities;

	private final Map<EntityId, PhysicalBody> physicalBodies;
	private final Map<EntityId, AABBCollider> AABBColliders;

	private boolean running;

	public PhysicsSystem(EntityData entityData) {
		this.physicalBodies = new HashMap<>();
		this.AABBColliders = new HashMap<>();

		// this.entityData = entityData;
		physicalBodyEntities = entityData.getEntities(PhysicalMass.class, Position.class, PhysicalCollision.class);
		physicalAABBColliderEntities = entityData.getEntities(PhysicalAABBCollider.class, Position.class,
				PhysicalCollision.class);
	}

	@Override
	public void run() {
		running = true;

		long currentTick, lastTick, delta = System.nanoTime();
		long sleepFor;

		while (running) {
			currentTick = System.nanoTime();

			updateAABBColliderEntities();
			updatePhysicalBodyEntities();

			lastTick = currentTick;
			currentTick = System.nanoTime();
			delta = currentTick - lastTick;

			if (delta < tickDelta) {
				try {
					sleepFor = (tickDelta - delta) / 1000000;
					Thread.sleep(sleepFor); // Nano seconds to Milliseconds.
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	private void updateAABBColliderEntities() {
		if (physicalAABBColliderEntities.applyChanges()) {
			removeAABBColiderEntities(physicalAABBColliderEntities.getRemovedEntities());
			addAABBColiderEntities(physicalAABBColliderEntities.getAddedEntities());
			updateAABBColiderEntities(physicalAABBColliderEntities.getChangedEntities());
		}

		for (AABBCollider collider : AABBColliders.values()) {
			collider.updateEntity();
		}
	}

	private void updateAABBColiderEntities(Set<Entity> changedEntities) {
		for (Entity e : changedEntities) {
			AABBColliders.put(e.getId(), new AABBCollider(e, e.get(Position.class).getPosition(),
					e.get(PhysicalAABBCollider.class).getBounds()));

			checkCollision(e);
		}
	}

	private void checkCollision(Entity entity) {
		AABBCollider collider = AABBColliders.get(entity.getId());

		for (AABBCollider otherCollider : AABBColliders.values()) {
			Vector3f collisionNormal = collider.intersect(otherCollider);
			if (collisionNormal != null) {
				collider.addCollision(otherCollider.getEntity().getId(), collisionNormal);
				otherCollider.addCollision(collider.getEntity().getId(), collisionNormal.negate());
			}
		}
	}

	private void addAABBColiderEntities(Set<Entity> addedEntities) {
		for (Entity e : addedEntities) {
			AABBColliders.put(e.getId(), new AABBCollider(e, e.get(Position.class).getPosition(),
					e.get(PhysicalAABBCollider.class).getBounds()));
		}
	}

	private void removeAABBColiderEntities(Set<Entity> removedEntities) {
		for (Entity e : removedEntities) {
			AABBColliders.remove(e.getId());
		}
	}

	private void updatePhysicalBodyEntities() {
		if (physicalBodyEntities.applyChanges()) {
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
			PhysicalMass mass = e.get(PhysicalMass.class);
			physicalBodies.put(e.getId(), new PhysicalBody(e, mass.getMass(), mass.getBouncing()));
		}
	}

	private void updatePhysicalBodies() {
		for (PhysicalBody body : physicalBodies.values()) {
			Entity entity = body.getEntity();
			PhysicalCollision collision = entity.get(PhysicalCollision.class);

			// Compute gravity force.
			Vector3f resultingForce = GRAVITY_DIR
					.mult(FastMath.clamp((GRAVITY_ACC * unitStep * body.getMass()), 0f, MAX_VELOCITY));

			// Add to existing force.
			resultingForce.addLocal(body.getForce());

			Vector3f finalPos = entity.get(Position.class).getPosition();
			
			// Check for collision.
			if (collision.isCollided()) {
				Vector3f reverseForce = new Vector3f();

				for (Vector3f collisionNormal : collision.getCollisionNormals()) {
					// Vector3f tmp = collisionNormal.normalize();

					// tmp.multLocal(1.0f + (body.getBouncing() * 0.9f));

					reverseForce.addLocal(collisionNormal);
				}
				
				//Revert back to collision point.
				finalPos.addLocal(reverseForce.negateLocal());
				
				Vector3f force = new Vector3f(FastMath.abs(resultingForce.x), FastMath.abs(resultingForce.y),
						FastMath.abs(resultingForce.z));

				Vector3f tmp = reverseForce.normalize().mult(force);
				Vector3f friction = force.mult(0.1f);
				Vector3f bouncing = force.mult(body.getBouncing());

				if (friction.x < 0.01f) {
					bouncing.x = 0;
					friction.x = 0;
				}

				if (friction.y < 0.01f) {
					bouncing.y = 0;
					friction.y = 0;
				}

				if (friction.z < 0.01f) {
					bouncing.z = 0;
					friction.z = 0;
				}

				resultingForce.addLocal(bouncing);
				resultingForce.subtractLocal(friction);
				System.out.println("Collided! Boucing: " + bouncing.lengthSquared() + " - resultingForce: " + resultingForce
						+ " - Friction" + friction.lengthSquared());

				resultingForce.addLocal(tmp);
			}

			System.out.println("resultingForce: " + resultingForce);

			body.setForce(resultingForce);

			entity.set(new Position(finalPos.addLocal(resultingForce)));
		}
	}

	public void stop() {
		this.running = false;
	}
}