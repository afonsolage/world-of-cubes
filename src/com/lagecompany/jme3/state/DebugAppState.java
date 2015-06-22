package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;
import com.lagecompany.jme3.input.CameraController;
import com.lagecompany.nifty.gui.DebugWindow;

public class DebugAppState extends AbstractAppState implements ActionListener, AnalogListener {

    private SimpleApplication app;
    private Node rootNode;
    private Node playerNode;
    private AssetManager assetManager;
    private InputManager inputManager;
    private AudioRenderer audioRenderer;
    private ViewPort guiViewPort;
    private FlyByCamera flyCam;
    private DebugWindow debugWindow;
    private AppStateManager stateManager;
    public static boolean wireframe;
    public static boolean backfaceCulled;
    public static boolean axisArrowsEnabled;

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	super.initialize(stateManager, application);
	this.app = (SimpleApplication) application;
	this.stateManager = stateManager;
	this.rootNode = app.getRootNode();
	this.inputManager = app.getInputManager();
	this.assetManager = app.getAssetManager();
	this.guiViewPort = app.getGuiViewPort();
	this.audioRenderer = app.getAudioRenderer();
	this.flyCam = app.getFlyByCamera();

	playerNode = stateManager.getState(WorldAppState.class).getPlayerNode();

	wireframe = false;
	backfaceCulled = false;
	axisArrowsEnabled = false;

	showPlayerNode();

