package com.lagecompany.woc.storage;

import java.util.Iterator;
//import static com.lagecompany.storage.AreMessage.AreMessageType.CHUNK_UPDATE;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Afonso Lage
 */
public class AreQueue {

    public static final int BEGIN_BATCH = 1;
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> detachQueue;
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> unloadQueue;
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> setupQueue;
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> lightQueue;
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> loadQueue;
//    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> updateQueue;
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> attachQueue;
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> specialVoxelAttachQueue;
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> specialVoxelDetachQueue;
    private final ConcurrentHashMap<AreQueueListener.Type, ConcurrentLinkedQueue<AreQueueListener>> listeners;
    private int batch = BEGIN_BATCH;

    public AreQueue() {
	detachQueue = new ConcurrentHashMap<>();
	unloadQueue = new ConcurrentHashMap<>();
	setupQueue = new ConcurrentHashMap<>();
	lightQueue = new ConcurrentHashMap<>();
	loadQueue = new ConcurrentHashMap<>();
//	updateQueue = new ConcurrentHashMap<>();
	attachQueue = new ConcurrentHashMap<>();
	listeners = new ConcurrentHashMap<>();
	specialVoxelAttachQueue = new ConcurrentHashMap<>();
	specialVoxelDetachQueue = new ConcurrentHashMap<>();
    }

    public synchronized int nextBatch() {
	return batch++;
    }

    public void addListener(AreQueueListener.Type type, AreQueueListener listener) {
	ConcurrentLinkedQueue<AreQueueListener> queue = listeners.get(type);
	if (queue == null) {
	    queue = new ConcurrentLinkedQueue<>();
	    listeners.put(type, queue);
	}
	queue.add(listener);
    }

    private void offer(ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> map, AreMessage message) {
	ConcurrentLinkedQueue<AreMessage> queue = map.get(message.getBatch());

	if (queue == null) {
	    queue = new ConcurrentLinkedQueue<>();
	    map.put(message.getBatch(), queue);
	}

	queue.offer(message);
    }

    private void offerUnique(ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> map, AreMessage message) {
	ConcurrentLinkedQueue<AreMessage> queue = map.get(message.getBatch());

	if (queue == null) {
	    queue = new ConcurrentLinkedQueue<>();
	    map.put(message.getBatch(), queue);
	}

	for (AreMessage msg : queue) {
	    if (msg.equals(message)) {
		return;
	    }
	}
	queue.offer(message);
    }

    public void queue(AreMessage message, boolean unique) {
	if (message.getBatch() < 0) {
	    message.setBatch(nextBatch());
	}

	ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> queue;

	switch (message.getType()) {
	    case CHUNK_DETACH: {
		queue = detachQueue;
		break;
	    }
	    case CHUNK_SETUP: {
		queue = setupQueue;
		break;
	    }
	    case CHUNK_LIGHT: {
		queue = lightQueue;
		break;
	    }
	    case CHUNK_LOAD: {
		queue = loadQueue;
		break;
	    }
	    case CHUNK_UNLOAD: {
		queue = unloadQueue;
		break;
	    }
//	    case CHUNK_UPDATE: {
//		offer(updateQueue, message, b);
//		break;
//	    }
	    case CHUNK_ATTACH: {
		queue = attachQueue;
		break;
	    }
	    case SPECIAL_VOXEL_ATTACH: {
		queue = specialVoxelAttachQueue;
		break;
	    }
	    case SPECIAL_VOXEL_DETACH: {
		queue = specialVoxelDetachQueue;
		break;
	    }
	    default: {
		System.out.println("Invalid message type received: " + message.getType().name());
		return;
	    }
	}

	if (unique) {
	    offerUnique(queue, message);
	} else {
	    offer(queue, message);
	}
    }

