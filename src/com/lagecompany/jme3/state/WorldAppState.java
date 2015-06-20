package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.lagecompany.jme3.input.CameraController;

public class WorldAppState extends AbstractAppState {

    private SimpleApplication app;
    private Node rootNode;
    private InputManager inputManager;
    private AssetManager assetManager;
    private FlyByCamera flyCam;
    private Camera cam;

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	super.initialize(stateManager, application);
	this.app = (SimpleApplication) application;
	this.rootNode = app.getRootNode();
	this.inputManager = app.getInputManager();
	this.assetManager = app.getAssetManager();
	this.flyCam = app.getFlyByCamera();
	this.cam = app.getCamera();

	CameraController.setup(flyCam, inputManager);
	inputManager.setCursorVisible(true);
	cam.setLocation(new Vector3f(180f, 100f, -50f));
	cam.lookAtDirection(new Vector3f(-1f, -1f, 1f), Vector3f.UNIT_Y);

	rootNode.setCullHint(Spatial.CullHint.Never);
	AmbientLight ambient = new AmbientLight();
	ambient.setColor(ColorRGBA.White.mult(0.25f));
	rootNode.addLight(ambient);

	DirectionalLight sun = new DirectionalLight();
	sun.setDirection(new Vector3f(-0.25f, -0.75f, -0.25f).normalizeLocal());

	sun.setColor(ColorRGBA.White);
	rootNode.addLight(sun);
    }

    @Override
    public void update(float tpf) {
	super.update(tpf); //To change body of generated methods, choose Tools | Templates.
    }
}
