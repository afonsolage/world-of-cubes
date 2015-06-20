package com.lagecompany;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

public class VoxelMesh extends Mesh {

    
    public VoxelMesh(float size) {
	float hs = size / 2; //Half Size;

	/*
	 * 
	 *	v6 +-------+ v7		y
	 *	 / |     / |		| Z 
	 *   v2 +-------+v3|		|/
	 *	|v4+-------+ v5		+-- X
	 *	| /	| /
	 *	+-------+ 
	 *     v0        v1
	 */

	Vector3f v0 = new Vector3f(-hs, -hs, hs);
	Vector3f v1 = new Vector3f(hs, -hs, hs);
	Vector3f v2 = new Vector3f(-hs, hs, hs);
	Vector3f v3 = new Vector3f(hs, hs, hs);
	Vector3f v4 = new Vector3f(-hs, -hs, -hs);
	Vector3f v5 = new Vector3f(hs, -hs, -hs);
	Vector3f v6 = new Vector3f(-hs, hs, -hs);
	Vector3f v7 = new Vector3f(hs, hs, -hs);

	Vector3f[] positions = new Vector3f[]{
	    v0, v1, v3, v2, //Front
	    v1, v5, v7, v3, //Right
	    v5, v4, v6, v7, //Back
	    v4, v0, v2, v6, //Left
	    v2, v3, v7, v6, //Top
	    v5, v1, v0, v4, //Down
	};

	int[] indexes = new int[]{
	    0, 1, 2, 2, 3, 0, //Front
	    4, 5, 6, 6, 7, 4, //Rigth
	    8, 9, 10, 10, 11, 8, //Back
	    12, 13, 14, 14, 15, 12, //Left
	    16, 17, 18, 18, 19, 16, //Top
	    20, 21, 22, 22, 23, 20, //Down
	};

	Vector3f[] normals = new Vector3f[]{
	    new Vector3f(0, 0, 1),
	    new Vector3f(0, 0, 1),
	    new Vector3f(0, 0, 1),
	    new Vector3f(0, 0, 1), //Front

	    new Vector3f(1, 0, 0),
	    new Vector3f(1, 0, 0),
	    new Vector3f(1, 0, 0),
	    new Vector3f(1, 0, 0), //Right

	    new Vector3f(0, 0, -1),
	    new Vector3f(0, 0, -1),
	    new Vector3f(0, 0, -1),
	    new Vector3f(0, 0, -1), //Back

	    new Vector3f(-1, 0, 0),
	    new Vector3f(-1, 0, 0),
	    new Vector3f(-1, 0, 0),
	    new Vector3f(-1, 0, 0), //Left

	    new Vector3f(0, 1, 0),
	    new Vector3f(0, 1, 0),
	    new Vector3f(0, 1, 0),
	    new Vector3f(0, 1, 0), //Top

	    new Vector3f(0, -1, 0),
	    new Vector3f(0, -1, 0),
	    new Vector3f(0, -1, 0),
	    new Vector3f(0, -1, 0), //Down
	};


	Vector2f[] textCoord = new Vector2f[]{
	    new Vector2f(0,0), new Vector2f(0,1), new Vector2f(1,1), new Vector2f(1,0), 
	    new Vector2f(0,0), new Vector2f(0,1), new Vector2f(1,1), new Vector2f(1,0),
	    new Vector2f(0,0), new Vector2f(0,1), new Vector2f(1,1), new Vector2f(1,0),
	    new Vector2f(0,0), new Vector2f(0,1), new Vector2f(1,1), new Vector2f(1,0),
	    new Vector2f(0,0), new Vector2f(0,1), new Vector2f(1,1), new Vector2f(1,0),
	    new Vector2f(0,0), new Vector2f(0,1), new Vector2f(1,1), new Vector2f(1,0),
	};


	this.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(positions));
	this.setBuffer(Type.Index, 1, BufferUtils.createIntBuffer(indexes));
	this.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
	this.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(textCoord));
	this.updateBound();
    }
}
