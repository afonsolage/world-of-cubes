package com.lagecompany.ui;

import com.jme3.scene.Node;
import com.lagecompany.manager.Global;
import com.simsilica.lemur.Container;

/**
 *
 * @author Afonso Lage
 */
public abstract class Window {

    protected Container mainContainer;
    protected int screenWidth;
    protected int screenHeight;

    protected Window(int width, int height) {
	this.screenWidth = width;
	this.screenHeight = height;
    }

    public abstract void build();

    public void show() {
	Global.winMan.getGuiNode().attachChild(mainContainer);
    }

    public void hide() {
	mainContainer.removeFromParent();
    }

    public abstract void set(String key, Object value);
}
