package com.lagecompany.woc.entity.system;

import java.util.Set;

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.lagecompany.woc.entity.EntityDataState;
import com.lagecompany.woc.entity.component.Model;
import com.lagecompany.woc.entity.component.PhysicalCollision;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.lemur.event.BaseAppState;

public class PhysicalCollisionState extends BaseAppState {

	private EntityData entityData;
	private EntitySet entities;

	@Override
	protected void initialize(Application app) {
		entityData = getState(EntityDataState.class).getEntityData();
		entities = entityData.getEntities(Model.class, PhysicalCollision.class);
	}

	@Override
	public void update(float tpf) {
		if (entities.applyChanges()) {
			updateEntities(entities.getChangedEntities());
		}
	}

	private void updateEntities(Set<Entity> changedEntities) {
		for (Entity entity : changedEntities) {

			PhysicalCollision collision = entity.get(PhysicalCollision.class);

			if (collision.isCollided()) {
				entity.set(new Model(entity.get(Model.class).getSize(), ColorRGBA.Red));
			} else {
				entity.set(new Model(entity.get(Model.class).getSize(), ColorRGBA.Blue));
			}

		}
	}

	@Override
	protected void cleanup(Application app) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void enable() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void disable() {
		// TODO Auto-generated method stub

	}

}
