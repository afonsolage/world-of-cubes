/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.ui;

import com.jme3.scene.Node;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.StyleLoader;
import com.simsilica.lemur.style.Styles;

/**
 *
 * @author Afonso Lage
 */
public class DebugWindow extends Window {

    private Container mainContainer;
    private Label playerAreText;
    private Label playerText;

    public DebugWindow(int width, int height) {
	super(width, height);
	Styles styles = GuiGlobals.getInstance().getStyles();
	new StyleLoader(styles).loadStyleResource("/com/lagecompany/ui/script/DebugStyle.script");
    }

    @Override
    public void show(Node guiNode) {
	mainContainer = new Container("debug");
	mainContainer.setLocalTranslation(5, screenHeight - 5, 0);
	mainContainer.setLayout(new BoxLayout(Axis.Y, FillMode.None));

	playerAreText = mainContainer.addChild(new Label("Player are position: ", new ElementId("title"), "debug"));
	playerText = mainContainer.addChild(new Label("Player position: ", new ElementId("title"), "debug"));

	guiNode.attachChild(mainContainer);
    }

    @Override
    public void hide() {
	mainContainer.removeFromParent();
    }

    public void setPlayerArePosition(String text) {
	playerAreText.setText("Player are position: " + text);
    }

    public void setPlayerPosition(String text) {
	playerText.setText("Player position: " + text);
    }
}
