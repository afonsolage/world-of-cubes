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

	public static final int BEGIN_BATCH = 1;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> detachQueue;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> unloadQueue;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> setupQueue;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> lightQueue;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> loadQueue;
	// private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreMessage>> updateQueue;
	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> attachQueue;
//	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> specialVoxelAttachQueue;
//	private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<AreQueueEntry>> specialVoxelDetachQueue;
	private final ConcurrentHashMap<AreQueueListener.Type, ConcurrentLinkedQueue<AreQueueListener>> listeners;
	private int batch = BEGIN_BATCH;

	public AreQueue() {
		detachQueue = new ConcurrentHashMap<>();
		unloadQueue = new ConcurrentHashMap<>();
		setupQueue = new ConcurrentHashMap<>();
		lightQueue = new ConcurrentHashMap<>();
		loadQueue = new ConcurrentHashMap<>();
		// updateQueue = new ConcurrentHashMap<>();
		attachQueue = new ConcurrentHashMap<>();
		listeners = new ConcurrentHashMap<>();
//		specialVoxelAttachQueue = new ConcurrentHashMap<>();
//		specialVoxelDetachQueue = new ConcurrentHashMap<>();
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
		case DETACH: {
			queue = detachQueue;
			break;
		}
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
		// case CHUNK_UPDATE: {
		// offer(updateQueue, message, b);
		// break;
		// }
		case ATTACH: {
			queue = attachQueue;
			break;
		}
		// case SPECIAL_VOXEL_ATTACH: {
		// queue = specialVoxelAttachQueue;
		// break;
		// }
		// case SPECIAL_VOXEL_DETACH: {
		// queue = specialVoxelDetachQueue;
		// break;
		// }
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
		case DETACH: {
			result = detachQueue.get(batch);
			break;
		}
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
		// case CHUNK_UPDATE: {
		// result = updateQueue.get(batch);
		// break;
		// }
		case ATTACH: {
			result = attachQueue.get(batch);
			break;
		}
		// case SPECIAL_VOXEL_ATTACH: {
		// result = specialVoxelAttachQueue.get(batch);
		// break;
		// }
		// case SPECIAL_VOXEL_DETACH: {
		// result = specialVoxelDetachQueue.get(batch);
		// break;
		// }
		default: {
			System.out.println("Invalid message type received: " + state.name());
			result = null;
		}
		}

		return result;
	}

	public void finishBatch(Chunk.State state, int batch) {
		switch (state) {
		case DETACH: {
			detachQueue.remove(batch);
			break;
		}
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
		case UNLOAD: {
			unloadQueue.remove(batch);
			break;
		}
		// case CHUNK_UPDATE: {
		// updateQueue.remove(batch);
		// break;
		// }
		case ATTACH: {
			attachQueue.remove(batch);
			break;
		}
		// case SPECIAL_VOXEL_ATTACH: {
		// specialVoxelAttachQueue.remove(batch);
		// break;
		// }
		// case SPECIAL_VOXEL_DETACH: {
		// specialVoxelDetachQueue.remove(batch);
		// break;
		// }
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
		case DETACH: {
			return detachQueue;
		}
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
		// case CHUNK_UPDATE: {
		// return updateQueue;
		// }
		case ATTACH: {
			return attachQueue;
		}
		// case SPECIAL_VOXEL_ATTACH: {
		// return specialVoxelAttachQueue;
		// }
		// case SPECIAL_VOXEL_DETACH: {
		// return specialVoxelDetachQueue;
		// }
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
}
