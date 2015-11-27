package com.lagecompany.storage.light;

import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;

/**
 *
 * @author Afonso Lage
 */
public class LightRemoveNode {

    public byte x;
    public byte y;
    public byte z;
    public int light;
    public Chunk chunk;

    public LightRemoveNode(Chunk chunk, Vec3 position, int light) {
	this(chunk, position.x, position.y, position.z, light);
    }

    public LightRemoveNode(Chunk chunk, int x, int y, int z, int light) {
	this.x = (byte) x;
	this.y = (byte) y;
	this.z = (byte) z;
	this.light = light;
	this.chunk = chunk;
    }

    public LightRemoveNode copy() {
	return new LightRemoveNode(chunk, x, y, z, light);
    }
}
