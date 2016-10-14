package com.lagecompany.woc.system.debug.component;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;

public class Position implements EntityComponent {

	private final Vector3f position;

	public Position(Vector3f position) {
		this.position = position;
	}

	public Vector3f getPosition() {
		return position;
	}

	@Override
	public String toString() {
		return "Position [position=" + position + "]";
	}
}
