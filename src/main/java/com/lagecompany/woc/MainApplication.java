package com.lagecompany.woc;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.util.BufferUtils;
import com.lagecompany.woc.manager.Global;
import com.lagecompany.woc.state.dev.PhysicsDevState;
import com.lagecompany.woc.system.debug.ModelState;
import com.lagecompany.woc.system.debug.PhysicalCollisionState;
import com.lagecompany.woc.system.entity.EntityDataState;
import com.lagecompany.woc.system.physics.PhysicsState;
import com.lagecompany.woc.util.PerformanceTrack;
import com.simsilica.lemur.GuiGlobals;

/**
 * Main application class based on JME3 SimpleApplication class. This one holds the application entry, the method main,
 * which start the application main loop.
 * 
 * @author Afonso Lage
 *
 */
public class MainApplication extends SimpleApplication {

	public static void main(String[] args) {
		MainApplication app = new MainApplication();

		BufferUtils.setTrackDirectMemoryEnabled(true);

		// Disable settings screen before launching the game.
		app.setShowSettings(false);
		app.start();
	}

	/**
	 * This method is called by SimpleApplication to start the game. This override version set resolution to 800 x 600
	 * for testing purpose.
	 *
	 * @param contextType
	 *            The JmeContext.Type use to start game.
	 */
	@Override
	public void start(JmeContext.Type contextType) {
		AppSettings sett = new AppSettings(true);
		sett.setResolution(800, 600);
		sett.setVSync(false);
		setSettings(sett);
		super.start(contextType);
	}

	/**
	 * Called after game context is created but before the first screen render, this method creates the initiate
	 * NiftyGui and attach a loading state.
	 */
	@Override
	public void simpleInitApp() {
		GuiGlobals.initialize(this);

		Global.winMan.setCamera(cam);
		Global.winMan.setGuiNode(guiNode);

		stateManager.attach(new EntityDataState());
		stateManager.attach(new ModelState());
		stateManager.attach(new PhysicsState());
		stateManager.attach(new PhysicalCollisionState());
		
		stateManager.attach(new PhysicsDevState());
	}

	@Override
	public void stop() {
		super.stop();
		PerformanceTrack.printResults();
	}

}
