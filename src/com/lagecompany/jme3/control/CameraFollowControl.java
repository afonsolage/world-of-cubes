package com.lagecompany.jme3.control;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public class CameraFollowControl extends AbstractControl {

    private Camera cam;
    private Vector3f camDir;
    private Vector3f camLeft;
    private Vector3f walkDir;
    private Node playerNode;
    private BetterCharacterControl playerControl;
    private byte leftRight;
    private byte backFront;
    private boolean jump;

    public CameraFollowControl(Camera cam) {
	this.cam = cam;
	this.camDir = new Vector3f();
	this.camLeft = new Vector3f();
	this.walkDir = new Vector3f();
    }

    @Override
    public void setSpatial(Spatial spatial) {
	super.setSpatial(spatial);
	this.playerNode = (Node) spatial;
	this.playerControl = playerNode.getControl(BetterCharacterControl.class);
    }

    @Override
    protected void controlUpdate(float tpf) {
//	camDir.set(cam.getDirection().clone());
//	camLeft.set(cam.getLeft().clone());
//
//	camDir.y = 0;
//	camDir.normalizeLocal();
//	camLeft.y = 0;
//	camLeft.normalizeLocal();
	camDir.set(0, 0, 1);
	walkDir.set(0, 0, 0);

//	if (leftRight > 0) {
//	    walkDir.addLocal(camLeft);
//	} else if (leftRight < 0) {
//	    walkDir.addLocal(camLeft.negate());
//	}

	if (backFront > 0) {
	    walkDir.addLocal(camDir.negate());
	} else if (backFront < 0) {
	    walkDir.addLocal(camDir);
	}

	walkDir.multLocal(200f * tpf);
	playerControl.setWalkDirection(walkDir);
	if (jump && playerControl.isOnGround()) {
	    playerControl.jump();
	}
	backFront = 0;
	jump = false;
//	cam.lookAtDirection(playerNode.getLocalRotation().mult(Vector3f.UNIT_Z), Vector3f.UNIT_Y);
//	cam.setLocation(playerNode.getLocalTransform().getTranslation());
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public void strafeLeft() {
	this.leftRight = -1;
    }

    public void strafeRight() {
	this.leftRight = 1;
    }

    public void moveBackward() {
	this.backFront = -1;
    }

    public void moveForward() {
	this.backFront = 1;
    }

    public void jump() {
	this.jump = true;
    }
}
