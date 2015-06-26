package com.lagecompany.jme3;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.lagecompany.jme3.state.DebugAppState;
import com.lagecompany.jme3.state.LoadingStage;
import com.lagecompany.jme3.state.TerrainAppState;
import com.lagecompany.jme3.state.WorldAppState;

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
	sett.setResolution(800, 600);
	sett.setFrameRate(-1);
	setSettings(sett);
	super.start(contextType);
    }

    @Override
    public void simpleInitApp() {
	stateManager.attach(new LoadingStage());
    }
}
