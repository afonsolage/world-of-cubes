package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.lagecompany.storage.Are;
import com.lagecompany.storage.AreMessage;
import com.lagecompany.ui.LoadingScreen;

/**
 * The loading app state of game. On this state, Are will be initiated and chunks being loaded, while a loading message
 * is shown.
 *
 * @author Afonso Lage
 */
public class LoadingStage extends AbstractAppState {

    private TerrainAppState terrainState;
    private WorldAppState worldState;
    private AppStateManager stateManager;
    private LoadingScreen loadingScreen;
    private Are are;
    private Camera cam;
    private Node guiNode;

    /**
     * Initialize this stage. Is called intenally by JME3.
     *
     * @param stateManager The StateManager used by JME3
     * @param app The application which this stage was attached to
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
	this.stateManager = stateManager;
	this.cam = app.getCamera();

	SimpleApplication simpleApp = (SimpleApplication) app;
	guiNode = simpleApp.getGuiNode();

	loadingScreen = new LoadingScreen(cam.getWidth(), cam.getHeight());
	loadingScreen.show(guiNode);

	are = Are.getInstance();

	attachStates();
    }

    /**
     * Update loop of this stage. Is called by main loop.
     *
     * @param tpf Time per frame in seconds.
     */
    @Override
    public void update(float tpf) {
	if (!are.isInited()) {
	    return;
	}
	int setupCount = (int) are.getChunkQueueSize(AreMessage.Type.CHUNK_SETUP);
	int loadCount = (int) are.getChunkQueueSize(AreMessage.Type.CHUNK_LOAD);
	int lightCount = (int) are.getChunkQueueSize(AreMessage.Type.CHUNK_LIGHT);
	int attachCount = (int) are.getChunkQueueSize(AreMessage.Type.CHUNK_ATTACH);
	int detachCount = (int) are.getChunkQueueSize(AreMessage.Type.CHUNK_DETACH);
	int unloadCount = (int) are.getChunkQueueSize(AreMessage.Type.CHUNK_UNLOAD);

	loadingScreen.setMessageCount("setup", setupCount);
	loadingScreen.setMessageCount("load", loadCount);
	loadingScreen.setMessageCount("light", lightCount);
	loadingScreen.setMessageCount("attach", attachCount);
	loadingScreen.setMessageCount("detach", detachCount);
	loadingScreen.setMessageCount("unload", unloadCount);

	if (setupCount + loadCount + lightCount + attachCount + detachCount + unloadCount == 0) {
	    stateManager.detach(this);
	}
    }

    /**
     * Attach all stages needed by game play.
     */
    private void attachStates() {
	stateManager.attach(new BulletAppState());
	stateManager.attach(new WorldAppState());
	terrainState = new TerrainAppState();
	terrainState.setShouldRender(false);
	stateManager.attach(terrainState);



	this.worldState = stateManager.getState(WorldAppState.class);
    }

    /**
     * This method is called by JME3 when this stage is detached, so it must be used for cleanup.
     */
    @Override
    public void cleanup() {
	super.cleanup();
	loadingScreen.hide();
	stateManager.attach(new DebugAppState());
	worldState.startEnvironment();
    }
}
