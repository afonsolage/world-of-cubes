package com.lagecompany.woc.manager;

import java.util.HashMap;

import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.lagecompany.woc.ui.CommandWindow;
import com.lagecompany.woc.ui.DebugWindow;
import com.lagecompany.woc.ui.LoadingScreen;
import com.lagecompany.woc.ui.ToolbarWindow;
import com.lagecompany.woc.ui.Window;

/**
 *
 * @author Afonso Lage
 */
public class WindowManager {

    public static final String TOOLBAR = "Toolbar Window";
    public static final String DEBUG = "Debug Window";
    public static final String LOADING = "Loading Window";
    public static final String COMMAND = "Command Window";
    private Camera cam;
    private Node guiNode;
    private final HashMap<String, Window> windows;

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
            case COMMAND: {
                w = new CommandWindow(getWindowWidth(), getWindowHeight());
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
