/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.nifty.gui;

import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.DefaultScreenController;

/**
 *
 * @author afonsolage
 */
public class LoadingScreen extends SimpleScreen {

    public static final String ID = "loading-gui";
    private TextRenderer textRenderer;

    public LoadingScreen() {
	super();
    }

    @Override
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

	textRenderer = getElement("foreground", "main_panel", "text_renderer").getRenderer(TextRenderer.class);

	nifty.addScreen(ID, screen);
    }

    @Override
    public void display() {
	nifty.gotoScreen(ID);
    }

    public void setMessage(String message) {
	textRenderer.setText(message);
    }
}
