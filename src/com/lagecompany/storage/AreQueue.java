package com.lagecompany.storage;

import static com.lagecompany.storage.AreMessage.AreMessageType.CHUNK_ATTACH;
import static com.lagecompany.storage.AreMessage.AreMessageType.CHUNK_DETACH;
import static com.lagecompany.storage.AreMessage.AreMessageType.CHUNK_LOAD;
import static com.lagecompany.storage.AreMessage.AreMessageType.CHUNK_SETUP;
import static com.lagecompany.storage.AreMessage.AreMessageType.CHUNK_UNLOAD;
import static com.lagecompany.storage.AreMessage.AreMessageType.CHUNK_UPDATE;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Afonso Lage
 */
public class AreQueue {

    public static final int BEGIN_BATCH = 1;
    private final HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> detachQueue;
    private final HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> unloadQueue;
    private final HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> setupQueue;
    private final HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> loadQueue;
    private final HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> updateQueue;
    private final HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> attachQueue;
    private int batch = BEGIN_BATCH;

    public AreQueue() {
	detachQueue = new HashMap<>();
	unloadQueue = new HashMap<>();
	setupQueue = new HashMap<>();
	loadQueue = new HashMap<>();
	updateQueue = new HashMap<>();
	attachQueue = new HashMap<>();
    }

    public synchronized int nextBatch() {
	return batch++;
    }

    private void offer(HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> map, AreMessage message, int batch) {
	ConcurrentLinkedQueue<AreMessage> queue = map.get(batch);

	if (queue == null) {
	    queue = new ConcurrentLinkedQueue<>();
	    map.put(batch, queue);
	}

	queue.offer(message);
    }

    public void queue(AreMessage message) {
	int b = message.getBatch();

	if (b < 0) {
	    b = nextBatch();
	}

	switch (message.getType()) {
	    case CHUNK_DETACH: {
		offer(detachQueue, message, b);
		break;
	    }
	    case CHUNK_SETUP: {
		offer(setupQueue, message, b);
		break;
	    }
	    case CHUNK_LOAD: {
		offer(loadQueue, message, b);
		break;
	    }
	    case CHUNK_UNLOAD: {
		offer(unloadQueue, message, b);
		break;
	    }
	    case CHUNK_UPDATE: {
		offer(updateQueue, message, b);
		break;
	    }
	    case CHUNK_ATTACH: {
		offer(attachQueue, message, b);
		break;
	    }
	    default: {
		System.out.println("Invalid message type received: " + message.getType().name());
	    }
	}
    }

    public ConcurrentLinkedQueue<AreMessage> getQueue(AreMessage.AreMessageType type, int batch) {
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
	    case CHUNK_LOAD: {
		result = loadQueue.get(batch);
		break;
	    }
	    case CHUNK_UNLOAD: {
		result = unloadQueue.get(batch);
		break;
	    }
	    case CHUNK_UPDATE: {
		result = updateQueue.get(batch);
		break;
	    }
	    case CHUNK_ATTACH: {
		result = attachQueue.get(batch);
		break;
	    }
	    default: {
		System.out.println("Invalid message type received: " + type.name());
		result = null;
	    }
	}

	return result;
    }

    public void finishBatch(AreMessage.AreMessageType type, int batch) {
	switch (type) {
	    case CHUNK_DETACH: {
		detachQueue.remove(batch);
		break;
	    }
	    case CHUNK_SETUP: {
		setupQueue.remove(batch);
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
	    case CHUNK_UPDATE: {
		updateQueue.remove(batch);
		break;
	    }
	    case CHUNK_ATTACH: {
		attachQueue.remove(batch);
		break;
	    }
	    default: {
		System.out.println("Invalid message type received: " + type.name());
	    }
	}
    }

    private HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> getMap(AreMessage.AreMessageType type) {
	switch (type) {
	    case CHUNK_DETACH: {
		return detachQueue;
	    }
	    case CHUNK_SETUP: {
		return setupQueue;
	    }
	    case CHUNK_LOAD: {
		return loadQueue;
	    }
	    case CHUNK_UNLOAD: {
		return unloadQueue;
	    }
	    case CHUNK_UPDATE: {
		return updateQueue;
	    }
	    case CHUNK_ATTACH: {
		return attachQueue;
	    }
	    default: {
		System.out.println("Invalid message type received: " + type.name());
		return null;
	    }
	}
    }

    public int getQueueSize(AreMessage.AreMessageType type) {
	int result = 0;
	HashMap<Integer, ConcurrentLinkedQueue<AreMessage>> map = getMap(type);

	if (map != null) {
	    for (ConcurrentLinkedQueue queue : map.values()) {
		if (queue != null) {
		    result += queue.size();
		}
	    }
	}
	
	return result;
    }
}
