package com.lagecompany.woc.entity.system;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jme3.app.Application;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.lagecompany.woc.entity.EntityDataState;
import com.lagecompany.woc.entity.component.PhysicalCollision;
import com.lagecompany.woc.entity.component.PhysicalMass;
import com.lagecompany.woc.entity.component.Position;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.lemur.event.BaseAppState;

public class PhysicalMassState extends BaseAppState {

	private static final float GRAVITY_ACC = 9.780f;
	private static final float MAX_VELOCITY = 1000f; // Limit velocity at 1k units per second
	private static final Vector3f GRAVITY_DIR = new Vector3f(0.0f, -1.0f, 0.0f);

	private EntityData entityData;
	private EntitySet entities;

	private final Map<EntityId, PhysicalBodyData> bodies;

	private float unitStep;

	private class PhysicalBodyData {
		final Entity entity;
		float velocity;

		public PhysicalBodyData(Entity entity) {
			this.entity = entity;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(velocity);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PhysicalBodyData other = (PhysicalBodyData) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (Double.doubleToLongBits(velocity) != Double.doubleToLongBits(other.velocity))
				return false;
			return true;
		}

		private PhysicalMassState getOuterType() {
			return PhysicalMassState.this;
		}
	}

	public PhysicalMassState() {
		bodies = new HashMap<>();
	}

	@Override
	protected void initialize(Application app) {
		entityData = getState(EntityDataState.class).getEntityData();
		entities = entityData.getEntities(PhysicalMass.class, PhysicalCollision.class, Position.class);
	}

	@Override
	public void update(float tpf) {
		if (entities.applyChanges()) {
			unitStep = tpf / 1000.0f;
			removeEntities(entities.getRemovedEntities());
			addEntities(entities.getAddedEntities());
		}

		updateBodies();
	}

	private void removeEntities(Set<Entity> removedEntities) {
		for (Entity e : removedEntities) {
			bodies.remove(e.getId());
		}
	}

	private void addEntities(Set<Entity> addedEntities) {
		for (Entity e : addedEntities) {
			bodies.put(e.getId(), new PhysicalBodyData(e));
		}
	}

	private void updateBodies() {
		for (PhysicalBodyData data : bodies.values()) {
			PhysicalCollision collision = data.entity.get(PhysicalCollision.class);

			if (collision.isCollided()) {
				data.velocity = 0;
			} else {
				PhysicalMass body = data.entity.get(PhysicalMass.class);
				data.velocity = FastMath.clamp(data.velocity + (GRAVITY_ACC * unitStep * body.getMass()), 0f,
						MAX_VELOCITY);
			}

			Vector3f move = GRAVITY_DIR.mult(data.velocity);

			if (move.length() >= 0.0f) {
				Position pos = data.entity.get(Position.class);
				data.entity.set(new Position(pos.getPosition().addLocal(move)));
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
