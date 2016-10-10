package com.lagecompany.woc.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.lagecompany.woc.storage.light.LightManager;
import com.lagecompany.woc.storage.voxel.VoxelReference;
import com.lagecompany.woc.util.MathUtils;

/**
 * An Are is made of 10 chunks². It is an analogy to the real Are which is 10 m². It doesnt store any voxels, just
 * chunks references.
 *
 * @author afonsolage
 */
public class Are implements Runnable {

	public static final int WIDTH = 16;
	public static final int HEIGHT = 16;
	public static final int LENGTH = 16;
	public static final int DATA_WIDTH = WIDTH * Chunk.SIZE;
	public static final int DATA_HEIGHT = HEIGHT * Chunk.SIZE;
	public static final int DATA_LENGHT = LENGTH * Chunk.SIZE;
	public static final int SIZE = WIDTH * HEIGHT * LENGTH;
	public static final Vec3 ARE_OFFSET = new Vec3((Are.DATA_WIDTH / 2), 8 * Chunk.SIZE, (Are.DATA_LENGHT / 2));
	private final HashMap<Vec3, Chunk> chunkMap;
	// private final BlockingQueue<AreQueueEntry> actionQueue;
	private final BlockingDeque<Integer> processBatchQueue;
	private final ConcurrentLinkedQueue<Integer> renderBatchQueue;
	private final AreQueue areQueue;
	// private final ConcurrentLinkedQueue<SpecialVoxelData> specialVoxelList;
	private Vec3 position;
	private boolean moving;
	private boolean inited = false;
	// private final AreWorker worker;

	private int currentBatch;
	private int currentRenderingBatch;

	private boolean running = false;

	public Are() {
		Thread.currentThread().setName("Are Thread");
		position = new Vec3();

		chunkMap = new HashMap<>();
		// actionQueue = new LinkedBlockingQueue<>();
		processBatchQueue = new LinkedBlockingDeque<>();
		renderBatchQueue = new ConcurrentLinkedQueue<>();
		// specialVoxelList = new ConcurrentLinkedQueue<>();

		areQueue = new AreQueue();
		LightManager.setup(this);
	}

	public void stop() {
		this.running = false;
	}

	@Override
	public void run() {
		this.running = true;
		this.init();
		boolean worked;
		ConcurrentLinkedQueue<AreQueueEntry> queue;
		while (running) {
			worked = false;
			try {
				currentBatch = processBatchQueue.take();

				queue = areQueue.getQueue(Chunk.State.UNLOAD, currentBatch);
				if (queue != null) {
					for (AreQueueEntry msg = queue.poll(); msg != null; msg = queue.poll()) {
						unloadChunk(msg);
					}
					areQueue.finishBatch(Chunk.State.UNLOAD, currentBatch);
				}

				queue = areQueue.getQueue(Chunk.State.SETUP, currentBatch);
				if (queue != null) {
					for (AreQueueEntry msg = queue.poll(); msg != null; msg = queue.poll()) {
						setupChunk(msg);
						worked = true;
					}
					areQueue.finishBatch(Chunk.State.SETUP, currentBatch);
				}

				queue = areQueue.getQueue(Chunk.State.LIGHT, currentBatch);
				if (queue != null) {
					for (AreQueueEntry msg = queue.poll(); msg != null; msg = queue.poll()) {
						lightChunk(msg);
						worked = true;
					}
					areQueue.finishBatch(Chunk.State.LIGHT, currentBatch);
				}

				queue = areQueue.getQueue(Chunk.State.LOAD, currentBatch);
				if (queue != null) {
					for (AreQueueEntry msg = queue.poll(); msg != null; msg = queue.poll()) {
						loadChunk(msg);
					}
					areQueue.finishBatch(Chunk.State.LOAD, currentBatch);
				}

				if (worked) {
					processNow(currentBatch);
				}

				renderBatchQueue.offer(currentBatch);
			} catch (InterruptedException ex) {
				break;
			}
		}
	}

