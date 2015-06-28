package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.InputManager;
import com.jme3.renderer.ViewPort;
import com.lagecompany.nifty.gui.LoadingScreen;
import com.lagecompany.storage.Are;

public class LoadingStage extends AbstractAppState {

    private TerrainAppState terrainState;
    private AppStateManager stateManager;
    private AssetManager assetManager;
    private InputManager inputManager;
    private AudioRenderer audioRenderer;
    private ViewPort guiViewPort;
    private LoadingScreen loadingScreen;
    private float total = -1;
    private float lastVal = 0f;
    private int state = 0;
    private Are are;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
	this.stateManager = stateManager;
	this.inputManager = app.getInputManager();
	this.assetManager = app.getAssetManager();
	this.guiViewPort = app.getGuiViewPort();
	this.audioRenderer = app.getAudioRenderer();

	loadingScreen = new LoadingScreen();
	loadingScreen.create();
	loadingScreen.display();

	are = Are.getInstance();

	attachStates();
    }

    @Override
    public void update(float tpf) {
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

    private void attachStates() {
	stateManager.attach(new BulletAppState());
	stateManager.attach(new WorldAppState());
	terrainState = new TerrainAppState();
	terrainState.setShouldRender(false);
	stateManager.attach(terrainState);
    }

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

    @Override
    public void cleanup() {
	super.cleanup();
	loadingScreen.delete();
	stateManager.attach(new DebugAppState());
    }
}
