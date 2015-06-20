package com.lagecompany.nifty.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.LayerBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.ScreenBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.screen.DefaultScreenController;

public class DebugWindow {
    
    public static final String ID = "debug-gui";
    private Nifty nifty;
    
    public DebugWindow(Nifty n) {
	this.nifty = n;
	
	nifty.loadStyleFile("nifty-default-styles.xml");
	nifty.loadControlFile("nifty-default-controls.xml");
    }
    
    public void delete() {
	nifty.removeScreen(ID);
    }
    
    public void create() {
	nifty.addScreen(ID, new ScreenBuilder("Debug GUI") {
	    {
		controller(new DefaultScreenController());
		layer(new LayerBuilder("foreground") {
		    {
			childLayoutVertical();
			backgroundColor("#0000");
			
			panel(new PanelBuilder("panel_top_left") {
			    {
				childLayoutVertical();
				alignLeft();
				backgroundColor("#0007");
				height("25%");
				width("30%");
				
				panel(new PanelBuilder("panel_title") {
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
				
				panel(new PanelBuilder("panel_title") {
				    {
					childLayoutHorizontal();
					alignCenter();
					
					panel(new PanelBuilder("panel_reload_label") {
					    {
						childLayoutHorizontal();
						alignLeft();
						width("50%");
						
						text(new TextBuilder() {
						    {
							text("Reload Chunk");
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
	}.build(nifty));
    }
    
    public void display() {
	nifty.gotoScreen(ID);
    }
}