	private void unloadChunk(AreQueueEntry message) {
		Chunk c = (Chunk) message.getChunk();

		try {
			c.lock();
			if (c.getCurrentState() != Chunk.State.UNLOAD) {
				System.out.println("Invalid chunk state " + c.getCurrentState() + ". Expected: " + Chunk.State.UNLOAD);
				return;
			}
			c.unload();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			c.setCurrentState(Chunk.State.FINISH);
			c.unlock();
		}
	}

	private void setupChunk(AreQueueEntry message) {
		Chunk c = (Chunk) message.getChunk();

		try {
			c.lock();
			if (c.getCurrentState() != Chunk.State.SETUP) {
				System.out.println("Invalid chunk state " + c.getCurrentState() + ". Expected: " + Chunk.State.SETUP);
				return;
			}

			c.setup();
			c.computeSunlight();
			c.computeSunlightReflection();
			c.setCurrentState(Chunk.State.LIGHT);
			message.setState(Chunk.State.LIGHT);
			postMessage(message);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			c.unlock();
		}
	}

	public void requestChunkUpdate(Chunk c, int batch) {
		if (c != null) {
			c.lock();
			c.setCurrentState(Chunk.State.LIGHT);
			postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, (batch >= 0) ? batch : currentBatch), true);
			c.unlock();
		}
	}

	private void lightChunk(AreQueueEntry message) {
		Chunk c = (Chunk) message.getChunk();
		try {
			if (c.getCurrentState() != Chunk.State.LIGHT) {
				System.out.println("Invalid chunk state " + c.getCurrentState() + ". Expected: " + Chunk.State.LIGHT);
				return;
			}

			c.reset();
			c.removeSunlight();
			c.removeLight();
			c.propagateSunlight();
			c.propagateLight();

			if (c.hasVisibleVoxel()) {
				c.setCurrentState(Chunk.State.LOAD);
				message.setState(Chunk.State.LOAD);
			} else {
				c.setCurrentState(Chunk.State.DETACH);
				message.setState(Chunk.State.DETACH);
			}
			postMessage(message);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void loadChunk(AreQueueEntry message) {
		Chunk c = (Chunk) message.getChunk();
		try {
			c.lock();
			if (c.getCurrentState() != Chunk.State.LOAD) {
				System.out.println("Invalid chunk state " + c.getCurrentState() + ". Expected: " + Chunk.State.LOAD);
				return;
			}

			if (c.load()) {
				c.setCurrentState(Chunk.State.ATTACH);
				message.setState(Chunk.State.ATTACH);
			} else {
				c.setCurrentState(Chunk.State.DETACH);
				message.setState(Chunk.State.DETACH);
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
			// Due to sun light, we need to always load chunks from top-down.
			for (int x = 0; x < WIDTH; x++) {
				for (int y = HEIGHT - 1; y >= 0; y--) {
					v = new Vec3(x, y, z);
					c = new Chunk(this, v);
					set(v, c);
					c.setCurrentState(Chunk.State.SETUP);
					postMessage(new AreQueueEntry(Chunk.State.SETUP, c, batch));
				}
			}
		}

		areQueue.addListener(AreQueueListener.Type.FINISH, new AreQueueListener(Chunk.State.LOAD, false) {
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

	protected VoxelReference getVoxel(Chunk c, VoxelReference voxel) {
		if (voxel.position.x >= 0 && voxel.position.x < Chunk.SIZE && voxel.position.y >= 0
				&& voxel.position.y < Chunk.SIZE && voxel.position.z >= 0 && voxel.position.z < Chunk.SIZE) {
			return (c.get(voxel) ? voxel : null);
		} else {
			int cx, cy, cz;

			if (voxel.position.x < 0) {
				cx = -1;
				voxel.position.x = Chunk.SIZE - 1;
			} else if (voxel.position.x >= Chunk.SIZE) {
				cx = 1;
				voxel.position.x = 0;
			} else {
				cx = 0;
			}

			if (voxel.position.y < 0) {
				cy = -1;
				voxel.position.y = Chunk.SIZE - 1;
			} else if (voxel.position.y >= Chunk.SIZE) {
				cy = 1;
				voxel.position.y = 0;
			} else {
				cy = 0;
			}

			if (voxel.position.z < 0) {
				cz = -1;
				voxel.position.z = Chunk.SIZE - 1;
			} else if (voxel.position.z >= Chunk.SIZE) {
				cz = 1;
				voxel.position.z = 0;
			} else {
				cz = 0;
			}

			return getVoxel(c.getPosition().copy().add(cx, cy, cz), voxel);
		}
	}

	public VoxelReference getVoxel(Vec3 chunkPos, VoxelReference voxel) {
		Chunk c = get(chunkPos);

		if (c == null) {
			return null;
		}

		return (c.get(voxel) ? voxel : null);
	}

	// Convert given coordinates to Chunk index on Are.
	public Vec3 toChunkPosition(int x, int y, int z) {
		return new Vec3(MathUtils.floorDiv(x + ARE_OFFSET.x, Chunk.SIZE),
				MathUtils.floorDiv(y + ARE_OFFSET.y, Chunk.SIZE), MathUtils.floorDiv(z + ARE_OFFSET.z, Chunk.SIZE));
	}

	// Convert given coordinates to Voxel index on Chunk.
	public Vec3 toVoxelPosition(int x, int y, int z) {
		return new Vec3(MathUtils.absMod(x + ARE_OFFSET.x, Chunk.SIZE), MathUtils.absMod(y + ARE_OFFSET.y, Chunk.SIZE),
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
		try {
			c.lock();

			Vec3 voxelPos = toVoxelPosition(x, y, z);

			VoxelReference voxel = c.createReference(voxelPos.x, voxelPos.y, voxelPos.z);

			c.updateVoxelType(voxel, type);

			int batch = areQueue.nextBatch();
			requestChunkUpdate(c, batch);
			updateNeighborhood(pos, voxelPos, batch);

			process(batch);
		} finally {
			c.unlock();
		}
	}

	public void updateNeighborhood(Vec3 chunkPos, Vec3 voxelPos, int batch) {
		Vec3 tmpVec = new Vec3();
		Chunk tmpChunk;

		batch = (batch >= 0) ? batch : currentBatch;

		// Side neighbors
		if (voxelPos.x == 0) {
			// Update left neighbor chunk;
			tmpVec.set(chunkPos.x - 1, chunkPos.y, chunkPos.z);
			tmpChunk = get(tmpVec);
			if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
				requestChunkUpdate(tmpChunk, batch);
			}

			if (voxelPos.y == 0) {
				tmpVec.set(chunkPos.x - 1, chunkPos.y - 1, chunkPos.z);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.y == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x - 1, chunkPos.y + 1, chunkPos.z);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}

			if (voxelPos.z == 0) {
				tmpVec.set(chunkPos.x - 1, chunkPos.y, chunkPos.z - 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.z == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x - 1, chunkPos.y, chunkPos.z + 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}

		} else if (voxelPos.x >= Chunk.SIZE - 1) {
			// Update right neighbor chunk;
			tmpVec.set(chunkPos.x + 1, chunkPos.y, chunkPos.z);
			tmpChunk = get(tmpVec);
			if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
				requestChunkUpdate(tmpChunk, batch);
			}

			if (voxelPos.y == 0) {
				tmpVec.set(chunkPos.x + 1, chunkPos.y - 1, chunkPos.z);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.y == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x + 1, chunkPos.y + 1, chunkPos.z);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}

			if (voxelPos.z == 0) {
				tmpVec.set(chunkPos.x + 1, chunkPos.y, chunkPos.z - 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.z == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x + 1, chunkPos.y, chunkPos.z + 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}
		}

		if (voxelPos.y == 0) {
			// Update bottom neighbor chunk;
			tmpVec.set(chunkPos.x, chunkPos.y - 1, chunkPos.z);
			tmpChunk = get(tmpVec);
			if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
				requestChunkUpdate(tmpChunk, batch);
			}

			if (voxelPos.x == 0) {
				tmpVec.set(chunkPos.x - 1, chunkPos.y - 1, chunkPos.z);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.x == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x + 1, chunkPos.y - 1, chunkPos.z);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}

			if (voxelPos.z == 0) {
				tmpVec.set(chunkPos.x, chunkPos.y - 1, chunkPos.z - 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.z == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x, chunkPos.y - 1, chunkPos.z + 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}
		} else if (voxelPos.y == Chunk.SIZE - 1) {
			// Update up neighbor chunk;
			tmpVec.set(chunkPos.x, chunkPos.y + 1, chunkPos.z);
			tmpChunk = get(tmpVec);
			if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
				requestChunkUpdate(tmpChunk, batch);
			}

			if (voxelPos.x == 0) {
				tmpVec.set(chunkPos.x - 1, chunkPos.y + 1, chunkPos.z);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.x == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x + 1, chunkPos.y + 1, chunkPos.z);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}

			if (voxelPos.z == 0) {
				tmpVec.set(chunkPos.x, chunkPos.y + 1, chunkPos.z - 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.z == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x, chunkPos.y + 1, chunkPos.z + 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}
		}

		if (voxelPos.z == 0) {
			// Update back neighbor chunk;
			tmpVec.set(chunkPos.x, chunkPos.y, chunkPos.z - 1);
			tmpChunk = get(tmpVec);
			if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
				requestChunkUpdate(tmpChunk, batch);
			}

			if (voxelPos.x == 0) {
				tmpVec.set(chunkPos.x - 1, chunkPos.y, chunkPos.z - 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.x == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x + 1, chunkPos.y, chunkPos.z - 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}

			if (voxelPos.y == 0) {
				tmpVec.set(chunkPos.x, chunkPos.y - 1, chunkPos.z - 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.y == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x, chunkPos.y + 1, chunkPos.z - 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}
		} else if (voxelPos.z >= Chunk.SIZE - 1) {
			// Update front neighbor chunk;
			tmpVec.set(chunkPos.x, chunkPos.y, chunkPos.z + 1);
			tmpChunk = get(tmpVec);
			if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
				requestChunkUpdate(tmpChunk, batch);
			}

			if (voxelPos.x == 0) {
				tmpVec.set(chunkPos.x - 1, chunkPos.y, chunkPos.z + 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.x == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x + 1, chunkPos.y, chunkPos.z + 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			}

			if (voxelPos.y == 0) {
				tmpVec.set(chunkPos.x, chunkPos.y - 1, chunkPos.z + 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
			} else if (voxelPos.y == Chunk.SIZE - 1) {
				tmpVec.set(chunkPos.x, chunkPos.y + 1, chunkPos.z + 1);
				tmpChunk = get(tmpVec);
				if (tmpChunk != null && tmpChunk.getCurrentState() != Chunk.State.LIGHT) {
					requestChunkUpdate(tmpChunk, batch);
				}
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

	// public void move(AreQueueEntry message) {
	// Vec3 direction = (Vec3) message.getChunk();
	//
	// if (direction == null || direction.equals(Vec3.ZERO)) {
	// moving = false;
	// return;
	// }
	//
	// position.add(direction);
	//
	// int batch = areQueue.nextBatch();
	//
	// if (direction.x > 0) {
	// moveRight(batch);
	// } else if (direction.x < 0) {
	// moveLeft(batch);
	// }
	//
	// if (direction.y > 0) {
	// moveUp(batch);
	// } else if (direction.y < 0) {
	// moveDown(batch);
	// }
	//
	// if (direction.z > 0) {
	// moveFront(batch);
	// } else if (direction.z < 0) {
	// moveBack(batch);
	// }
	//
	// process(batch);
	// moving = false;
	// }
	//
	// /**
	// * Move all chunks into X+ direction. The left most chunk is set to NULL and the right most is created. Since Are
	// * position is the LEFT, BOTTOM, Back cornder, we need to remove the 0 X axis chunks and create a new one at
	// * DATA_WIDTH + 1 X axis.
	// */
	// private void moveRight(int batch) {
	// int boundBegin = 0;
	// int boundEnd = WIDTH - 1;
	// for (int y = 0; y < HEIGHT; y++) {
	// for (int z = 0; z < LENGTH; z++) {
	// // Remove the left most and detach it from scene.
	// Chunk c = get(position.copy().add(boundBegin - 1, y, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.UNLOAD, c, batch));
	// postMessage(new AreQueueEntry(Chunk.State.DETACH, c, batch));
	// }
	//
	// // Reload chunk at new left border.
	// c = get(position.copy().add(boundBegin, y, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	//
	// // Add new chunks at right most of Are.
	// Vec3 v = position.copy().add(boundEnd, y, z);
	// c = new Chunk(this, v);
	// set(v, c);
	// postMessage(new AreQueueEntry(Chunk.State.SETUP, c, batch));
	//
	// // Reload the previous right border.
	// c = get(position.copy().add(boundEnd - 1, y, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	// }
	// }
	// }
	//
	// private void moveLeft(int batch) {
	// int boundBegin = WIDTH - 1;
	// int boundEnd = 0;
	// for (int y = 0; y < HEIGHT; y++) {
	// for (int z = 0; z < LENGTH; z++) {
	// // Remove the left most and detach it from scene.
	// Chunk c = get(position.copy().add(boundBegin + 1, y, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.UNLOAD, c, batch));
	// postMessage(new AreQueueEntry(Chunk.State.DETACH, c, batch));
	// }
	//
	// // Reload chunk at new left border.
	// c = get(position.copy().add(boundBegin, y, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	//
	// // Add new chunks at right most of Are.
	// Vec3 v = position.copy().add(boundEnd, y, z);
	// c = new Chunk(this, v);
	// set(v, c);
	// postMessage(new AreQueueEntry(Chunk.State.SETUP, c, batch));
	//
	// // Reload the previous right border.
	// c = get(position.copy().add(boundEnd + 1, y, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	// }
	// }
	// }
	//
	// private void moveFront(int batch) {
	// int boundBegin = 0;
	// int boundEnd = LENGTH - 1;
	// for (int y = 0; y < HEIGHT; y++) {
	// for (int x = 0; x < WIDTH; x++) {
	// // Remove the back most and detach it from scene.
	// Chunk c = get(position.copy().add(x, y, boundBegin - 1));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.UNLOAD, c, batch));
	// postMessage(new AreQueueEntry(Chunk.State.DETACH, c, batch));
	// }
	//
	// // Reload chunk at new back border.
	// c = get(position.copy().add(x, y, boundBegin));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	//
	// // Add new chunks at front most of Are.
	// Vec3 v = position.copy().add(x, y, boundEnd);
	// c = new Chunk(this, v);
	// set(v, c);
	// postMessage(new AreQueueEntry(Chunk.State.SETUP, c, batch));
	//
	// // Reload the previous front border.
	// c = get(position.copy().add(x, y, boundEnd - 1));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	// }
	// }
	// }
	//
	// private void moveBack(int batch) {
	// int boundBegin = LENGTH - 1;
	// int boundEnd = 0;
	// for (int y = 0; y < HEIGHT; y++) {
	// for (int x = 0; x < WIDTH; x++) {
	// // Remove the back most and detach it from scene.
	// Chunk c = get(position.copy().add(x, y, boundBegin + 1));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.UNLOAD, c, batch));
	// postMessage(new AreQueueEntry(Chunk.State.DETACH, c, batch));
	// }
	//
	// // Reload chunk at new back border.
	// c = get(position.copy().add(x, y, boundBegin));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	//
	// // Add new chunks at front most of Are.
	// Vec3 v = position.copy().add(x, y, boundEnd);
	// c = new Chunk(this, v);
	// set(v, c);
	// postMessage(new AreQueueEntry(Chunk.State.SETUP, c, batch));
	//
	// // Reload the previous front border.
	// c = get(position.copy().add(x, y, boundEnd + 1));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	// }
	// }
	// }
	//
	// private void moveUp(int batch) {
	// int boundBegin = 0;
	// int boundEnd = HEIGHT - 1;
	// for (int x = 0; x < WIDTH; x++) {
	// for (int z = 0; z < LENGTH; z++) {
	// // Remove the back most and detach it from scene.
	// Chunk c = get(position.copy().add(x, boundBegin - 1, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.UNLOAD, c, batch));
	// postMessage(new AreQueueEntry(Chunk.State.DETACH, c, batch));
	// }
	//
	// // Reload chunk at new back border.
	// c = get(position.copy().add(x, boundBegin, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	//
	// // Add new chunks at front most of Are.
	// Vec3 v = position.copy().add(x, boundEnd, z);
	// c = new Chunk(this, v);
	// set(v, c);
	// postMessage(new AreQueueEntry(Chunk.State.SETUP, c, batch));
	//
	// // Reload the previous front border.
	// c = get(position.copy().add(x, boundEnd - 1, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	// }
	// }
	// }
	//
	// private void moveDown(int batch) {
	// int boundBegin = HEIGHT - 1;
	// int boundEnd = 0;
	// for (int x = 0; x < WIDTH; x++) {
	// for (int z = 0; z < LENGTH; z++) {
	// // Remove the back most and detach it from scene.
	// Chunk c = get(position.copy().add(x, boundBegin + 1, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.UNLOAD, c, batch));
	// postMessage(new AreQueueEntry(Chunk.State.DETACH, c, batch));
	// }
	//
	// // Reload chunk at new back border.
	// c = get(position.copy().add(x, boundBegin, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	//
	// // Add new chunks at front most of Are.
	// Vec3 v = position.copy().add(x, boundEnd, z);
	// c = new Chunk(this, v);
	// set(v, c);
	// postMessage(new AreQueueEntry(Chunk.State.SETUP, c, batch));
	//
	// // Reload the previous front border.
	// c = get(position.copy().add(x, boundEnd + 1, z));
	// if (c != null) {
	// postMessage(new AreQueueEntry(Chunk.State.LIGHT, c, batch));
	// }
	// }
	// }
	// }

	public Vec3 getAbsoluteChunkPosition(Vec3 chunkPosition) {
		return new Vec3((chunkPosition.x * Chunk.SIZE) - ARE_OFFSET.x, (chunkPosition.y * Chunk.SIZE) - ARE_OFFSET.y, // TODO:
																														// Add
																														// "-
																														// (DATA_HEIGHT
																														// /
																														// 2)"
				chunkPosition.z * Chunk.SIZE - ARE_OFFSET.z);
	}

	public Vec3 getRelativePosition(Vec3 chunkPosition) {
		return getRelativePosition(chunkPosition.x, chunkPosition.y, chunkPosition.z);
	}

	public Vec3 getRelativePosition(int x, int y, int z) {
		// TODO: Add "y + (DATA_HEIGHT / 2)"
		return new Vec3(x + (DATA_WIDTH / 2), y + (8 * Chunk.SIZE), (z + (DATA_LENGHT / 2)));
	}

	public void postMessage(AreQueueEntry message) {
		postMessage(message, false);
	}

	public void postMessage(AreQueueEntry message, boolean unique) {
		areQueue.queue(message, unique);
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

	public ConcurrentLinkedQueue<AreQueueEntry> getQueue(Integer batch, Chunk.State state) {
		return areQueue.getQueue(state, batch);
	}

	public void finishBatch(Chunk.State state, Integer batch) {
		areQueue.finishBatch(state, batch);
	}

	public float getChunkQueueSize(Chunk.State state) {
		return areQueue.getQueueSize(state);
	}

	public void setVoxel(Vec3 v, short type) {
		updateVoxel(v.x, v.y, v.z, type);
	}

	/**
	 * Validade if given voxel position returns a valid voxel. It checks for chunk boundary and return the chunk and
	 * voxel based on it.
	 *
	 * @param chunk
	 *            The current Chunk to be validated.
	 * @param voxelPos
	 *            Desired voxel position. This will be updated if given one extrapolates chunk boundary.
	 * @return A valid chunk to access the voxel position.
	 */
	public Chunk validateChunkAndVoxel(Chunk chunk, Vec3 voxelPos) {
		if (voxelPos.x >= 0 && voxelPos.x < Chunk.SIZE && voxelPos.y >= 0 && voxelPos.y < Chunk.SIZE && voxelPos.z >= 0
				&& voxelPos.z < Chunk.SIZE) {
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

	public void detach(Chunk c) {
		postMessage(new AreQueueEntry(Chunk.State.DETACH, c, currentRenderingBatch));
	}

	public void unload(Chunk c) {
		postMessage(new AreQueueEntry(Chunk.State.UNLOAD, c, currentRenderingBatch));
	}

	public List<Chunk> getAttachChunks() {
		List<Chunk> chunks = new ArrayList<>();

		ConcurrentLinkedQueue<AreQueueEntry> queue = getQueue(currentRenderingBatch, Chunk.State.ATTACH);
		if (queue != null) {
			for (AreQueueEntry message = queue.poll(); message != null; message = queue.poll()) {
				chunks.add(message.getChunk());
			}
			finishBatch(Chunk.State.ATTACH, currentRenderingBatch);
		}

		return chunks;
	}

	public List<Chunk> getDetachChunks() {
		List<Chunk> chunks = new ArrayList<>();

		ConcurrentLinkedQueue<AreQueueEntry> queue = getQueue(currentRenderingBatch, Chunk.State.DETACH);
		if (queue != null) {
			for (AreQueueEntry message = queue.poll(); message != null; message = queue.poll()) {
				chunks.add(message.getChunk());
			}
			finishBatch(Chunk.State.DETACH, currentRenderingBatch);
		}

		return chunks;
	}

	public boolean prepareUpdate() {
		Integer batch = renderBatchQueue.poll();
		if (batch == null) {
			return false;
		} else {
			currentRenderingBatch = batch; 
			return true;
		}
	}

	// public void addSpecialVoxel(Chunk chunk, int x, int y, int z) {
	// for (SpecialVoxelData data : specialVoxelList) {
	// if (data.x == x && data.y == y && data.z == z && data.chunk == chunk && data.chunk.equals(chunk)) {
	// data.active = true;
	// return;
	// }
	// }
	// this.specialVoxelList.add(new SpecialVoxelData(chunk, x, y, z));
	// }
	//
	// public void removeSpecialVoxel(Chunk chunk, int x, int y, int z) {
	// for (SpecialVoxelData data : specialVoxelList) {
	// if (data.x == x && data.y == y && data.z == z && data.chunk == chunk && data.chunk.equals(chunk)) {
	// data.active = false;
	// return;
	// }
	// }
	// }

	// public void requestSpecialVoxelAttach(SpecialVoxelData data) {
	// postMessage(new AreMessage(SPECIAL_VOXEL_ATTACH, data, currentBatch + 1));
	// process(currentBatch + 1);
	// }
	//
	// public void requestSpecialVoxelDetach(SpecialVoxelData data) {
	// postMessage(new AreMessage(Type.SPECIAL_VOXEL_DETACH, data, currentBatch + 1));
	// process(currentBatch + 1);
	// }
}
