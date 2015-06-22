package com.lagecompany.storage;

import com.lagecompany.storage.AreMessage.AreMessageType;
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
    public static final int DATA_LENGHT = LENGTH * Chunk.LENGTH;
    private final HashMap<Vec3, Chunk> chunkMap;
    private final LinkedBlockingQueue<AreMessage> mesherQueue;
    private final ConcurrentLinkedQueue<AreMessage> rendererQueue;
    private boolean changed;
    private Vec3 position;

    public Are(ConcurrentLinkedQueue<AreMessage> rendererQueue) {
	/*
	 * The memory needed by this class will be (WIDTH * HEIGHT * LENGTH * (Chunk memory usage)) + 12;
	 * The memory usage will range from 64k ~ 448k ~ 640k, but this may be compressed later.
	 */
	this.setName("Are Thread");

	position = new Vec3();

	chunkMap = new HashMap<>();
	mesherQueue = new LinkedBlockingQueue<>();
	this.rendererQueue = rendererQueue;
    }

    @Override
    public void run() {
	while (!Thread.currentThread().isInterrupted()) {
	    try {
		AreMessage message = mesherQueue.take();
		switch (message.getType()) {
		    case ARE_MOVE: {
			move(message);
			break;
		    }
		    case CHUNK_UNLOAD: {
			unloadChunk(message);
			break;
		    }
		    case CHUNK_SETUP: {
			setupChunk(message);
			break;
		    }
		    case CHUNK_LOAD: {
			loadChunk(message);
			break;
		    }
		    case CHUNK_RELOAD: {
			reloadChunk(message);
			break;
		    }
		    default: {
			System.out.println(String.format("Invalid are message received: %s", message));
		    }
		}
	    } catch (InterruptedException ex) {
		return;
	    }
	}
    }

    private void unloadChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();

	chunkMap.remove(c.getPosition());
	c.unload();
    }

    private void setupChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();

	if (c.setup()) {
	    message.setType(AreMessageType.CHUNK_LOAD);
	} else {
	    message.setType(AreMessageType.CHUNK_UNLOAD);
	}

	postMessage(message);
    }

    private void loadChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();
	if (c.load()) {
	    message.setType(AreMessageType.CHUNK_ATTACH);
	    rendererQueue.offer(message);
	}
    }

    private void reloadChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();
	if (c.load()) {
	    message.setType(AreMessageType.CHUNK_ATTACH);
	    rendererQueue.offer(message);
	} else {
	    message.setType(AreMessageType.CHUNK_DETACH);
	    rendererQueue.offer(message);
	}
    }

    public void init() {
	for (int x = 0; x < WIDTH; x++) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int z = 0; z < LENGTH; z++) {
		    Vec3 v = new Vec3(x, y, z);
		    Chunk c = new Chunk(this, v);
		    set(v, c);
		    postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));
		}
	    }
	}
    }

    public boolean isChanged() {
	return changed;
    }

    protected Voxel getVoxel(Vec3 chunkPos, int x, int y, int z) {
	Chunk c = get(chunkPos);

	if (c == null) {
	    return null;
	}

	return c.get(x, y, z);
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
	int boundBegin = 0;
	int boundEnd = WIDTH - 1;
	for (int y = 0; y < HEIGHT; y++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the left most and detach it from scene.
		Chunk c = get(position.copy().add(boundBegin - 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c));
		    rendererQueue.offer(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new left border.
		c = get(position.copy().add(boundBegin, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_RELOAD, c));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd - 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_RELOAD, c));
		}
	    }
	}
    }

    private void moveLeft() {
	int boundBegin = WIDTH - 1;
	int boundEnd = 0;
	for (int y = 0; y < HEIGHT; y++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the left most and detach it from scene.
		Chunk c = get(position.copy().add(boundBegin + 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c));
		    rendererQueue.offer(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new left border.
		c = get(position.copy().add(boundBegin, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_RELOAD, c));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd + 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_RELOAD, c));
		}
	    }
	}
    }

    private void moveFront() {
	int boundBegin = 0;
	int boundEnd = LENGTH - 1;
	for (int y = 0; y < HEIGHT; y++) {
	    for (int x = 0; x < WIDTH; x++) {
		//Remove the back most and detach it from scene.
		Chunk c = get(position.copy().add(x, y, boundBegin - 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c));
		    rendererQueue.offer(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, y, boundBegin));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_RELOAD, c));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd - 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_RELOAD, c));
		}
	    }
	}
    }

    private void moveBack() {
    }

    private void moveUp() {
    }

    private void moveDown() {
    }

    public Vec3 getAbsoluteChunkPosition(Vec3 chunkPosition) {
	return new Vec3((chunkPosition.getX() * Chunk.WIDTH) - (DATA_WIDTH / 2),
		chunkPosition.getY() * Chunk.HEIGHT, //TODO: Add "- (DATA_HEIGHT / 2)"
		chunkPosition.getZ() * Chunk.LENGTH - (DATA_LENGHT / 2));
    }

    public void postMessage(AreMessage message) {
	if (!mesherQueue.offer(message)) {
	    System.out.println(String.format("Failed to add message: %s", message));
	}
    }

    public Vec3 getPosition() {
	return position;
    }

    public Vec3 setPosition(int x, int y, int z) {
	return position = new Vec3(x, y, z);
    }
}