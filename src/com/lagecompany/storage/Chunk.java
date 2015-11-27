package com.lagecompany.storage;

import com.lagecompany.storage.voxel.Voxel;
import static com.lagecompany.storage.voxel.Voxel.VS_BACK;
import static com.lagecompany.storage.voxel.Voxel.VS_DOWN;
import static com.lagecompany.storage.voxel.Voxel.VS_FRONT;
import static com.lagecompany.storage.voxel.Voxel.VS_LEFT;
import static com.lagecompany.storage.voxel.Voxel.VS_RIGHT;
import static com.lagecompany.storage.voxel.Voxel.VS_TOP;
import static com.lagecompany.storage.voxel.Voxel.VT_NONE;
import static com.lagecompany.storage.voxel.Voxel.VT_STONE;
import com.lagecompany.storage.voxel.VoxelReference;
import com.lagecompany.util.TerrainNoise;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Chunk {

    public static final int SIZE = 16;
    public static final int SIZE_SHIFT_Y = 4;
    public static final int SIZE_SHIFT_X = 8;

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
    private ChunkSideBuffer buffer;
    private ChunkBuffer chunkBuffer;
    private final Queue<VoxelReference> sunlightPropagationQueue;
    private boolean loaded;
    private boolean updateFlag;
    private boolean directSunLight;
    private int visibleVoxels;
    private final Are are;
    private final Vec3 position;
    private final String name;
    private final Lock lock;

    public Chunk(Are are, Vec3 position) {
        this.are = are;
        this.position = position;
        this.name = "Chunk " + position.toString();
        this.lock = new ReentrantLock(true);
        this.buffer = new ChunkSideBuffer();
        this.updateFlag = false;
        this.chunkBuffer = new ChunkBuffer();
        this.sunlightPropagationQueue = new LinkedList<>();
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

    public VoxelReference get(int x, int y, int z) {
        return (chunkBuffer == null) ? null : chunkBuffer.get(x, y, z);
    }

    public VoxelReference get(Vec3 v) {
        return (chunkBuffer == null) ? null : chunkBuffer.get(v.x, v.y, v.z);
    }

    void set(VoxelReference newVoxel) {
        chunkBuffer.set(newVoxel);
    }

    public void set(int x, int y, int z, short type) {
        chunkBuffer.set(x, y, z, type);
    }

    public VoxelReference getAreVoxel(int x, int y, int z, byte direction) {
        VoxelReference result = null;

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
                    are.setLight(position.copy().add(-1, 0, 0), SIZE - 1, y, z, light);
                } else {
                    get(x - 1, y, z).setLight(light);
                }
                break;
            }
            case VS_RIGHT: {
                if (x == SIZE - 1) {
                    are.setLight(position.copy().add(1, 0, 0), 0, y, z, light);
                } else {
                    get(x + 1, y, z).setLight(light);
                }
                break;
            }
            case VS_DOWN: {
                if (y == 0) {
                    are.setLight(position.copy().add(0, -1, 0), x, SIZE - 1, z, light);
                } else {
                    get(x, y - 1, z).setLight(light);
                }
                break;
            }
            case VS_TOP: {
                if (y == SIZE - 1) {
                    are.setLight(position.copy().add(0, 1, 0), x, 0, z, light);
                } else {
                    get(x, y + 1, z).setLight(light);
                }
                break;
            }
            case VS_BACK: {
                if (z == 0) {
                    are.setLight(position.copy().add(0, 0, -1), x, y, SIZE - 1, light);
                } else {
                    get(x, y, z - 1).setLight(light);
                }
                break;
            }
            case VS_FRONT: {
                if (z == SIZE - 1) {
                    are.setLight(position.copy().add(0, 0, 1), x, y, 0, light);
                } else {
                    get(x, y, z + 1).setLight(light);
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
                    result = are.getLight(position.copy().add(-1, 0, 0), SIZE - 1, y, z);
                } else {
                    result = get(x - 1, y, z).getLight();
                }
                break;
            }
            case VS_RIGHT: {
                if (x == SIZE - 1) {
                    result = are.getLight(position.copy().add(1, 0, 0), 0, y, z);
                } else {
                    result = get(x + 1, y, z).getLight();
                }
                break;
            }
            case VS_DOWN: {
                if (y == 0) {
                    result = are.getLight(position.copy().add(0, -1, 0), x, SIZE - 1, z);
                } else {
                    result = get(x, y - 1, z).getLight();
                }
                break;
            }
            case VS_TOP: {
                if (y == SIZE - 1) {
                    result = are.getLight(position.copy().add(0, 1, 0), x, 0, z);
                } else {
                    result = get(x, y + 1, z).getLight();
                }
                break;
            }
            case VS_BACK: {
                if (z == 0) {
                    result = are.getLight(position.copy().add(0, 0, -1), x, y, SIZE - 1);
                } else {
                    result = get(x, y, z - 1).getLight();
                }
                break;
            }
            case VS_FRONT: {
                if (z == SIZE - 1) {
                    result = are.getLight(position.copy().add(0, 0, 1), x, y, 0);
                } else {
                    result = get(x, y, z + 1).getLight();
                }
                break;
            }
        }

        return result;
    }

    public void reset() {
        chunkBuffer.resetMergedVisible();
        buffer = new ChunkSideBuffer();
        loaded = false;
    }

    public boolean load() {
        if (isLoaded() && buffer != null) {
            reset();
        }
        checkVisibleFaces();
        mergeVisibleFaces();
        loaded = true;
        updateFlag = false;
        return (hasVertext());
    }

    public boolean update() {
        if (isLoaded() && buffer != null) {
            reset();
        }
        return load();
    }

    public void setup() {
        int px = position.x;
        int py = position.y;
        int pz = position.z;

        short type;

        for (int z = 0; z < SIZE; z++) {
            for (int x = 0; x < SIZE; x++) {
                double noiseHeight = TerrainNoise.getHeight(x + px * SIZE, z + pz * SIZE);
                for (int y = SIZE - 1; y >= 0; y--) {
                    if (y + py * SIZE < noiseHeight && !(py == Are.HEIGHT - 1 && y == SIZE - 1)) {
                        type = VT_STONE;
                        visibleVoxels++;
                        set(x, y, z, type);
                    }
                }
            }
        }
        updateFlag = true;
    }

    /**
     * Compute all voxels that receive direct sunlight. This method uses a
     * TOP-DOWN approach and propagate light downwards until it finds an opaque
     * voxel. Due to performance issues, other method will reflect sun light.
     */
    public void computeSunlight() {
        Chunk top = are.get(position.x, position.y + 1, position.z);

        Queue<VoxelReference> lightQueue = new LinkedList<>();
        VoxelReference voxel;

        if (top == null) {
            //If there is no top chunk, this means we are receiving sunlight directly.
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    voxel = get(x, Chunk.SIZE - 1, z);

                    if (voxel.isTransparent()) {
                        voxel.setSunLight(Voxel.SUN_LIGHT);
                        lightQueue.add(get(voxel.x, voxel.y - 1, voxel.z));
                    }
                }
            }
        } else {
            //Else, check if top chunk has sunlight on voxels at y = 0; 
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    voxel = get(x, Chunk.SIZE - 1, z);

                    if (voxel.isTransparent() && top.get(x, 0, z).getSunLight() == Voxel.SUN_LIGHT) {
                        voxel.setSunLight(Voxel.SUN_LIGHT);
                        lightQueue.add(get(voxel.x, voxel.y - 1, voxel.z));
                    }
                }
            }
        }

        //Lets iterate over light bfs queue;
        while (!lightQueue.isEmpty()) {
            //Get next queue element;
            voxel = lightQueue.poll();

            if (voxel.isTransparent() && voxel.getSunLight() != Voxel.SUN_LIGHT) {
                //Set sunlight and if there is more voxels bellow it, propagate also.
                voxel.setSunLight(Voxel.SUN_LIGHT);
                if (voxel.y > 0) {
                    lightQueue.add(get(voxel.x, voxel.y - 1, voxel.z));
                }
            }
        }
    }

    /**
     * Compute sun light reflection on voxels that are surrounded by direct sun
     * light voxels. This method first search for all voxels that have a
     * neighbor with direct sun light (sunLight = Voxel.SUN_LIGHT), then it set
     * it's own sun light power (Voxel.SUN_LIGHT - 1 if the sunlight isn't above
     * it) and is added to sunlightPropagationQueue.
     */
    public void computeSunlightReflection() {
        Vec3 tmpVec = new Vec3();

        VoxelReference voxel, neighbor;
        Chunk tmpChunk;

        //Search for voxels surrounded by sunLight (15) and propagate this light.
        for (int y = 0; y < Chunk.SIZE; y++) {
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    voxel = get(x, y, z);

                    //If this voxel is transparent and haven't the max sunLight
                    if (voxel.isTransparent() && voxel.getSunLight() < Voxel.SUN_LIGHT - 1) {
                        //Look in all directions for a voxel with direct sun light (sunLight = Voxel.SUN_LIGHT)
                        for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
                            tmpVec.set(voxel.x + dir.x, voxel.y + dir.y, voxel.z + dir.z);
                            tmpChunk = are.validateChunkAndVoxel(this, tmpVec);

                            if (tmpChunk == null) {
                                continue;
                            }

                            neighbor = tmpChunk.get(tmpVec);

                            //If this voxel has direct sun light
                            if (neighbor.isTransparent() && neighbor.getSunLight() == Voxel.SUN_LIGHT) {
                                //Set sunLight on current voxel and add to propagation queue. If the direct sun light voxel is upwards, then
                                //this voxel receives direct sun light, else Voxel.SUN_LIGHT - 1.
                                voxel.setSunLight((byte) ((dir == Vec3.UP) ? Voxel.SUN_LIGHT : Voxel.SUN_LIGHT - 1));
                                sunlightPropagationQueue.add(voxel);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Propagate sun light across neigbor voxels. This method uses a Passive
     * Flood Fill algorithm.
     */
    public void propagateSunlight() {
        int lightLevel;
        byte propagatedLightLevel;

        VoxelReference neighborVoxel, voxel;
        Vec3 tmpVec = new Vec3();
        Chunk tmpChunk;

        while (!sunlightPropagationQueue.isEmpty()) {
            voxel = sunlightPropagationQueue.poll();

            lightLevel = voxel.getSunLight();

            //If current light level is equals or less then 1, we can't propagate light.
            if (lightLevel <= 1) {
                continue;
            }

            //Look for all neighbors and check if the light can be propagated.
            for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
                tmpVec.set(voxel.x + dir.x, voxel.y + dir.y, voxel.z + dir.z);
                tmpChunk = are.validateChunkAndVoxel(this, tmpVec);

                neighborVoxel = tmpChunk.get(tmpVec);

                //If current light level is direct sun light (Voxel.SUN_LIGHT) and we are propagating downwards, we don't decrease light level.
                propagatedLightLevel = (byte) ((dir == Vec3.DOWN && lightLevel == Voxel.SUN_LIGHT) ? Voxel.SUN_LIGHT : (lightLevel - 1));

                //If this neighbor is transparent and have a light power lower then our, lets propagate it.
                if (neighborVoxel.isTransparent() && neighborVoxel.getSunLight() < propagatedLightLevel) {
                    
                    //If the chunk isn't the same as it and isn't flagged to update, let's request it to update.
                    if (tmpChunk != this && !tmpChunk.isFlaggedToUpdate()) {
                        are.requestChunkUpdate(tmpChunk);
                    }
                    
                    neighborVoxel.setSunLight(propagatedLightLevel);
                    //We may propagate light only if it's greater then 1.
                    if (lightLevel > 1) {
                        tmpChunk.sunlightPropagationQueue.add(neighborVoxel);
                    }
                }
            }
        }
    }

    void unload() {
        chunkBuffer = null;
        buffer = null;
    }

    public boolean hasVisibleVoxel() {
        return visibleVoxels > 0;
    }

    public boolean hasDirectSunLight() {
        return directSunLight;
    }

    public void setDirectSunLight(boolean directSunLight) {
        this.directSunLight = directSunLight;
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
        VoxelReference neighborVoxel, voxel;
        byte faces;

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y < SIZE; y++) {
                    voxel = get(x, y, z);

                    if (voxel.getType() == VT_NONE) {
                        continue;
                    }

                    neighborVoxel = getAreVoxel(x, y, z, VS_TOP);
                    if (neighborVoxel == null) {
                        voxel.setSideVisible(Voxel.TOP);
                    } else if (neighborVoxel.isTransparent()) {
                        voxel.setSideVisible(Voxel.TOP);
                        voxel.setSideLight(neighborVoxel.getFinalLight(), Voxel.TOP);
                    }

                    neighborVoxel = getAreVoxel(x, y, z, VS_DOWN);
                    if (neighborVoxel == null) {
                        voxel.setSideVisible(Voxel.DOWN);
                    } else if (neighborVoxel.isTransparent()) {
                        voxel.setSideVisible(Voxel.DOWN);
                        voxel.setSideLight(neighborVoxel.getFinalLight(), Voxel.DOWN);
                    }

                    neighborVoxel = getAreVoxel(x, y, z, VS_LEFT);
                    if (neighborVoxel == null) {
                        voxel.setSideVisible(Voxel.LEFT);
                    } else if (neighborVoxel.isTransparent()) {
                        voxel.setSideVisible(Voxel.LEFT);
                        voxel.setSideLight(neighborVoxel.getFinalLight(), Voxel.LEFT);
                    }

                    neighborVoxel = getAreVoxel(x, y, z, VS_RIGHT);
                    if (neighborVoxel == null) {
                        voxel.setSideVisible(Voxel.RIGHT);
                    } else if (neighborVoxel.isTransparent()) {
                        voxel.setSideVisible(Voxel.RIGHT);
                        voxel.setSideLight(neighborVoxel.getFinalLight(), Voxel.RIGHT);
                    }

                    neighborVoxel = getAreVoxel(x, y, z, VS_FRONT);
                    if (neighborVoxel == null) {
                        voxel.setSideVisible(Voxel.FRONT);
                    } else if (neighborVoxel.isTransparent()) {
                        voxel.setSideVisible(Voxel.FRONT);
                        voxel.setSideLight(neighborVoxel.getFinalLight(), Voxel.FRONT);
                    }

                    neighborVoxel = getAreVoxel(x, y, z, VS_BACK);
                    if (neighborVoxel == null) {
                        voxel.setSideVisible(Voxel.BACK);
                    } else if (neighborVoxel.isTransparent()) {
                        voxel.setSideVisible(Voxel.BACK);
                        voxel.setSideLight(neighborVoxel.getFinalLight(), Voxel.BACK);
                    }
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
     * Merge all voxels with a front (Z+) face of same type and visible. This
     * methods uses a simple logic: It just iterate over X looking for neighbor
     * voxels who can be merged (share the same type and have front face visible
     * also). When it reaches the at right most voxel, it start looking for
     * neigor at top (Y+) and repeat the proccess looking into right voxels
     * until it reaches the right most and top most voxels, when it combine all
     * those voxels on a singles float array and returns it.
     *
     */
    private void mergeFrontFaces() {
        //Voxel and Neightbor Voxel
        VoxelReference v, nv;

        //Merge origin x and y to keep track of merged face bounds.
        int ox, oy;
        boolean done;
        short currentType;
        byte currentLight;

        int side = Voxel.FRONT;

        //When looking for front faces (Z+) we need to check X axis and later Y axis, becouse this, the iteration
        //occurs this way.
        for (int z = 0; z < SIZE; z++) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {

                    v = get(x, y, z);
                    currentType = v.getType();
                    currentLight = v.getSideLight(side);

                    //If vox is invalid or is merged already, skip it;
                    if (v.getType() == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)) {
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
                        if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                || !nv.isSideVisible(side)
                                || (nv.getType() != currentType)
                                || (nv.getSideLight(side) != currentLight)) {
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
                            if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                    || !nv.isSideVisible(side)
                                    || (nv.getType() != currentType)
                                    || (nv.getSideLight(side) != currentLight)) {
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
                            get(a, b, z).setSideMerged(side);
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
        VoxelReference v, nv;

        //Merge origin x and y
        int ox, oy;
        boolean done;
        short currentType;
        int currentLight;

        int side = Voxel.BACK;

        for (int z = 0; z < SIZE; z++) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = SIZE - 1; x > -1; x--) {

                    v = get(x, y, z);
                    currentType = v.getType();
                    currentLight = v.getSideLight(side);

                    //If vox is invalid or is merged already, skip it;
                    if (v.getType() == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)) {

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

                        if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                || !nv.isSideVisible(side)
                                || (nv.getType() != currentType)
                                || (nv.getSideLight(side) != currentLight)) {
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
                            if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                    || !nv.isSideVisible(side)
                                    || (nv.getType() != currentType)
                                    || (nv.getSideLight(side) != currentLight)) {
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
                            get(a, b, z).setSideMerged(side);
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
        VoxelReference v, nv;

        //Merge origin x and y
        int ox, oz;
        boolean done;
        short currentType;
        int currentLight;

        int side = Voxel.TOP;

        for (int y = SIZE - 1; y > -1; y--) {
            for (int z = SIZE - 1; z > -1; z--) {
                for (int x = 0; x < SIZE; x++) {

                    v = get(x, y, z);
                    currentType = v.getType();

                    //If vox is invalid or is merged already, skip it;
                    if (v.getType() == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)) {
                        continue;
                    }

                    currentLight = v.getSideLight(side);

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

                        if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                || !nv.isSideVisible(side)
                                || (nv.getType() != currentType)
                                || (nv.getSideLight(side) != currentLight)) {
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
                            if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                    || !nv.isSideVisible(side)
                                    || (nv.getType() != currentType)
                                    || (nv.getSideLight(side) != currentLight)) {
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
                            get(a, y, b).setSideMerged(side);
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
        VoxelReference v, nv;

        //Merge origin x and y
        int ox, oz;
        boolean done;
        short currentType;
        int currentLight;

        int side = Voxel.DOWN;

        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {

                    v = get(x, y, z);
                    currentType = v.getType();
                    currentLight = v.getSideLight(side);

                    //If vox is invalid or is merged already, skip it;
                    if (v.getType() == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)) {
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

                        if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                || !nv.isSideVisible(side)
                                || (nv.getType() != currentType)
                                || (nv.getSideLight(side) != currentLight)) {
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
                            if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                    || !nv.isSideVisible(side)
                                    || (nv.getType() != currentType)
                                    || (nv.getSideLight(side) != currentLight)) {
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
                            get(a, y, b).setSideMerged(side);
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
        VoxelReference v, nv;

        //Merge origin x and y
        int oz, oy;
        boolean done;
        short currentType;
        int currentLight;

        int side = Voxel.RIGHT;

        for (int x = SIZE - 1; x > -1; x--) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = SIZE - 1; z > -1; z--) {

                    v = get(x, y, z);
                    currentType = v.getType();
                    currentLight = v.getSideLight(side);

                    //If vox is invalid or is merged already, skip it;
                    if (v.getType() == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)) {
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

                        if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                || !nv.isSideVisible(side)
                                || (nv.getType() != currentType)
                                || (nv.getSideLight(side) != currentLight)) {
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
                            if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                    || !nv.isSideVisible(side)
                                    || (nv.getType() != currentType)
                                    || (nv.getSideLight(side) != currentLight)) {
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
                            get(x, b, a).setSideMerged(side);
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
        VoxelReference v, nv;

        //Merge origin x and y
        int oz, oy;
        boolean done;
        short currentType;
        int currentLight;

        int side = Voxel.LEFT;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {

                    v = get(x, y, z);
                    currentType = v.getType();
                    currentLight = v.getSideLight(side);

                    //If vox is invalid or is merged already, skip it;
                    if (v.getType() == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)) {
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

                        if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                || !nv.isSideVisible(side)
                                || (nv.getType() != currentType)
                                || (nv.getSideLight(side) != currentLight)) {
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
                            if (nv.getType() == VT_NONE || nv.isSideMerged(side)
                                    || !nv.isSideVisible(side)
                                    || (nv.getType() != currentType)
                                    || (nv.getSideLight(side) != currentLight)) {
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
                            get(x, b, a).setSideMerged(side);
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

    public void addSunLightPropagationQueue(VoxelReference voxel) {
        this.sunlightPropagationQueue.add(voxel);
    }
}
