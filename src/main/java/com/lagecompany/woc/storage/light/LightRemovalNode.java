package com.lagecompany.woc.storage.light;

/**
 *
 * @author Afonso Lage
 */
public class LightRemovalNode {

    public int x;
    public int y;
    public int z;
    public int previousLight;

    public LightRemovalNode(int x, int y, int z, int previousLight) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.previousLight = previousLight;
    }
}
