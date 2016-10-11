package com.lagecompany.woc.storage;

import java.util.Iterator;
// import static com.lagecompany.storage.AreMessage.AreMessageType.CHUNK_UPDATE;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Afonso Lage
 */
class AreQueue {

	public static final int FIRST_BATCH = 1;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> unloadQueue;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> setupQueue;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> lightQueue;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> loadQueue;

	private final ConcurrentLinkedQueue<ChunkMesh> attachQueue;
	private final ConcurrentLinkedQueue<Vec3> detachQueue;

	private final ConcurrentHashMap<AreQueueListener.Type, ConcurrentLinkedQueue<AreQueueListener>> listeners;
	private int batch = FIRST_BATCH;

	public AreQueue() {
		unloadQueue = new ConcurrentHashMap<>();
		setupQueue = new ConcurrentHashMap<>();
		lightQueue = new ConcurrentHashMap<>();
		loadQueue = new ConcurrentHashMap<>();
		listeners = new ConcurrentHashMap<>();

		attachQueue = new ConcurrentLinkedQueue<>();
		detachQueue = new ConcurrentLinkedQueue<>();
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

	private void offer(ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> map, AreQueueEntry message) {
		ConcurrentLinkedQueue<AreQueueEntry> queue = map.get(message.getBatch());

		if (queue == null) {
			queue = new ConcurrentLinkedQueue<>();
			map.put(message.getBatch(), queue);
		}

		queue.offer(message);
	}

	private void offerUnique(ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> map,
			AreQueueEntry message) {
		ConcurrentLinkedQueue<AreQueueEntry> queue = map.get(message.getBatch());

		if (queue == null) {
			queue = new ConcurrentLinkedQueue<>();
			map.put(message.getBatch(), queue);
		}

		for (AreQueueEntry msg : queue) {
			if (msg.equals(message)) {
				return;
			}
		}
		queue.offer(message);
	}

	public void queue(AreQueueEntry message, boolean unique) {
		if (message.getBatch() < 0) {
			message.setBatch(nextBatch());
		}

		ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> queue;

		switch (message.getState()) {
		case SETUP: {
			queue = setupQueue;
			break;
		}
		case LIGHT: {
			queue = lightQueue;
			break;
		}
		case LOAD: {
			queue = loadQueue;
			break;
		}
		case UNLOAD: {
			queue = unloadQueue;
			break;
		}
		default: {
			System.out.println("Invalid message type received: " + message.getState().name());
			return;
		}
		}

		if (unique) {
			offerUnique(queue, message);
		} else {
			offer(queue, message);
		}
	}

	public ConcurrentLinkedQueue<AreQueueEntry> getQueue(Chunk.State state, int batch) {
		ConcurrentLinkedQueue<AreQueueEntry> result;

		switch (state) {
		case SETUP: {
			result = setupQueue.get(batch);
			break;
		}
		case LIGHT: {
			result = lightQueue.get(batch);
			break;
		}
		case LOAD: {
			result = loadQueue.get(batch);
			break;
		}
		case UNLOAD: {
			result = unloadQueue.get(batch);
			break;
		}
		default: {
			System.out.println("Invalid message type received: " + state.name());
			result = null;
		}
		}

		return result;
	}

	public void finishBatch(Chunk.State state, int batch) {
		switch (state) {
		case SETUP: {
			setupQueue.remove(batch);
			break;
		}
		case LIGHT: {
			lightQueue.remove(batch);
			break;
		}
		case LOAD: {
			loadQueue.remove(batch);
			break;
		}
		default: {
			System.out.println("Invalid message type received: " + state.name());
		}
		}

		ConcurrentLinkedQueue<AreQueueListener> queue = listeners.get(AreQueueListener.Type.FINISH);
		if (queue != null) {
			for (Iterator<AreQueueListener> it = queue.iterator(); it.hasNext();) {
				AreQueueListener listener = it.next();
				listener.checkDoAction(state, batch);
				if (listener.shouldRemove()) {
					it.remove();
				}
			}
		}
	}

	private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> getMap(Chunk.State state) {
		switch (state) {
		case SETUP: {
			return setupQueue;
		}
		case LIGHT: {
			return lightQueue;
		}
		case LOAD: {
			return loadQueue;
		}
		case UNLOAD: {
			return unloadQueue;
		}
		default: {
			System.out.println("Invalid message type received: " + state.name());
			return null;
		}
		}
	}

	public int getQueueSize(Chunk.State state) {
		int result = 0;
		ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> map = getMap(state);

		if (map != null) {
			for (@SuppressWarnings("rawtypes")
			ConcurrentLinkedQueue queue : map.values()) {
				if (queue != null) {
					result += queue.size();
				}
			}
		}

		return result;
	}

	public void offerAttach(ChunkMesh mesh) {
		attachQueue.offer(mesh);
	}

	public ChunkMesh pollAttach() {
		return attachQueue.poll();
	}

	public void offerDetach(Vec3 v) {
		detachQueue.offer(v);
	}

	public Vec3 pollDetach() {
		return detachQueue.poll();
	}
}
