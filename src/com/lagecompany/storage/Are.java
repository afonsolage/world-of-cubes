package com.lagecompany.storage;

import com.lagecompany.storage.voxel.Voxel;
import com.lagecompany.storage.AreMessage.AreMessageType;
import com.lagecompany.storage.voxel.VoxelNode;
import com.lagecompany.util.MathUtils;
import java.util.HashMap;
import java.util.Queue;
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
    public static final int HEIGHT = 32;
    public static final int LENGTH = 16;
    public static final int DATA_WIDTH = WIDTH * Chunk.SIZE;
    public static final int DATA_HEIGHT = HEIGHT * Chunk.SIZE;
    public static final int DATA_LENGHT = LENGTH * Chunk.SIZE;
    private static Are instance;
    private final HashMap<Vec3, Chunk> chunkMap;
    private final BlockingQueue<AreMessage> actionQueue;
    private final BlockingDeque<Integer> processBatchQueue;
    private final ConcurrentLinkedQueue<Integer> renderBatchQueue;
    private final Queue<VoxelNode> propagateSunLightQueue;
    private final Queue<VoxelNode> removeSunLightQueue;
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
	propagateSunLightQueue = new LinkedBlockingQueue<>();
	removeSunLightQueue = new LinkedBlockingQueue<>();
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

		propagateSunLight(currentBatch);
		removeSunLight(currentBatch);

		queue = areQueue.getQueue(AreMessageType.CHUNK_LIGHT, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			lightChunk(msg);
			worked = true;
		    }
		    areQueue.finishBatch(AreMessageType.CHUNK_LIGHT, currentBatch);
		}

		queue = areQueue.getQueue(AreMessageType.CHUNK_LOAD, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			loadChunk(msg);
		    }
		    areQueue.finishBatch(AreMessageType.CHUNK_LOAD, currentBatch);
		}

//		queue = areQueue.getQueue(AreMessageType.CHUNK_UPDATE, currentBatch);
//		if (queue != null) {
//		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
//			updateChunk(msg);
//		    }
//		    areQueue.finishBatch(AreMessageType.CHUNK_UPDATE, currentBatch);
//		}

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
//	set(c.getPosition(), null);

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
	    c.setup();

	    //Check for sunLight;
	    if (c.getPosition().getY() == HEIGHT - 1) {
		Voxel v;
		for (int cx = 0; cx < Chunk.SIZE; cx++) {
		    for (int cz = 0; cz < Chunk.SIZE; cz++) {
			v = c.get(cx, Chunk.SIZE - 1, cz);
			if (v.getType() == Voxel.VT_NONE) {
			    propagateSunLightQueue.add(new VoxelNode(c, new Vec3(cx, 0, cz), Voxel.LIGHT_SUN));
			}
		    }
		}
	    }

	    message.setType(AreMessageType.CHUNK_LIGHT);
//	    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, currentBatch));
	    postMessage(message);
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    c.unlock();
	}
    }

    private void changeLight(Queue<VoxelNode> queue, byte light, int batch) {
	VoxelNode node;
	Voxel v;
	while (!queue.isEmpty()) {
	    node = queue.poll();
	    Chunk c = node.chunk;
	    c.setLight(node.x, node.y, node.z, light);
	    node.y--;
	    if (node.y < 0) {
		c = get(Vec3.copyAdd(c.getPosition(), 0, -1, 0));
		if (c == null) {
		    continue;
		} else {
		    requestChunkUpdate(c, batch);
		    node.chunk = c;
		    node.y = Chunk.SIZE - 1;

		    if (node.x == 0) {
			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), -1, 0, 0)), batch);
		    } else if (node.x == Chunk.SIZE - 1) {
			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 1, 0, 0)), batch);
		    }

		    if (node.z == 0) {
			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 0, -1)), batch);
		    } else if (node.z == Chunk.SIZE - 1) {
			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 0, 1)), batch);
		    }
		}
	    }
	    v = c.get(node.x, node.y, node.z);
	    if (v.getType() == Voxel.VT_NONE) {
		queue.add(node);
	    }
	}
    }

    private void removeSunLight(int batch) {
	changeLight(removeSunLightQueue, (byte) 0, batch);
    }

    private void propagateSunLight(int batch) {
	changeLight(propagateSunLightQueue, (byte) Voxel.LIGHT_SUN, batch);
    }

    private void requestChunkUpdate(Chunk c, int batch) {
	if (c != null && c.isLoaded()) {
	    c.lock();
	    c.reset();
	    c.unlock();
	    postMessage(new AreMessage(AreMessageType.CHUNK_LOAD, c, batch));
	}
    }

    private void lightChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();
	try {
//	    c.propagateSunLight();
//	    c.propagateAreaLight();
	    if (c.hasVisibleVoxel()) {
		message.setType(AreMessageType.CHUNK_LOAD);
	    } else {
		message.setType(AreMessageType.CHUNK_UNLOAD);
	    }
	    postMessage(message);
	} catch (Exception ex) {
	    ex.printStackTrace();
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
	Vec3 v;
	Chunk c;
	int batch = areQueue.nextBatch();
	for (int z = 0; z < LENGTH; z++) {
	    //Due to sun light, we need to always load chunks from top-down.
	    for (int x = 0; x < WIDTH; x++) {
		for (int y = HEIGHT - 1; y >= 0; y--) {
		    v = new Vec3(x, y, z);
		    c = new Chunk(this, v);
		    set(v, c);
		    postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));
		}
	    }
	}
	inited = true;

	process(batch);
    }

