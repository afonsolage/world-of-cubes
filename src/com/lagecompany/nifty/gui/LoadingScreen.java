/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.nifty.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.DefaultScreenController;
import de.lessvoid.nifty.screen.Screen;

/**
 *
 * @author afonsolage
 */
public class LoadingScreen {

    public static final String ID = "loading-gui";
    private Nifty nifty;
    private Screen screen;

    public LoadingScreen(Nifty n) {
	this.nifty = n;

	nifty.loadStyleFile("nifty-default-styles.xml");
	nifty.loadControlFile("nifty-default-controls.xml");
    }

    public void delete() {
	nifty.removeScreen(ID);
    }

    public void create() {
	screen = new ScreenBuilder("Loading GUI") {
	    {
		controller(new DefaultScreenController());
		layer(new LayerBuilder("foreground") {
		    {
			childLayoutVertical();
			backgroundColor("#000000FF");
			height("100%");
			width("100%");
			
			panel(new PanelBuilder("main_panel") {
			    {
				childLayoutCenter();
				alignCenter();
				backgroundColor("#000000FF");
				height("100%");
				width("100%");

				text(new TextBuilder("text_renderer") {
				    {
					text("Loading...0%");
					font("Interface/Fonts/sz_16.fnt");
				    }
				});
			    }
			});
		    }
		});
	    }
	}.build(nifty);

	nifty.addScreen(ID, screen);
    }

    public void display() {
	nifty.gotoScreen(ID);
    }

    public void setMessage(String message) {
	Element e = screen.findElementByName("foreground");
	e = e.findElementByName("main_panel");
	e = e.findElementByName("text_renderer");
	TextRenderer renderer = e.getRenderer(TextRenderer.class);
	renderer.setText(message);
    }
}
