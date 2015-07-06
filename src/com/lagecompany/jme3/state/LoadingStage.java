package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.lagecompany.nifty.gui.LoadingScreen;
import com.lagecompany.storage.Are;

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
    private float total = -1;
    private float lastVal = 0f;
    private int state = 0;
    private Are are;

    /**
     * Initialize this stage. Is called intenally by JME3.
     *
     * @param stateManager The StateManager used by JME3
     * @param app The application which this stage was attached to
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
	this.stateManager = stateManager;

	loadingScreen = new LoadingScreen();
	loadingScreen.create();
	loadingScreen.display();

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
	// The loading has tree stages: 0 - Loading Chunks, 1 - Rendering Chunks and 2 - Go to next screen.
	if (are.isInited()) {
	    switch (state) {
		case 0: {
		    showLoadingChunk();
		    break;
		}
		case 1: {
		    showRenderingChunk();
		    break;
		}
		case 2: {
		    stateManager.detach(this);
		}
	    }
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
     * Show the loading chunks message.
     */
    private void showLoadingChunk() {
	if (total < 0) {
	    total = are.getChunkQueueSize();
	} else {
	    float val = (((float) are.getChunkQueueSize() / total)) * 100f;
	    val = 100f - val;
	    if (val > lastVal) {
		loadingScreen.setMessage(String.format("Loading chunks...%.2f%%", val));
		lastVal = val;
	    }
	    if (lastVal >= 100f) {
		state++;
		total = -1;
		lastVal = 0;
	    }
	}
    }

    /**
     * Show the rendering chunks message.
     */
    private void showRenderingChunk() {
	if (total < 0) {
	    total = are.getAttachQueueSize();
	    terrainState.setShouldRender(true);
	} else {
	    float val = (((float) are.getAttachQueueSize() / total)) * 100f;
	    val = 100f - val;
	    if (val > lastVal) {
		loadingScreen.setMessage(String.format("Rendering chunks...%.2f%%", val));
		lastVal = val;
	    }
	    if (lastVal >= 100f) {
		state++;
	    }
	}
    }

    /**
     * This method is called by JME3 when this stage is detached, so it must be used for cleanup.
     */
    @Override
    public void cleanup() {
	super.cleanup();
	loadingScreen.delete();
	stateManager.attach(new DebugAppState());
	worldState.startEnvironment();
    }
}
