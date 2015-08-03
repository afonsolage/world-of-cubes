package com.lagecompany.jme3;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.util.BufferUtils;
import com.lagecompany.jme3.state.LoadingStage;
import com.simsilica.lemur.GuiGlobals;

/**
 * Main class of the game, it attach needed stages and basic settings. This class extends SimpleApplication, which is a
 * common base class for every game on JME3
 *
 * @author Afonso Lage
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
	Main app = new Main();

	BufferUtils.setTrackDirectMemoryEnabled(true);

	// Disable settings screen before launching the game.
	app.setShowSettings(false);
	app.start();
    }

    /**
     * This method is called by SimpleApplication to start the game. This overrided version set resolution to 1024 x 768
     * for testing purpose.
     *
     * @param contextType The JmeContext.Type use to start game.
     */
    @Override
    public void start(JmeContext.Type contextType) {
	AppSettings sett = new AppSettings(true);
	sett.setResolution(800, 600);
	sett.setVSync(false);
	sett.setFrameRate(-1);
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
	stateManager.attach(new LoadingStage());
    }
}
