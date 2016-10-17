package com.lagecompany.woc.system.physics;

import com.jme3.app.Application;
import com.lagecompany.woc.system.entity.EntityDataState;
import com.simsilica.es.EntityData;
import com.simsilica.lemur.event.BaseAppState;

public class PhysicsState extends BaseAppState {

	private EntityData entityData;
	private PhysicsSystem physicsSystem;
	
	@Override
	protected void initialize(Application app) {
		entityData = getState(EntityDataState.class).getEntityData();
		physicsSystem = new PhysicsSystem(entityData);
		new Thread(physicsSystem).start();
	}

	@Override
	public void update(float tpf) {
	}


	@Override
	protected void cleanup(Application app) {
		physicsSystem.stop();
	}

	@Override
	protected void enable() {

	}

	@Override
	protected void disable() {

	}

}
