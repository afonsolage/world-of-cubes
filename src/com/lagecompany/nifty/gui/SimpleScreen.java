/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.nifty.gui;

import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.ViewPort;
import static com.lagecompany.nifty.gui.DebugScreen.ID;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;

/**
 *
 * @author afonsolage
 */
public abstract class SimpleScreen {

    protected Nifty nifty;
    protected Screen screen;

    SimpleScreen() {
	this.nifty = NiftyJmeHelper.getNifty();

	nifty.loadStyleFile("nifty-default-styles.xml");
	nifty.loadControlFile("nifty-default-controls.xml");
    }

    public void delete() {
	nifty.removeScreen(ID);
    }

    protected Element getElement(String... elements) {
	if (elements == null || elements.length < 1) {
	    return null;
	}

	Element result = screen.findElementByName(elements[0]);

	for (int i = 1; i < elements.length && result != null; i++) {
	    String s = elements[i];
	    if (s == null) {
		break;
	    }
	    result = result.findElementByName(s);
	}
	return result;
    }

    public abstract void create();

    public abstract void display();
}
