package com.lagecompany.storage;

import com.lagecompany.storage.AreMessage.AreMessageType;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

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
    private static Are instance;
    private final HashMap<Vec3, Chunk> chunkMap;
    private final BlockingQueue<AreMessage> actionQueue;
    private final ConcurrentLinkedQueue<AreMessage> detachQueue;
    private final ConcurrentLinkedQueue<AreMessage> unloadQueue;
    private final ConcurrentLinkedQueue<AreMessage> setupQueue;
    private final ConcurrentLinkedQueue<AreMessage> loadQueue;
    private final ConcurrentLinkedQueue<AreMessage> updateQueue;
    private final ConcurrentLinkedQueue<AreMessage> attachQueue;
    private final Semaphore semaphore;
    private Vec3 position;
    private boolean moving;
    private boolean inited = false;
    private AreWorker worker;
    private float timePast;

    public static Are getInstance() {
	if (instance == null) {
	    instance = new Are();
	}
	return instance;
    }

    public static boolean isInstanciated() {
	return Are.instance != null;
    }

    private Are() {
	/*
	 * The memory needed by this class will be (WIDTH * HEIGHT * LENGTH * (Chunk memory usage)) + 12;
	 * The memory usage will range from 64k ~ 448k ~ 640k, but this may be compressed later.
	 */
	this.setName("Are Thread");
	position = new Vec3();

	chunkMap = new HashMap<>();
	actionQueue = new LinkedBlockingQueue<>();
	detachQueue = new ConcurrentLinkedQueue<>();
	unloadQueue = new ConcurrentLinkedQueue<>();
	setupQueue = new ConcurrentLinkedQueue<>();
	loadQueue = new ConcurrentLinkedQueue<>();
	updateQueue = new ConcurrentLinkedQueue<>();
	attachQueue = new ConcurrentLinkedQueue<>();
	semaphore = new Semaphore(-1, true);
	worker = new AreWorker(actionQueue, this);
	worker.setName("Are Worker");
    }

    public ConcurrentLinkedQueue<AreMessage> getAttachQueue() {
	return attachQueue;
    }

    public ConcurrentLinkedQueue<AreMessage> getDetachQueue() {
	return detachQueue;
    }

    public void tick(float tpf) {
	timePast += tpf;

	if (timePast > 0.5f) {
//	    if (permitCount > 500) {
//		int remain = permitCount - 500;
//		permitCount = 500;
//		System.out.println("Releasing " + permitCount + " permits. " + remain + " remain...");
//		process();
//		permitCount = remain;
//	    } else {
//		System.out.println("Releasing " + permitCount + " permits.");
	    process();
//	    }
	    timePast -= 0.5f;
	}

    }

    @Override
    public void run() {
	worker.start();
	boolean worked;
	while (!Thread.currentThread().isInterrupted()) {
	    worked = false;
	    try {
		semaphore.acquire();
		for (AreMessage msg = unloadQueue.poll(); msg != null; msg = unloadQueue.poll()) {
		    unloadChunk(msg);
		    worked = true;
		}

		for (AreMessage msg = setupQueue.poll(); msg != null; msg = setupQueue.poll()) {
		    setupChunk(msg);
		    worked = true;
		}

		for (AreMessage msg = loadQueue.poll(); msg != null; msg = loadQueue.poll()) {
		    loadChunk(msg);
		    worked = true;
		}

		for (AreMessage msg = updateQueue.poll(); msg != null; msg = updateQueue.poll()) {
		    updateChunk(msg);
		    worked = true;
		}
		if (worked) {
		    System.out.println("Wasted permit...");
		}
	    } catch (InterruptedException ex) {
		break;
	    }
	}

	worker.interrupt();
    }

    public int getChunkQueueSize() {
	return loadQueue.size() + setupQueue.size();
    }

    public int getAttachQueueSize() {
	return attachQueue.size();
    }

    private void unloadChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();
	set(c.getPosition(), null);

	try {
	    c.lock();
	    c.unload();
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    c.unlock();
	}
    }

    private void setupChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();

	try {
	    c.lock();
	    if (c.setup()) {
		message.setType(AreMessageType.CHUNK_LOAD);
	    } else {
		message.setType(AreMessageType.CHUNK_UNLOAD);
	    }
	    postMessage(message);
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    c.unlock();
	}

    }

    private void loadChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();
	try {
	    c.lock();
	    if (c.load()) {
		message.setType(AreMessageType.CHUNK_ATTACH);
		postMessage(message);
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    c.unlock();
	}
    }

    private void updateChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();

	try {
	    c.lock();
	    if (c.update()) {
		message.setType(AreMessageType.CHUNK_ATTACH);
	    } else {
		message.setType(AreMessageType.CHUNK_DETACH);
	    }
	    postMessage(message);
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    c.unlock();
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
	inited = true;
	process();
    }

    public boolean isMoving() {
	return moving;
    }

    public void setMoving(boolean moving) {
	this.moving = moving;
    }

    public boolean isInited() {
	return inited;
    }

    public void setInited(boolean inited) {
	this.inited = inited;
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
	set(v, c);
    }

    public void set(Vec3 v, Chunk c) {
	if (c == null) {
	    chunkMap.remove(v);
	} else {
	    chunkMap.put(v, c);
	}
    }

    public void move(AreMessage message) {
	Vec3 direction = (Vec3) message.getData();

	if (direction == null || direction.equals(Vec3.ZERO)) {
	    moving = false;
	    return;
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

	moving = false;
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new left border.
		c = get(position.copy().add(boundBegin, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd - 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new left border.
		c = get(position.copy().add(boundBegin, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd + 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, y, boundBegin));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd - 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}
	    }
	}
    }

    private void moveBack() {
	int boundBegin = LENGTH - 1;
	int boundEnd = 0;
	for (int y = 0; y < HEIGHT; y++) {
	    for (int x = 0; x < WIDTH; x++) {
		//Remove the back most and detach it from scene.
		Chunk c = get(position.copy().add(x, y, boundBegin + 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, y, boundBegin));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd + 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}
	    }
	}
    }

    private void moveUp() {
	int boundBegin = 0;
	int boundEnd = HEIGHT - 1;
	for (int x = 0; x < WIDTH; x++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the back most and detach it from scene.
		Chunk c = get(position.copy().add(x, boundBegin - 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, boundBegin, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, boundEnd, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous front border.
		c = get(position.copy().add(x, boundEnd - 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}
	    }
	}
    }

    private void moveDown() {
	int boundBegin = HEIGHT - 1;
	int boundEnd = 0;
	for (int x = 0; x < WIDTH; x++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the back most and detach it from scene.
		Chunk c = get(position.copy().add(x, boundBegin + 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, boundBegin, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, boundEnd, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c));

		//Reload the previous front border.
		c = get(position.copy().add(x, boundEnd + 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c));
		}
	    }
	}
    }

    public Vec3 getAbsoluteChunkPosition(Vec3 chunkPosition) {
	return new Vec3((chunkPosition.getX() * Chunk.WIDTH) - (DATA_WIDTH / 2),
		(chunkPosition.getY() - 8) * Chunk.HEIGHT, //TODO: Add "- (DATA_HEIGHT / 2)"
		chunkPosition.getZ() * Chunk.LENGTH - (DATA_LENGHT / 2));
    }

    public void postMessage(AreMessage message) {
	switch (message.getType()) {
	    case CHUNK_DETACH: {
		detachQueue.offer(message);
		break;
	    }
	    case CHUNK_SETUP: {
		setupQueue.offer(message);
		break;
	    }
	    case CHUNK_LOAD: {
		loadQueue.offer(message);
		break;
	    }
	    case CHUNK_UNLOAD: {
		unloadQueue.offer(message);
		break;
	    }
	    case CHUNK_UPDATE: {
		updateQueue.offer(message);
		break;
	    }
	    case CHUNK_ATTACH: {
		attachQueue.offer(message);
		break;
	    }
	    default: {
		actionQueue.offer(message);
	    }
	}
    }

    public void process() {
	semaphore.release();
//	permitCount = 0;
    }

    public Vec3 getPosition() {
	return position;
    }

    public Vec3 setPosition(int x, int y, int z) {
	return position = new Vec3(x, y, z);
    }
}