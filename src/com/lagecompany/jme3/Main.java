package com.lagecompany.jme3;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.lagecompany.jme3.state.LoadingStage;
import com.lagecompany.nifty.gui.NiftyJmeHelper;

/**
 * test
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    public static void main(String[] args) {
	Main app = new Main();
	app.setShowSettings(false);
	app.start();
    }

    @Override
    public void start(JmeContext.Type contextType) {
	AppSettings sett = new AppSettings(true);
	sett.setResolution(1024, 768);
	sett.setFrameRate(-1);
	setSettings(sett);
	super.start(contextType);
    }

    @Override
    public void simpleInitApp() {
	NiftyJmeHelper.init(assetManager, inputManager, audioRenderer, guiViewPort);
	stateManager.attach(new LoadingStage());
    }
}
