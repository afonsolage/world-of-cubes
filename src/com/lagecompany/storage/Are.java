package com.lagecompany.storage;

import com.lagecompany.storage.voxel.Voxel;
import com.lagecompany.storage.AreMessage.Type;
import static com.lagecompany.storage.AreMessage.Type.SPECIAL_VOXEL_ATTACH;
import com.lagecompany.storage.light.LightNode;
import com.lagecompany.storage.light.LightRemoveNode;
import com.lagecompany.storage.voxel.SpecialVoxelData;
import com.lagecompany.util.MathUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
    public static final int SIZE = WIDTH * HEIGHT * LENGTH;
    public static final Vec3 ARE_OFFSET = new Vec3((Are.DATA_WIDTH / 2), 8 * Chunk.SIZE, (Are.DATA_LENGHT / 2));
    private static Are instance;
    private final HashMap<Vec3, Chunk> chunkMap;
    private final BlockingQueue<AreMessage> actionQueue;
    private final BlockingDeque<Integer> processBatchQueue;
    private final ConcurrentLinkedQueue<Integer> renderBatchQueue;
    private final Queue<LightNode> lightQueue;
    private final Queue<LightNode> sunLightQueue;
    private final Queue<LightRemoveNode> lightRemovalQueue;
    private final Queue<LightRemoveNode> sunLightRemovalQueue;
    private final AreQueue areQueue;
    private final ConcurrentLinkedQueue<SpecialVoxelData> specialVoxelList;
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
	lightQueue = new LinkedList<>();
	sunLightQueue = new LinkedList<>();
	lightRemovalQueue = new LinkedList<>();
	sunLightRemovalQueue = new LinkedList<>();
	specialVoxelList = new ConcurrentLinkedQueue<>();

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

		queue = areQueue.getQueue(Type.CHUNK_UNLOAD, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			unloadChunk(msg);
		    }
		    areQueue.finishBatch(Type.CHUNK_UNLOAD, currentBatch);
		}

		queue = areQueue.getQueue(Type.CHUNK_SETUP, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			setupChunk(msg);
			worked = true;
		    }
		    areQueue.finishBatch(Type.CHUNK_SETUP, currentBatch);
		}

