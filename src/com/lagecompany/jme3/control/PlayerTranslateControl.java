package com.lagecompany.jme3.control;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.lagecompany.jme3.listener.PlayerTranslateListener;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;

public class PlayerTranslateControl extends AbstractControl {

    private Vec3 lastLocation;
    private final PlayerTranslateListener listener;

    public PlayerTranslateControl(PlayerTranslateListener listener) {
	this.listener = listener;
    }

    @Override
    public void setSpatial(Spatial spatial) {
	super.setSpatial(spatial);
	this.lastLocation = toArePosition(spatial.getLocalTranslation());
    }

    @Override
    protected void controlUpdate(float tpf) {
	Vec3 currentLocation = toArePosition(this.getSpatial().getLocalTranslation());
	boolean walked = !currentLocation.equals(lastLocation);
	lastLocation = currentLocation;

	if (walked) {
	    listener.doAction(currentLocation);
	}
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    private Vec3 toArePosition(Vector3f position) {
	return new Vec3((int) (position.getX() / Chunk.WIDTH),
		(int) (position.getY() / Chunk.HEIGHT),
		(int) (position.getZ() / Chunk.LENGTH));
    }
}
