package com.lagecompany.storage;

import static com.lagecompany.storage.Chunk.DATA_LENGTH;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * An Are is made of 10 chunks². It is an analogy to the real Are which is 10 m². It doesnt store any voxels, just
 * chunks references.
 *
 * @author afonsolage
 */
public class Are extends Thread {

    public static final int WIDTH = 16;
    public static final int HEIGHT = 64;
    public static final int LENGTH = 16;
    public static final int DATA_WIDTH = WIDTH * Chunk.WIDTH;
    public static final int DATA_HEIGHT = HEIGHT * Chunk.HEIGHT;
    public static final int DATA_LEGTH = LENGTH * Chunk.LENGTH;
    public static final int DATA_SIZE = WIDTH * HEIGHT * LENGTH;
    public static final int BATCH_SIZE = 1;
    public static final int IT_LOAD = 0;
    public static final int IT_UPDATE = 1;
    public static final int IT_ATTACH = 2;
    public static final int IT_DETACH = 3;
    public static final int IT_UNLOAD = 4;
    private final HashMap<Vec3, Chunk> chunkMap;
    private final HashMap<Vec3, Chunk> loadQueue;
    private final HashMap<Vec3, Chunk> updateQueue;
    private final HashMap<Vec3, Chunk> attachQueue;
    private final HashMap<Vec3, Chunk> detachQueue;
    private final HashMap<Vec3, Chunk> unloadQueue;
    private final LinkedList<AreMessage> messages;
    private boolean changed;
    private Vec3 position;
    private final Object lock;

    public Are() {
	/*
	 * The memory needed by this class will be (WIDTH * HEIGHT * LENGTH * (Chunk memory usage)) + 12;
	 * The memory usage will range from 64k ~ 448k ~ 640k, but this may be compressed later.
	 */
	chunkMap = new HashMap<>();
	loadQueue = new HashMap<>();
	updateQueue = new HashMap<>();
	attachQueue = new HashMap<>();
	detachQueue = new HashMap<>();
	unloadQueue = new HashMap<>();
	position = new Vec3();
	lock = new Object();
	messages = new LinkedList<>();
	this.setName("Are Thread");
    }

    @Override
    public void run() {
	while (!Thread.currentThread().isInterrupted()) {
	    boolean c;

	    c = processMessages() || unloadChunks() || loadChunks() || updateChunks();

	    synchronized (lock) {
		changed = c;
		if (!changed) {
		    try {
			lock.wait();
		    } catch (InterruptedException ex) {
			return;
		    }
		}
	    }
	}
    }

