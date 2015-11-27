/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.ui;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;

/**
 *
 * @author Afonso Lage
 */
public class ToolbarWindow extends Window {

    private static final int SLOT_WIDTH = 50;
    private static final int SLOT_HEIGHT = 50;
    private static final int SLOT_COUNT = 5;
    private static final int BORDER_SIZE = 5;
    private static final String SLOT_OBJECT = "SLOT OBJECT";
    private int base_width;
    private int base_height;
    private Container[] slots;

    public ToolbarWindow(int screenWidth, int screenHeight) {
	super(screenWidth, screenHeight);
    }

    @Override
    public void build() {
	base_width = BORDER_SIZE + (SLOT_COUNT * (SLOT_WIDTH + BORDER_SIZE));
	base_height = BORDER_SIZE + SLOT_HEIGHT + BORDER_SIZE;
	slots = new Container[SLOT_COUNT];
	
	mainContainer = new Container("debug");
	mainContainer.setLocalTranslation((screenWidth / 2) - (base_width / 2), base_height, 0);
	mainContainer.setLayout(new BoxLayout(Axis.Y, FillMode.None));
	mainContainer.setPreferredSize(new Vector3f(base_width, base_height, 0));

	mainContainer.addChild(new Panel(BORDER_SIZE, BORDER_SIZE, ColorRGBA.DarkGray));

	Container basePanel = mainContainer.addChild(new Container());
	basePanel.setLayout(new BoxLayout(Axis.X, FillMode.None));
	basePanel.setPreferredSize(new Vector3f(base_width, SLOT_HEIGHT, 0));

	basePanel.addChild(new Panel(BORDER_SIZE, BORDER_SIZE, ColorRGBA.DarkGray));

	Container panel;
	for (int i = 0; i < SLOT_COUNT; i++) {
	    panel = basePanel.addChild(new Container());
	    panel.setBackground(new QuadBackgroundComponent(
		    new ColorRGBA(0f, 0f, 0f, 0.5f)));
	    panel.setPreferredSize(new Vector3f(SLOT_WIDTH, SLOT_HEIGHT, 0));

	    Label label = panel.addChild(new Label("" + (i + 1)));
	    label.setFontSize(13f);

	    slots[i] = panel;
	    basePanel.addChild(new Panel(BORDER_SIZE, BORDER_SIZE, ColorRGBA.DarkGray));
	}

	mainContainer.addChild(new Panel(BORDER_SIZE, BORDER_SIZE, ColorRGBA.DarkGray));
    }

    @Override
    public void set(String key, Object value) {
	int slot = Integer.parseInt(key);
	setSlot(slot, (Spatial) value);
    }

    private void setSlot(int slotId, Spatial object) {
	if (slotId < 0 || slotId > SLOT_COUNT) {
	    throw new RuntimeException("Invalid slot " + slotId);
	}

	Container slot = slots[slotId];

	if (object != null) {
	    slot.attachChild(object);
	    object.setLocalTranslation(SLOT_WIDTH * 0.10f, -SLOT_HEIGHT * 0.75f, 0.01f);
	    object.setLocalScale(SLOT_WIDTH * 0.7f, SLOT_HEIGHT * 0.7f, SLOT_HEIGHT * 0.7f);
//	    object.rotate(-0.5f, 0.5f, 0.0f);
	    object.setName(SLOT_OBJECT);
	} else {
	    slot.detachChildNamed(SLOT_OBJECT);
	}
    }
}
