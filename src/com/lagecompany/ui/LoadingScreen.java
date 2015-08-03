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

    private Container mainContainer;
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
    public void show(Node guiNode) {
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
	guiNode.attachChild(mainContainer);
    }

    @Override
    public void hide() {
	mainContainer.removeFromParent();
    }

    public void setMessageCount(String type, int count) {
	String message = " " + count;
	if (type.equalsIgnoreCase("setup")) {
	    message = "Setup queue..." + message;
	    setupText.setText(message);
	} else if (type.equalsIgnoreCase("light")) {
	    message = "Light queue..." + message;
	    lightText.setText(message);
	} else if (type.equalsIgnoreCase("load")) {
	    message = "Load queue...." + message;
	    loadText.setText(message);
	} else if (type.equalsIgnoreCase("attach")) {
	    message = "Attach queue.." + message;
	    attachText.setText(message);
	} else if (type.equalsIgnoreCase("detach")) {
	    message = "Detach queue.." + message;
	    detachText.setText(message);
	} else {
	    message = "Unload queue.." + message;
	    unloadText.setText(message);
	}
    }
}