    public void init() {
	for (int x = 0; x < WIDTH; x++) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int z = 0; z < LENGTH; z++) {
		    Vec3 v = new Vec3(x, y, z);
		    Chunk c = new Chunk(this, v, Chunk.FLAG_LOAD);
		    set(v, c);
		    put(v, c, loadQueue);
		}
	    }
	}
	changeNotify();
    }

    public boolean isChanged() {
	return changed;
    }

    private boolean processMessages() {
	boolean result = false;
	synchronized (messages) {
	    while (!messages.isEmpty()) {
		AreMessage message = messages.poll();

		switch (message) {
		    case MOVE: {
			result = result || move((Vec3) message.getData());
			break;
		    }
		}

	    }
	}
	return result;
    }

    private boolean unloadChunks() {
	int worked = 0;
	boolean result = false;
	for (Iterator<Entry<Vec3, Chunk>> it = iterator(IT_UNLOAD); it.hasNext();) {
	    Entry<Vec3, Chunk> entry = it.next();
	    Vec3 v = entry.getKey();
	    Chunk c = entry.getValue();

	    if (c == null || c.getFlag() != Chunk.FLAG_UNLOAD) {
		it.remove();
		continue;
	    }

	    //Relevante change detected. Set changed to true.
	    result = true;

	    set(v, null);
	    c.unload();
	    c.setFlag(Chunk.FLAG_NONE);
	    it.remove();
//	    if (++worked == BATCH_SIZE) {
//		return true;
//	    }
	}
	return result;
    }

    private boolean loadChunks() {
	boolean result = false;
	for (Iterator<Entry<Vec3, Chunk>> it = iterator(IT_LOAD); it.hasNext();) {
	    Entry<Vec3, Chunk> entry = it.next();
	    Vec3 v = entry.getKey();
	    Chunk c = entry.getValue();

	    if (c == null || c.getFlag() != Chunk.FLAG_LOAD) {
		it.remove();
		continue;
	    }

	    //Relevante change detected. Set changed to true.
	    result = true;

	    if (c.load(v.getX(), v.getY(), v.getZ())) {
		c.setFlag(Chunk.FLAG_UPDATE);
		put(v, c, updateQueue);
	    } else {
		c.setFlag(Chunk.FLAG_UNLOAD);
		put(v, c, unloadQueue);
	    }
	    it.remove();
	}
	return result;
    }

    private void put(Vec3 v, Chunk c, HashMap<Vec3, Chunk> m) {
	synchronized (m) {
	    m.put(v, c);
	}
    }

    private void changeNotify() {
	synchronized (lock) {
	    lock.notify();
	}
    }

    private boolean updateChunks() {
	int worked = 0;
	boolean result = false;
	for (Iterator<Entry<Vec3, Chunk>> it = iterator(IT_UPDATE); it.hasNext();) {
	    Entry<Vec3, Chunk> entry = it.next();
	    Vec3 v = entry.getKey();
	    Chunk c = entry.getValue();

	    if (c == null || c.getFlag() != Chunk.FLAG_UPDATE) {
		it.remove();
		continue;
	    }

	    //Relevante change detected. Set changed to true.
	    result = true;
	    c.update();

	    //If chunk has no vertex, we dont need to attach it.
	    if (c.getVertexCount() == 0) {
		c.setFlag(Chunk.FLAG_NONE);
	    } else {
		c.setFlag(Chunk.FLAG_ATTACH);
		put(v, c, attachQueue);
	    }
	    it.remove();

//	    if (++worked == BATCH_SIZE) {
//		return true;
//	    }
	}
	return result;
    }

    protected Voxel getVoxel(Vec3 chunkPos, Vec3 voxelPos) {
	Chunk c = get(chunkPos);

	if (c == null) {
	    return null;
	}

	return c.get(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ());
    }

    public Chunk get(int x, int y, int z) {
	Vec3 v = new Vec3(x, y, z);
	return get(v);
    }

    public Chunk get(Vec3 v) {
	synchronized (chunkMap) {
	    return chunkMap.get(v);
	}
    }

    public void set(int x, int y, int z, Chunk c) {
	Vec3 v = new Vec3(x, y, z);
	chunkMap.put(v, c);
    }

    public void set(Vec3 v, Chunk c) {
	put(v, c, chunkMap);
    }

    public boolean move(Vec3 direction) {
	if (direction == Vec3.ZERO()) {
	    return false;
	}

	position.add(direction);

	if (direction.getX() > 0) {
	    moveRight();
	} else if (direction.getX() < 0) {
	    moveLeft();
	}

	if (direction.getY() > 0) {
	    moveUp();
	} else if (direction.getY() < 0) {
	    moveDown();
	}

	if (direction.getZ() > 0) {
	    moveFront();
	} else if (direction.getZ() < 0) {
	    moveBack();
	}

	return true;
    }

    /**
     * Move all chunks into X+ direction. The left most chunk is set to NULL and the right most is created. Since Are
     * position is the LEFT, BOTTOM, Back cornder, we need to remove the 0 X axis chunks and create a new one at
     * DATA_WIDTH + 1 X axis.
     */
    private void moveRight() {
	int rightMost = position.getX() + (WIDTH - 1);
	Vec3 v = new Vec3();
	for (int y = 0; y < HEIGHT; y++) {
	    v.setY(y + position.getY());
	    for (int z = 0; z < LENGTH; z++) {
		v.setZ(z + position.getZ());

		//Remove the left most.
		v.setX(position.getX() - 1);
		{
		    Chunk c = chunkMap.get(v);

		    if (c != null) {
			Vec3 chunkPos = new Vec3(v);
			if (c.getVertexCount() == 0) {
			    c.setFlag(Chunk.FLAG_UNLOAD);
			    put(chunkPos, c, unloadQueue);
			} else {
			    c.setFlag(Chunk.FLAG_DETACH);
			    put(chunkPos, c, detachQueue);
			}
		    }
		}

		//Add on right most.
		v.setX(rightMost);
		{
		    Vec3 chunkPos = new Vec3(v);
		    Chunk c = new Chunk(this, chunkPos, Chunk.FLAG_LOAD);
		    set(chunkPos, c);
		    put(chunkPos, c, loadQueue);
		}
	    }
	}
    }

    private void moveLeft() {
	for (int x = 0; x < DATA_WIDTH; x++) {
	    for (int y = 0; y < DATA_LENGTH; y++) {
		for (int z = 0; z < DATA_LEGTH; z++) {
		}
	    }
	}
    }

    private void moveFront() {
    }

    private void moveBack() {
    }

    private void moveUp() {
    }

    private void moveDown() {
    }

    private Iterator<Entry<Vec3, Chunk>> getIterator(HashMap<Vec3, Chunk> map) {
	HashMap<Vec3, Chunk> clone;
	synchronized (map) {
	    clone = (HashMap<Vec3, Chunk>) map.clone();
	    map.clear();
	}
	return clone.entrySet().iterator();
    }

    public Iterator<Entry<Vec3, Chunk>> iterator(int it) {
	switch (it) {
	    case IT_LOAD: {
		return getIterator(loadQueue);
	    }
	    case IT_UPDATE: {
		return getIterator(updateQueue);
	    }
	    case IT_ATTACH: {
		return getIterator(attachQueue);
	    }
	    case IT_DETACH: {
		return getIterator(detachQueue);
	    }
	    case IT_UNLOAD: {
		return getIterator(unloadQueue);
	    }
	    default:
		return null;
	}
    }

    public Vec3 getAbsoluteChunkPosition(Vec3 chunkPosition) {
	return new Vec3(chunkPosition.getX() * Chunk.WIDTH,
		chunkPosition.getY() * Chunk.HEIGHT,
		chunkPosition.getZ() * Chunk.LENGTH);
    }

    public void unload(Vec3 v, Chunk c) {
	put(v, c, unloadQueue);
	changeNotify();
    }

    public void postMessage(AreMessage message) {
	synchronized (messages) {
	    messages.push(message);
	}
	changeNotify();
    }

    public int getQueueSize(int it) {
	switch (it) {
	    case IT_LOAD: {
		return loadQueue.size();
	    }
	    case IT_UPDATE: {
		return updateQueue.size();
	    }
	    case IT_ATTACH: {
		return attachQueue.size();
	    }
	    case IT_DETACH: {
		return detachQueue.size();
	    }
	    case IT_UNLOAD: {
		return unloadQueue.size();
	    }
	    default:
		return 0;
	}
    }
}