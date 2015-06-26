package com.lagecompany.jme3.listener;

import com.lagecompany.storage.Vec3;

/**
 *
 * @author afonsolage
 */
public interface PlayerTranslateListener {

    public abstract void doAction(Vec3 currentLocation);
}
