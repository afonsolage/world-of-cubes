package com.lagecompany.woc.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.lagecompany.woc.manager.Global;
import com.lagecompany.woc.manager.WindowManager;
import com.lagecompany.woc.storage.Are;
import com.lagecompany.woc.storage.Chunk;
import com.lagecompany.woc.ui.LoadingScreen;

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
	// private Camera cam;
	// private Node guiNode;

	/**
	 * Initialize this stage. Is called intenally by JME3.
	 *
	 * @param stateManager
	 *            The StateManager used by JME3
	 * @param app
	 *            The application which this stage was attached to
	 */
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		this.stateManager = stateManager;
		// this.cam = app.getCamera();

		// SimpleApplication simpleApp = (SimpleApplication) app;
		// guiNode = simpleApp.getGuiNode();

		loadingScreen = (LoadingScreen) Global.winMan.get(WindowManager.LOADING);
		loadingScreen.build();
		loadingScreen.show();

		are = new Are();

		attachStates();
	}

	/**
	 * Update loop of this stage. Is called by main loop.
	 *
	 * @param tpf
	 *            Time per frame in seconds.
	 */
	@Override
	public void update(float tpf) {
		int setupCount = (int) are.getChunkQueueSize(Chunk.State.SETUP);
		int loadCount = (int) are.getChunkQueueSize(Chunk.State.LOAD);
		int lightCount = (int) are.getChunkQueueSize(Chunk.State.LIGHT);
		int attachCount = (int) are.getChunkQueueSize(Chunk.State.ATTACH);
		int detachCount = (int) are.getChunkQueueSize(Chunk.State.DETACH);
		int unloadCount = (int) are.getChunkQueueSize(Chunk.State.UNLOAD);

		loadingScreen.set(LoadingScreen.SETUP_QUEUE, setupCount);
		loadingScreen.set(LoadingScreen.LOAD_QUEUE, loadCount);
		loadingScreen.set(LoadingScreen.LIGHT_QUEUE, lightCount);
		loadingScreen.set(LoadingScreen.ATTACH_QUEUE, attachCount);
		loadingScreen.set(LoadingScreen.DETACH_QUEUE, detachCount);
		loadingScreen.set(LoadingScreen.UNLOAD_QUEUE, unloadCount);

		if (are.isInited()) {
			stateManager.detach(this);
		}
	}

	/**
	 * Attach all stages needed by game play.
	 */
	private void attachStates() {
		stateManager.attach(new BulletAppState());
		stateManager.attach(new WorldAppState());
		terrainState = new TerrainAppState(are);
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
		stateManager.attach(new DebugAppState(are));
		worldState.startEnvironment();
	}
}
