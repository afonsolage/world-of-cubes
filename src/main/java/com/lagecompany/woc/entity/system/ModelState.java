package com.lagecompany.woc.entity.system;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.lagecompany.woc.entity.EntityDataState;
import com.lagecompany.woc.entity.component.Model;
import com.lagecompany.woc.entity.component.Position;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.lemur.event.BaseAppState;

public class ModelState extends BaseAppState {

	private EntityData entityData;
	private EntitySet entities;

	private AssetManager assetManager;
	private Node rootNode;

	private final Map<EntityId, Geometry> models;

	public ModelState() {
		models = new HashMap<>();
	}

	@Override
	protected void initialize(Application app) {
		entityData = getState(EntityDataState.class).getEntityData();
		entities = entityData.getEntities(Position.class, Model.class);

		SimpleApplication simpleApp = (SimpleApplication) app;
		assetManager = simpleApp.getAssetManager();
		rootNode = simpleApp.getRootNode();
	}

	@Override
	public void update(float tpf) {
		if (entities.applyChanges()) {
			destroyModels(entities.getRemovedEntities());
			addModels(entities.getAddedEntities());
			updateModels(entities.getChangedEntities());
		}
	}

	private void updateModels(Set<Entity> changedEntities) {
		for (Entity e : changedEntities) {
			updateCube(e);
		}
	}

	private void addModels(Set<Entity> addedEntities) {
		for (Entity e : addedEntities) {
			models.put(e.getId(), createCubeModel(e));
		}
	}

	private void destroyModels(Set<Entity> removedEntities) {
		for (Entity e : removedEntities) {
			Geometry geo = models.remove(e.getId());
			geo.removeFromParent();
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

	private Geometry createCubeModel(Entity entity) {
		Model model = entity.get(Model.class);
		Position pos = entity.get(Position.class);

		Box cube1 = new Box(model.getSize(), model.getSize(), model.getSize());
		Geometry geo = new Geometry("Box at " + pos, cube1);
		geo.setLocalTranslation(pos.getPosition());
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", model.getColor());
		geo.setMaterial(mat);
		rootNode.attachChild(geo);
		return geo;
	}

	private void updateCube(Entity entity) {
		Geometry geo = models.get(entity.getId());

		Position pos = entity.get(Position.class);
		Model model = entity.get(Model.class);

		geo.setLocalTranslation(pos.getPosition());

		Box box = (Box) geo.getMesh();

		if (box.xExtent != model.getSize()) {
			geo.setMesh(new Box(model.getSize(), model.getSize(), model.getSize()));
		}

		MatParam param = geo.getMaterial().getParam("Color");

		if (!((ColorRGBA) param.getValue()).equals(model.getColor())) {
			geo.getMaterial().setColor("Color", model.getColor());
		}
	}
}