	this.bindKeys();
	//this.initGUI();
    }

    @Override
    public void update(float tpf) {
    }

    private void bindKeys() {
	inputManager.addMapping("TOGGLE_WIREFRAME", new KeyTrigger(KeyInput.KEY_F1));
	inputManager.addMapping("TOGGLE_CURSOR", new KeyTrigger(KeyInput.KEY_F2));
	inputManager.addMapping("TOGGLE_CULLING", new KeyTrigger(KeyInput.KEY_F3));
	inputManager.addMapping("TOGGLE_AXISARROWS", new KeyTrigger(KeyInput.KEY_F4));
	inputManager.addMapping("UPDATE_GUI", new KeyTrigger(KeyInput.KEY_F6));
	inputManager.addMapping("CUSTOM_FUNCTION", new KeyTrigger(KeyInput.KEY_F7));
	inputManager.addMapping("MOVE_FORWARD", new KeyTrigger(KeyInput.KEY_UP));
	inputManager.addMapping("MOVE_BACKWARD", new KeyTrigger(KeyInput.KEY_DOWN));
	inputManager.addMapping("TURN_LEFT", new KeyTrigger(KeyInput.KEY_LEFT));
	inputManager.addMapping("TURN_RIGHT", new KeyTrigger(KeyInput.KEY_RIGHT));

	inputManager.addMapping("MOVESPEED_UP", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
	inputManager.addMapping("MOVESPEED_DOWN", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

	inputManager.addListener(this, "TOGGLE_WIREFRAME", "TOGGLE_CURSOR", "TOGGLE_CULLING", "TOGGLE_AXISARROWS",
		"MOVESPEED_UP", "MOVESPEED_DOWN", "UPDATE_GUI", "CUSTOM_FUNCTION", "MOVE_FORWARD", "MOVE_BACKWARD",
		"TURN_LEFT", "TURN_RIGHT");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
	if ("TOGGLE_WIREFRAME".equals(name) && !isPressed) {
	    wireframe = !wireframe;
	    toggleWireframe(rootNode, wireframe);
	}

	if ("TOGGLE_CURSOR".equals(name) && !isPressed) {
	    toggleCursor();
	}

	if ("TOGGLE_CULLING".equals(name) && !isPressed) {
	    backfaceCulled = !backfaceCulled;
	    toggleBackfaceCulling(rootNode);
	}

	if ("TOGGLE_AXISARROWS".equals(name) && !isPressed) {
	    toggleAxisArrows();
	}

	if ("UPDATE_GUI".equals(name) && !isPressed) {
	    // updateGUI();
	}

	if ("CUSTOM_FUNCTION".equals(name) && !isPressed) {
	    customFunction();
	}
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
	switch (name) {
	    case "MOVESPEED_UP": {
		speedUpMove(value);
		break;
	    }
	    case "MOVESPEED_DOWN": {
		speedDownMove(value);
		break;
	    }
	    case "MOVE_FORWARD": {
		moveForward(value);
		break;
	    }
	    case "MOVE_BACKWARD": {
		moveBackward(value);
		break;
	    }
	    case "TURN_LEFT": {
		turnLeft(value);
		break;
	    }
	    case "TURN_RIGHT": {
		turnRight(value);
		break;
	    }
	}

    }

    private void toggleWireframe(Node node, boolean enabled) {
	for (Spatial spatial : node.getChildren()) {
	    if (spatial instanceof Geometry) {
		Geometry geometry = (Geometry) spatial;

		geometry.getMaterial().getAdditionalRenderState().setWireframe(enabled);
	    } else if (spatial instanceof Node) {
		toggleWireframe((Node) spatial, enabled);
	    }
	}
    }

    private void toggleCursor() {
	boolean isEnabled = inputManager.isCursorVisible();

	if (!isEnabled) {
	    CameraController.clearKeys(inputManager);
	} else {
	    CameraController.setupKeys(inputManager);
	}

	inputManager.setCursorVisible(!isEnabled);
    }

    private void toggleBackfaceCulling(Node node) {
	for (Spatial spatial : node.getChildren()) {
	    if (spatial instanceof Geometry) {
		Geometry geometry = (Geometry) spatial;

		if (backfaceCulled) {
		    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		} else {
		    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
		}

	    } else if (spatial instanceof Node) {
		toggleBackfaceCulling((Node) spatial);
	    }
	}
    }

    private void toggleAxisArrows() {
	if (axisArrowsEnabled) {
	    for (Spatial spatial : rootNode.getChildren()) {
		if (spatial instanceof Geometry && spatial.getName().equals("Axis Arrow")) {
		    rootNode.detachChild(spatial);
		}
	    }
	} else {
	    Arrow arrow = new Arrow(Vector3f.UNIT_X);
	    arrow.setLineWidth(4);
	    attachArrow(arrow, ColorRGBA.Blue);

	    arrow = new Arrow(Vector3f.UNIT_Y);
	    arrow.setLineWidth(4);
	    attachArrow(arrow, ColorRGBA.Green);

	    arrow = new Arrow(Vector3f.UNIT_Z);
	    arrow.setLineWidth(4);
	    attachArrow(arrow, ColorRGBA.Red);

	    attachGrid();
	}

	axisArrowsEnabled = !axisArrowsEnabled;
    }

    private void attachGrid() {
	Geometry g = new Geometry("Axis Arrow", new Grid(200, 200, 1f));
	Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
	mat.setColor("Color", ColorRGBA.DarkGray);
	g.setMaterial(mat);
	g.setLocalTranslation(new Vector3f(-100, 0, -100));
	rootNode.attachChild(g);
    }

    private void attachArrow(Arrow arrow, ColorRGBA color) {
	Geometry g = new Geometry("Axis Arrow", arrow);
	Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
	mat.setColor("Color", color);
	g.setMaterial(mat);
	g.setLocalTranslation(Vector3f.ZERO);
	rootNode.attachChild(g);
    }

    private void speedUpMove(float value) {
	float speed = flyCam.getMoveSpeed() + value;
	flyCam.setMoveSpeed(speed);
    }

    private void speedDownMove(float value) {
	float speed = flyCam.getMoveSpeed() - value;
	flyCam.setMoveSpeed((speed < 2f) ? 2f : speed); //Minimum walk speed.
    }

    private void initGUI() {
	NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
	guiViewPort.addProcessor(niftyDisplay);
	debugWindow = new DebugWindow(niftyDisplay.getNifty());
	debugWindow.create();

	debugWindow.display();
    }

    private void updateGUI() {
	debugWindow.delete();
	debugWindow.create();
	debugWindow.display();
    }

    private void customFunction() {
	//
    }

    private void showPlayerNode() {
	Box b = new Box(1, 1, 1);
	Geometry geom = new Geometry("PlayerNode Box", b);
	Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
	mat.setColor("Color", ColorRGBA.DarkGray);
	geom.setMaterial(mat);
	playerNode.attachChild(geom);
    }

    private void moveForward(float value) {
	Vector3f forward = playerNode.getLocalRotation().mult(new Vector3f(0, 0, value * 2));
	playerNode.move(forward);
    }

    private void moveBackward(float value) {
	Vector3f forward = playerNode.getLocalRotation().mult(new Vector3f(0, 0, value * 2).negateLocal());
	playerNode.move(forward);
    }

    private void turnLeft(float value) {
	playerNode.rotate(0, value, 0);
    }

    private void turnRight(float value) {
	playerNode.rotate(0, -value, 0);
    }
}