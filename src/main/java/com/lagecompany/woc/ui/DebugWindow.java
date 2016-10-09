/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.woc.ui;

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

    public static final String PLAYER_ARE_POSITION = "playerArePosition";
    public static final String PLAYER_POSITION = "playerPosition";
    private Label playerAreText;
    private Label playerText;

    public DebugWindow(int width, int height) {
	super(width, height);
	Styles styles = GuiGlobals.getInstance().getStyles();
	new StyleLoader(styles).loadStyleResource("/com/lagecompany/woc/ui/script/DebugStyle.script");
    }

    @Override
    public void build() {
	mainContainer = new Container("debug");
	mainContainer.setLocalTranslation(5, screenHeight - 5, 0);
	mainContainer.setLayout(new BoxLayout(Axis.Y, FillMode.None));

	playerAreText = mainContainer.addChild(new Label("Player are position: ", new ElementId("title"), "debug"));
	playerText = mainContainer.addChild(new Label("Player position: ", new ElementId("title"), "debug"));
    }

    @Override
    public void set(String key, Object value) {
	if (PLAYER_ARE_POSITION.equals(key)) {
	    playerAreText.setText("Player are position: " + value);
	} else if (PLAYER_POSITION.equals(key)) {
	    playerText.setText("Player position: " + value);
	} else {
	    throw new RuntimeException("Invalid key name: " + key);
	}
    }
}
