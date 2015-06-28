package com.lagecompany.nifty.gui;

import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.DefaultScreenController;

public class DebugScreen extends SimpleScreen {

    public static final String ID = "debug-gui";
    private TextRenderer playerAreTextRenderer;
    private TextRenderer playerTextRenderer;

    public DebugScreen() {
	super();
    }

    @Override
    public void create() {
	screen = new ScreenBuilder("Debug GUI") {
	    {
		controller(new DefaultScreenController());
		layer(new LayerBuilder("foreground") {
		    {
			childLayoutVertical();
			backgroundColor("#0000");
			panel(new PanelBuilder("root") {
			    {
				childLayoutVertical();
				alignLeft();
				backgroundColor("#0007");
				height("150px");
				width("300px");

				panel(new PanelBuilder("title") {
				    {
					childLayoutCenter();
					width("100%");
					text(new TextBuilder() {
					    {
						text("Debug Tools");
						font("Interface/Fonts/sz_16.fnt");
					    }
					});
				    }
				});

				panel(new PanelBuilder("content") {
				    {
					childLayoutVertical();
					alignLeft();
					width("100%");
					
					panel(new PanelBuilder("player position") {
					    {
						childLayoutVertical();
						width("100%");

						text(new TextBuilder("player position text") {
						    {
							text("Player Position: 0, 0, 0");
							font("Interface/Fonts/sz_12.fnt");
						    }
						});
					    }
					});
					panel(new PanelBuilder("player are position") {
					    {
						childLayoutVertical();
						width("100%");

						text(new TextBuilder("player are position text") {
						    {
							text("Player Position: 0, 0, 0");
							font("Interface/Fonts/sz_12.fnt");
						    }
						});
					    }
					});

				    }
				});
			    }
			});
		    }
		});
	    }
	}.build(nifty);

	playerAreTextRenderer = getElement("foreground",
		"root",
		"content",
		"player are position",
		"player are position text").getRenderer(TextRenderer.class);

	playerTextRenderer = getElement("foreground",
		"root",
		"content",
		"player position",
		"player position text").getRenderer(TextRenderer.class);

	nifty.addScreen(ID, screen);
    }

    @Override
    public void display() {
	nifty.gotoScreen(ID);
    }

    public void setPlayerArePosition(String text) {
	playerAreTextRenderer.setText("Player are position: " + text);
    }

    public void setPlayerPosition(String text) {
	playerTextRenderer.setText("Player position: " + text);
    }
}