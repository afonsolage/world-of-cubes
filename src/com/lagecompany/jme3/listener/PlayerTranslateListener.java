package com.lagecompany.jme3.listener;

import com.jme3.math.Vector3f;

/**
 *
 * @author afonsolage
 */
public interface PlayerTranslateListener {

    public abstract void doAction(Vector3f currentLocation);
}
