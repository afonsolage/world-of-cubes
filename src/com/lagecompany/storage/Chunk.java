package com.lagecompany.storage;

import com.lagecompany.storage.voxel.Voxel;
import static com.lagecompany.storage.voxel.Voxel.*;
import com.lagecompany.util.TerrainNoise;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Chunk {

    public static final int SIZE = 16;
    public static final int DATA_LENGTH = SIZE * SIZE * SIZE;
    public static final int X_UNIT = SIZE * SIZE;
    public static final int Y_UNIT = SIZE;
    public static final int Z_UNIT = 1;
    public static final int FS_FRONT = 0;
    public static final int FS_RIGHT = 1;
    public static final int FS_BACK = 2;
    public static final int FS_LEFT = 3;
    public static final int FS_TOP = 4;
    public static final int FS_DOWN = 5;
    public static final int FS_COUNT = 6;
    private final Voxel[] voxels;
    private int[] lightMap;
    private ChunkSideBuffer buffer;
    private boolean loaded;
    private boolean updateFlag;
    private int visibleVoxels;
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
	this.voxels = new Voxel[DATA_LENGTH];
	this.lightMap = new int[DATA_LENGTH];
	this.buffer = new ChunkSideBuffer();
	this.updateFlag = false;
    }

    public boolean isLoaded() {
	return loaded;
    }

    public void setLoaded(boolean loaded) {
	this.loaded = loaded;
    }

    public boolean isFlaggedToUpdate() {
	return updateFlag;
    }

    public void flagToUpdate() {
	this.updateFlag = true;
    }

    public String getName() {
	return name;
    }

    public Voxel get(Vec3 v) {
	return get(v.getX(), v.getY(), v.getZ());
    }

    public Voxel get(int x, int y, int z) {
	if (x < 0 || x >= Chunk.SIZE
		|| y < 0 || y >= Chunk.SIZE
		|| z < 0 || z >= Chunk.SIZE) {
	    return null;
	}
	return get(z + SIZE * (y + SIZE * x));
    }

    public Voxel get(int i) {
	return (voxels != null && i < voxels.length && i >= 0) ? voxels[i] : null;
    }

    public int getLight(Vec3 pos) {
	return getLight(pos.getX(), pos.getY(), pos.getZ());
    }

    public int getLight(int x, int y, int z) {
	return getLight(z + SIZE * (y + SIZE * x));
    }

    public int getLight(int i) {
	if (lightMap == null || i >= lightMap.length || i < 0) {
	    throw new RuntimeException(String.format("Invalid voxel index: %d", i));
	} else {
	    return lightMap[i] & 0x0F;
	}
    }

    public int getSunLight(Vec3 pos) {
	return getSunLight(pos.getX(), pos.getY(), pos.getZ());
    }

    public int getSunLight(int x, int y, int z) {
	return getSunLight(z + SIZE * (y + SIZE * x));
    }

    public int getSunLight(int i) {
	if (lightMap == null || i >= lightMap.length || i < 0) {
	    throw new RuntimeException(String.format("Invalid voxel index: %d", i));
	} else {
	    int a = (lightMap[i] >>> 4) & 0xF;
	    return a;
	}
    }

    public void set(Vec3 pos, Voxel v) {
	set(pos.getX(), pos.getY(), pos.getZ(), v);
    }

    public void set(int x, int y, int z, Voxel v) {
	voxels[z + SIZE * (y + SIZE * x)] = v;
    }

    public void setLight(Vec3 v, int light) {
	setLight(v.getX(), v.getY(), v.getZ(), light);
    }

    public void setLight(int x, int y, int z, int v) {
	int idx = z + SIZE * (y + SIZE * x);

	//We are using only one byte of lightMap at moment.
	//First set v to 0000 FFFF, to ensure only a light less then 15 will be get (0x0F = 15);
	//Then clear the bits on lightMap also, by using and AND 0xF0. So we'll have XXXX 0000.
	//At least, just OR both XXXX 0000 OR 0000 XXXX, this will ensure that we change only desired bits.
	lightMap[idx] = lightMap[idx] & 0xF0 | v & 0x0F;
    }

    public void setSunLight(Vec3 v, int light) {
	setSunLight(v.getX(), v.getY(), v.getZ(), light);
    }

    public void setSunLight(int x, int y, int z, int v) {
	int idx = z + SIZE * (y + SIZE * x);

	//We are using only one byte of lightMap at moment.
	//First set v to 0000 FFFF, to ensure only a light less then 15 will be get (0x0F = 15);
	//Then clear the bits on lightMap also, by using and AND 0x0F. So we'll have 0000 XXXX.
	//At least, just OR both 0000 XXXX OR XXXX 0000, this will ensure that we change only desired bits.
	lightMap[idx] = lightMap[idx] & 0xF | ((v & 0x0F) << 4);
    }

    public void flagSkyLight(int x, int y, int z, boolean skyLight) {
	int idx = z + SIZE * (y + SIZE * x);

	//Set the flag on light to identify if this is a sky light or no. The flag is placed at 8 most bit: 
	//FXXX XXXX. So let's clear or set flag.
	if (skyLight) {
	    lightMap[idx] |= 0x80; //0x80 = 1000 0000
	} else {
	    lightMap[idx] &= 0x7F; //0x7F = 0111 1111
	}

    }

    public boolean isFlaggedSkyLight(int x, int y, int z) {
	int idx = z + SIZE * (y + SIZE * x);

	//0x80 = 1000 0000, so if they remain the same after AND operation, this means it is flagged.
	return (lightMap[idx] & 0x80) == 0x80;
    }

    public void flagReflectedLight(int x, int y, int z, boolean skyLight) {
	int idx = z + SIZE * (y + SIZE * x);

	//Set the flag on light to identify if it was reflected: 
	//XFXX XXXX. So let's clear or set flag.
	if (skyLight) {
	    lightMap[idx] |= 0x40; //0x40 = 0100 0000
	} else {
	    lightMap[idx] &= 0xBF; //0xBF = 1011 1111
	}
    }

    public boolean isFlaggedReflectedLight(int x, int y, int z) {
	int idx = z + SIZE * (y + SIZE * x);

	//0x40 = 0100 0000, so if they remain the same after AND operation, this means it is flagged.
	return (lightMap[idx] & 0x40) == 0x40;
    }

    public Voxel getAreVoxel(int x, int y, int z, byte direction) {
	Voxel result = null;

	switch (direction) {
	    case VS_LEFT: {
		if (x == 0) {
		    result = are.getVoxel(Vec3.copyAdd(position, -1, 0, 0), SIZE - 1, y, z);
		} else {
		    result = get(x - 1, y, z);
		}
		break;
	    }
	    case VS_RIGHT: {
		if (x == SIZE - 1) {
		    result = are.getVoxel(Vec3.copyAdd(position, 1, 0, 0), 0, y, z);
		} else {
		    result = get(x + 1, y, z);
		}
		break;
	    }
	    case VS_DOWN: {
		if (y == 0) {
		    result = are.getVoxel(Vec3.copyAdd(position, 0, -1, 0), x, SIZE - 1, z);
		} else {
		    result = get(x, y - 1, z);
		}
		break;
	    }
	    case VS_TOP: {
		if (y == SIZE - 1) {
		    result = are.getVoxel(Vec3.copyAdd(position, 0, 1, 0), x, 0, z);
		} else {
		    result = get(x, y + 1, z);
		}
		break;
	    }
	    case VS_BACK: {
		if (z == 0) {
		    result = are.getVoxel(Vec3.copyAdd(position, 0, 0, -1), x, y, SIZE - 1);
		} else {
		    result = get(x, y, z - 1);
		}
		break;
	    }
	    case VS_FRONT: {
		if (z == SIZE - 1) {
		    result = are.getVoxel(Vec3.copyAdd(position, 0, 0, 1), x, y, 0);
		} else {
		    result = get(x, y, z + 1);
		}
		break;
	    }
	}

	return result;
    }

    public void setAreLight(int x, int y, int z, byte direction, byte light) {
	switch (direction) {
	    case VS_LEFT: {
		if (x == 0) {
		    are.setLight(Vec3.copyAdd(position, -1, 0, 0), SIZE - 1, y, z, light);
		} else {
		    setLight(x - 1, y, z, light);
		}
		break;
	    }
	    case VS_RIGHT: {
		if (x == SIZE - 1) {
		    are.setLight(Vec3.copyAdd(position, 1, 0, 0), 0, y, z, light);
		} else {
		    setLight(x + 1, y, z, light);
		}
		break;
	    }
	    case VS_DOWN: {
		if (y == 0) {
		    are.setLight(Vec3.copyAdd(position, 0, -1, 0), x, SIZE - 1, z, light);
		} else {
		    setLight(x, y - 1, z, light);
		}
		break;
	    }
	    case VS_TOP: {
		if (y == SIZE - 1) {
		    are.setLight(Vec3.copyAdd(position, 0, 1, 0), x, 0, z, light);
		} else {
		    setLight(x, y + 1, z, light);
		}
		break;
	    }
	    case VS_BACK: {
		if (z == 0) {
		    are.setLight(Vec3.copyAdd(position, 0, 0, -1), x, y, SIZE - 1, light);
		} else {
		    setLight(x, y, z - 1, light);
		}
		break;
	    }
	    case VS_FRONT: {
		if (z == SIZE - 1) {
		    are.setLight(Vec3.copyAdd(position, 0, 0, 1), x, y, 0, light);
		} else {
		    setLight(x, y, z + 1, light);
		}
		break;
	    }
	}
    }

    public int getAreLight(int x, int y, int z, byte direction) {
	int result = 0;

	switch (direction) {
	    case VS_LEFT: {
		if (x == 0) {
		    result = are.getLight(Vec3.copyAdd(position, -1, 0, 0), SIZE - 1, y, z);
		} else {
		    result = getLight(x - 1, y, z);
		}
		break;
	    }
	    case VS_RIGHT: {
		if (x == SIZE - 1) {
		    result = are.getLight(Vec3.copyAdd(position, 1, 0, 0), 0, y, z);
		} else {
		    result = getLight(x + 1, y, z);
		}
		break;
	    }
	    case VS_DOWN: {
		if (y == 0) {
		    result = are.getLight(Vec3.copyAdd(position, 0, -1, 0), x, SIZE - 1, z);
		} else {
		    result = getLight(x, y - 1, z);
		}
		break;
	    }
	    case VS_TOP: {
		if (y == SIZE - 1) {
		    result = are.getLight(Vec3.copyAdd(position, 0, 1, 0), x, 0, z);
		} else {
		    result = getLight(x, y + 1, z);
		}
		break;
	    }
	    case VS_BACK: {
		if (z == 0) {
		    result = are.getLight(Vec3.copyAdd(position, 0, 0, -1), x, y, SIZE - 1);
		} else {
		    result = getLight(x, y, z - 1);
		}
		break;
	    }
	    case VS_FRONT: {
		if (z == SIZE - 1) {
		    result = are.getLight(Vec3.copyAdd(position, 0, 0, 1), x, y, 0);
		} else {
		    result = getLight(x, y, z + 1);
		}
		break;
	    }
	}

	return result;
    }

    public int getAreFinalLight(int x, int y, int z, byte direction) {
	int result = 0, light, sunLight;

	switch (direction) {
	    case VS_LEFT: {
		if (x == 0) {
		    light = are.getLight(Vec3.copyAdd(position, -1, 0, 0), SIZE - 1, y, z);
		    sunLight = are.getSunLight(Vec3.copyAdd(position, -1, 0, 0), SIZE - 1, y, z);
		} else {
		    light = getLight(x - 1, y, z);
		    sunLight = getSunLight(x - 1, y, z);
		}
		result = (sunLight > light) ? sunLight : light;
		break;
	    }
	    case VS_RIGHT: {
		if (x == SIZE - 1) {
		    light = are.getLight(Vec3.copyAdd(position, 1, 0, 0), 0, y, z);
		    sunLight = are.getSunLight(Vec3.copyAdd(position, 1, 0, 0), 0, y, z);
		} else {
		    light = getLight(x + 1, y, z);
		    sunLight = getSunLight(x + 1, y, z);
		}
		result = (sunLight > light) ? sunLight : light;
		break;
	    }
	    case VS_DOWN: {
		if (y == 0) {
		    light = are.getLight(Vec3.copyAdd(position, 0, -1, 0), x, SIZE - 1, z);
		    sunLight = are.getSunLight(Vec3.copyAdd(position, 0, -1, 0), x, SIZE - 1, z);
		} else {
		    light = getLight(x, y - 1, z);
		    sunLight = getSunLight(x, y - 1, z);
		}
		result = (sunLight > light) ? sunLight : light;
		break;
	    }
	    case VS_TOP: {
		if (y == SIZE - 1) {
		    light = are.getLight(Vec3.copyAdd(position, 0, 1, 0), x, 0, z);
		    sunLight = are.getSunLight(Vec3.copyAdd(position, 0, 1, 0), x, 0, z);
		} else {
		    light = getLight(x, y + 1, z);
		    sunLight = getSunLight(x, y + 1, z);
		}
		result = (sunLight > light) ? sunLight : light;
		break;
	    }
	    case VS_BACK: {
		if (z == 0) {
		    light = are.getLight(Vec3.copyAdd(position, 0, 0, -1), x, y, SIZE - 1);
		    sunLight = are.getSunLight(Vec3.copyAdd(position, 0, 0, -1), x, y, SIZE - 1);
		} else {
		    light = getLight(x, y, z - 1);
		    sunLight = getSunLight(x, y, z - 1);
		}
		result = (sunLight > light) ? sunLight : light;
		break;
	    }
	    case VS_FRONT: {
		if (z == SIZE - 1) {
		    light = are.getLight(Vec3.copyAdd(position, 0, 0, 1), x, y, 0);
		    sunLight = are.getSunLight(Vec3.copyAdd(position, 0, 0, 1), x, y, 0);
		} else {
		    light = getLight(x, y, z + 1);
		    sunLight = getSunLight(x, y, z + 1);
		}
		result = (sunLight > light) ? sunLight : light;
		break;
	    }
	}

	return result;
    }

    public void reset() {
	for (Voxel v : voxels) {
	    if (v == null) {
		continue;
	    }
	    v.setVisibleSides(VS_NONE);
	    v.setMergedSides(VS_NONE);
	}
	buffer = new ChunkSideBuffer();
	loaded = false;
    }

    public boolean load() {
	if (isLoaded() && voxels != null) {
	    reset();
	}
	checkVisibleFaces();
	mergeVisibleFaces();
	loaded = true;
	updateFlag = false;
	return (hasVertext());
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
	    buffer = new ChunkSideBuffer();
	}
	return load();
    }

    public void setup() {
	int px = position.getX();
	int py = position.getY();
	int pz = position.getZ();
	short type;
	for (int z = 0; z < SIZE; z++) {
	    for (int x = 0; x < SIZE; x++) {
		double noiseHeight = TerrainNoise.getHeight(x + px * SIZE, z + pz * SIZE);
		for (int y = SIZE - 1; y >= 0; y--) {
		    if (y + py * SIZE < noiseHeight && !(py == Are.HEIGHT - 1 && y == SIZE - 1)) {
			type = VT_STONE;
			visibleVoxels++;
		    } else {
			type = VT_NONE;
		    }
		    set(x, y, z, new Voxel(type));
		}
	    }
	}
    }

    void unload() {
	buffer = null;
    }

    boolean hasVisibleVoxel() {
	return visibleVoxels > 0;
    }

    public float[] getNormalList() {
	return buffer.getNormalList();
    }

    public int[] getIndexList() {
	return buffer.getIndexList();
    }

    public float[] getVertexList() {
	return (buffer == null) ? ChunkSideBuffer.EMPTY_FLOAT_BUFFER : buffer.get();
    }

    public float[] getTexColor() {
	return buffer.getTexColor();
    }

    public float[] getTextCoord() {
	return buffer.getTextCoord();
    }

    public float[] getTileCoord() {
	return buffer.getTileCoord();
    }

    private void checkVisibleFaces() {
	//Neighbor Voxel.
	Voxel nv;
	byte faces;

	for (int x = 0; x < SIZE; x++) {
	    for (int z = 0; z < SIZE; z++) {
		for (int y = 0; y < SIZE; y++) {
		    Voxel v = get(x, y, z);

		    if (v.getType() == VT_NONE) {
			continue;
		    }

		    faces = VS_NONE;

		    if (Voxel.isSpecial(v)) {
			int light = getLight(x, y, z);
			v.setVisibleSides(VS_ALL);
			v.setAllSidesLight((byte) light);
			continue;
		    }

		    if ((nv = getAreVoxel(x, y, z, VS_TOP)) == null) {
			faces |= VS_TOP;
		    } else if (!Voxel.isOpaque(nv)) {
			faces |= VS_TOP;
			v.setTopLight((byte) getAreFinalLight(x, y, z, VS_TOP));
		    }

		    if ((nv = getAreVoxel(x, y, z, VS_DOWN)) == null) {
			faces |= VS_DOWN;
		    } else if (!Voxel.isOpaque(nv)) {
			faces |= VS_DOWN;
			v.setDownLight((byte) getAreFinalLight(x, y, z, VS_DOWN));
		    }

		    if ((nv = getAreVoxel(x, y, z, VS_LEFT)) == null) {
			faces |= VS_LEFT;
		    } else if (!Voxel.isOpaque(nv)) {
			faces |= VS_LEFT;
			v.setLeftLight((byte) getAreFinalLight(x, y, z, VS_LEFT));
		    }
		    if ((nv = getAreVoxel(x, y, z, VS_RIGHT)) == null) {
			faces |= VS_RIGHT;
		    } else if (!Voxel.isOpaque(nv)) {
			faces |= VS_RIGHT;
			v.setRightLight((byte) getAreFinalLight(x, y, z, VS_RIGHT));
		    }
		    if ((nv = getAreVoxel(x, y, z, VS_FRONT)) == null) {
			faces |= VS_FRONT;
		    } else if (!Voxel.isOpaque(nv)) {
			faces |= VS_FRONT;
			v.setFrontLight((byte) getAreFinalLight(x, y, z, VS_FRONT));
		    }
		    if ((nv = getAreVoxel(x, y, z, VS_BACK)) == null) {
			faces |= VS_BACK;
		    } else if (!Voxel.isOpaque(nv)) {
			faces |= VS_BACK;
			v.setBackLight((byte) getAreFinalLight(x, y, z, VS_BACK));
		    }

		    v.setVisibleSides(faces);
		}
	    }
	}
    }

    private void mergeVisibleFaces() {
	mergeFrontFaces();
	mergeRightFaces();
	mergeBackFaces();
	mergeLeftFaces();
	mergeTopFaces();
	mergeDownFaces();
    }

    public boolean hasVertext() {
	return (buffer != null && !buffer.isEmpty());
    }

    /**
     * Merge all voxels with a front (Z+) face of same type and visible. This methods uses a simple logic: It just
     * iterate over X looking for neighbor voxels who can be merged (share the same type and have front face visible
     * also). When it reaches the at right most voxel, it start looking for neigor at top (Y+) and repeat the proccess
     * looking into right voxels until it reaches the right most and top most voxels, when it combine all those voxels
     * on a singles float array and returns it.
     *
     */
    private void mergeFrontFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y to keep track of merged face bounds.
	int ox, oy;
	boolean done;
	short currentType;
	byte currentLight;

	//When looking for front faces (Z+) we need to check X axis and later Y axis, becouse this, the iteration
	//occurs this way.
	for (int z = 0; z < SIZE; z++) {
	    for (int y = 0; y < SIZE; y++) {
		for (int x = 0; x < SIZE; x++) {

		    v = get(x, y, z);
		    currentType = v.getType();
		    currentLight = v.getFrontLight();

		    //If vox is invalid or is merged already, skip it;
		    if (v.getType() == VT_NONE || Voxel.isSpecial(v) || (v.getVisibleSides() & VS_FRONT) != VS_FRONT || (v.getMergedSides() & VS_FRONT) == VS_FRONT) {
			continue;
		    }

		    float[] vertices = new float[12];
		    ox = x;
		    oy = y;
		    done = false;

		    /*
		     * 
		     *      v7 +-------+ v6	y
		     *      / |      / |	|  
		     *   v3 +-------+v2|	|
		     *      |v4+-------+ v5	+-- X
		     *      | /     | /		 \
		     *      +-------+		  Z
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
			if (x == SIZE - 1) {
			    break;
			}

			//Move to the next voxel on X axis.
			nv = get(++x, y, z);

			//If the next voxel is invalid
			if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_FRONT) != VS_FRONT
				|| (nv.getMergedSides() & VS_FRONT) == VS_FRONT
				|| (nv.getType() != currentType)
				|| (nv.getFrontLight() != currentLight)) {
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
			if (y == SIZE - 1) {
			    //We are on the boundary of chunk. Stop it;
			    break;
			}

			y++;

			for (int k = ox; k <= x; k++) {
			    nv = get(k, y, z);
			    if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_FRONT) != VS_FRONT
				    || (nv.getMergedSides() & VS_FRONT) == VS_FRONT
				    || (nv.getType() != currentType)
				    || (nv.getFrontLight() != currentLight)) {
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

		    buffer.add(currentType, currentLight, VS_FRONT, vertices);
		    y = oy; //Set the y back to orignal location, so we can iterate over it again.
		}
	    }
	}
    }

    private void mergeBackFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int ox, oy;
	boolean done;
	short currentType;
	int currentLight;

	for (int z = 0; z < SIZE; z++) {
	    for (int y = 0; y < SIZE; y++) {
		for (int x = SIZE - 1; x > -1; x--) {

		    v = get(x, y, z);
		    currentType = v.getType();
		    currentLight = v.getBackLight();

		    //If vox is invalid or is merged already, skip it;
		    if (v.getType() == VT_NONE || Voxel.isSpecial(v) || (v.getVisibleSides() & VS_BACK) != VS_BACK || (v.getMergedSides() & VS_BACK) == VS_BACK) {
			continue;
		    }

		    float[] vertices = new float[12];
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

			if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_BACK) != VS_BACK
				|| (nv.getMergedSides() & VS_BACK) == VS_BACK
				|| (nv.getType() != currentType)
				|| (nv.getBackLight() != currentLight)) {
			    ++x; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (y == SIZE - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			y++;

			for (int k = ox; k >= x; k--) {
			    nv = get(k, y, z);
			    if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_BACK) != VS_BACK
				    || (nv.getMergedSides() & VS_BACK) == VS_BACK
				    || (nv.getType() != currentType)
				    || (nv.getBackLight() != currentLight)) {
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

		    buffer.add(currentType, currentLight, VS_BACK, vertices);
		    y = oy; //Set the y back to orignal location, so we can iterate over it again.
		}
	    }
	}
    }

    private void mergeTopFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int ox, oz;
	boolean done;
	short currentType;
	int currentLight;

	for (int y = SIZE - 1; y > -1; y--) {
	    for (int z = SIZE - 1; z > -1; z--) {
		for (int x = 0; x < SIZE; x++) {

		    v = get(x, y, z);
		    currentType = v.getType();
		    currentLight = v.getTopLight();

		    //If vox is invalid or is merged already, skip it;
		    if (v.getType() == VT_NONE || Voxel.isSpecial(v) || (v.getVisibleSides() & VS_TOP) != VS_TOP || (v.getMergedSides() & VS_TOP) == VS_TOP) {
			continue;
		    }

		    float[] vertices = new float[12];
		    ox = x;
		    oz = z;
		    done = false;

		    //The top face is composed of v3, v2, v6 and v7.
		    System.arraycopy(Voxel.v3(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (x == SIZE - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(++x, y, z);

			if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_TOP) != VS_TOP
				|| (nv.getMergedSides() & VS_TOP) == VS_TOP
				|| (nv.getType() != currentType
				|| (nv.getTopLight() != currentLight))) {
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
			    if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_TOP) != VS_TOP
				    || (nv.getMergedSides() & VS_TOP) == VS_TOP
				    || (nv.getType() != currentType
				    || (nv.getTopLight() != currentLight))) {
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

		    buffer.add(currentType, currentLight, VS_TOP, vertices);
		    z = oz; //Set the z back to orignal location, so we can iterate over it again.
		}
	    }
	}
    }

    private void mergeDownFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int ox, oz;
	boolean done;
	short currentType;
	int currentLight;

	for (int y = 0; y < SIZE; y++) {
	    for (int z = 0; z < SIZE; z++) {
		for (int x = 0; x < SIZE; x++) {

		    v = get(x, y, z);
		    currentType = v.getType();
		    currentLight = v.getDownLight();

		    //If vox is invalid or is merged already, skip it;
		    if (v.getType() == VT_NONE || Voxel.isSpecial(v) || (v.getVisibleSides() & VS_DOWN) != VS_DOWN || (v.getMergedSides() & VS_DOWN) == VS_DOWN) {
			continue;
		    }

		    float[] vertices = new float[12];
		    ox = x;
		    oz = z;
		    done = false;

		    //The down face is composed of v4, v5, v1 and v0.
		    System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (x == SIZE - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(++x, y, z);

			if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_DOWN) != VS_DOWN
				|| (nv.getMergedSides() & VS_DOWN) == VS_DOWN
				|| (nv.getType() != currentType
				|| (nv.getDownLight() != currentLight))) {
			    --x; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v5(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (z == SIZE - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			z++;

			for (int k = ox; k <= x; k++) {
			    nv = get(k, y, z);
			    if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_DOWN) != VS_DOWN
				    || (nv.getMergedSides() & VS_DOWN) == VS_DOWN
				    || (nv.getType() != currentType
				    || (nv.getDownLight() != currentLight))) {
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

		    buffer.add(currentType, currentLight, VS_DOWN, vertices);
		    z = oz; //Set the z back to orignal location, so we can iterate over it again.
		}
	    }
	}
    }

    private void mergeRightFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int oz, oy;
	boolean done;
	short currentType;
	int currentLight;

	for (int x = SIZE - 1; x > -1; x--) {
	    for (int y = 0; y < SIZE; y++) {
		for (int z = SIZE - 1; z > -1; z--) {

		    v = get(x, y, z);
		    currentType = v.getType();
		    currentLight = v.getRightLight();

		    //If vox is invalid or is merged already, skip it;
		    if (v.getType() == VT_NONE || Voxel.isSpecial(v) || (v.getVisibleSides() & VS_RIGHT) != VS_RIGHT || (v.getMergedSides() & VS_RIGHT) == VS_RIGHT) {
			continue;
		    }

		    float[] vertices = new float[12];
		    oz = z;
		    oy = y;
		    done = false;

		    //The right face is composed of v1, v5, v6 and v2.
		    System.arraycopy(Voxel.v1(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (z == 0) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(x, y, --z);

			if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_RIGHT) != VS_RIGHT
				|| (nv.getMergedSides() & VS_RIGHT) == VS_RIGHT
				|| (nv.getType() != currentType)
				|| (nv.getRightLight() != currentLight)) {
			    ++z; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v5(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (y == SIZE - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			y++;

			for (int k = oz; k >= z; k--) {
			    nv = get(x, y, k);
			    if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_RIGHT) != VS_RIGHT
				    || (nv.getMergedSides() & VS_RIGHT) == VS_RIGHT
				    || (nv.getType() != currentType)
				    || (nv.getRightLight() != currentLight)) {
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

		    buffer.add(currentType, currentLight, VS_RIGHT, vertices);
		    y = oy; //Set the y back to orignal location, so we can iterate over it again.
		}
	    }
	}
    }

    private void mergeLeftFaces() {
	//Voxel and Neightbor Voxel
	Voxel v, nv;

	//Merge origin x and y
	int oz, oy;
	boolean done;
	short currentType;
	int currentLight;

	for (int x = 0; x < SIZE; x++) {
	    for (int y = 0; y < SIZE; y++) {
		for (int z = 0; z < SIZE; z++) {

		    v = get(x, y, z);
		    currentType = v.getType();
		    currentLight = v.getLeftLight();

		    //If vox is invalid or is merged already, skip it;
		    if (v.getType() == VT_NONE || Voxel.isSpecial(v) || (v.getVisibleSides() & VS_LEFT) != VS_LEFT || (v.getMergedSides() & VS_LEFT) == VS_LEFT) {
			continue;
		    }

		    float[] vertices = new float[12];
		    oy = y;
		    oz = z;
		    done = false;

		    //The left face is composed of v4, v0, v3 and v7.
		    System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 0, 3);

		    while (true) {
			if (z == SIZE - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			//Move to the next voxel on X axis.
			nv = get(x, y, ++z);

			if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_LEFT) != VS_LEFT
				|| (nv.getMergedSides() & VS_LEFT) == VS_LEFT
				|| (nv.getType() != currentType
				|| (nv.getLeftLight() != currentLight))) {
			    --z; //Go back to previous voxel;
			    break;
			}

			//v = nv; //Set current voxel as next one, so repeat the check until end.
		    }

		    System.arraycopy(Voxel.v0(x, y, z), 0, vertices, 3, 3);

		    while (!done) {
			if (y == SIZE - 1) {
			    break; //We are on the boundary of chunk. Stop it;
			}

			y++;

			for (int k = oz; k <= z; k++) {
			    nv = get(x, y, k);
			    if (nv.getType() == VT_NONE || Voxel.isSpecial(v) || (nv.getVisibleSides() & VS_LEFT) != VS_LEFT
				    || (nv.getMergedSides() & VS_LEFT) == VS_LEFT
				    || (nv.getType() != currentType
				    || (nv.getLeftLight() != currentLight))) {
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

		    buffer.add(currentType, currentLight, VS_LEFT, vertices);
		    y = oy; //Set the y back to orignal location, so we can iterate over it again.
		}
	    }
	}
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

    @Override
    public int hashCode() {
	int hash = 7;
	hash = 89 * hash + Objects.hashCode(this.position);
	return hash;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final Chunk other = (Chunk) obj;
	if (!Objects.equals(this.position, other.position)) {
	    return false;
	}
	return true;
    }
}