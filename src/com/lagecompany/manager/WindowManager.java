package com.lagecompany.manager;

import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.lagecompany.ui.DebugWindow;
import com.lagecompany.ui.LoadingScreen;
import com.lagecompany.ui.ToolbarWindow;
import com.lagecompany.ui.Window;
import java.util.HashMap;

/**
 *
 * @author Afonso Lage
 */
public class WindowManager {

    public static final String TOOLBAR = "Toolbar Window";
    public static final String DEBUG = "Debug Window";
    public static final String LOADING = "Loading Window";
    private Camera cam;
    private Node guiNode;
    private HashMap<String, Window> windows;

    WindowManager() {
	windows = new HashMap<>();
    }

    public void setCamera(Camera cam) {
	this.cam = cam;
    }

    public int getWindowWidth() {
	return cam.getWidth();
    }

    public int getWindowHeight() {
	return cam.getHeight();
    }

    public void setGuiNode(Node guiNode) {
	this.guiNode = guiNode;
    }

    public Node getGuiNode() {
	return guiNode;
    }

    public Window get(String name) {
	Window w = windows.get(name);

	if (w == null) {
	    w = create(name);
	}

	return w;
    }

    private Window create(String name) {
	Window w;
	switch (name) {
	    case TOOLBAR: {
		w = new ToolbarWindow(getWindowWidth(), getWindowHeight());
		break;
	    }
	    case LOADING: {
		w = new LoadingScreen(getWindowWidth(), getWindowHeight());
		break;
	    }
	    case DEBUG: {
		w = new DebugWindow(getWindowWidth(), getWindowHeight());
		break;
	    }
	    default: {
		throw new RuntimeException("Unknown window name: " + name);
	    }
	}
	windows.put(name, w);
	return w;
    }
}
