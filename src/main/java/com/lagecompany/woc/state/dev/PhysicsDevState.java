package com.lagecompany.woc.state.dev;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.lagecompany.woc.entity.EntityDataState;
import com.lagecompany.woc.entity.component.Model;
import com.lagecompany.woc.entity.component.PhysicalBoxCollider;
import com.lagecompany.woc.entity.component.PhysicalCollision;
import com.lagecompany.woc.entity.component.PhysicalMass;
import com.lagecompany.woc.entity.component.Position;
import com.lagecompany.woc.manager.CameraMan;
import com.simsilica.es.EntityData;
import com.simsilica.lemur.event.BaseAppState;

public class PhysicsDevState extends BaseAppState {

	private EntityData entityData;

	@Override
	public void initialize(Application app) {
		entityData = getState(EntityDataState.class).getEntityData();

		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(-8.0f, 6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.1f),
				new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());

		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(-4.0f, 6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.08f),
				new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());
		
		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(0.0f, 6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.06f),
				new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());
		
		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(4.0f, 6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.04f),
				new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());
		
		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(8.0f, 6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalMass(.02f),
				new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)), new PhysicalCollision());
		
		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(-8.0f, -6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
				new PhysicalCollision());

		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(-4.0f, -6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
				new PhysicalCollision());
		
		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(0.0f, -6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
				new PhysicalCollision());
		
		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(4.0f, -6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
				new PhysicalCollision());
		
		entityData.setComponents(entityData.createEntity(), new Position(new Vector3f(8.0f, -6.0f, -15.0f)),
				new Model(1.0f, ColorRGBA.Blue), new PhysicalBoxCollider(new Vector3f(1.0f, 1.0f, 1.0f)),
				new PhysicalCollision());
		
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
