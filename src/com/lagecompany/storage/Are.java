package com.lagecompany.storage;

import com.lagecompany.storage.voxel.Voxel;
import com.lagecompany.storage.AreMessage.Type;
import static com.lagecompany.storage.AreMessage.Type.SPECIAL_VOXEL_ATTACH;
import com.lagecompany.storage.light.LightNode;
import com.lagecompany.storage.light.LightRemoveNode;
import com.lagecompany.storage.voxel.SpecialVoxelData;
import com.lagecompany.storage.voxel.VoxelReference;
import com.lagecompany.util.MathUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An Are is made of 10 chunks². It is an analogy to the real Are which is 10
 * m². It doesnt store any voxels, just chunks references.
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
        this.init();
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
            c.computeSunlight();
            message.setType(Type.CHUNK_LIGHT);
            postMessage(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            c.unlock();
        }
    }

    public void requestChunkUpdate(Chunk c) {
        if (c != null && c.isLoaded()) {
            c.lock();
            c.flagToUpdate();
            c.reset();
            c.unlock();
            postMessage(new AreMessage(Type.CHUNK_LIGHT, c, currentBatch), true);
        }
    }

    private void lightChunk(AreMessage message) {
        Chunk c = (Chunk) message.getData();
        try {
            c.computeSunlightReflection();
            c.propagateSunlight();

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

        areQueue.addListener(AreQueueListener.Type.FINISH, new AreQueueListener(Type.CHUNK_LOAD, false) {
            @Override
            public void doAction(int batch) {
                inited = true;
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

    protected VoxelReference getVoxel(Chunk c, int x, int y, int z) {
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

            return getVoxel(c.getPosition().copy().add(cx, cy, cx), x, y, z);
        }
    }

    protected VoxelReference getVoxel(Vec3 chunkPos, int x, int y, int z) {
        Chunk c = get(chunkPos);

        if (c == null) {
            return null;
        }

        return c.get(x, y, z);
    }

    protected void setLight(Chunk c, int x, int y, int z, int light) {
        if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
            c.get(x, y, z).setLight((byte) (light));
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

            setLight(c.getPosition().copy().add(cx, cy, cx), x, y, z, light);
        }
    }

    protected void setLight(Vec3 chunkPos, int x, int y, int z, int light) {
        Chunk c = get(chunkPos);

        if (c == null) {
            return;
        }

        c.get(x, y, z).setLight((byte) light);
    }

    protected int getSunLight(Vec3 chunkPos, int x, int y, int z) {
        Chunk c = get(chunkPos);

        if (c == null) {
            return -1;
        }

        return c.get(x, y, z).getSunLight();
    }

    protected int getSunLight(Chunk c, int x, int y, int z) {
        if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
            return c.get(x, y, z).getSunLight();
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

        return c.get(x, y, z).getLight();
    }

    protected int getLight(Chunk c, int x, int y, int z) {
        if (x >= 0 && x < Chunk.SIZE && y >= 0 && y < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
            return c.get(x, y, z).getLight();
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

            return getLight(c.getPosition().copy().add(cx, cy, cx), x, y, z);
        }
    }

    //Convert given coordinates to Chunk index on Are.
    public Vec3 toChunkPosition(int x, int y, int z) {
        return new Vec3(MathUtils.floorDiv(x + ARE_OFFSET.x, Chunk.SIZE),
                MathUtils.floorDiv(y + ARE_OFFSET.y, Chunk.SIZE),
                MathUtils.floorDiv(z + ARE_OFFSET.z, Chunk.SIZE));
    }

    //Convert given coordinates to Voxel index on Chunk.
    public Vec3 toVoxelPosition(int x, int y, int z) {
        return new Vec3(MathUtils.absMod(x + ARE_OFFSET.x, Chunk.SIZE),
                MathUtils.absMod(y + ARE_OFFSET.y, Chunk.SIZE),
                MathUtils.absMod(z + ARE_OFFSET.z, Chunk.SIZE));
    }

    /*
     * Update a Voxel in a given position. The position must be in World
     * Coodinates.
     */
    protected void updateVoxel(int x, int y, int z, short type) {
        Vec3 pos = toChunkPosition(x, y, z);
        Chunk c = get(pos);
        if (c == null) {
            return;
        }

        Vec3 voxelPos = toVoxelPosition(x, y, z), tmpVec = new Vec3();

        c.lock();
        VoxelReference voxel = c.get(voxelPos);
        voxel.reset();
        voxel.setType(type);
        c.unlock();

        Chunk tmpChunk;
        for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
            tmpVec.set(voxelPos.x + dir.x, voxelPos.y + dir.y, voxelPos.z + dir.z);
            tmpChunk = validateChunkAndVoxel(c, tmpVec);

            tmpChunk.addSunLightPropagationQueue(tmpChunk.get(tmpVec));
        }

        int batch = areQueue.nextBatch();
        postMessage(new AreMessage(Type.CHUNK_LIGHT, c, batch));

        updateNeighborhood(pos, voxelPos, batch);

        process(batch);
    }

    public void updateNeighborhood(Vec3 chunkPos, Vec3 voxelPos, int batch) {
        Chunk c;
        if (voxelPos.x == 0) {
            c = get(chunkPos.copy().add(-1, 0, 0));
            if (c != null) {
                postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
            }
        } else if (voxelPos.x == Chunk.SIZE - 1) {
            c = get(chunkPos.copy().add(1, 0, 0));
            if (c != null) {
                postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
            }
        }

        if (voxelPos.y == 0) {
            c = get(chunkPos.copy().add(0, -1, 0));
            if (c != null) {
                postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
            }
        } else if (voxelPos.y == Chunk.SIZE - 1) {
            c = get(chunkPos.copy().add(0, 1, 0));
            if (c != null) {
                postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
            }
        }

        if (voxelPos.z == 0) {
            c = get(chunkPos.copy().add(0, 0, -1));
            if (c != null) {
                postMessage(new AreMessage(Type.CHUNK_LOAD, c, batch));
            }
        } else if (voxelPos.z == Chunk.SIZE - 1) {
            c = get(chunkPos.copy().add(0, 0, 1));
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

        if (direction.x > 0) {
            moveRight(batch);
        } else if (direction.x < 0) {
            moveLeft(batch);
        }

        if (direction.y > 0) {
            moveUp(batch);
        } else if (direction.y < 0) {
            moveDown(batch);
        }

        if (direction.z > 0) {
            moveFront(batch);
        } else if (direction.z < 0) {
            moveBack(batch);
        }

        process(batch);
        moving = false;
    }

    /**
     * Move all chunks into X+ direction. The left most chunk is set to NULL and
     * the right most is created. Since Are position is the LEFT, BOTTOM, Back
     * cornder, we need to remove the 0 X axis chunks and create a new one at
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
        return new Vec3((chunkPosition.x * Chunk.SIZE) - ARE_OFFSET.x,
                (chunkPosition.y * Chunk.SIZE) - ARE_OFFSET.y, //TODO: Add "- (DATA_HEIGHT / 2)"
                chunkPosition.z * Chunk.SIZE - ARE_OFFSET.z);
    }

    public Vec3 getRelativePosition(Vec3 chunkPosition) {
        return getRelativePosition(chunkPosition.x, chunkPosition.y, chunkPosition.z);
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
        updateVoxel(v.x, v.y, v.z, type);
    }

    /**
     * Validade if given voxel position returns a valid voxel. It checks for
     * chunk boundary and return the chunk and voxel based on it.
     *
     * @param chunk The current Chunk to be validated.
     * @param voxelPos Desired voxel position. This will be updated if given one
     * extrapolates chunk boundary.
     * @return A valid chunk to access the voxel position.
     */
    public Chunk validateChunkAndVoxel(Chunk chunk, Vec3 voxelPos) {
        if (voxelPos.x >= 0 && voxelPos.x < Chunk.SIZE
                && voxelPos.y >= 0 && voxelPos.y < Chunk.SIZE
                && voxelPos.z >= 0 && voxelPos.z < Chunk.SIZE) {
            return chunk;
        } else {
            int cx, cy, cz;
            if (voxelPos.x < 0) {
                cx = -1;
                voxelPos.x = Chunk.SIZE - 1;
            } else if (voxelPos.x >= Chunk.SIZE) {
                cx = 1;
                voxelPos.x = 0;
            } else {
                cx = 0;
            }

            if (voxelPos.y < 0) {
                cy = -1;
                voxelPos.y = Chunk.SIZE - 1;
            } else if (voxelPos.y >= Chunk.SIZE) {
                cy = 1;
                voxelPos.y = 0;
            } else {
                cy = 0;
            }

            if (voxelPos.z < 0) {
                cz = -1;
                voxelPos.z = Chunk.SIZE - 1;
            } else if (voxelPos.z >= Chunk.SIZE) {
                cz = 1;
                voxelPos.z = 0;
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
