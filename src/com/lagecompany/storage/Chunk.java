package com.lagecompany.storage;

import static com.lagecompany.storage.Voxel.*;
import com.lagecompany.util.ArrayUtils;
import com.lagecompany.util.TerrainNoise;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Chunk {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 16;
    public static final int LENGTH = 16;
    public static final int DATA_LENGTH = WIDTH * HEIGHT * LENGTH;
    public static final int X_UNIT = HEIGHT * LENGTH;
    public static final int Y_UNIT = LENGTH;
    public static final int Z_UNIT = 1;
    public static final int FS_FRONT = 0;
    public static final int FS_RIGHT = 1;
    public static final int FS_BACK = 2;
    public static final int FS_LEFT = 3;
    public static final int FS_TOP = 4;
    public static final int FS_DOWN = 5;
    public static final int FS_COUNT = 6;
    private Voxel[] voxels;
    private float[] buffer;
    private int[] bufferFacesOffset;
    private boolean loaded;
    private final Are are;
    private final Vec3 position;
    private String name;
    private Lock lock;

    public Chunk(Are are, Vec3 position) {
	/*
	 * The memory needed by this class will be (WIDTH * HEIGHT * LENGTH * 16) + 12.
	 * The memory usage for each chunk will range from 16k to 40k of memory.
	 */
	this.are = are;
	this.position = position;
	this.name = "Chunk " + position.toString();
	this.lock = new ReentrantLock(true);
    }

    public boolean isLoaded() {
	return loaded;
    }

    public void setLoaded(boolean loaded) {
	this.loaded = loaded;
    }

    public String getName() {
	return name;
    }

    public Voxel get(int x, int y, int z) {
	return get(z + LENGTH * (y + HEIGHT * x));
    }

    public Voxel get(int i) {
	return (voxels != null && i < voxels.length && i >= 0) ? voxels[i] : null;
    }

    public void set(int x, int y, int z, Voxel v) {
	voxels[z + LENGTH * (y + HEIGHT * x)] = v;
    }

    public Voxel getAreVoxel(int x, int y, int z, byte direction) {
	Voxel result = null;

	switch (direction) {
	    case VS_LEFT: {
		if (x == 0) {
		    result = are.getVoxel(Vec3.copyAdd(position, -1, 0, 0), WIDTH - 1, y, z);
		} else {
		    result = get(x - 1, y, z);
		}
		break;
	    }
	    case VS_RIGHT: {
		if (x == WIDTH - 1) {
		    result = are.getVoxel(Vec3.copyAdd(position, 1, 0, 0), 0, y, z);
		} else {
		    result = get(x + 1, y, z);
		}
		break;
	    }
	    case VS_DOWN: {
		if (y == 0) {
		    result = are.getVoxel(Vec3.copyAdd(position, 0, -1, 0), x, HEIGHT - 1, z);
		} else {
		    result = get(x, y - 1, z);
		}
		break;
	    }
	    case VS_TOP: {
		if (y == HEIGHT - 1) {
		    result = are.getVoxel(Vec3.copyAdd(position, 0, 1, 0), x, 0, z);
		} else {
		    result = get(x, y + 1, z);
		}
		break;
	    }
	    case VS_BACK: {
		if (z == 0) {
		    result = are.getVoxel(Vec3.copyAdd(position, 0, 0, -1), x, y, LENGTH - 1);
		} else {
		    result = get(x, y, z - 1);
		}
		break;
	    }
	    case VS_FRONT: {
		if (z == LENGTH - 1) {
		    result = are.getVoxel(Vec3.copyAdd(position, 0, 0, 1), x, y, 0);
		} else {
		    result = get(x, y, z + 1);
		}
		break;
	    }
	}

	return result;
    }

    public boolean load() {
	checkVisibleFaces();
	mergeVisibleFaces();
	loaded = true;

	return (getVertexCount() > 0);
    }

    public boolean update() {
	if (isLoaded() && voxels != null) {
	    for (Voxel v : voxels) {
		if (v == null) {
		    continue;
		}
		v.setVisibleSides(VS_NONE);
		v.setMergedSides(VS_NONE);
	    }
	    loaded = false;
	}
	return load();
    }

    public boolean setup() {
	voxels = new Voxel[DATA_LENGTH];
	bufferFacesOffset = new int[6];

	int setupCount = 0;

	int x = position.getX();
	int y = position.getY();
	int z = position.getZ();

	for (int vX = 0; vX < WIDTH; vX++) {
	    for (int vZ = 0; vZ < LENGTH; vZ++) {
		double noiseHeight = TerrainNoise.getHeight(vX + x * WIDTH, vZ + z * HEIGHT);
		for (int vY = 0; vY < HEIGHT; vY++) {
		    if (vY + y * HEIGHT < noiseHeight) {
			setupCount++;
			set(vX, vY, vZ, new Voxel(VT_ROCK));
		    }
		}
	    }
	}

	return setupCount > 0;
    }

    void unload() {
	buffer = null;
	voxels = null;
    }

    public float[] getNormalList() {
	if (buffer == null) {
	    return new float[]{};
	}

	float[] r = new float[buffer.length]; //Each vertex needs a normal;
	int n = 0;

	//Front face
	for (int i = 0; i < bufferFacesOffset[FS_FRONT];) {
	    r[n++] = 0;
	    r[n++] = 0;
	    r[n++] = 1;

	    i += 3;
	}

	//Right face
	for (int i = 0; i < bufferFacesOffset[FS_RIGHT];) {
	    r[n++] = 1;
	    r[n++] = 0;
	    r[n++] = 0;

	    i += 3;
	}

	//Back face
	for (int i = 0; i < bufferFacesOffset[FS_BACK];) {
	    r[n++] = 0;
	    r[n++] = 0;
	    r[n++] = -1;

	    i += 3;
	}

	//Left face
	for (int i = 0; i < bufferFacesOffset[FS_LEFT];) {
	    r[n++] = -1;
	    r[n++] = 0;
	    r[n++] = 0;

	    i += 3;

	}

	//Top face
	for (int i = 0; i < bufferFacesOffset[FS_TOP];) {
	    r[n++] = 0;
	    r[n++] = 1;
	    r[n++] = 0;

	    i += 3;
	}

	//Down face
	for (int i = 0; i < bufferFacesOffset[FS_DOWN];) {
	    r[n++] = 0;
	    r[n++] = -1;
	    r[n++] = 0;

	    i += 3;
	}

	return r;
    }

    public int[] getIndexList() {
	if (buffer == null) {
	    return new int[]{};
	}

	//Each four vertex make up a plane with six indexes for each plane.
	int vertexCount = buffer.length / 3; //Get the vertex count (each vertex has 3 floats)
	int[] result = new int[(int) (vertexCount * 1.5)]; //4 vertex needs 6 index (4 * 1.5 = 6)
	int n = 0;

	/*  Vertexes are built using the counter-clockwise, we just need to follow this index pattern:
	 *		     3		2   2
	 *		     +--------+    + 
	 *		     |       /   / |
	 *		     |     /   /   |
	 *		     |   /   /     |
	 *		     | /   /	   |
	 *		     +   +---------+
	 *		    0    0	    1
	 */

	for (int i = 0; i < result.length;) {
	    result[i++] = n; //0
	    result[i++] = n + 1; //1
	    result[i++] = n + 2; //2
	    result[i++] = n + 2;   //2
	    result[i++] = n + 3; //3
	    result[i++] = n; //0

	    n += 4;
	}

	return result;
    }

    public float[] getVertexList() {
	return (buffer == null) ? new float[]{} : buffer;
    }

    public float[] getTextCoord() {
	if (buffer == null) {
	    return new float[]{};
	}

	//A vertex is made of 3 floats.
	int vertexCount = buffer.length / 3;

	float[] r = new float[vertexCount * 2]; //Each vertex needs a UV text coord;

	Vec3 v1 = new Vec3();
	Vec3 v2 = new Vec3();
	Vec3 v3 = new Vec3();
	Vec3 v4 = new Vec3();

	int xTile = 0;
	int yTile = 0;

	for (int i = 0, j = 0; i < buffer.length;) {
	    v1.setX((int) buffer[i++]);
	    v1.setY((int) buffer[i++]);
	    v1.setZ((int) buffer[i++]);

	    v2.setX((int) buffer[i++]);
	    v2.setY((int) buffer[i++]);
	    v2.setZ((int) buffer[i++]);

	    v3.setX((int) buffer[i++]);
	    v3.setY((int) buffer[i++]);
	    v3.setZ((int) buffer[i++]);

	    v4.setX((int) buffer[i++]);
	    v4.setY((int) buffer[i++]);
	    v4.setZ((int) buffer[i++]);

	    xTile = Math.abs(v1.getX() - v2.getX()) + Math.abs(v1.getY() - v2.getY()) + Math.abs(v1.getZ() - v2.getZ());
	    yTile = Math.abs(v1.getX() - v4.getX()) + Math.abs(v1.getY() - v4.getY()) + Math.abs(v1.getZ() - v4.getZ());

	    r[j++] = xTile;
	    r[j++] = 0;
	    r[j++] = 0;
	    r[j++] = 0;
	    r[j++] = 0;
	    r[j++] = yTile;
	    r[j++] = xTile;
	    r[j++] = yTile;
	}

	return r;
    }

    private void checkVisibleFaces() {
	Voxel nv; //Neighbor Voxel.
	byte faces;

	for (int x = 0; x < WIDTH; x++) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int z = 0; z < LENGTH; z++) {
		    Voxel v = get(x, y, z);

		    if (v == null) {
			continue;
		    }

		    faces = VS_NONE;

		    if ((nv = getAreVoxel(x, y, z, VS_LEFT)) == null || nv.getType() == VT_NONE) {
			faces |= VS_LEFT;
		    }

		    if ((nv = getAreVoxel(x, y, z, VS_RIGHT)) == null || nv.getType() == VT_NONE) {
			faces |= VS_RIGHT;
		    }

		    if ((nv = getAreVoxel(x, y, z, VS_DOWN)) == null || nv.getType() == VT_NONE) {
			faces |= VS_DOWN;
		    }

		    if ((nv = getAreVoxel(x, y, z, VS_TOP)) == null || nv.getType() == VT_NONE) {
			faces |= VS_TOP;
		    }

		    if ((nv = getAreVoxel(x, y, z, VS_BACK)) == null || nv.getType() == VT_NONE) {
			faces |= VS_BACK;
		    }

		    if ((nv = getAreVoxel(x, y, z, VS_FRONT)) == null || nv.getType() == VT_NONE) {
			faces |= VS_FRONT;
		    }

		    v.setVisibleSides(faces);
		}
	    }
	}
    }

    private void mergeVisibleFaces() {
	bufferFacesOffset = new int[FS_COUNT];
	buffer = null;
	int lastSize = 0;

	buffer = ArrayUtils.append(buffer, mergeFrontFaces());
	bufferFacesOffset[FS_FRONT] = buffer.length - lastSize;
	lastSize = buffer.length;

	buffer = ArrayUtils.append(buffer, mergeRightFaces());
	bufferFacesOffset[FS_RIGHT] = buffer.length - lastSize;
	lastSize = buffer.length;

	buffer = ArrayUtils.append(buffer, mergeBackFaces());
	bufferFacesOffset[FS_BACK] = buffer.length - lastSize;
	lastSize = buffer.length;

	buffer = ArrayUtils.append(buffer, mergeLeftFaces());
	bufferFacesOffset[FS_LEFT] = buffer.length - lastSize;
	lastSize = buffer.length;

	buffer = ArrayUtils.append(buffer, mergeTopFaces());
	bufferFacesOffset[FS_TOP] = buffer.length - lastSize;
	lastSize = buffer.length;

	buffer = ArrayUtils.append(buffer, mergeDownFaces());
	bufferFacesOffset[FS_DOWN] = buffer.length - lastSize;
    }

    public int getVertexCount() {
	return (buffer == null) ? 0 : buffer.length;
    }

    /**
     * Merge all voxels with a front (Z+) face of same type and visible. This methods uses a simple logic: It just
     * iterate over X looking for neighbor voxels who can be merged (share the same type and have front face visible
     * also). When it reaches the at right most voxel, it start looking for neigor at top (Y+) and repeat the proccess
     * looking into right voxels until it reaches the right most and top most voxels, when it combine all those voxels
     * on a singles float array and returns it.
     *
     * @return A float aray with 4 voxels of 3 floats each.
     */
    private float[] mergeFrontFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y to keep track of merged face bounds.
	int ox, oy;
	boolean done;
	float[] result = null;

	//When looking for front faces (Z+) we need to check X axis and later Y axis, becouse this, the iteration
	//occurs this way.
	for (int z = 0; z < LENGTH; z++) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int x = 0; x < WIDTH; x++) {
		    float[] vertices = new float[12];

		    v = get(x, y, z);

		    //If vox is invalid or is merged already, skip it;
		    if (v == null || (v.getVisibleSides() & VS_FRONT) != VS_FRONT || (v.getMergedSides() & VS_FRONT) == VS_FRONT) {
			continue;
		    }

		    ox = x;
		    oy = y;
		    done = false;

		    /*
		     * 
		     *      v7 +-------+ v6	y
		     *      / |      / |		|  
		     *   v3 +-------+v2|		|
		     *      |v4+-------+ v5	+-- X
		     *      | /     | /		 \
		     *      +-------+		 Z
		     *     v0        v1
		     */

		    //The front face is composed of v0, v1, v2 and v3.
		    System.arraycopy(Voxel.v0(x, y, z), 0, vertices, 0, 3);

		    /* 
		     *	Following the counter-clockwise rendering order.
		     * 
		     *	    |
		     *	    +
		     *	    |		    --->
		     *	    +---+---
		     *	  v0			       
		     */
		    while (true) {

			//We are on the boundary of chunk. Stop it;
			if (x == WIDTH - 1) {
			    break;
			}

			//Move to the next voxel on X axis.
			nv = get(++x, y, z);

			//If the next voxel is invalid
			if (nv == null || (nv.getVisibleSides() & VS_FRONT) != VS_FRONT
				|| (nv.getMergedSides() & VS_FRONT) == VS_FRONT) {
			    //Go back to previous voxel;
			    --x;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    /*
		     * 
		     *	    |		|
		     *	    +	        +
		     *	    |		|	    
		     *	    +---+---+---+
		     *	  v0		 v1	       
		     * 
		     *	At this moment, we reached the right most X, so set it as v1.
		     */
		    System.arraycopy(Voxel.v1(x, y, z), 0, vertices, 3, 3);

		    //Go one step on Y direction and repeat the previous logic.
		    while (!done) {
			if (y == HEIGHT - 1) {
			    //We are on the boundary of chunk. Stop it;
			    break;
			}

			y++;

			for (int k = ox; k <= x; k++) {
			    nv = get(k, y, z);
			    if (nv == null || (nv.getVisibleSides() & VS_FRONT) != VS_FRONT || (nv.getMergedSides() & VS_FRONT) == VS_FRONT) {
				--y; //Go back to previous voxel;
				done = true;
				break;
			    }
			}
		    }

		    /*	  v3		 v2
		     *	    +---+---+---+
		     *	    |		|
		     *	    +	        +
		     *	    |		|	    
		     *	    +---+---+---+
		     *	  v0		 v1	       
		     * 
		     *	At this moment, we reached the right most and top most, so lets track v2 and v3.
		     */
		    System.arraycopy(Voxel.v2(x, y, z), 0, vertices, 6, 3);
		    System.arraycopy(Voxel.v3(ox, y, z), 0, vertices, 9, 3);

		    for (int a = ox; a <= x; a++) {
			for (int b = oy; b <= y; b++) {
			    get(a, b, z).toggleMergedSide(VS_FRONT);
			}
		    }

		    result = ArrayUtils.append(result, vertices);
		    y = oy; //Set the y back to orignal location, so we can iterate over it again.
		}
	    }
	}
	return result;
    }

    private float[] mergeBackFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int ox, oy;
	boolean done;
	float[] result = null;

	for (int z = 0; z < LENGTH; z++) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int x = WIDTH - 1; x > -1; x--) {
		    float[] vertices = new float[12];

		    v = get(x, y, z);

		    //If vox is invalid or is merged already, skip it;
		    if (v == null || (v.getVisibleSides() & VS_BACK) != VS_BACK || (v.getMergedSides() & VS_BACK) == VS_BACK) {
			continue;
		    }

		    ox = x;
		    oy = y;
		    done = false;

		    //The back face is composed of v5, v4, v7 and v6.
		    System.arraycopy(Voxel.v5(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (x == 0) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(--x, y, z);

			if (nv == null || (nv.getVisibleSides() & VS_BACK) != VS_BACK || (nv.getMergedSides() & VS_BACK) == VS_BACK) {
			    ++x; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (y == HEIGHT - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			y++;

			for (int k = ox; k >= x; k--) {
			    nv = get(k, y, z);
			    if (nv == null || (nv.getVisibleSides() & VS_BACK) != VS_BACK || (nv.getMergedSides() & VS_BACK) == VS_BACK) {
				--y; //Go back to previous voxel;
				done = true;
				break;
			    }
			}
		    }

		    System.arraycopy(Voxel.v7(x, y, z), 0, vertices, 6, 3);
		    System.arraycopy(Voxel.v6(ox, y, z), 0, vertices, 9, 3);

		    for (int a = ox; a >= x; a--) {
			for (int b = oy; b <= y; b++) {
			    get(a, b, z).toggleMergedSide(VS_BACK);
			}
		    }

		    result = ArrayUtils.append(result, vertices);
		    y = oy; //Set the y back to orignal location, so we can iterate over it again.
		}
	    }
	}
	return result;
    }

    private float[] mergeTopFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int ox, oz;
	boolean done;
	float[] result = null;

	for (int y = HEIGHT - 1; y > -1; y--) {
	    for (int z = LENGTH - 1; z > -1; z--) {
		for (int x = 0; x < WIDTH; x++) {
		    v = get(x, y, z);

		    //If vox is invalid or is merged already, skip it;
		    if (v == null || (v.getVisibleSides() & VS_TOP) != VS_TOP || (v.getMergedSides() & VS_TOP) == VS_TOP) {
			continue;
		    }

		    float[] vertices = new float[12];
		    ox = x;
		    oz = z;
		    done = false;

		    //The top face is composed of v3, v2, v6 and v7.
		    System.arraycopy(Voxel.v3(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (x == WIDTH - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(++x, y, z);

			if (nv == null || (nv.getVisibleSides() & VS_TOP) != VS_TOP || (nv.getMergedSides() & VS_TOP) == VS_TOP) {
			    --x; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v2(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (z == 0) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			z--;

			for (int k = ox; k <= x; k++) {
			    nv = get(k, y, z);
			    if (nv == null || (nv.getVisibleSides() & VS_TOP) != VS_TOP || (nv.getMergedSides() & VS_TOP) == VS_TOP) {
				++z; //Go back to previous voxel;
				done = true;
				break;
			    }
			}
		    }

		    System.arraycopy(Voxel.v6(x, y, z), 0, vertices, 6, 3);
		    System.arraycopy(Voxel.v7(ox, y, z), 0, vertices, 9, 3);

		    for (int a = ox; a <= x; a++) {
			for (int b = oz; b >= z; b--) {
			    get(a, y, b).toggleMergedSide(VS_TOP);
			}
		    }

		    result = ArrayUtils.append(result, vertices);
		    z = oz; //Set the z back to orignal location, so we can iterate over it again.
		}
	    }
	}
	return result;
    }

    private float[] mergeDownFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int ox, oz;
	boolean done;
	float[] result = null;

	for (int y = 0; y < HEIGHT; y++) {
	    for (int z = 0; z < LENGTH; z++) {
		for (int x = 0; x < WIDTH; x++) {
		    float[] vertices = new float[12];

		    v = get(x, y, z);

		    //If vox is invalid or is merged already, skip it;
		    if (v == null || (v.getVisibleSides() & VS_DOWN) != VS_DOWN || (v.getMergedSides() & VS_DOWN) == VS_DOWN) {
			continue;
		    }

		    ox = x;
		    oz = z;
		    done = false;

		    //The top face is composed of v4, v5, v1 and v0.
		    System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (x == WIDTH - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(++x, y, z);

			if (nv == null || (nv.getVisibleSides() & VS_DOWN) != VS_DOWN || (nv.getMergedSides() & VS_DOWN) == VS_DOWN) {
			    --x; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v5(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (z == LENGTH - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			z++;

			for (int k = ox; k <= x; k++) {
			    nv = get(k, y, z);
			    if (nv == null || (nv.getVisibleSides() & VS_DOWN) != VS_DOWN || (nv.getMergedSides() & VS_DOWN) == VS_DOWN) {
				--z; //Go back to previous voxel;
				done = true;
				break;
			    }
			}
		    }

		    System.arraycopy(Voxel.v1(x, y, z), 0, vertices, 6, 3);
		    System.arraycopy(Voxel.v0(ox, y, z), 0, vertices, 9, 3);

		    for (int a = ox; a <= x; a++) {
			for (int b = oz; b <= z; b++) {
			    get(a, y, b).toggleMergedSide(VS_DOWN);
			}
		    }

		    result = ArrayUtils.append(result, vertices);
		    z = oz; //Set the z back to orignal location, so we can iterate over it again.
		}
	    }
	}
	return result;
    }

    private float[] mergeRightFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int oz, oy;
	boolean done;
	float[] result = null;

	for (int x = WIDTH - 1; x > -1; x--) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int z = LENGTH - 1; z > -1; z--) {
		    float[] vertices = new float[12];

		    v = get(x, y, z);

		    //If vox is invalid or is merged already, skip it;
		    if (v == null || (v.getVisibleSides() & VS_RIGHT) != VS_RIGHT || (v.getMergedSides() & VS_RIGHT) == VS_RIGHT) {
			continue;
		    }

		    oz = z;
		    oy = y;
		    done = false;

		    //The top face is composed of v1, v5, v6 and v2.
		    System.arraycopy(Voxel.v1(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (z == 0) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(x, y, --z);

			if (nv == null || (nv.getVisibleSides() & VS_RIGHT) != VS_RIGHT || (nv.getMergedSides() & VS_RIGHT) == VS_RIGHT) {
			    ++z; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v5(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (y == HEIGHT - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			y++;

			for (int k = oz; k >= z; k--) {
			    nv = get(x, y, k);
			    if (nv == null || (nv.getVisibleSides() & VS_RIGHT) != VS_RIGHT || (nv.getMergedSides() & VS_RIGHT) == VS_RIGHT) {
				--y; //Go back to previous voxel;
				done = true;
				break;
			    }
			}
		    }

		    System.arraycopy(Voxel.v6(x, y, z), 0, vertices, 6, 3);
		    System.arraycopy(Voxel.v2(x, y, oz), 0, vertices, 9, 3);

		    for (int a = oz; a >= z; a--) {
			for (int b = oy; b <= y; b++) {
			    get(x, b, a).toggleMergedSide(VS_RIGHT);
			}
		    }

		    result = ArrayUtils.append(result, vertices);
		    y = oy; //Set the y back to orignal location, so we can iterate over it again.
		}
	    }
	}
	return result;
    }

    private float[] mergeLeftFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int oz, oy;
	boolean done;
	float[] result = null;

	for (int x = 0; x < WIDTH; x++) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int z = 0; z < LENGTH; z++) {
		    float[] vertices = new float[12];

		    v = get(x, y, z);

		    //If vox is invalid or is merged already, skip it;
		    if (v == null || (v.getVisibleSides() & VS_LEFT) != VS_LEFT || (v.getMergedSides() & VS_LEFT) == VS_LEFT) {
			continue;
		    }

		    oz = z;
		    oy = y;
		    done = false;

		    //The top face is composed of v4, v0, v3 and v7.
		    System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (z == LENGTH - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(x, y, ++z);

			if (nv == null || (nv.getVisibleSides() & VS_LEFT) != VS_LEFT || (nv.getMergedSides() & VS_LEFT) == VS_LEFT) {
			    --z; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v0(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (y == HEIGHT - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			y++;

			for (int k = oz; k <= z; k++) {
			    nv = get(x, y, k);
			    if (nv == null || (nv.getVisibleSides() & VS_LEFT) != VS_LEFT || (nv.getMergedSides() & VS_LEFT) == VS_LEFT) {
				--y; //Go back to previous voxel;
				done = true;
				break;
			    }
			}
		    }

		    System.arraycopy(Voxel.v3(x, y, z), 0, vertices, 6, 3);
		    System.arraycopy(Voxel.v7(x, y, oz), 0, vertices, 9, 3);

		    for (int a = oz; a <= z; a++) {
			for (int b = oy; b <= y; b++) {
			    get(x, b, a).toggleMergedSide(VS_LEFT);
			}
		    }

		    result = ArrayUtils.append(result, vertices);
		    y = oy; //Set the y back to orignal location, so we can iterate over it again.
		}
	    }
	}
	return result;
    }

    public void lock() {
	lock.lock();
    }

    public void unlock() {
	lock.unlock();
    }

    public Vec3 getPosition() {
	return this.position;
    }
}