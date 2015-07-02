package com.lagecompany.storage;

import com.lagecompany.storage.AreMessage.AreMessageType;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
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
    private static Are instance;
    private final HashMap<Vec3, Chunk> chunkMap;
    private final BlockingQueue<AreMessage> actionQueue;
    private final BlockingDeque<Integer> processBatchQueue;
    private final ConcurrentLinkedQueue<Integer> renderBatchQueue;
    private final AreQueue areQueue;
    private Vec3 position;
    private boolean moving;
    private boolean inited = false;
    private AreWorker worker;
    private float timePast;
    private int currentBatch;

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
	processBatchQueue = new LinkedBlockingDeque<>();
	renderBatchQueue = new ConcurrentLinkedQueue<>();
	areQueue = new AreQueue();
	worker = new AreWorker(actionQueue, this);
	worker.setName("Are Worker");
    }

    public ConcurrentLinkedQueue<Integer> getRenderBatchQueue() {
	return renderBatchQueue;
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
//	    }
	    timePast -= 0.5f;
	}

    }

    @Override
    public void run() {
	worker.start();
	boolean worked;
	ConcurrentLinkedQueue<AreMessage> queue;
	while (!Thread.currentThread().isInterrupted()) {
	    worked = false;
	    try {
		currentBatch = processBatchQueue.take();

		queue = areQueue.getQueue(AreMessageType.CHUNK_UNLOAD, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			unloadChunk(msg);
		    }
		    areQueue.finishBatch(AreMessageType.CHUNK_UNLOAD, currentBatch);
		}

		queue = areQueue.getQueue(AreMessageType.CHUNK_SETUP, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			setupChunk(msg);
			worked = true;
		    }
		    areQueue.finishBatch(AreMessageType.CHUNK_SETUP, currentBatch);
		}

		queue = areQueue.getQueue(AreMessageType.CHUNK_LOAD, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			loadChunk(msg);
		    }
		    areQueue.finishBatch(AreMessageType.CHUNK_LOAD, currentBatch);
		}

		queue = areQueue.getQueue(AreMessageType.CHUNK_UPDATE, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			updateChunk(msg);
		    }
		    areQueue.finishBatch(AreMessageType.CHUNK_UPDATE, currentBatch);
		}

		if (worked) {
		    processNow(currentBatch);
		}

		renderBatchQueue.offer(currentBatch);
	    } catch (InterruptedException ex) {
		break;
	    }
	}

	worker.interrupt();
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
	    message.setBatch(currentBatch);
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
	int batch = areQueue.nextBatch();
	for (int x = 0; x < WIDTH; x++) {
	    for (int y = 0; y < HEIGHT; y++) {
		for (int z = 0; z < LENGTH; z++) {
		    Vec3 v = new Vec3(x, y, z);
		    Chunk c = new Chunk(this, v);
		    set(v, c);
		    postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));
		}
	    }
	}
	inited = true;
	process(batch);
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

    protected void updateVoxel(int x, int y, int z, Voxel v) {
	int chunkX = x / Chunk.WIDTH;
	int chunkY = y / Chunk.HEIGHT;
	int chunkZ = z / Chunk.LENGTH;

	Chunk c = get(chunkX, chunkY, chunkZ);

	if (c == null) {
	    return;
	}

	int voxelX = x % Chunk.WIDTH;
	int voxelY = y % Chunk.HEIGHT;
	int voxelZ = z % Chunk.LENGTH;

	c.lock();
	Voxel vo = c.get(voxelX, voxelY, voxelZ);
	c.set(voxelX, voxelY, voxelZ, v);
	c.unlock();

	int batch = areQueue.nextBatch();
	postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));

	updateNeighborhood(chunkX, chunkY, chunkZ, batch);

	process(batch);
    }

    public void updateNeighborhood(Vec3 v, int batch) {
	updateNeighborhood(v.getX(), v.getY(), v.getZ(), batch);
    }

    public void updateNeighborhood(int x, int y, int z, int batch) {
	Chunk c = get(x - 1, y, z);
	{
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
	    }
	}

	c = get(x + 1, y, z);
	{
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
	    }
	}

	c = get(x, y - 1, z);
	{
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
	    }
	}

	c = get(x, y + 1, z);
	{
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
	    }
	}

	c = get(x, y, z - 1);
	{
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
	    }
	}

	c = get(x, y, z + 1);
	{
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
	    }
	}
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

	int batch = areQueue.nextBatch();

	if (direction.getX() > 0) {
	    moveRight(batch);
	} else if (direction.getX() < 0) {
	    moveLeft(batch);
	}

	if (direction.getY() > 0) {
	    moveUp(batch);
	} else if (direction.getY() < 0) {
	    moveDown(batch);
	}

	if (direction.getZ() > 0) {
	    moveFront(batch);
	} else if (direction.getZ() < 0) {
	    moveBack(batch);
	}

	process(batch);
	moving = false;
    }

    /**
     * Move all chunks into X+ direction. The left most chunk is set to NULL and the right most is created. Since Are
     * position is the LEFT, BOTTOM, Back cornder, we need to remove the 0 X axis chunks and create a new one at
     * DATA_WIDTH + 1 X axis.
     */
    private void moveRight(int batch) {
	int boundBegin = 0;
	int boundEnd = WIDTH - 1;
	for (int y = 0; y < HEIGHT; y++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the left most and detach it from scene.
		Chunk c = get(position.copy().add(boundBegin - 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new left border.
		c = get(position.copy().add(boundBegin, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd - 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}
	    }
	}
    }

    private void moveLeft(int batch) {
	int boundBegin = WIDTH - 1;
	int boundEnd = 0;
	for (int y = 0; y < HEIGHT; y++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the left most and detach it from scene.
		Chunk c = get(position.copy().add(boundBegin + 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new left border.
		c = get(position.copy().add(boundBegin, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd + 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}
	    }
	}
    }

    private void moveFront(int batch) {
	int boundBegin = 0;
	int boundEnd = LENGTH - 1;
	for (int y = 0; y < HEIGHT; y++) {
	    for (int x = 0; x < WIDTH; x++) {
		//Remove the back most and detach it from scene.
		Chunk c = get(position.copy().add(x, y, boundBegin - 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, y, boundBegin));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd - 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}
	    }
	}
    }

    private void moveBack(int batch) {
	int boundBegin = LENGTH - 1;
	int boundEnd = 0;
	for (int y = 0; y < HEIGHT; y++) {
	    for (int x = 0; x < WIDTH; x++) {
		//Remove the back most and detach it from scene.
		Chunk c = get(position.copy().add(x, y, boundBegin + 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, y, boundBegin));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd + 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}
	    }
	}
    }

    private void moveUp(int batch) {
	int boundBegin = 0;
	int boundEnd = HEIGHT - 1;
	for (int x = 0; x < WIDTH; x++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the back most and detach it from scene.
		Chunk c = get(position.copy().add(x, boundBegin - 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, boundBegin, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, boundEnd, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, boundEnd - 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}
	    }
	}
    }

    private void moveDown(int batch) {
	int boundBegin = HEIGHT - 1;
	int boundEnd = 0;
	for (int x = 0; x < WIDTH; x++) {
	    for (int z = 0; z < LENGTH; z++) {
		//Remove the back most and detach it from scene.
		Chunk c = get(position.copy().add(x, boundBegin + 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(AreMessageType.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, boundBegin, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, boundEnd, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, boundEnd + 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_UPDATE, c, batch));
		}
	    }
	}
    }

    public Vec3 getAbsoluteChunkPosition(Vec3 chunkPosition) {
	return new Vec3((chunkPosition.getX() * Chunk.WIDTH) - (DATA_WIDTH / 2),
		(chunkPosition.getY() - 8) * Chunk.HEIGHT, //TODO: Add "- (DATA_HEIGHT / 2)"
		chunkPosition.getZ() * Chunk.LENGTH - (DATA_LENGHT / 2));
    }

    public Vec3 getRelativePosition(Vec3 chunkPosition) {
	return getRelativePosition(chunkPosition.getX(), chunkPosition.getY(), chunkPosition.getZ());
    }

    public Vec3 getRelativePosition(int x, int y, int z) {
	//TODO: Add "y + (DATA_HEIGHT / 2)"
	return new Vec3(x + (DATA_WIDTH / 2), y + (8 * Chunk.HEIGHT), (z + (DATA_LENGHT / 2)));
    }

    public void postMessage(AreMessage message) {
	switch (message.getType()) {
	    case CHUNK_DETACH: {
	    }
	    case CHUNK_SETUP: {
	    }
	    case CHUNK_LOAD: {
	    }
	    case CHUNK_UNLOAD: {
	    }
	    case CHUNK_UPDATE: {
	    }
	    case CHUNK_ATTACH: {
		areQueue.queue(message);
		break;
	    }
	    default: {
		actionQueue.offer(message);
	    }
	}
    }

    public void process(int batch) {
	processBatchQueue.offer(batch);
    }

    private void processNow(int batch) {
	processBatchQueue.offerFirst(batch);
    }

    public Vec3 getPosition() {
	return position;
    }

    public Vec3 setPosition(int x, int y, int z) {
	return position = new Vec3(x, y, z);
    }

    public ConcurrentLinkedQueue<AreMessage> getAttachQueue(Integer batch) {
	return areQueue.getQueue(AreMessageType.CHUNK_ATTACH, batch);
    }

    public ConcurrentLinkedQueue<AreMessage> getDetachQueue(Integer batch) {
	return areQueue.getQueue(AreMessageType.CHUNK_DETACH, batch);
    }

    public void finishBatch(AreMessage.AreMessageType type, Integer batch) {
	areQueue.finishBatch(type, batch);
    }

    public float getChunkQueueSize() {
	int size = areQueue.getQueueSize(AreMessageType.CHUNK_SETUP);
	size += areQueue.getQueueSize(AreMessageType.CHUNK_LOAD);
	return size;
    }

    public float getAttachQueueSize() {
	return areQueue.getQueueSize(AreMessageType.CHUNK_ATTACH);
    }

    public void setVoxel(Vec3 v, short type) {
	if (type == Voxel.VT_NONE) {
	    updateVoxel(v.getX(), v.getY(), v.getZ(), null);
	} else {
	    updateVoxel(v.getX(), v.getY(), v.getZ(), new Voxel(type));
	}
    }
}