//		propagateSunLight(currentBatch);
//		removeSunLight(currentBatch);
//		removeReflectedSunLight(currentBatch);
//		reflectSunLight(currentBatch);

		queue = areQueue.getQueue(Type.CHUNK_LIGHT, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			lightChunk(msg);
			worked = true;
		    }
		    areQueue.finishBatch(Type.CHUNK_LIGHT, currentBatch);
		}

		queue = areQueue.getQueue(Type.CHUNK_LOAD, currentBatch);
		if (queue != null) {
		    for (AreMessage msg = queue.poll(); msg != null; msg = queue.poll()) {
			loadChunk(msg);
		    }
		    areQueue.finishBatch(Type.CHUNK_LOAD, currentBatch);
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
	    message.setType(Type.CHUNK_LIGHT);
	    postMessage(message);
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    c.unlock();
	}
    }

    private void propagateLight(int batch) {
	LightNode node;
	Vec3 voxelPos = new Vec3();
	Chunk c;
	int lightLevel;
	List<Chunk> touchedList = new ArrayList<>();

	while (!lightQueue.isEmpty()) {
	    node = lightQueue.remove();
	    lightLevel = node.chunk.getLight(node.x, node.y, node.z);
	    for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
		voxelPos.set(node.x + dir.getX(), node.y + dir.getY(), node.z + dir.getZ());
		c = validateChunkAndVoxel(node.chunk, voxelPos);

		if (c == null) {
		    continue;
		}

		if (!Voxel.isOpaque(c.get(voxelPos)) && c.getLight(voxelPos) + 2 <= lightLevel) {
		    c.setLight(voxelPos, lightLevel - 1);
		    lightQueue.add(new LightNode(c, voxelPos));

		    if (c != node.chunk && !touchedList.contains(c)) {
			touchedList.add(c);
		    }
		}
	    }
	}

	for (Chunk chunk : touchedList) {
	    postMessage(new AreMessage(Type.CHUNK_LOAD, chunk, batch), true);
	}
    }

    /**
     * Propagate sun light over chunks. This function uses a Flood Fill algorithm, so before calling it, a propagation
     * base voxel should be added on sunLightQueue.
     *
     * @param batch The batch id used to reloaded touched chunks.
     */
    public void propagateSunLight(int batch) {
	//Temp variables
	LightNode node;
	Vec3 voxelPos = new Vec3();
	Chunk c;
	int lightLevel, propagatedLightLevel;
	List<Chunk> touchedList = new ArrayList<>();

	while (!sunLightQueue.isEmpty()) {
	    //Get next node on queue.
	    node = sunLightQueue.remove();
	    lightLevel = node.chunk.getSunLight(node.x, node.y, node.z);

	    //Let's the neighborhood of current voxel and check if the light can be propagated.
	    for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
		voxelPos.set(node.x + dir.getX(), node.y + dir.getY(), node.z + dir.getZ());
		c = validateChunkAndVoxel(node.chunk, voxelPos);

		if (c == null) {
		    continue;
		}

		//Since this is a sun light, whenever we are propagating downwards and current node ligut power is sun light,
		//it should not decrease light level.
		propagatedLightLevel = (dir == Vec3.DOWN && lightLevel == Voxel.LIGHT_SUN) ? Voxel.LIGHT_SUN : lightLevel - 1;

		//If current neighbor voxel isn't opaque and it has a sun light value lower then propagated one, let's do it.
		if (!Voxel.isOpaque(c.get(voxelPos)) && c.getSunLight(voxelPos) < propagatedLightLevel) {
		    //Set desired light level and add a new node to queue.
		    c.setSunLight(voxelPos, propagatedLightLevel);
		    sunLightQueue.add(new LightNode(c, voxelPos));

		    //If this chunk isn't the same on node and it isn't already on touchedList, let's add it.
		    if (c != node.chunk && !touchedList.contains(c)) {
			touchedList.add(c);
		    }
		}
	    }
	}

	//Send an update message to all chunks touched by this function. Also to avoid duplicating chunks, we add only
	//chunks that isn't already on queue to load.
	for (Chunk chunk : touchedList) {
	    postMessage(new AreMessage(Type.CHUNK_LOAD, chunk, batch), true);
	}
    }

    private void removeLight(int batch) {
	LightRemoveNode node;
	Vec3 voxelPos = new Vec3();
	Chunk c;
	int previousLightLevel;
	int neighborLightLevel;
	List<Chunk> touchedList = new ArrayList<>();

	while (!lightRemovalQueue.isEmpty()) {
	    node = lightRemovalQueue.remove();
	    previousLightLevel = node.light;
	    for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
		voxelPos.set(node.x + dir.getX(), node.y + dir.getY(), node.z + dir.getZ());
		c = validateChunkAndVoxel(node.chunk, voxelPos);

		if (c == null) {
		    continue;
		}

		neighborLightLevel = c.getLight(voxelPos);
		if (neighborLightLevel > 0 && neighborLightLevel < previousLightLevel) {
		    c.setLight(voxelPos, 0);

		    if (c != node.chunk && !touchedList.contains(c)) {
			touchedList.add(c);
		    }
		    lightRemovalQueue.add(new LightRemoveNode(c, voxelPos, neighborLightLevel));
		} else if (neighborLightLevel >= previousLightLevel) {
		    lightQueue.add(new LightNode(c, voxelPos));

		    if (c != node.chunk && !touchedList.contains(c)) {
			touchedList.add(c);
		    }
		}
	    }
	}

	for (Chunk chunk : touchedList) {
	    postMessage(new AreMessage(Type.CHUNK_LOAD, chunk, batch), true);
	}

	propagateLight(batch);
    }

    private void removeSunLight(int batch) {
	LightRemoveNode node;
	Vec3 voxelPos = new Vec3();
	Chunk c;
	int previousLightLevel;
	int neighborLightLevel;
	List<Chunk> touchedList = new ArrayList<>();

	while (!sunLightRemovalQueue.isEmpty()) {
	    node = sunLightRemovalQueue.remove();
	    previousLightLevel = node.light;
	    for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
		voxelPos.set(node.x + dir.getX(), node.y + dir.getY(), node.z + dir.getZ());
		c = validateChunkAndVoxel(node.chunk, voxelPos);

		if (c == null) {
		    continue;
		}

		neighborLightLevel = c.getSunLight(voxelPos);
		if ((dir == Vec3.DOWN && previousLightLevel == Voxel.LIGHT_SUN)
			|| (neighborLightLevel > 0 && neighborLightLevel < previousLightLevel)) {
		    c.setSunLight(voxelPos, 0);

		    if (c != node.chunk && !touchedList.contains(c)) {
			touchedList.add(c);
		    }

		    sunLightRemovalQueue.add(new LightRemoveNode(c, voxelPos, neighborLightLevel));
		} else if (neighborLightLevel >= previousLightLevel) {
		    sunLightQueue.add(new LightNode(c, voxelPos));

		    if (c != node.chunk && !touchedList.contains(c)) {
			touchedList.add(c);
		    }
		}
	    }
	}

	for (Chunk chunk : touchedList) {
	    postMessage(new AreMessage(Type.CHUNK_LOAD, chunk, batch), true);
	}

	propagateSunLight(batch);
    }

    private void requestChunkUpdate(Chunk c, int batch) {
	if (c != null && c.isLoaded()) {
	    c.lock();
	    c.reset();
	    c.unlock();
	    c.flagToUpdate();
	    postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
	}
    }

    private void lightChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();
	try {
	    Voxel v;
	    if (c.getPosition().getY() == HEIGHT - 1) {
		for (int x = 0; x < Chunk.SIZE; x++) {
		    for (int z = 0; z < Chunk.SIZE; z++) {
			v = c.get(x, Chunk.SIZE - 1, z);
			if (!Voxel.isOpaque(v)) {
			    c.setSunLight(x, Chunk.SIZE - 1, z, Voxel.LIGHT_SUN);
			    sunLightQueue.add(new LightNode(c, x, Chunk.SIZE - 1, z));
			}
		    }
		}
	    }

	    if (c.hasVisibleVoxel()) {
		message.setType(Type.CHUNK_LOAD);
	    } else {
		message.setType(Type.CHUNK_UNLOAD);
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
		message.setType(Type.CHUNK_ATTACH);
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
		message.setType(Type.CHUNK_ATTACH);
	    } else {
		message.setType(Type.CHUNK_DETACH);
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
		    postMessage(new AreMessage(Type.CHUNK_SETUP, c, batch));
		}
	    }
	}
	inited = true;

	areQueue.addListener(AreQueueListener.Type.FINISH, new AreQueueListener(Type.CHUNK_LIGHT, false) {
	    @Override
	    public void doAction(int batch) {
		propagateSunLight(batch);
	    }
	});

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

    protected Voxel getVoxel(Chunk c, int x, int y, int z) {
	if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
	    return c.get(x, y, z);
	} else {
	    int cx, cy, cz;

	    if (x < 0) {
		cx = -1;
		x = Chunk.SIZE - 1;
	    } else if (x >= Chunk.SIZE) {
		cx = 1;
		x = 0;
	    } else {
		cx = 0;
	    }

	    if (y < 0) {
		cy = -1;
		y = Chunk.SIZE - 1;
	    } else if (y >= Chunk.SIZE) {
		cy = 1;
		y = 0;
	    } else {
		cy = 0;
	    }

	    if (z < 0) {
		cz = -1;
		z = Chunk.SIZE - 1;
	    } else if (z >= Chunk.SIZE) {
		cz = 1;
		z = 0;
	    } else {
		cz = 0;
	    }

	    return getVoxel(Vec3.copyAdd(c.getPosition(), cx, cy, cx), x, y, z);
	}
    }

    protected Voxel getVoxel(Vec3 chunkPos, int x, int y, int z) {
	Chunk c = get(chunkPos);

	if (c == null) {
	    return null;
	}

	return c.get(x, y, z);
    }

    protected void setLight(Chunk c, int x, int y, int z, int light) {
	if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
	    c.setLight(x, y, z, light);
	} else {
	    int cx, cy, cz;

	    if (x < 0) {
		cx = -1;
		x = Chunk.SIZE - 1;
	    } else if (x >= Chunk.SIZE) {
		cx = 1;
		x = 0;
	    } else {
		cx = 0;
	    }

	    if (y < 0) {
		cy = -1;
		y = Chunk.SIZE - 1;
	    } else if (y >= Chunk.SIZE) {
		cy = 1;
		y = 0;
	    } else {
		cy = 0;
	    }

	    if (z < 0) {
		cz = -1;
		z = Chunk.SIZE - 1;
	    } else if (z >= Chunk.SIZE) {
		cz = 1;
		z = 0;
	    } else {
		cz = 0;
	    }

	    setLight(Vec3.copyAdd(c.getPosition(), cx, cy, cx), x, y, z, light);
	}
    }

    protected void setLight(Vec3 chunkPos, int x, int y, int z, int light) {
	Chunk c = get(chunkPos);

	if (c == null) {
	    return;
	}

	c.setLight(x, y, z, light);
    }

    protected int getSunLight(Vec3 chunkPos, int x, int y, int z) {
	Chunk c = get(chunkPos);

	if (c == null) {
	    return -1;
	}

	return c.getSunLight(x, y, z);
    }

    protected int getSunLight(Chunk c, int x, int y, int z) {
	if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
	    return c.getLight(x, y, z);
	} else {
	    int cx, cy, cz;

	    if (x < 0) {
		cx = -1;
		x = Chunk.SIZE - 1;
	    } else if (x >= Chunk.SIZE) {
		cx = 1;
		x = 0;
	    } else {
		cx = 0;
	    }

	    if (y < 0) {
		cy = -1;
		y = Chunk.SIZE - 1;
	    } else if (y >= Chunk.SIZE) {
		cy = 1;
		y = 0;
	    } else {
		cy = 0;
	    }

	    if (z < 0) {
		cz = -1;
		z = Chunk.SIZE - 1;
	    } else if (z >= Chunk.SIZE) {
		cz = 1;
		z = 0;
	    } else {
		cz = 0;
	    }

	    return getSunLight(Vec3.copyAdd(c.getPosition(), cx, cy, cx), x, y, z);
	}
    }

    protected int getLight(Vec3 chunkPos, int x, int y, int z) {
	Chunk c = get(chunkPos);

	if (c == null) {
	    return -1;
	}

	return c.getLight(x, y, z);
    }

    protected int getLight(Chunk c, int x, int y, int z) {
	if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
	    return c.getLight(x, y, z);
	} else {
	    int cx, cy, cz;

	    if (x < 0) {
		cx = -1;
		x = Chunk.SIZE - 1;
	    } else if (x >= Chunk.SIZE) {
		cx = 1;
		x = 0;
	    } else {
		cx = 0;
	    }

	    if (y < 0) {
		cy = -1;
		y = Chunk.SIZE - 1;
	    } else if (y >= Chunk.SIZE) {
		cy = 1;
		y = 0;
	    } else {
		cy = 0;
	    }

	    if (z < 0) {
		cz = -1;
		z = Chunk.SIZE - 1;
	    } else if (z >= Chunk.SIZE) {
		cz = 1;
		z = 0;
	    } else {
		cz = 0;
	    }

	    return getLight(Vec3.copyAdd(c.getPosition(), cx, cy, cx), x, y, z);
	}
    }

    //Convert given coordinates to Chunk index on Are.
    public Vec3 toChunkPosition(int x, int y, int z) {
	return new Vec3(MathUtils.floorDiv(x + ARE_OFFSET.getX(), Chunk.SIZE),
		MathUtils.floorDiv(y + ARE_OFFSET.getY(), Chunk.SIZE),
		MathUtils.floorDiv(z + ARE_OFFSET.getZ(), Chunk.SIZE));
    }

    //Convert given coordinates to Voxel index on Chunk.
    public Vec3 toVoxelPosition(int x, int y, int z) {
	return new Vec3(MathUtils.absMod(x + ARE_OFFSET.getX(), Chunk.SIZE),
		MathUtils.absMod(y + ARE_OFFSET.getY(), Chunk.SIZE),
		MathUtils.absMod(z + ARE_OFFSET.getZ(), Chunk.SIZE));
    }

    /**
     * Update a Voxel in a given position. The position must be in World Coodinates.
     */
    protected void updateVoxel(int x, int y, int z, Voxel newVoxel) {
	Vec3 pos = toChunkPosition(x, y, z);
	Chunk c = get(pos);
	if (c == null) {
	    return;
	}

	Voxel oldVoxel;

	Vec3 voxelPos = toVoxelPosition(x, y, z);
	c.lock();
	oldVoxel = c.get(voxelPos);
	c.set(voxelPos, newVoxel);
	c.unlock();

	int batch = areQueue.nextBatch();
	postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));

	if (newVoxel.getType() == Voxel.VT_TORCH) {
	    c.setLight(voxelPos, 10);
	    lightQueue.add(new LightNode(c, voxelPos));
	    sunLightQueue.add(new LightNode(c, voxelPos));
	} else if (Voxel.isOpaque(oldVoxel)) {
	    Chunk neighborChunk = c;
	    Vec3 neigborVoxelPos;
	    byte light = 0;
	    for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
		if (dir == Vec3.UP) {
		    light += oldVoxel.getTopLight();
		} else if (dir == Vec3.DOWN) {
		    light += oldVoxel.getDownLight();
		} else if (dir == Vec3.FRONT) {
		    light += oldVoxel.getFrontLight();
		} else if (dir == Vec3.BACK) {
		    light += oldVoxel.getBackLight();
		} else if (dir == Vec3.RIGHT) {
		    light += oldVoxel.getRightLight();
		} else {
		    light += oldVoxel.getLeftLight();
		}

		if (light > 0) {
		    neigborVoxelPos = Vec3.copyAdd(voxelPos, dir);
		    neighborChunk = validateChunkAndVoxel(c, neigborVoxelPos);
		    lightQueue.add(new LightNode(neighborChunk, neigborVoxelPos));
		    sunLightQueue.add(new LightNode(neighborChunk, neigborVoxelPos));
		}
	    }
	}
	
	if (Voxel.isOpaque(newVoxel) || oldVoxel.getType() == Voxel.VT_TORCH) {
	    int oldLightPower = c.getLight(voxelPos);
	    if (oldLightPower > 0) {
		lightRemovalQueue.add(new LightRemoveNode(c, voxelPos, oldLightPower));
		c.setLight(voxelPos, 0);
		removeLight(batch);
	    }

	    oldLightPower = c.getSunLight(voxelPos);
	    if (oldLightPower > 0) {
		sunLightRemovalQueue.add(new LightRemoveNode(c, voxelPos, oldLightPower));
		c.setSunLight(voxelPos, 0);
		removeSunLight(batch);
	    }
	}


	if (Voxel.isSpecial(oldVoxel)) {
	    postMessage(new AreMessage(Type.SPECIAL_VOXEL_DETACH, new SpecialVoxelData(c, voxelPos), batch));
	}

	if (Voxel.isSpecial(newVoxel)) {
	    postMessage(new AreMessage(Type.SPECIAL_VOXEL_ATTACH, new SpecialVoxelData(c, voxelPos), batch));
	}

	propagateLight(batch);
	propagateSunLight(batch);

	updateNeighborhood(pos, voxelPos, batch);

	process(batch);
    }

    public void updateNeighborhood(Vec3 chunkPos, Vec3 voxelPos, int batch) {
	Chunk c;
	if (voxelPos.getX() == 0) {
	    c = get(Vec3.copyAdd(chunkPos, -1, 0, 0));
	    if (c != null) {
		postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
	    }
	} else if (voxelPos.getX() == Chunk.SIZE - 1) {
	    c = get(Vec3.copyAdd(chunkPos, 1, 0, 0));
	    if (c != null) {
		postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
	    }
	}

	if (voxelPos.getY() == 0) {
	    c = get(Vec3.copyAdd(chunkPos, 0, -1, 0));
	    if (c != null) {
		postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
	    }
	} else if (voxelPos.getY() == Chunk.SIZE - 1) {
	    c = get(Vec3.copyAdd(chunkPos, 0, 1, 0));
	    if (c != null) {
		postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
	    }
	}

	if (voxelPos.getZ() == 0) {
	    c = get(Vec3.copyAdd(chunkPos, 0, 0, -1));
	    if (c != null) {
		postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
	    }
	} else if (voxelPos.getZ() == Chunk.SIZE - 1) {
	    c = get(Vec3.copyAdd(chunkPos, 0, 0, 1));
	    if (c != null) {
		postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
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
		    postMessage(new AreMessage(Type.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(Type.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new left border.
		c = get(position.copy().add(boundBegin, y, z));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(Type.CHUNK_SETUP, c, batch));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd - 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(Type.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(Type.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new left border.
		c = get(position.copy().add(boundBegin, y, z));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at right most of Are.
		Vec3 v = position.copy().add(boundEnd, y, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(Type.CHUNK_SETUP, c, batch));

		//Reload the previous right border.
		c = get(position.copy().add(boundEnd + 1, y, z));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(Type.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(Type.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, y, boundBegin));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(Type.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd - 1));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(Type.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(Type.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, y, boundBegin));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, y, boundEnd);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(Type.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, y, boundEnd + 1));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(Type.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(Type.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, boundBegin, z));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, boundEnd, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(Type.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, boundEnd - 1, z));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
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
		    postMessage(new AreMessage(Type.CHUNK_UNLOAD, c, batch));
		    postMessage(new AreMessage(Type.CHUNK_DETACH, c, batch));
		}

		//Reload chunk at new back border.
		c = get(position.copy().add(x, boundBegin, z));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
		}

		//Add new chunks at front most of Are.
		Vec3 v = position.copy().add(x, boundEnd, z);
		c = new Chunk(this, v);
		set(v, c);
		postMessage(new AreMessage(Type.CHUNK_SETUP, c, batch));

		//Reload the previous front border.
		c = get(position.copy().add(x, boundEnd + 1, z));
		if (c != null) {
		    postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
		}
	    }
	}
    }

    public Vec3 getAbsoluteChunkPosition(Vec3 chunkPosition) {
	return new Vec3((chunkPosition.getX() * Chunk.SIZE) - ARE_OFFSET.getX(),
		(chunkPosition.getY() * Chunk.SIZE) - ARE_OFFSET.getY(), //TODO: Add "- (DATA_HEIGHT / 2)"
		chunkPosition.getZ() * Chunk.SIZE - ARE_OFFSET.getZ());
    }

    public Vec3 getRelativePosition(Vec3 chunkPosition) {
	return getRelativePosition(chunkPosition.getX(), chunkPosition.getY(), chunkPosition.getZ());
    }

    public Vec3 getRelativePosition(int x, int y, int z) {
	//TODO: Add "y + (DATA_HEIGHT / 2)"
	return new Vec3(x + (DATA_WIDTH / 2), y + (8 * Chunk.SIZE), (z + (DATA_LENGHT / 2)));
    }

    public void postMessage(AreMessage message) {
	postMessage(message, false);
    }

    public void postMessage(AreMessage message, boolean unique) {
	switch (message.getType()) {
	    case CHUNK_DETACH:
	    case CHUNK_SETUP:
	    case CHUNK_LOAD:
	    case CHUNK_UNLOAD:
	    case CHUNK_LIGHT:
	    case SPECIAL_VOXEL_ATTACH:
	    case SPECIAL_VOXEL_DETACH:
	    case CHUNK_ATTACH: {
		areQueue.queue(message, unique);
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

    public ConcurrentLinkedQueue<AreMessage> getQueue(Integer batch, AreMessage.Type type) {
	return areQueue.getQueue(type, batch);
    }

    public void finishBatch(AreMessage.Type type, Integer batch) {
	areQueue.finishBatch(type, batch);
    }

    public float getChunkQueueSize(Type type) {
	return areQueue.getQueueSize(type);
    }

    public void setVoxel(Vec3 v, short type) {
	updateVoxel(v.getX(), v.getY(), v.getZ(), new Voxel(type));
    }

    /**
     * Validade if given voxel position returns a valid voxel. It checks for chunk boundary and return the chunk and
     * voxel based on it.
     *
     * @param chunk The current Chunk to be validated.
     * @param voxelPos Desired voxel position. This will be updated if given one extrapolates chunk boundary.
     * @return A valid chunk to access the voxel position.
     */
    private Chunk validateChunkAndVoxel(Chunk chunk, Vec3 voxelPos) {
	if (voxelPos.getX() >= 0 && voxelPos.getX() < Chunk.SIZE
		&& voxelPos.getY() >= 0 && voxelPos.getY() < Chunk.SIZE
		&& voxelPos.getZ() >= 0 && voxelPos.getZ() < Chunk.SIZE) {
	    return chunk;
	} else {
	    int cx, cy, cz;
	    if (voxelPos.getX() < 0) {
		cx = -1;
		voxelPos.setX(Chunk.SIZE - 1);
	    } else if (voxelPos.getX() >= Chunk.SIZE) {
		cx = 1;
		voxelPos.setX(0);
	    } else {
		cx = 0;
	    }

	    if (voxelPos.getY() < 0) {
		cy = -1;
		voxelPos.setY(Chunk.SIZE - 1);
	    } else if (voxelPos.getY() >= Chunk.SIZE) {
		cy = 1;
		voxelPos.setY(0);
	    } else {
		cy = 0;
	    }

	    if (voxelPos.getZ() < 0) {
		cz = -1;
		voxelPos.setZ(Chunk.SIZE - 1);
	    } else if (voxelPos.getZ() >= Chunk.SIZE) {
		cz = 1;
		voxelPos.setZ(0);
	    } else {
		cz = 0;
	    }

	    return get(Vec3.copyAdd(chunk.getPosition(), cx, cy, cz));
	}
    }

    public void addSpecialVoxel(Chunk chunk, int x, int y, int z) {
	for (SpecialVoxelData data : specialVoxelList) {
	    if (data.x == x && data.y == y && data.z == z && data.chunk == chunk && data.chunk.equals(chunk)) {
		data.active = true;
		return;
	    }
	}
	this.specialVoxelList.add(new SpecialVoxelData(chunk, x, y, z));
    }

    public void removeSpecialVoxel(Chunk chunk, int x, int y, int z) {
	for (SpecialVoxelData data : specialVoxelList) {
	    if (data.x == x && data.y == y && data.z == z && data.chunk == chunk && data.chunk.equals(chunk)) {
		data.active = false;
		return;
	    }
	}
    }
}