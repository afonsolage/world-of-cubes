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

    public static final int WIDTH = 2;
    public static final int HEIGHT = 2;
    public static final int LENGTH = 2;
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
    private final Queue<LightRemoveNode> lightRemovalQueue;
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
	lightRemovalQueue = new LinkedList<>();
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
	    //Check for sunLight;
//	    if (c.getPosition().getY() == HEIGHT - 1) {
//		Voxel v;
//		for (int cx = 0; cx < Chunk.SIZE; cx++) {
//		    for (int cz = 0; cz < Chunk.SIZE; cz++) {
//			v = c.get(cx, Chunk.SIZE - 1, cz);
//			if (v.getType() == Voxel.VT_NONE) {
//			    propagateSunLightQueue.add(new VoxelNode(c, new Vec3(cx, 0, cz), Voxel.LIGHT_SUN));
//			}
//		    }
//		}
//	    }
//	    postMessage(new AreMessage(AreMessageType.CHUNK_LIGHT, c, currentBatch));
	    message.setType(Type.CHUNK_LIGHT);
	    postMessage(message);
	} catch (Exception ex) {
	    ex.printStackTrace();
	} finally {
	    c.unlock();
	}
    }

//    private void removeReflectedSunLight(int batch) {
//	VoxelNode node;
//	while (!removeReflectSunLightQueue.isEmpty()) {
//	    node = removeReflectSunLightQueue.poll();
//
//	    if (node.light == 0) {
//		continue;
//	    }
//
//	    Chunk c = node.chunk;
//	    Chunk tmpC;
//	    Vec3 tmpV = new Vec3();
//
//	    //Top
//	    int light = c.getAreLight(node.x, node.y, node.z, Voxel.VS_TOP);
//	    if (light < node.light) {
//		if (node.y == Chunk.SIZE - 1) {
//		    tmpC = get(Vec3.copyAdd(c.getPosition(), 0, 1, 0));
//		    tmpV.setY(0);
//		    requestChunkUpdate(tmpC, batch);
//		} else {
//		    tmpC = c;
//		    tmpV.set(node.x, node.y + 1, node.z);
//		}
//		tmpC.setLight(tmpV.getX(), tmpV.getY(), tmpV.getZ(), 0);
//		removeReflectSunLightQueue.add(new VoxelNode(tmpC, tmpV, light));
//	    }
//	    //Down
//	    light = c.getAreLight(node.x, node.y, node.z, Voxel.VS_DOWN);
//	    if (light < node.light) {
//		if (node.y == 0) {
//		    tmpC = get(Vec3.copyAdd(c.getPosition(), 0, -1, 0));
//		    tmpV.setY(Chunk.SIZE - 1);
//		    requestChunkUpdate(tmpC, batch);
//		} else {
//		    tmpC = c;
//		    tmpV.set(node.x, node.y - 1, node.z);
//		}
//		tmpC.setLight(tmpV.getX(), tmpV.getY(), tmpV.getZ(), 0);
//		removeReflectSunLightQueue.add(new VoxelNode(tmpC, tmpV, light));
//	    }
//	    //Right
//	    light = c.getAreLight(node.x, node.y, node.z, Voxel.VS_RIGHT);
//	    if (light < node.light) {
//		if (node.x == Chunk.SIZE - 1) {
//		    tmpC = get(Vec3.copyAdd(c.getPosition(), 1, 0, 0));
//		    tmpV.setX(0);
//		    requestChunkUpdate(tmpC, batch);
//		} else {
//		    tmpC = c;
//		    tmpV.set(node.x + 1, node.y, node.z);
//		}
//		tmpC.setLight(tmpV.getX(), tmpV.getY(), tmpV.getZ(), 0);
//		removeReflectSunLightQueue.add(new VoxelNode(tmpC, tmpV, light));
//	    }
//	    //Left
//	    light = c.getAreLight(node.x, node.y, node.z, Voxel.VS_LEFT);
//	    if (light < node.light) {
//		if (node.x == 0) {
//		    tmpC = get(Vec3.copyAdd(c.getPosition(), -1, 0, 0));
//		    tmpV.setX(Chunk.SIZE - 1);
//		    requestChunkUpdate(tmpC, batch);
//		} else {
//		    tmpC = c;
//		    tmpV.set(node.x - 1, node.y, node.z);
//		}
//		tmpC.setLight(tmpV.getX(), tmpV.getY(), tmpV.getZ(), 0);
//		removeReflectSunLightQueue.add(new VoxelNode(tmpC, tmpV, light));
//	    }
//	    //Front
//	    light = c.getAreLight(node.x, node.y, node.z, Voxel.VS_FRONT);
//	    if (light < node.light) {
//		if (node.z == Chunk.SIZE - 1) {
//		    tmpC = get(Vec3.copyAdd(c.getPosition(), 0, 0, 1));
//		    tmpV.setZ(0);
//		    requestChunkUpdate(tmpC, batch);
//		} else {
//		    tmpC = c;
//		    tmpV.set(node.x, node.y, node.z + 1);
//		}
//		tmpC.setLight(tmpV.getX(), tmpV.getY(), tmpV.getZ(), 0);
//		removeReflectSunLightQueue.add(new VoxelNode(tmpC, tmpV, light));
//	    }
//	    //Back
//	    light = c.getAreLight(node.x, node.y, node.z, Voxel.VS_BACK);
//	    if (light < node.light) {
//		if (node.z == 0) {
//		    tmpC = get(Vec3.copyAdd(c.getPosition(), 0, 0, -1));
//		    tmpV.setZ(Chunk.SIZE - 1);
//		    requestChunkUpdate(tmpC, batch);
//		} else {
//		    tmpC = c;
//		    tmpV.set(node.x, node.y, node.z - 1);
//		}
//		tmpC.setLight(tmpV.getX(), tmpV.getY(), tmpV.getZ(), 0);
//		removeReflectSunLightQueue.add(new VoxelNode(tmpC, tmpV, light));
//	    }
//	}
//    }
//
//    /**
//     * Propagate Sun lighting over chunks.
//     *
//     * @param batch The batch to process are messages.
//     */
//    private void propagateSunLight(int batch) {
//	VoxelNode node;
//	Voxel v;
//	//This queue is used to have a BSF algorithm, so we can process lighting as needed, not iterate over
//	//all chunks.
//	while (!propagateSunLightQueue.isEmpty()) {
//	    node = propagateSunLightQueue.poll();
//	    Chunk c = node.chunk;
//
//	    //Since we are propagating sun light, set it on current Voxel and flag it as sky light.
//	    c.setLight(node.x, node.y, node.z, Voxel.LIGHT_SUN);
//	    c.flagSkyLight(node.x, node.y, node.z, true);
//
//	    //Decrease node by one, to propagate light down.
//	    node.y--;
//	    if (node.y < 0) {
//		//We need to check bellow chunk, to propagate light to it.
//		c = get(Vec3.copyAdd(c.getPosition(), 0, -1, 0));
//		if (c == null) {
//		    continue;
//		} else {
//		    //This chunk will receive light, so it needs to be updated.
//		    requestChunkUpdate(c, batch);
//		    node.chunk = c;
//		    node.y = Chunk.SIZE - 1;
//
//		    //If we are on bounds of current chunk, we need to update neigborhood.
//		    if (node.x == 0) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), -1, 0, 0)), batch);
//		    } else if (node.x == Chunk.SIZE - 1) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 1, 0, 0)), batch);
//		    }
//
//		    if (node.z == 0) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 0, -1)), batch);
//		    } else if (node.z == Chunk.SIZE - 1) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 0, 1)), batch);
//		    }
//		}
//	    }
//	    //Let's check for the next voxel, if it is air, propagate the light over it.
//	    v = c.get(node.x, node.y, node.z);
//	    if (v.getType() == Voxel.VT_NONE) {
//		propagateSunLightQueue.add(node);
//	    }
//	}
//    }
//
//    /**
//     * Remove Sun lighting on chunks.
//     *
//     * @param batch The batch to process are messages.
//     */
//    private void removeSunLight(int batch) {
//	VoxelNode node;
//	Voxel v;
//	//This queue is used to have a BSF algorithm, so we can process lighting as needed, not iterate over
//	//all chunks. This queue is populated when a new block is added and it's blocking sun light.
//	while (!removeSunLightQueue.isEmpty()) {
//	    node = removeSunLightQueue.poll();
//	    Chunk c = node.chunk;
//
//	    //If current voxel propagated light, we have to remove light propagation also.
//	    if (c.isFlaggedReflectedLight(node.x, node.y, node.z)) {
//		removeReflectSunLightQueue.add(new VoxelNode(c, node.x, node.y, node.z, c.getLight(node.x, node.y, node.x)));
//	    }
//
//	    //This voxel isn't facing sky animore and it's light will be 0.
//	    c.flagSkyLight(node.x, node.y, node.z, false);
//	    c.setLight(node.x, node.y, node.z, 0);
//
//	    //Let's check the next bellow chunk.
//	    node.y--;
//	    if (node.y < 0) {
//		//If we are on bounds of chunk, we have to check the bellow chunk.
//		c = get(Vec3.copyAdd(c.getPosition(), 0, -1, 0));
//		if (c == null) {
//		    continue;
//		} else {
//		    //This chunk will not receive light anymore, so it needs to be updated.
//		    requestChunkUpdate(c, batch);
//		    node.chunk = c;
//		    node.y = Chunk.SIZE - 1;
//
//		    //If we are on bounds of current chunk, we need to update neigborhood.
//		    if (node.x == 0) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), -1, 0, 0)), batch);
//		    } else if (node.x == Chunk.SIZE - 1) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 1, 0, 0)), batch);
//		    }
//
//		    if (node.z == 0) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 0, -1)), batch);
//		    } else if (node.z == Chunk.SIZE - 1) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 0, 1)), batch);
//		    }
//		}
//	    }
//
//	    //Let's check for the next voxel, if it is air, propagate the light over it.
//	    v = c.get(node.x, node.y, node.z);
//	    if (v.getType() == Voxel.VT_NONE) {
//		removeSunLightQueue.add(node);
//	    }
//	}
//    }
//
//    /**
//     * Reflect the sun light to neigborhood. This function needs to be called after sun light propagation.
//     *
//     * @param batch The batch to process are messages.
//     */
//    private void reflectSunLight(int batch) {
//	//TODO: Add another smart way to propagate light. At moment we check what chunks was modified (flagged to
//	//update) and reflect light on it.
//	for (Chunk c : chunkMap.values()) {
//	    //If this chunk is flagged to be updated, this mean it was changed, so let's check for light propagation.
//	    if (c.isFlaggedToUpdate()) {
//		for (int x = 0; x < Chunk.SIZE; x++) {
//		    for (int y = 0; y < Chunk.SIZE; y++) {
//			for (int z = 0; z < Chunk.SIZE; z++) {
//			    //If current voxel is air and have sun light, we reflect it on all directions
//			    if (c.get(x, y, z).getType() == Voxel.VT_NONE && c.getLight(x, y, z) == Voxel.LIGHT_SUN) {
//
//				//We have to reflect only on directions that have no sun light.
//				if (c.getAreLight(x, y, z, Voxel.VS_TOP) < Voxel.LIGHT_SUN) {
//				    reflectSunLightQueue.add(new VoxelNode(c, x, y, z, Voxel.LIGHT_SUN));
//				} else if (c.getAreLight(x, y, z, Voxel.VS_DOWN) < Voxel.LIGHT_SUN) {
//				    reflectSunLightQueue.add(new VoxelNode(c, x, y, z, Voxel.LIGHT_SUN));
//				} else if (c.getAreLight(x, y, z, Voxel.VS_RIGHT) < Voxel.LIGHT_SUN) {
//				    reflectSunLightQueue.add(new VoxelNode(c, x, y, z, Voxel.LIGHT_SUN));
//				} else if (c.getAreLight(x, y, z, Voxel.VS_LEFT) < Voxel.LIGHT_SUN) {
//				    reflectSunLightQueue.add(new VoxelNode(c, x, y, z, Voxel.LIGHT_SUN));
//				} else if (c.getAreLight(x, y, z, Voxel.VS_FRONT) < Voxel.LIGHT_SUN) {
//				    reflectSunLightQueue.add(new VoxelNode(c, x, y, z, Voxel.LIGHT_SUN));
//				} else if (c.getAreLight(x, y, z, Voxel.VS_BACK) < Voxel.LIGHT_SUN) {
//				    reflectSunLightQueue.add(new VoxelNode(c, x, y, z, Voxel.LIGHT_SUN));
//				}
//			    }
//			}
//		    }
//		}
//	    }
//	}
//
//	//Now that all reflectors voxels was added to queue, let's iterate over our BFS queue.
//	VoxelNode node;
//	while (!reflectSunLightQueue.isEmpty()) {
//	    node = reflectSunLightQueue.poll();
//
//	    //Try to reflect on all directions.
//	    if (reflectSunLightUp(node, batch)
//		    | reflectSunLightDown(node, batch)
//		    | reflectSunLightRight(node, batch)
//		    | reflectSunLightLeft(node, batch)
//		    | reflectSunLightFront(node, batch)
//		    | reflectSunLightBack(node, batch)) {
//		//If we succed in reflecting light, we have to mark this voxel to remove reflection when needed.
//		node.chunk.flagReflectedLight(node.x, node.y, node.z, true);
//	    } else {
//		node.chunk.flagReflectedLight(node.x, node.y, node.z, false);
//	    }
//	}
//    }
//
//    private boolean reflectSunLightUp(VoxelNode node, int batch) {
//	Chunk c;
//	Vec3 v = new Vec3(node.x, node.y + 1, node.z);
//	if (v.getY() > Chunk.SIZE - 1) {
//	    c = get(Vec3.copyAdd(node.chunk.getPosition(), 0, 1, 0));
//	    v.setY(0);
//	} else {
//	    c = node.chunk;
//	}
//
//	return reflectLight(c, v, node.light, batch);
//    }
//
//    private boolean reflectSunLightDown(VoxelNode node, int batch) {
//	Chunk c;
//	Vec3 v = new Vec3(node.x, node.y - 1, node.z);
//	if (v.getY() < 0) {
//	    c = get(Vec3.copyAdd(node.chunk.getPosition(), 0, -1, 0));
//	    v.setY(Chunk.SIZE - 1);
//	} else {
//	    c = node.chunk;
//	}
//
//	return reflectLight(c, v, node.light, batch);
//    }
//
//    private boolean reflectSunLightRight(VoxelNode node, int batch) {
//	Chunk c;
//	Vec3 v = new Vec3(node.x + 1, node.y, node.z);
//	if (v.getX() > Chunk.SIZE - 1) {
//	    c = get(Vec3.copyAdd(node.chunk.getPosition(), 1, 0, 0));
//	    v.setX(0);
//	} else {
//	    c = node.chunk;
//	}
//
//	return reflectLight(c, v, node.light, batch);
//    }
//
//    private boolean reflectSunLightLeft(VoxelNode node, int batch) {
//	Chunk c;
//	Vec3 v = new Vec3(node.x - 1, node.y, node.z);
//	if (v.getX() < 0) {
//	    c = get(Vec3.copyAdd(node.chunk.getPosition(), -1, 0, 0));
//	    v.setX(Chunk.SIZE - 1);
//	} else {
//	    c = node.chunk;
//	}
//
//	return reflectLight(c, v, node.light, batch);
//    }
//
//    private boolean reflectSunLightFront(VoxelNode node, int batch) {
//	Chunk c;
//	Vec3 v = new Vec3(node.x, node.y, node.z + 1);
//	if (v.getZ() > Chunk.SIZE - 1) {
//	    c = get(Vec3.copyAdd(node.chunk.getPosition(), 0, 0, 1));
//	    v.setZ(0);
//	} else {
//	    c = node.chunk;
//	}
//
//	return reflectLight(c, v, node.light, batch);
//    }
//
//    private boolean reflectSunLightBack(VoxelNode node, int batch) {
//	Chunk c;
//	Vec3 v = new Vec3(node.x, node.y, node.z - 1);
//	if (v.getZ() < 0) {
//	    c = get(Vec3.copyAdd(node.chunk.getPosition(), 0, 0, -1));
//	    v.setZ(Chunk.SIZE - 1);
//	} else {
//	    c = node.chunk;
//	}
//
//	return reflectLight(c, v, node.light, batch);
//    }
//
//    /**
//     * Reflect the light on voxel.
//     *
//     * @param c The chunk where the voxel is placed in
//     * @param voxPos The position of voxel
//     * @param light The light value to reflect.
//     * @param batch Current batch, to process are messages.
//     * @return
//     */
//    private boolean reflectLight(Chunk c, Vec3 voxPos, int light, int batch) {
//	light--;
//	if (c != null) {
//	    //If the specified voxel is air.
//	    Voxel v = c.get(voxPos.getX(), voxPos.getY(), voxPos.getZ());
//	    if (v.getType() == Voxel.VT_NONE) {
//		//And it's light is bellow the reflected value minus one.
//		int l = c.getLight(voxPos.getX(), voxPos.getY(), voxPos.getZ());
//		if (l < light) {
//		    //Set the light value and update the chunk.
//		    c.setLight(voxPos.getX(), voxPos.getY(), voxPos.getZ(), light);
//		    requestChunkUpdate(c, batch);
//
//		    //And update the neighborhood if needed.
//		    if (voxPos.getX() == 0) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), -1, 0, 0)), batch);
//		    } else if (voxPos.getX() == Chunk.SIZE - 1) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 1, 0, 0)), batch);
//		    }
//
//		    if (voxPos.getY() == 0) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, -1, 0)), batch);
//		    } else if (voxPos.getY() == Chunk.SIZE - 1) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 1, 0)), batch);
//		    }
//
//		    if (voxPos.getZ() == 0) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 0, -1)), batch);
//		    } else if (voxPos.getZ() == Chunk.SIZE - 1) {
//			requestChunkUpdate(get(Vec3.copyAdd(c.getPosition(), 0, 0, 1)), batch);
//		    }
//
//		    //If the currrent light is greater then 2, this means it can be reflected, so add it to queue.
//		    if (light > 1) {
//			reflectSunLightQueue.add(new VoxelNode(c, voxPos.getX(), voxPos.getY(), voxPos.getZ(), light));
//		    }
//		    return true;
//		}
//	    }
//	}
//	return false;
//    }
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
		c = checkChunkAndVoxelPostion(node.chunk, voxelPos);

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
		c = checkChunkAndVoxelPostion(node.chunk, voxelPos);

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
//	    c.propagateSunLight(message.getBatch());
//	    c.propagateAreaLight();
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

