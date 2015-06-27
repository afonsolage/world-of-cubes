package com.lagecompany.nifty.gui;

import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.DefaultScreenController;

public class DebugScreen extends SimpleScreen {

    public static final String ID = "debug-gui";
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
				height("25%");
				width("30%");

				panel(new PanelBuilder("title") {
				    {
					childLayoutCenter();
					alignCenter();
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
					childLayoutHorizontal();
					alignCenter();

					panel(new PanelBuilder("player position") {
					    {
						childLayoutHorizontal();
						alignLeft();
						width("100%");
						x("10");

						text(new TextBuilder("player position text") {
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

    public void setPlayerPosition(String text) {
	playerTextRenderer.setText("Player position: " + text);
    }
}