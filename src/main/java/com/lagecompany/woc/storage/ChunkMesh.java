package com.lagecompany.woc.storage;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

public class ChunkMesh extends Mesh {

	private Vec3 position;

	public ChunkMesh(Vec3 position, ChunkSideBuffer chunkSideBuffer) {
		this.position = position;

		if (!chunkSideBuffer.isEmpty()) {
			setBuffer(VertexBuffer.Type.Position, 3, chunkSideBuffer.get());
			setBuffer(VertexBuffer.Type.Index, 1, chunkSideBuffer.getIndexList());
			setBuffer(VertexBuffer.Type.Normal, 3, chunkSideBuffer.getNormalList());
			setBuffer(VertexBuffer.Type.TexCoord, 2, chunkSideBuffer.getTextCoord());
			setBuffer(VertexBuffer.Type.TexCoord2, 2, chunkSideBuffer.getTileCoord());
			setBuffer(VertexBuffer.Type.Color, 4, chunkSideBuffer.getTexColor());
		}
	}

	public Vec3 getPosition() {
		return position;
	}
	
	public boolean isValid() {
		return this.getVertexCount() > 0;
	}
}