//	areQueue.addListener(AreQueueListener.Type.FINISH, new AreQueueListener(Type.CHUNK_SETUP, false) {
//	    @Override
//	    public void doAction(int batch) {
//		castSunLight(batch);
//	    }
//	});

	process(batch);
    }

//    public void castSunLight(int batch) {
//	Chunk c;
//	Voxel v;
//	for (int x = 0; x < WIDTH; x++) {
//	    for (int z = 0; z < LENGTH; z++) {
//		c = get(x, HEIGHT - 1, z);
//		for (int cx = 0; cx < Chunk.SIZE; cx++) {
//		    for (int cz = 0; cz < Chunk.SIZE; cz++) {
//			v = c.get(cx, Chunk.SIZE - 1, cz);
//			if (v.getType() == Voxel.VT_NONE) {
//			    c.castLighting(cx, Chunk.SIZE - 1, cz, Voxel.LIGHT_SUN);
//			}
//		    }
//		}
//		postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));
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

    protected int getLight(Vec3 chunkPos, int x, int y, int z) {
	Chunk c = get(chunkPos);

	if (c == null) {
	    return -1;
	}

	return c.getLight(x, y, z);
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
    protected void updateVoxel(int x, int y, int z, Voxel v) {
	Vec3 pos = toChunkPosition(x, y, z);
	Chunk c = get(pos);
	if (c == null) {
	    return;
	}

	Vec3 voxelPos = toVoxelPosition(x, y, z);
	c.lock();
	short removedType = c.get(voxelPos).getType();
	c.set(voxelPos, v);
	c.unlock();

	int batch = areQueue.nextBatch();
	postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));

	if (v.getType() == Voxel.VT_TORCH) {
	    c.setLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), 10);
	    lightQueue.add(new LightNode(c, voxelPos));
	    propagateLight(batch);
	    postMessage(new AreMessage(Type.SPECIAL_VOXEL_ATTACH, new SpecialVoxelData(c, voxelPos), batch));
	} else if (v.getType() == Voxel.VT_NONE) {
	    int lightLevel = c.getLight(voxelPos);
	    if (lightLevel > 0) {
		c.setLight(voxelPos, 0);
	    }
	    lightRemovalQueue.add(new LightRemoveNode(c, voxelPos, lightLevel));
	    removeLight(batch);
	    if (Voxel.isSpecial(removedType)) {
		postMessage(new AreMessage(Type.SPECIAL_VOXEL_DETACH, new SpecialVoxelData(c, voxelPos), batch));
	    }
	}