//    private void propagateLight(AreMessage msg) {
//	final Vec3 v = (Vec3) msg.getData();
//	executorService.submit(new Runnable() {
//	    @Override
//	    public void run() {
//		propagateChunkLight(v.getX(), HEIGHT - 1, v.getY());
//	    }
//	});
//    }
//
//    //TODO: Add light direction, light length and type.
//    public void propagateChunkLight(final int x, final int y, final int z) {
//	final Chunk c = get(x, y, z);
//	if (c != null) {
//	    if (c.isEmpty()) {
//		c.setLight(Voxel.LIGHT_SUN);
//		executorService.submit(new Runnable() {
//		    @Override
//		    public void run() {
//			propagateChunkLight(x, y - 1, z);
//		    }
//		});
//	    } else {
//		for (int cx = 0; cx < WIDTH; cx++) {
//		    for (int cz = 0; cz < LENGTH; cz++) {
//			executorService.submit(new Runnable() {
//			    private int a;
//			    private int b;
//			    
//			    Runnable setParam(int a, int b) {
//				this.a = a;
//				this.b = b;
//				return this;
//			    }
//			    
//			    @Override
//			    public void run() {
//				propagateVoxelLight(c, a, HEIGHT - 1, b);
//			    }
//			}.setParam(cx, cz));
//			
//		    }
//		}
//	    }
//	}
//    }
//    
//    public void propagateVoxelLight(final Chunk c, final int x, final int y, final int z) {
//	if (y < 0) {
//	    final Vec3 v = c.getPosition();
//	    executorService.submit(new Runnable() {
//		@Override
//		public void run() {
//		    propagateChunkLight(v.getX(), v.getY() - 1, v.getZ());
//		}
//	    });
//	} else {
//	    Voxel v = c.get(x, y, z);
//	    if (v != null && v.getType() == Voxel.VT_NONE) {
//		v.setLightPower(Voxel.LIGHT_SUN);
//		executorService.submit(new Runnable() {
//		    @Override
//		    public void run() {
//			propagateVoxelLight(c, x, y - 1, z);
//		    }
//		});
//	    }
//	}
//    }
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

    protected int getLight(Vec3 chunkPos, int x, int y, int z) {
	Chunk c = get(chunkPos);

	if (c == null) {
	    return -1;
	}

	return c.getLight(x, y, z);
    }

    protected Vec3 toChunkPosition(int x, int y, int z) {
	return new Vec3(MathUtils.floorDiv(x, Chunk.SIZE),
		MathUtils.floorDiv(y, Chunk.SIZE),
		MathUtils.floorDiv(z, Chunk.SIZE));
    }

    protected Vec3 toVoxelPosition(int x, int y, int z) {
	return new Vec3(MathUtils.absMod(x, Chunk.SIZE),
		MathUtils.absMod(y, Chunk.SIZE),
		MathUtils.absMod(z, Chunk.SIZE));
    }

    protected void updateVoxel(int x, int y, int z, Voxel v) {
	Vec3 pos = toChunkPosition(x, y, z);
	Chunk c = get(pos);
	if (c == null) {
	    return;
	}

	Vec3 voxelPos = toVoxelPosition(x, y, z);
	c.lock();
	c.set(voxelPos, v);
	c.unlock();

	if (v.getType() == Voxel.VT_NONE) {
	    if (c.getAreLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), Voxel.VS_TOP) == Voxel.LIGHT_SUN) {
		propagateSunLightQueue.add(new VoxelNode(c, voxelPos, Voxel.LIGHT_SUN));
	    } else {
		removeSunLightQueue.add(new VoxelNode(c, voxelPos, 0));
	    }
	} else {
	    c.setLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), 0);
	    if (voxelPos.getY() == 0) {
		Chunk c2 = get(pos.getX(), pos.getY() - 1, pos.getZ());
		if (c2 != null) {
		    Vec3 v2 = voxelPos.copy();
		    v2.setY(Chunk.SIZE - 1);
		    removeSunLightQueue.add(new VoxelNode(c2, v2, 0));
		}
	    } else {
		removeSunLightQueue.add(new VoxelNode(c, Vec3.copyAdd(voxelPos, 0, -1, 0), 0));
	    }
	}

	int batch = areQueue.nextBatch();
	postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));

	updateNeighborhood(pos, voxelPos, batch);

	process(batch);
    }

    public void updateNeighborhood(Vec3 chunkPos, Vec3 voxelPos, int batch) {
	Chunk c;
	if (voxelPos.getX() == 0) {
	    c = get(Vec3.copyAdd(chunkPos, -1, 0, 0));
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_LOAD, c, batch));
	    }
	} else if (voxelPos.getX() == Chunk.SIZE - 1) {
	    c = get(Vec3.copyAdd(chunkPos, 1, 0, 0));
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_LOAD, c, batch));
	    }
	}

	if (voxelPos.getY() == 0) {
	    c = get(Vec3.copyAdd(chunkPos, 0, -1, 0));
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_LOAD, c, batch));
	    }
	} else if (voxelPos.getY() == Chunk.SIZE - 1) {
	    c = get(Vec3.copyAdd(chunkPos, 0, 1, 0));
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_LOAD, c, batch));
	    }
	}

	if (voxelPos.getZ() == 0) {
	    c = get(Vec3.copyAdd(chunkPos, 0, 0, -1));
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_LOAD, c, batch));
	    }
	} else if (voxelPos.getZ() == Chunk.SIZE - 1) {
	    c = get(Vec3.copyAdd(chunkPos, 0, 0, 1));
	    if (c != null) {
		postMessage(new AreMessage(AreMessageType.CHUNK_LOAD, c, batch));
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd - 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd + 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd - 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd + 1));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, boundEnd, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, boundEnd - 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, boundEnd, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(AreMessageType.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, boundEnd + 1, z));
		if (c != null) {
		    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, batch));
		}
	    }
	}
    }

    public Vec3 getAbsoluteChunkPosition(Vec3 chunkPosition) {
	return new Vec3((chunkPosition.getX() * Chunk.SIZE) - (DATA_WIDTH / 2),
		(chunkPosition.getY() - 8) * Chunk.SIZE, //TODO: Add "- (DATA_HEIGHT / 2)"
		chunkPosition.getZ() * Chunk.SIZE - (DATA_LENGHT / 2));
    }

    public Vec3 getRelativePosition(Vec3 chunkPosition) {
	return getRelativePosition(chunkPosition.getX(), chunkPosition.getY(), chunkPosition.getZ());
    }

    public Vec3 getRelativePosition(int x, int y, int z) {
	//TODO: Add "y + (DATA_HEIGHT / 2)"
	return new Vec3(x + (DATA_WIDTH / 2), y + (8 * Chunk.SIZE), (z + (DATA_LENGHT / 2)));
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
	    case CHUNK_LIGHT: {
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

    public float getChunkQueueSize(AreMessageType type) {
	return areQueue.getQueueSize(type);
    }

    public void setVoxel(Vec3 v, short type) {
	updateVoxel(v.getX(), v.getY(), v.getZ(), new Voxel(type));
    }
}