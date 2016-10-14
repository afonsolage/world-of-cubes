package com.lagecompany.woc.system.debug.component;

import com.jme3.math.ColorRGBA;
import com.simsilica.es.EntityComponent;

public class Model implements EntityComponent {
	private final float size;
	private final ColorRGBA color;

	public Model(float size, ColorRGBA color) {
		this.size = size;
		this.color = color;
	}

	public float getSize() {
		return size;
	}

	public ColorRGBA getColor() {
		return color;
	}

	@Override
	public String toString() {
		return "Model [size=" + size + ", color=" + color + "]";
	}
}
