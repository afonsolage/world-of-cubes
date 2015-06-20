package com.lagecompany;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.lagecompany.state.DebugAppState;
import com.lagecompany.state.TerrainAppState;
import com.lagecompany.state.WorldAppState;

/**
 * test
 * @author normenhansen
 */
public class Main extends SimpleApplication {

    private WorldAppState worldState;
    private DebugAppState debugState;
    private TerrainAppState terrainState;
    
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
	worldState = new WorldAppState();
	stateManager.attach(worldState);
	
	terrainState = new TerrainAppState();
	stateManager.attach(terrainState);
	
	debugState = new DebugAppState();
	stateManager.attach(debugState);
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
