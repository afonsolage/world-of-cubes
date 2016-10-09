/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.woc.ui;

import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;

/**
 *
 * @author TI.Afonso
 */
public class CommandWindow extends Window {

    private static final int WINDOW_HEIGHT = 20;

    private final int wWidth;
    private TextField textField;
//    private KeyActionListener listener;

    public CommandWindow(int width, int height) {
        super(width, height);

        wWidth = (int) (width * 0.3f);
    }

    @Override
    public void build() {
        mainContainer = new Container("CommandWindow");
        mainContainer.setLocalTranslation((screenWidth / 2) - (wWidth / 2), WINDOW_HEIGHT + 5, 0);
        mainContainer.setLayout(new BoxLayout(Axis.Y, FillMode.First));

        if (textField == null) {
            textField = mainContainer.addChild(new TextField(""));
            textField.setPreferredWidth(wWidth);
            textField.setBackground(new QuadBackgroundComponent(ColorRGBA.DarkGray));
        } else {
            mainContainer.addChild(textField);
        }

        GuiGlobals.getInstance().requestFocus(textField);
    }

    @Override
    public void set(String key, Object value) {

    }

    @Override
    public void hide() {
        super.hide();
        GuiGlobals.getInstance().requestFocus(null);
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

}
