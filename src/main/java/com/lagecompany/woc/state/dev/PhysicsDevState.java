package com.lagecompany.woc.state.dev;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.lagecompany.woc.manager.CameraMan;
import com.lagecompany.woc.system.debug.component.Model;
import com.lagecompany.woc.system.debug.component.Position;
import com.lagecompany.woc.system.entity.EntityDataState;
import com.lagecompany.woc.system.physics.component.PhysicalAABBCollider;
import com.lagecompany.woc.system.physics.component.PhysicalCollision;
import com.lagecompany.woc.system.physics.component.PhysicalMass;
import com.simsilica.es.EntityData;
import com.simsilica.lemur.event.BaseAppState;

public class PhysicsDevState extends BaseAppState {

	private EntityData entityData;

	@Override
	public void initialize(Application app) {
		entityData = getState(EntityDataState.class).getEntityData();

		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(-8.0f, 6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.1f, .9f),
				new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());

//		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(-4.0f, 6.0f, -15.0f)),
//				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.08f, .6f),
//				new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());
//
//		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(0.0f, 6.0f, -15.0f)),
//				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.06f, .4f),
//				new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());
//
//		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(4.0f, 6.0f, -15.0f)),
//				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.04f, .2f),
//				new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());
//
//		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(8.0f, 6.0f, -15.0f)),
//				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.02f, .0f),
//				new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());

		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(-8.0f, -6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
				new PhysicalCollision());

//		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(-4.0f, -6.0f, -15.0f)),
//				new Model(1.0f, ColorRGBA.Blue), new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
//				new PhysicalCollision());
//
//		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(0.0f, -6.0f, -15.0f)),
//				new Model(1.0f, ColorRGBA.Blue), new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
//				new PhysicalCollision());
//
//		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(4.0f, -6.0f, -15.0f)),
//				new Model(1.0f, ColorRGBA.Blue), new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
//				new PhysicalCollision());
//
//		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(8.0f, -6.0f, -15.0f)),
//				new Model(1.0f, ColorRGBA.Blue), new PhysicalAABBCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
//				new PhysicalCollision());

		SimpleApplication simpleApp = (SimpleApplication) app;
		new CameraMan(simpleApp.getFlyByCamera(), simpleApp.getCamera(), simpleApp.getInputManager());
	}

	@Override
	public void update(float tpf) {
		super.update(tpf);
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
