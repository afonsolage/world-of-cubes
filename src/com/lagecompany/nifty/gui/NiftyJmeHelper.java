package com.lagecompany.nifty.gui;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.InputManager;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;

public class NiftyJmeHelper {

    private static Nifty nifty;

    public static void init(AssetManager assetManager, InputManager inputManager, AudioRenderer audioRenderer, ViewPort guiViewPort) {
	NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
	guiViewPort.addProcessor(niftyDisplay);

	nifty = niftyDisplay.getNifty();
    }

    public static Nifty getNifty() {
	return nifty;
    }
}
