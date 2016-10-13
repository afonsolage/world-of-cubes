package com.lagecompany.woc.entity;

import com.jme3.app.Application;
import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.lemur.event.BaseAppState;

public class EntityDataState extends BaseAppState {

	private EntityData entityData;

	public EntityDataState() {
		this(new DefaultEntityData());
	}

	public EntityDataState(EntityData entityData) {
		this.entityData = entityData;
	}

	@Override
	protected void initialize(Application app) {
	}

	public EntityData getEntityData() {
		return entityData;
	}

	@Override
	protected void cleanup(Application app) {
		entityData.close();
		entityData = null;
	}

	@Override
	protected void enable() {
	}

	@Override
	protected void disable() {
	}
}
