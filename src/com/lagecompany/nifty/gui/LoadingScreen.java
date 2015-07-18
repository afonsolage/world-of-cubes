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
    private TextRenderer setupTextRenderer;
    private TextRenderer lightTextRenderer;
    private TextRenderer loadTextRenderer;
    private TextRenderer attachTextRenderer;
    private TextRenderer detachTextRenderer;
    private TextRenderer unloadTextRenderer;

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
				childLayoutVertical();
				alignLeft();
				backgroundColor("#000000FF");

				text(new TextBuilder("text_setup_queue") {
				    {
					text("Setup queue...0");
					font("Interface/Fonts/sz_16.fnt");
				    }
				});

				text(new TextBuilder("text_light_queue") {
				    {
					text("Light queue...0");
					font("Interface/Fonts/sz_16.fnt");
				    }
				});

				text(new TextBuilder("text_load_queue") {
				    {
					text("Load queue....0");
					font("Interface/Fonts/sz_16.fnt");
				    }
				});

				text(new TextBuilder("text_attach_queue") {
				    {
					text("Attach queue..0");
					font("Interface/Fonts/sz_16.fnt");
				    }
				});

				text(new TextBuilder("text_detach_queue") {
				    {
					text("Detach queue..0");
					font("Interface/Fonts/sz_16.fnt");
				    }
				});

				text(new TextBuilder("text_unload_queue") {
				    {
					text("Unload queue..0");
					font("Interface/Fonts/sz_16.fnt");
				    }
				});
			    }
			});
		    }
		});
	    }
	}.build(nifty);

	setupTextRenderer = getElement("foreground", "main_panel", "text_setup_queue").getRenderer(TextRenderer.class);
	lightTextRenderer = getElement("foreground", "main_panel", "text_light_queue").getRenderer(TextRenderer.class);
	loadTextRenderer = getElement("foreground", "main_panel", "text_load_queue").getRenderer(TextRenderer.class);
	attachTextRenderer = getElement("foreground", "main_panel", "text_attach_queue").getRenderer(TextRenderer.class);
	detachTextRenderer = getElement("foreground", "main_panel", "text_detach_queue").getRenderer(TextRenderer.class);
	unloadTextRenderer = getElement("foreground", "main_panel", "text_unload_queue").getRenderer(TextRenderer.class);

	nifty.addScreen(ID, screen);
    }

    @Override
    public void display() {
	nifty.gotoScreen(ID);
    }

    public void setMessageCount(String type, int count) {
	String message = " " + count;
	if (type.equalsIgnoreCase("setup")) {
	    message = "Setup queue..." + message;
	    setupTextRenderer.setText(message);
	} else if (type.equalsIgnoreCase("light")) {
	    message = "Light queue..." + message;
	    lightTextRenderer.setText(message);
	} else if (type.equalsIgnoreCase("load")) {
	    message = "Load queue...." + message;
	    loadTextRenderer.setText(message);
	} else if (type.equalsIgnoreCase("attach")) {
	    message = "Attach queue.." + message;
	    attachTextRenderer.setText(message);
	} else if (type.equalsIgnoreCase("detach")) {
	    message = "Detach queue.." + message;
	    detachTextRenderer.setText(message);
	} else {
	    message = "Unload queue.." + message;
	    unloadTextRenderer.setText(message);
	}
    }
}
