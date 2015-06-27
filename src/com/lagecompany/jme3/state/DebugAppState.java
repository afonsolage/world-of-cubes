package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.control.BetterCharacterControl;
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
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;
import com.lagecompany.jme3.control.CameraFollowControl;
import com.lagecompany.jme3.control.AreFollowControl;
import com.lagecompany.jme3.input.CameraController;
import com.lagecompany.nifty.gui.DebugScreen;

public class DebugAppState extends AbstractAppState implements ActionListener, AnalogListener {

    private SimpleApplication app;
    private Node rootNode;
    private Node playerNode;
    private AssetManager assetManager;
    private InputManager inputManager;
    private FlyByCamera flyCam;
    private DebugScreen debugScreen;
    private BetterCharacterControl characterController;
    private CameraFollowControl followControl;
    private AreFollowControl translateControl;
    public static boolean wireframe;
    public static boolean backfaceCulled;
    public static boolean axisArrowsEnabled;
    public static boolean playerFollow;

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	super.initialize(stateManager, application);
	this.app = (SimpleApplication) application;
	this.rootNode = app.getRootNode();
	this.inputManager = app.getInputManager();
	this.assetManager = app.getAssetManager();
	this.flyCam = app.getFlyByCamera();

	playerNode = stateManager.getState(WorldAppState.class).getPlayerNode();
	characterController = playerNode.getControl(BetterCharacterControl.class);
	followControl = playerNode.getControl(CameraFollowControl.class);
	translateControl = playerNode.getControl(AreFollowControl.class);

	wireframe = false;
	backfaceCulled = false;
	axisArrowsEnabled = false;
	playerFollow = false;

	showPlayerNode();

	this.bindKeys();
	this.initGUI();
    }

    @Override
    public void update(float tpf) {
	updateGUI();
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
	inputManager.addMapping("TOGGLE_CAM_VIEW", new KeyTrigger(KeyInput.KEY_TAB));

	inputManager.addMapping("MOVESPEED_UP", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
	inputManager.addMapping("MOVESPEED_DOWN", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

	inputManager.addListener(this, "TOGGLE_WIREFRAME", "TOGGLE_CURSOR", "TOGGLE_CULLING", "TOGGLE_AXISARROWS",
		"MOVESPEED_UP", "MOVESPEED_DOWN", "UPDATE_GUI", "CUSTOM_FUNCTION", "MOVE_FORWARD", "MOVE_BACKWARD",
		"TURN_LEFT", "TURN_RIGHT", "TOGGLE_CAM_VIEW");

	inputManager.addListener(this, "PLAYER_MOVE_FORWARD", "PLAYER_MOVE_BACKWARD", "PLAYER_STRAFE_LEFT",
		"PLAYER_STRAFE_RIGHT", "PLAYER_JUMP");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
	if ("TOGGLE_WIREFRAME".equals(name) && !isPressed) {
	    wireframe = !wireframe;
	    toggleWireframe(rootNode, wireframe);
	} else if ("TOGGLE_CURSOR".equals(name) && !isPressed) {
	    toggleCursor();
	} else if ("TOGGLE_CULLING".equals(name) && !isPressed) {
	    backfaceCulled = !backfaceCulled;
	    toggleBackfaceCulling(rootNode);
	} else if ("TOGGLE_AXISARROWS".equals(name) && !isPressed) {
	    toggleAxisArrows();
	} else if ("UPDATE_GUI".equals(name) && !isPressed) {
	    // updateGUI();
	} else if ("TOGGLE_CAM_VIEW".equals(name) && !isPressed) {
	    toggleCamView();
	} else if ("CUSTOM_FUNCTION".equals(name) && !isPressed) {
	    //customFunction();
	    movePlayerForward();
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
	    case "PLAYER_MOVE_FORWARD": {
		followControl.moveForward();
		break;
	    }
	    case "PLAYER_MOVE_BACKWARD": {
		followControl.moveBackward();
		break;
	    }
	    case "PLAYER_JUMP": {
		followControl.jump();
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

    private void toggleCamView() {
	playerFollow = !playerFollow;

	characterController.setEnabled(playerFollow);
	followControl.setEnabled(playerFollow);
	toggleControls(playerFollow);
    }

    private void initGUI() {
	debugScreen = new DebugScreen();
	debugScreen.create();
	debugScreen.display();
    }

    private void updateGUI() {
	debugScreen.setPlayerPosition(translateControl.getPosition().toString());
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
	geom.move(0, 1f, 0);
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

    private void toggleControls(boolean playerFollow) {
	if (playerFollow) {
	    inputManager.addMapping("PLAYER_MOVE_FORWARD", new KeyTrigger(KeyInput.KEY_W));
	    inputManager.addMapping("PLAYER_MOVE_BACKWARD", new KeyTrigger(KeyInput.KEY_S));
	    inputManager.addMapping("PLAYER_STRAFE_LEFT", new KeyTrigger(KeyInput.KEY_A));
	    inputManager.addMapping("PLAYER_STRAFE_RIGHT", new KeyTrigger(KeyInput.KEY_D));
	    inputManager.addMapping("PLAYER_JUMP", new KeyTrigger(KeyInput.KEY_SPACE));
	} else {
	    inputManager.deleteTrigger("PLAYER_MOVE_FORWARD", new KeyTrigger(KeyInput.KEY_W));
	    inputManager.deleteTrigger("PLAYER_MOVE_BACKWARD", new KeyTrigger(KeyInput.KEY_S));
	    inputManager.deleteTrigger("PLAYER_STRAFE_LEFT", new KeyTrigger(KeyInput.KEY_A));
	    inputManager.deleteTrigger("PLAYER_STRAFE_RIGHT", new KeyTrigger(KeyInput.KEY_D));
	    inputManager.deleteTrigger("PLAYER_JUMP", new KeyTrigger(KeyInput.KEY_SPACE));
	}
    }

    private void movePlayerForward() {
    }
}
