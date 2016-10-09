package com.lagecompany.woc.ui;

import com.lagecompany.woc.manager.Global;
import com.simsilica.lemur.Container;

/**
 *
 * @author Afonso Lage
 */
public abstract class Window {

    protected Container mainContainer;
    protected int screenWidth;
    protected int screenHeight;
    protected boolean shown;

    protected Window(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public abstract void build();

    public void show() {
        Global.winMan.getGuiNode().attachChild(mainContainer);
        shown = true;
    }

    public void hide() {
        mainContainer.removeFromParent();
        shown = false;
    }

    public boolean isShown() {
        return shown;
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }

    public abstract void set(String key, Object value);
}
