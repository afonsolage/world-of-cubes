/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.lagecompany.woc.manager;

import com.lagecompany.woc.object.entity.Player;

/**
 *
 * @author Afonso Lage
 */
public abstract class Global {
    public static final WindowManager winMan = new WindowManager();
    public static final Player player = new Player();
}
