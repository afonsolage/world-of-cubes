package com.lagecompany.storage.light;

import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;

/**
 *
 * @author Afonso Lage
 */
public class LightNode {

    public byte x;
    public byte y;
    public byte z;
    public Chunk chunk;

    public LightNode(Chunk chunk, Vec3 position) {
	this(chunk, position.x, position.y, position.z);
    }

    public LightNode(Chunk chunk, int x, int y, int z) {
	this.x = (byte) x;
	this.y = (byte) y;
	this.z = (byte) z;
	this.chunk = chunk;
    }

    public LightNode copy() {
	return new LightNode(chunk, x, y, z);
    }
}
