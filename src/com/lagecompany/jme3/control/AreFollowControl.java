package com.lagecompany.jme3.control;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.lagecompany.jme3.state.DebugAppState;
import com.lagecompany.storage.Are;
import com.lagecompany.storage.AreMessage;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;

public class AreFollowControl extends AbstractControl {

    private Vec3 lastLocation;
    private Are are;
    private DebugAppState debugState;

    public AreFollowControl() {
	are = Are.getInstance();
    }

    @Override
    public void setSpatial(Spatial spatial) {
	super.setSpatial(spatial);
	this.lastLocation = toArePosition(spatial.getLocalTranslation());
    }

    @Override
    protected void controlUpdate(float tpf) {
	if (are.isMoving()) {
	    return;
	}
	Vec3 currentLocation = toArePosition(getSpatial().getLocalTranslation());

	if (currentLocation.equals(lastLocation)) {
	    return;
	}
	lastLocation = currentLocation;
	Vec3 moved = lastLocation.subtractNew(are.getPosition());
	moved.setY(0);

	if (moved.equals(Vec3.ZERO)) {
	    return;
	}

	System.out.println("Moving Are chunk to: " + moved + " - " + are.getPosition());
	are.setMoving(true);
	are.postMessage(new AreMessage(AreMessage.AreMessageType.ARE_MOVE, moved));
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    private Vec3 toArePosition(Vector3f position) {
	return new Vec3((int) (position.getX() / Chunk.WIDTH),
		(int) (position.getY() / Chunk.HEIGHT),
		(int) (position.getZ() / Chunk.LENGTH));
    }

    public Vec3 getPosition() {
	return lastLocation;
    }
}
