package com.lagecompany.storage.voxel;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.lagecompany.util.ArrayUtils;

/**
 *
 * @author Afonso Lage
 */
public abstract class VoxelMesh {

    private static final int SIDES = 6;
    private static final int TILECOORD_PER_SIDE = 4;
    private static final int COLOR_PER_SIDE = 4;

    private VoxelMesh() {
    }

    public static Mesh getMesh(short type) {
	Mesh mesh = new Mesh();

	float[] vertices = getVertices(0, 0, 0, 1, 1, 1);
	int[] indexes = getIndexes();
	float[] normals = getNormals();
	float[] texCoord = getTextCoord(1, 1, 0, 0);
	float[] tileCoord = new float[SIDES * TILECOORD_PER_SIDE * 2];
	float[] color = new float[SIDES * COLOR_PER_SIDE * 4];

	byte side;
	int t = 0, c = 0;
	for (int i = 1; i <= SIDES; i++) {
	    side = (byte) (1 << i & 0xFF);
	    for (int k = 0; k < 4; k++) {
		System.arraycopy(Voxel.getTile(type, side), 0, tileCoord, t, 2);
		System.arraycopy(Voxel.getColor(type, side), 0, color, c, 4);
		t += 2;
		c += 4;
	    }
	}

	mesh.setBuffer(VertexBuffer.Type.Position, 3, vertices);
	mesh.setBuffer(VertexBuffer.Type.Index, 1, indexes);
	mesh.setBuffer(VertexBuffer.Type.Normal, 3, normals);
	mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texCoord);
	mesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, tileCoord);
	mesh.setBuffer(VertexBuffer.Type.Color, 4, color);
	return mesh;
    }

    public static float[] getVertices(float x1, float y1, float z1, float x2, float y2, float z2) {
	return new float[]{
	    //Front
	    x1, y2, z2, //v7
	    x1, y2, z1, //v3
	    x1, y1, z1, //v0
	    x1, y1, z2, //v4

	    //Right
	    x2, y2, z2, //v6
	    x1, y2, z2, //v7
	    x1, y1, z2, //v4
	    x2, y1, z2, //v5

	    //Back
	    x2, y2, z1, //v2
	    x2, y2, z2, //v6
	    x2, y1, z2, //v5
	    x2, y1, z1, //v1

	    //Left
	    x1, y2, z1, //v3
	    x2, y2, z1, //v2
	    x2, y1, z1, //v1
	    x1, y1, z1, //v0

	    //Top
	    x1, y2, z2, //v7
	    x2, y2, z2, //v6
	    x2, y2, z1, //v2
	    x1, y2, z1, //v3

	    //Down
	    x1, y1, z1, //v0
	    x2, y1, z1, //v1
	    x2, y1, z2, //v5
	    x1, y1, z2, //v4
	};
    }

    public static int[] getIndexes() {
	return new int[]{
	    //Front
	    0, 1, 2,
	    2, 3, 0,
	    //Right
	    4, 5, 6,
	    6, 7, 4,
	    //Back
	    8, 9, 10,
	    10, 11, 8,
	    //Left
	    12, 13, 14,
	    14, 15, 12,
	    //Top
	    16, 17, 18,
	    18, 19, 16,
	    //Down
	    20, 21, 22,
	    22, 23, 20
	};
    }

    public static float[] getNormals() {
	return new float[]{
	    //Front
	    0, 0, 1,
	    0, 0, 1,
	    0, 0, 1,
	    0, 0, 1,
	    //Right
	    1, 0, 0,
	    1, 0, 0,
	    1, 0, 0,
	    1, 0, 0,
	    //Back
	    0, 0, -1,
	    0, 0, -1,
	    0, 0, -1,
	    0, 0, -1,
	    //Left
	    -1, 0, 0,
	    -1, 0, 0,
	    -1, 0, 0,
	    -1, 0, 0,
	    //Top
	    0, 1, 0,
	    0, 1, 0,
	    0, 1, 0,
	    0, 1, 0,
	    //Down
	    0, -1, 0,
	    0, -1, 0,
	    0, -1, 0,
	    0, -1, 0
	};
    }

    public static float[] getTextCoord(float x1, float y1, float x2, float y2) {
	return new float[]{
	    //Front
	    1, 0,
	    0, 0,
	    0, 1,
	    1, 1,
	    //Right
	    1, 0,
	    0, 0,
	    0, 1,
	    1, 1,
	    //Back
	    1, 0,
	    0, 0,
	    0, 1,
	    1, 1,
	    //Left
	    1, 0,
	    0, 0,
	    0, 1,
	    1, 1,
	    //Top
	    1, 0,
	    0, 0,
	    0, 1,
	    1, 1,
	    //Down
	    1, 0,
	    0, 0,
	    0, 1,
	    1, 1,};
    }
}
