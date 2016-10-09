package com.lagecompany.woc.control;

import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_Backward;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_Down;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_Forward;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_Left;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_Lower;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_Right;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_Rise;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_StrafeLeft;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_StrafeRight;
import static com.lagecompany.woc.manager.CameraMan.PLAYER_NODE_Up;

import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * A control that makes the camera follow the player like a first person camera.
 *
 * @author Afonso Lage
 */
public class CameraFollowControl extends AbstractControl implements ActionListener, AnalogListener {

    public static float PLAYER_HEAD = 3.8f;
    private Camera cam;
    private Vector3f camDir;
    private Vector3f camLeft;
    private Vector3f walkDir;
    private Node playerNode;
    private PlayerControl playerControl;
    private InputManager inputManager;
    private int leftRight;
    private int backFront;
    private boolean jump;

    /**
     * Creates a new instancia of this control. Expects a Camera and a InputManager
     *
     * @param cam The camera to be controled.
     * @param inputManager The input manager to register the controls.
     */
    public CameraFollowControl(Camera cam, InputManager inputManager) {
	this.cam = cam;
	this.camDir = new Vector3f();
	this.camLeft = new Vector3f();
	this.walkDir = new Vector3f();
	this.inputManager = inputManager;
    }

    /**
     * This method is called by JME3 when this Control is attached to a Spatial.
     *
     * @param spatial
     */
    @Override
    public void setSpatial(Spatial spatial) {
	super.setSpatial(spatial);
	this.playerNode = (Node) spatial;
	this.playerControl = playerNode.getControl(PlayerControl.class);
    }

    /**
     * Enable or disable the current control. By disabling it is removed from main game loop.
     *
     * @param enabled
     */
    @Override
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);

	if (enabled) {
	    cam.lookAtDirection(playerControl.getViewDirection(), Vector3f.UNIT_Y);
	    inputManager.setCursorVisible(true);
	}
    }

    /**
     * Main update method, called each frame.
     *
     * @param tpf Time per fame in seconds.
     */
    @Override
    protected void controlUpdate(float tpf) {
	//Get the looking direction of camera, to move our player on this direction.
	camDir.set(cam.getDirection()).multLocal(1f, 0f, 1f);
	camLeft.set(cam.getLeft()).multLocal(1f);


	walkDir.set(0, 0, 0);
	if (leftRight > 0) {
	    walkDir.addLocal(camLeft.negate());
	} else if (leftRight < 0) {
	    walkDir.addLocal(camLeft);
	}

	if (backFront > 0) {
	    walkDir.addLocal(camDir.negate());
	} else if (backFront < 0) {
	    walkDir.addLocal(camDir);
	}

	if (jump && playerControl.isOnGround()) {
	    playerControl.jump();
	    jump = false;
	}

	//TODO: Configure the walk speed and add run speed.
	if (walkDir.equals(Vector3f.ZERO)) {
	    playerControl.setWalkDirection(walkDir.normalizeLocal(), 0f);
	} else {
	    playerControl.setWalkDirection(walkDir.normalizeLocal(), 4f);
	}
	playerControl.setViewDirection(camDir);

	//Set the camera location the same as player, plus 1.5f to match the head of player.
	cam.setLocation(spatial.getLocalTranslation().add(0f, PLAYER_HEAD, 0f));
    }

    /**
     * Action listener of registered events. An Action Listener is called when a binary input is received (like a key
     * press or a mouse click).
     *
     * @param name Name of key binding
     * @param isPressed Is key pressed or released?
     * @param tpf Time per frame in seconds
     */
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
	switch (name) {
	    case PLAYER_NODE_StrafeLeft: {
		leftRight = (isPressed) ? -1 : 0;
		break;
	    }
	    case PLAYER_NODE_StrafeRight: {
		leftRight = (isPressed) ? 1 : 0;
		break;
	    }
	    case PLAYER_NODE_Forward: {
		backFront = (isPressed) ? -1 : 0;
		break;
	    }
	    case PLAYER_NODE_Backward: {
		backFront = (isPressed) ? 1 : 0;
		break;
	    }
	    case PLAYER_NODE_Rise: {
		jump = isPressed;
		break;
	    }
	    case PLAYER_NODE_Lower: {
	    }
	}
    }

    /**
     * Analog listener of registered events. An Analog Listener is called when a analog input is received (like a mouse
     * moviment or a key is pressed).
     *
     * @param name Name of key binding
     * @param value Input value intensity
     * @param tpf Time per frame in seconds
     */
    @Override
    public void onAnalog(String name, float value, float tpf) {
	switch (name) {
	    case PLAYER_NODE_Left: {
		rotateCamera(value, Vector3f.UNIT_Y);
		break;
	    }
	    case PLAYER_NODE_Right: {
		rotateCamera(-value, Vector3f.UNIT_Y);
		break;
	    }
	    case PLAYER_NODE_Up: {
		rotateCamera(-value, cam.getLeft());
		break;
	    }
	    case PLAYER_NODE_Down: {
		rotateCamera(value, cam.getLeft());
		break;
	    }
	}
    }

    /**
     * Got from FlyByCamera. Rorate the camera by a value along an given Axis.
     *
     * @param value Roration force.
     * @param axis Axis of rotation.
     */
    private void rotateCamera(float value, Vector3f axis) {
	Matrix3f mat = new Matrix3f();

	//TODO: Add camera move speed configuration.
	mat.fromAngleNormalAxis(value * 2f, axis);

	Vector3f up = cam.getUp();
	Vector3f left = cam.getLeft();
	Vector3f dir = cam.getDirection();

	mat.mult(up, up);
	mat.mult(left, left);
	mat.mult(dir, dir);

	Quaternion q = new Quaternion();
	q.fromAxes(left, up, dir);
	q.normalizeLocal();
	cam.setAxes(q);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
    
    public Camera getCamera() {
	return cam;
    }
}