//	Chunk tmpC;
//	Vec3 topV = voxelPos.copy();
//	//If we are removing a Voxel, let's check for light propagation.
//	if (v.getType() == Voxel.VT_NONE) {
//	    //If we are o top bound, let's check top chunk
//	    if (voxelPos.getY() == Chunk.SIZE - 1) {
//		tmpC = get(Vec3.copyAdd(c.getPosition(), 0, 1, 0));
//		topV.setY(0);
//	    } else {
//		tmpC = c;
//		topV.add(0, 1, 0);
//	    }
//
//	    //tmpC and topV will contains the chunk and voxel position to check the current top voxel.
//
//	    if (tmpC != null) {
//		//Get top light.
//		int tmpLight = tmpC.getLight(topV.getX(), topV.getY(), topV.getZ());
//		//If top light is a direct sun light, propagate it.
//		if (tmpLight == Voxel.LIGHT_SUN) {
//		    propagateSunLightQueue.add(new VoxelNode(c, voxelPos, Voxel.LIGHT_SUN));
//		} else {
//		    //Else, we have to reflect lighting for all neighborhood to reflect current voxel.
//		    tmpC = c;
//
//		    //Top
//		    //Get top light. If it is greater then 0, let's reflect.
//		    tmpLight = c.getAreLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), Voxel.VS_TOP);
//		    if (tmpLight > 0) {
//			//If we are on boundary, let's get chunk above and it's down most chunk (y = 0).
//			if (voxelPos.getY() == Chunk.SIZE - 1) {
//			    tmpC = get(Vec3.copyAdd(c.getPosition(), 0, 1, 0));
//			    if (tmpC != null) {
//				reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX(), 0, voxelPos.getZ(), tmpLight));
//			    }
//			} else {
//			    reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX(), voxelPos.getY() + 1, voxelPos.getZ(), tmpLight));
//			}
//		    }
//
//		    //Down
//		    tmpC = c;
//		    tmpLight = tmpC.getAreLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), Voxel.VS_DOWN);
//		    if (tmpLight > 0) {
//			if (voxelPos.getY() == 0) {
//			    tmpC = get(Vec3.copyAdd(c.getPosition(), 0, -1, 0));
//			    if (tmpC != null) {
//				reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX(), Chunk.SIZE - 1, voxelPos.getZ(), tmpLight));
//			    }
//			} else {
//			    reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX(), voxelPos.getY() - 1, voxelPos.getZ(), tmpLight));
//			}
//		    }
//
//		    //Right
//		    tmpC = c;
//		    tmpLight = tmpC.getAreLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), Voxel.VS_RIGHT);
//		    if (tmpLight > 0) {
//			if (voxelPos.getX() == Chunk.SIZE - 1) {
//			    tmpC = get(Vec3.copyAdd(c.getPosition(), 1, 0, 0));
//			    if (tmpC != null) {
//				reflectSunLightQueue.add(new VoxelNode(tmpC, 0, voxelPos.getY(), voxelPos.getZ(), tmpLight));
//			    } else {
//				tmpC = c;
//			    }
//			} else {
//			    reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX() + 1, voxelPos.getY(), voxelPos.getZ(), tmpLight));
//			}
//		    }
//
//		    //Left
//		    tmpC = c;
//		    tmpLight = tmpC.getAreLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), Voxel.VS_LEFT);
//		    if (tmpLight > 0) {
//			if (voxelPos.getX() == 0) {
//			    tmpC = get(Vec3.copyAdd(c.getPosition(), -1, 0, 0));
//			    if (tmpC != null) {
//				reflectSunLightQueue.add(new VoxelNode(tmpC, Chunk.SIZE - 1, voxelPos.getY(), voxelPos.getZ(), tmpLight));
//			    }
//			} else {
//			    reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX() - 1, voxelPos.getY(), voxelPos.getZ(), tmpLight));
//			}
//		    }
//
//		    //Front
//		    tmpC = c;
//		    tmpLight = tmpC.getAreLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), Voxel.VS_FRONT);
//		    if (tmpLight > 0) {
//			if (voxelPos.getZ() == Chunk.SIZE - 1) {
//			    tmpC = get(Vec3.copyAdd(c.getPosition(), 0, 0, 1));
//			    if (tmpC != null) {
//				reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX(), voxelPos.getY(), 0, tmpLight));
//			    }
//			} else {
//			    reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX(), voxelPos.getY(), voxelPos.getZ() + 1, tmpLight));
//			}
//		    }
//
//		    //Back
//		    tmpC = c;
//		    tmpLight = tmpC.getAreLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ(), Voxel.VS_BACK);
//		    if (tmpLight > 0) {
//			if (voxelPos.getZ() == 0) {
//			    tmpC = get(Vec3.copyAdd(c.getPosition(), 0, 0, -1));
//			    if (tmpC != null) {
//				reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX(), voxelPos.getY(), Chunk.SIZE - 1, tmpLight));
//			    }
//			} else {
//			    reflectSunLightQueue.add(new VoxelNode(tmpC, voxelPos.getX(), voxelPos.getY(), voxelPos.getZ() - 1, tmpLight));
//			}
//		    }
//		}
//	    } else {
//		//If top chunk is null, this means we are on world top edge.
//		propagateSunLightQueue.add(new VoxelNode(c, voxelPos, Voxel.LIGHT_SUN));
//	    }
//	} else {
//	    //Else, we have to check for light blocking.
//	    int previousLight = c.getLight(voxelPos.getX(), voxelPos.getY(), voxelPos.getZ());
//
//	    if (previousLight == Voxel.LIGHT_SUN) {
//		removeSunLightQueue.add(new VoxelNode(c, voxelPos, 0));
//	    } else if (previousLight > 0) {
//		removeReflectSunLightQueue.add(new VoxelNode(c, voxelPos, previousLight));
//	    }
//	}

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

    private Chunk checkChunkAndVoxelPostion(Chunk chunk, Vec3 voxelPos) {
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