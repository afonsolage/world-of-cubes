/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.ui;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;

/**
 *
 * @author Afonso Lage
 */
public class LoadingScreen extends Window {

    public static final String SETUP_QUEUE = "setupQueue";
    public static final String LIGHT_QUEUE = "lightQueue";
    public static final String LOAD_QUEUE = "loadQueue";
    public static final String ATTACH_QUEUE = "attachQueue";
    public static final String DETACH_QUEUE = "detachQueue";
    public static final String UNLOAD_QUEUE = "unloadQueue";
    private Label setupText;
    private Label lightText;
    private Label loadText;
    private Label attachText;
    private Label detachText;
    private Label unloadText;

    public LoadingScreen(int width, int height) {
	super(width, height);
    }

    @Override
    public void build() {
	mainContainer = new Container();
	mainContainer.setLocalTranslation(0, screenHeight, 0);
	mainContainer.setBackground(new QuadBackgroundComponent(ColorRGBA.Black));
	mainContainer.setLayout(new BoxLayout(Axis.Y, FillMode.None));

	setupText = mainContainer.addChild(new Label("Setup queue..."));
	lightText = mainContainer.addChild(new Label("Light queue..."));
	loadText = mainContainer.addChild(new Label("Load queue..."));
	attachText = mainContainer.addChild(new Label("Attach queue..."));
	detachText = mainContainer.addChild(new Label("Detach queue..."));
	unloadText = mainContainer.addChild(new Label("Unload queue..."));

	mainContainer.setPreferredSize(new Vector3f(screenWidth, screenHeight, 0));
    }

    @Override
    public void set(String key, Object value) {
	if (SETUP_QUEUE.equals(key)) {
	    setupText.setText("Setup queue..." + value);
	} else if (LIGHT_QUEUE.equals(key)) {
	    lightText.setText("Light queue..." + value);
	} else if (LOAD_QUEUE.equals(key)) {
	    loadText.setText("Load queue..." + value);
	} else if (ATTACH_QUEUE.equals(key)) {
	    attachText.setText("Attach queue..." + value);
	} else if (DETACH_QUEUE.equals(key)) {
	    detachText.setText("Detach queue..." + value);
	} else if (UNLOAD_QUEUE.equals(key)) {
	    unloadText.setText("Unload queue..." + value);
	} else {
	    throw new RuntimeException("Invalid key name: " + key);
	}
    }
}
