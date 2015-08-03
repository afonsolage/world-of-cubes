package com.lagecompany.ui;

import com.jme3.scene.Node;

/**
 *
 * @author Afonso Lage
 */
public abstract class Window {

    protected int screenWidth;
    protected int screenHeight;

    protected Window(int width, int height) {
	this.screenWidth = width;
	this.screenHeight = height;
    }

    public abstract void show(Node guiNode);

    public abstract void hide();
}
