package com.lagecompany.storage;

import static com.lagecompany.storage.Chunk.DATA_LENGTH;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private final HashMap<Vec3, Chunk> updatePending;
    private final LinkedBlockingQueue<AreMessage> mesherQueue;
    private final ConcurrentLinkedQueue<Chunk> rendererQueue;
    private boolean changed;
    private Vec3 position;

    public Are(ConcurrentLinkedQueue<Chunk> rendererQueue) {
	/*
	 * The memory needed by this class will be (WIDTH * HEIGHT * LENGTH * (Chunk memory usage)) + 12;
	 * The memory usage will range from 64k ~ 448k ~ 640k, but this may be compressed later.
	 */
	this.setName("Are Thread");

	position = new Vec3();

	chunkMap = new HashMap<>();
	updatePending = new HashMap<>();
	mesherQueue = new LinkedBlockingQueue<>();
	this.rendererQueue = rendererQueue;
    }

    @Override
    public void run() {
	while (!Thread.currentThread().isInterrupted()) {
	    try {
		AreMessage message = mesherQueue.take();
		switch (message.getType()) {
		    case MOVE: {
			move(message);
			break;
		    }
		    case PROCESS_CHUNK: {
			processChunk(message);
			break;
		    }
		}
	    } catch (InterruptedException ex) {
		return;
	    }
	}
    }

    private void processChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();

	if (c == null) {
	    return;
	}
	Vec3 v = c.getPosition();

	switch (c.getFlag()) {
	    case Chunk.FLAG_LOAD: {
		if (c.load(v.getX(), v.getY(), v.getZ())) {
		    c.setFlag(Chunk.FLAG_UPDATE);
		    offerChunk(c);
		} else {
		    unload(c.getPosition());
		}
		break;
	    }
	    case Chunk.FLAG_UPDATE: {
		c.update();
		updateNeigbor(c);

		//If chunk has no vertex, we dont need to attach it.
		if (c.getVertexCount() == 0) {
		    c.setFlag(Chunk.FLAG_NONE);
		} else {
		    c.setFlag(Chunk.FLAG_ATTACH);
		    rendererQueue.offer(c);
		}
		break;
	    }
	}
    }

    public void init() {
	for (int x = 0; x < WIDTH; x++) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int z = 0; z < LENGTH; z++) {
		    Vec3 v = new Vec3(true, x, y, z);
		    Chunk c = new Chunk(this, v, Chunk.FLAG_LOAD);
		    set(v, c);
		    offerChunk(c);
		}
	    }
	}
    }

    public boolean isChanged() {
	return changed;
    }

    protected Voxel getVoxel(Vec3 chunkPos, Vec3 voxelPos) {
	Chunk c = get(chunkPos);

	if (c == null || c.getFlag() == Chunk.FLAG_DETACH) {
	    return null;
	}

	return c.get(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ());
    }

    public Chunk get(int x, int y, int z) {
	Vec3 v = new Vec3(x, y, z);
	return get(v);
    }

    public Chunk get(Vec3 v) {
	return chunkMap.get(v);
    }

    public void set(int x, int y, int z, Chunk c) {
	Vec3 v = new Vec3(x, y, z);
	chunkMap.put(v, c);
    }

    public void set(Vec3 v, Chunk c) {
	if (c == null) {
	    chunkMap.remove(v);
	} else {
	    chunkMap.put(v, c);
	}
    }

    public boolean move(AreMessage message) {
	Vec3 direction = (Vec3) message.getData();

	if (direction == null || direction.equals(Vec3.ZERO())) {
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
	for (int y = 0; y < HEIGHT; y++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the left most.

		//Get the first Chunk on X axis and tell him to update again.
		Chunk c = get(position.copy().add(0, y, z));
		if (c != null) {
		    updatePending.put(c.getPosition(), c);
		}

		//Get the first Chunk on X axis and tell him to update again.
		c = get(position.copy().add(WIDTH - 2, y, z));
		if (c != null) {
		    updatePending.put(c.getPosition(), c);
		}

		c = get(position.copy().add(-1, y, z));
		if (c != null) {
		    if (c.getVertexCount() == 0) {
			unload(c.getPosition());
		    } else {
			c.unload();
			c.setFlag(Chunk.FLAG_DETACH);
			rendererQueue.offer(c);
		    }
		}

		//Add new chunks at right most of Are.
		Vec3 chunkPos = new Vec3(true, position.copy().add(WIDTH - 1, y, z));
		c = new Chunk(this, chunkPos, Chunk.FLAG_LOAD);
		set(chunkPos, c);
		offerChunk(c);

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

    public Vec3 getAbsoluteChunkPosition(Vec3 chunkPosition) {
	return new Vec3(chunkPosition.getX() * Chunk.WIDTH,
		chunkPosition.getY() * Chunk.HEIGHT,
		chunkPosition.getZ() * Chunk.LENGTH);
    }

    public void unload(Vec3 v) {
	//System.out.println(String.format("Unloading chunk at %s", v));
	Chunk c = chunkMap.remove(v);
	c.setFlag(Chunk.FLAG_NONE);
	c.unload();
	updateNeigbor(c);
    }

    public void postMessage(AreMessage message) {
	if (!mesherQueue.offer(message)) {
	    System.out.println(String.format("Failed to add message: %s", message));
	}
    }

    public Vec3 getPosition() {
	return position;
    }

    private void offerChunk(Chunk c) {
	AreMessage message = new AreMessage(AreMessage.AreMessageType.PROCESS_CHUNK, c);
	postMessage(message);
    }

    private void updateNeigbor(Chunk c) {
	Vec3 v = c.getPosition();

	Chunk nc;
	if ((nc = updatePending.get(v.copy().add(-1, 0, 0))) != null) {
	    nc.setFlag(Chunk.FLAG_UPDATE);
	    offerChunk(nc);
	    updatePending.remove(nc.getPosition());
	}
	if ((nc = updatePending.get(v.copy().add(1, 0, 0))) != null) {
	    nc.setFlag(Chunk.FLAG_UPDATE);
	    offerChunk(nc);
	    updatePending.remove(nc.getPosition());
	}
	if ((nc = updatePending.get(v.copy().add(0, -1, 0))) != null) {
	    nc.setFlag(Chunk.FLAG_UPDATE);
	    offerChunk(nc);
	    updatePending.remove(nc.getPosition());
	}
	if ((nc = updatePending.get(v.copy().add(0, 1, 0))) != null) {
	    nc.setFlag(Chunk.FLAG_UPDATE);
	    offerChunk(nc);
	    updatePending.remove(nc.getPosition());
	}
	if ((nc = updatePending.get(v.copy().add(0, 0, -1))) != null) {
	    nc.setFlag(Chunk.FLAG_UPDATE);
	    offerChunk(nc);
	    updatePending.remove(nc.getPosition());
	}
	if ((nc = updatePending.get(v.copy().add(0, 0, 1))) != null) {
	    nc.setFlag(Chunk.FLAG_UPDATE);
	    offerChunk(nc);
	    updatePending.remove(nc.getPosition());
	}

    }
}