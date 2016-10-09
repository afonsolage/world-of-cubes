package com.lagecompany.woc.control;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.lagecompany.woc.storage.Are;
import com.lagecompany.woc.storage.AreMessage;
import com.lagecompany.woc.storage.Chunk;
import com.lagecompany.woc.storage.Vec3;

/**
 * A Control that tracks spatial location and move Are based on it.
 * @author Afonso Lage
 */
public class AreFollowControl extends AbstractControl {

    private Vec3 lastAreLocation;
    private Vector3f lastPlayerLocation;
    private Are are;

    /**
     * Create an instance and get Are instance.
     */
    public AreFollowControl() {
	are = Are.getInstance();
    }

    /**
     * This method is called by JME3 when this Control is attached to a Spatial.
     * @param spatial 
     */
    @Override
    public void setSpatial(Spatial spatial) {
	super.setSpatial(spatial);
	this.lastAreLocation = toArePosition(spatial.getLocalTranslation());
    }

    /**
     * Main update method, called each frame.
     * @param tpf Time per fame in seconds.
     */
    @Override
    protected void controlUpdate(float tpf) {
	lastPlayerLocation = getSpatial().getLocalTranslation();

	//TODO: Create a margin area to avoid constant load and unload.
	if (are.isMoving()) {
	    return;
	}
	Vec3 currentLocation = toArePosition(getSpatial().getLocalTranslation());

	if (currentLocation.equals(lastAreLocation)) {
	    return;
	}
	lastAreLocation = currentLocation;
	Vec3 moved = lastAreLocation.subtractNew(are.getPosition());
	
	//TODO: For debug reason, we wont move Are on Y axis.
	moved.y = 0;

	if (moved.equals(Vec3.ZERO)) {
	    return;
	}

	//Send a message to Are move it self.
	System.out.println("Moving Are chunk to: " + moved + " - " + are.getPosition());
	are.setMoving(true);
	are.postMessage(new AreMessage(AreMessage.Type.ARE_MOVE, moved));
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    /**
     * Ulitity class used to convert a Vector3f position to a Vec3 position, used by Are.
     * @param A Vector3f position to be converted.
     * @return A Vec3 position converted.
     */
    private Vec3 toArePosition(Vector3f position) {
	return new Vec3((int) (position.getX() / Chunk.SIZE),
		(int) (position.getY() / Chunk.SIZE),
		(int) (position.getZ() / Chunk.SIZE));
    }

    /**
     * Returns the current Are position tracked by this control.
     * @return The last Are position tracked.
     */
    public Vec3 getArePosition() {
	return lastAreLocation;
    }

    /**
     * Returns the current player position tracked by this control.
     * @return The last player position tracked.
     */
    public Vector3f getPlayerPosition() {
	return lastPlayerLocation;
    }
}
