package com.lagecompany.jme3.control;

import com.jme3.bullet.control.BetterCharacterControl;
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
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_Left;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_Right;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_Up;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_Down;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_Backward;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_Forward;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_Lower;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_Rise;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_StrafeLeft;
import static com.lagecompany.jme3.input.CameraMan.PLAYER_NODE_StrafeRight;
import com.lagecompany.jme3.state.DebugAppState;

public class CameraFollowControl extends AbstractControl implements ActionListener, AnalogListener {
    
    private Camera cam;
    private Vector3f camDir;
    private Vector3f camLeft;
    private Vector3f walkDir;
    private Node playerNode;
    private BetterCharacterControl playerControl;
    private InputManager inputManager;
    private int leftRight;
    private int backFront;
    private boolean jump;
    
    public CameraFollowControl(Camera cam, InputManager inputManager) {
	this.cam = cam;
	this.camDir = new Vector3f();
	this.camLeft = new Vector3f();
	this.walkDir = new Vector3f();
	this.inputManager = inputManager;
    }
    
    @Override
    public void setSpatial(Spatial spatial) {
	super.setSpatial(spatial);
	this.playerNode = (Node) spatial;
	this.playerControl = playerNode.getControl(BetterCharacterControl.class);
	playerControl.setPhysicsDamping(0);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
	super.setEnabled(enabled);
	
	if (enabled) {
	    cam.lookAtDirection(playerControl.getViewDirection(), Vector3f.UNIT_Y);
	    inputManager.setCursorVisible(true);
	}
    }
    
    @Override
    protected void controlUpdate(float tpf) {
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
	
	playerControl.setWalkDirection(walkDir.multLocal(2f));
	playerControl.setViewDirection(camDir);
	cam.setLocation(spatial.getLocalTranslation().add(0f, 1.8f, 0f));
    }
    
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
    
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
     * Got from FlyByCamera.
     *
     * @param value Roration force.
     * @param axis Axis of rotation.
     */
    private void rotateCamera(float value, Vector3f axis) {
	Matrix3f mat = new Matrix3f();
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
}