    public ConcurrentLinkedQueue<AreMessage> getQueue(AreMessage.Type type, int batch) {
	ConcurrentLinkedQueue<AreMessage> result;

	switch (type) {
	    case CHUNK_DETACH: {
		result = detachQueue.get(batch);
		break;
	    }
	    case CHUNK_SETUP: {
		result = setupQueue.get(batch);
		break;
	    }
	    case CHUNK_LIGHT: {
		result = lightQueue.get(batch);
		break;
	    }
	    case CHUNK_LOAD: {
		result = loadQueue.get(batch);
		break;
	    }
	    case CHUNK_UNLOAD: {
		result = unloadQueue.get(batch);
		break;
	    }
//	    case CHUNK_UPDATE: {
//		result = updateQueue.get(batch);
//		break;
//	    }
	    case CHUNK_ATTACH: {
		result = attachQueue.get(batch);
		break;
	    }
	    case SPECIAL_VOXEL_ATTACH: {
		result = specialVoxelAttachQueue.get(batch);
		break;
	    }
	    case SPECIAL_VOXEL_DETACH: {
		result = specialVoxelDetachQueue.get(batch);
		break;
	    }
	    default: {
		System.out.println("Invalid message type received: " + type.name());
		result = null;
	    }
	}

	return result;
    }

    public void finishBatch(AreMessage.Type type, int batch) {
	switch (type) {
	    case CHUNK_DETACH: {
		detachQueue.remove(batch);
		break;
	    }
	    case CHUNK_SETUP: {
		setupQueue.remove(batch);
		break;
	    }
	    case CHUNK_LIGHT: {
		lightQueue.remove(batch);
		break;
	    }
	    case CHUNK_LOAD: {
		loadQueue.remove(batch);
		break;
	    }
	    case CHUNK_UNLOAD: {
		unloadQueue.remove(batch);
		break;
	    }
//	    case CHUNK_UPDATE: {
//		updateQueue.remove(batch);
//		break;
//	    }
	    case CHUNK_ATTACH: {
		attachQueue.remove(batch);
		break;
	    }
	    case SPECIAL_VOXEL_ATTACH: {
		specialVoxelAttachQueue.remove(batch);
		break;
	    }
	    case SPECIAL_VOXEL_DETACH: {
		specialVoxelDetachQueue.remove(batch);
		break;
	    }
	    default: {
		System.out.println("Invalid message type received: " + type.name());
	    }
	}

	ConcurrentLinkedQueue<AreQueueListener> queue = listeners.get(AreQueueListener.Type.FINISH);
	if (queue != null) {
	    for (Iterator<AreQueueListener> it = queue.iterator(); it.hasNext();) {
		AreQueueListener listener = it.next();
		listener.checkDoAction(type, batch);
		if (listener.shouldRemove()) {
		    it.remove();
		}
	    }
	}
    }

    private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> getMap(AreMessage.Type type) {
	switch (type) {
	    case CHUNK_DETACH: {
		return detachQueue;
	    }
	    case CHUNK_SETUP: {
		return setupQueue;
	    }
	    case CHUNK_LIGHT: {
		return lightQueue;
	    }
	    case CHUNK_LOAD: {
		return loadQueue;
	    }
	    case CHUNK_UNLOAD: {
		return unloadQueue;
	    }
//	    case CHUNK_UPDATE: {
//		return updateQueue;
//	    }
	    case CHUNK_ATTACH: {
		return attachQueue;
	    }
	    case SPECIAL_VOXEL_ATTACH: {
		return specialVoxelAttachQueue;
	    }
	    case SPECIAL_VOXEL_DETACH: {
		return specialVoxelDetachQueue;
	    }
	    default: {
		System.out.println("Invalid message type received: " + type.name());
		return null;
	    }
	}
    }

    public int getQueueSize(AreMessage.Type type) {
	int result = 0;
	ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> map = getMap(type);

	if (map != null) {
	    for (@SuppressWarnings("rawtypes") ConcurrentLinkedQueue queue : map.values()) {
		if (queue != null) {
		    result += queue.size();
		}
	    }
	}

	return result;
    }
}
