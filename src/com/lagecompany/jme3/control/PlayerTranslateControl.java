package com.lagecompany.jme3.control;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.lagecompany.jme3.listener.PlayerTranslateListener;

public class PlayerTranslateControl extends AbstractControl {

    private Vector3f lastLocation;
    private final PlayerTranslateListener listener;

    public PlayerTranslateControl(PlayerTranslateListener listener) {
	this.listener = listener;
    }

    @Override
    public void setSpatial(Spatial spatial) {
	super.setSpatial(spatial);
	this.lastLocation = spatial.getLocalTranslation();
    }

    @Override
    protected void controlUpdate(float tpf) {
	Vector3f currentLocation = this.getSpatial().getLocalTranslation();
	boolean walked = !currentLocation.equals(lastLocation);
	lastLocation = currentLocation.clone();

	if (walked) {
	    listener.doAction(currentLocation);
	}
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
