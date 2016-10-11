package com.lagecompany.woc.storage;

import static com.lagecompany.woc.storage.voxel.Voxel.VS_BACK;
import static com.lagecompany.woc.storage.voxel.Voxel.VS_DOWN;
import static com.lagecompany.woc.storage.voxel.Voxel.VS_FRONT;
import static com.lagecompany.woc.storage.voxel.Voxel.VS_LEFT;
import static com.lagecompany.woc.storage.voxel.Voxel.VS_RIGHT;
import static com.lagecompany.woc.storage.voxel.Voxel.VS_TOP;
import static com.lagecompany.woc.storage.voxel.Voxel.VT_NONE;
import static com.lagecompany.woc.storage.voxel.Voxel.VT_STONE;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import com.lagecompany.woc.storage.light.LightData;
import com.lagecompany.woc.storage.light.LightManager;
import com.lagecompany.woc.storage.light.LightRemovalNode;
import com.lagecompany.woc.storage.voxel.Voxel;
import com.lagecompany.woc.storage.voxel.VoxelReference;
import com.lagecompany.woc.util.MathUtils;
import com.lagecompany.woc.util.TerrainNoise;

public class Chunk {

	public enum State {
		EMPTY, SETUP, LIGHT, LOAD, UNLOAD, FINISH;
	}

	public static final int SIZE = 16;
	public static final int SIZE_SHIFT_Y = 4;
	public static final int SIZE_SHIFT_X = 8;

	public static final int DATA_LENGTH = SIZE * SIZE * SIZE;
	public static final int X_UNIT = SIZE * SIZE;
	public static final int Y_UNIT = SIZE;
	public static final int Z_UNIT = 1;

	public static final int SIDE_TOP = 0;
	public static final int SIDE_RIGHT = 1;
	public static final int SIDE_DOWN = 2;
	public static final int SIDE_LEFT = 3;
	public static final int SIDE_FRONT = 4;
	public static final int SIDE_BACK = 5;
	public static final int SIDE_COUNT = 6;

	private ChunkMesh mesh;
	private final ChunkBuffer chunkBuffer;
	private final Queue<VoxelReference> sunlightPropagationQueue;
	private final Queue<LightRemovalNode> sunlightRemovalQueue;
	private final Queue<VoxelReference> lightPropagationQueue;
	private final Queue<LightRemovalNode> lightRemovalQueue;
	private LightData[] lightDataArray;
	private int visibleVoxels;
	private final Are are;
	private final Vec3 position;
	private final String name;
	private State currentState;
	private final Chunk[] neighborhood;

	public static int oppositeSide(int side) {
		assert side >= 0 && side < SIDE_COUNT;

		if (side == SIDE_TOP)
			return SIDE_DOWN;
		else if (side == SIDE_DOWN)
			return SIDE_TOP;
		else if (side == SIDE_RIGHT)
			return SIDE_LEFT;
		else if (side == SIDE_LEFT)
			return SIDE_RIGHT;
		else if (side == SIDE_FRONT)
			return SIDE_BACK;
		else
			return SIDE_FRONT;
	}

	public Chunk(Are are, Vec3 position) {
		this.are = are;
		this.position = position;
		this.name = "Chunk " + position.toString();
		this.chunkBuffer = new ChunkBuffer();
		this.sunlightPropagationQueue = new LinkedList<>();
		this.sunlightRemovalQueue = new LinkedList<>();
		this.lightPropagationQueue = new LinkedList<>();
		this.lightRemovalQueue = new LinkedList<>();
		this.currentState = State.EMPTY;
		this.neighborhood = new Chunk[SIDE_COUNT];
	}

	public String getName() {
		return name;
	}

	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}

	public void setLightData(LightData data) {
		int index = (int) (data.index / Voxel.SIZE);

		assert index < lightDataArray.length;

		lightDataArray[index] = data;
	}

	public LightData findLightData(int x, int y, int z) {
		return findLightData(MathUtils.toVoxelIndex(x, y, z) * 6); // 6 sides.
	}

	public LightData findLightData(int index) {
		index = (int) (index / Voxel.SIZE);

		assert index < lightDataArray.length;

		return lightDataArray[index];
	}

	public void setNeighbor(int side, Chunk neighbor) {
		assert side < neighborhood.length;

		neighborhood[side] = neighbor;
	}

	public VoxelReference cloneReference(VoxelReference voxel) {
		return createReference(voxel.position.x, voxel.position.y, voxel.position.z);
	}

	public VoxelReference createReference(int x, int y, int z) {
		VoxelReference voxel = new VoxelReference();
		voxel.position.set(x, y, z);
		chunkBuffer.get(voxel);
		return voxel;
	}

	public boolean get(VoxelReference voxel) {
		return chunkBuffer.get(voxel);
	}

	// void set(VoxelReference newVoxel) {
	// chunkBuffer.set(newVoxel);
	// }

	public void set(VoxelReference voxel, short type) {
		chunkBuffer.set(voxel, type);
	}

	public void reset() {
		chunkBuffer.resetMergedVisible();
	}

	public boolean load() {
		reset();
		checkVisibleFaces();
		mergeVisibleFaces();

		return mesh.isValid();
	}

	/**
	 * Setup chunk using a SimplexNoise algorithm. This function should be called whenever the chunk should be filled
	 * with voxels.
	 */
	public void setup() {
		int px = position.x;
		int py = position.y;
		int pz = position.z;

		short type;
		VoxelReference voxel = new VoxelReference(); // Temp variable.

		for (int z = 0; z < SIZE; z++) {
			voxel.position.z = z;
			for (int x = 0; x < SIZE; x++) {
				voxel.position.x = x;
				double noiseHeight = TerrainNoise.getHeight(x + px * SIZE, z + pz * SIZE);
				for (int y = SIZE - 1; y >= 0; y--) {
					voxel.position.y = y;
					if (y + py * SIZE < noiseHeight && !(py == Are.HEIGHT - 1 && y == SIZE - 1)) {
						type = VT_STONE;
						visibleVoxels++;
						set(voxel, type);
					}
				}
			}
		}
	}

	/**
	 * Compute all voxels that receive direct sunlight. This method uses a TOP-DOWN approach and propagate light
	 * downwards until it finds an opaque voxel. Due to performance issues, other method will reflect sun light.
	 */
	public void computeSunlight() {
		Chunk top = are.get(position.x, position.y + 1, position.z);

		Queue<VoxelReference> lightQueue = new LinkedList<>();
		VoxelReference voxel = new VoxelReference();
		VoxelReference topVoxel = new VoxelReference();

		// We'll always look at the top most voxel on chunk.
		voxel.position.y = Chunk.SIZE - 1;

		if (top == null) {
			// If there is no top chunk, this means we are receiving sunlight directly.
			for (int x = 0; x < Chunk.SIZE; x++) {
				voxel.position.x = x;
				for (int z = 0; z < Chunk.SIZE; z++) {
					voxel.position.z = z;
					if (get(voxel) && voxel.isTransparent()) {
						voxel.setSunLight(Voxel.SUN_LIGHT);
						lightQueue.add(createReference(voxel.position.x, voxel.position.y - 1, voxel.position.z));
					}
				}
			}
		} else {
			// Else, check if top chunk has sunlight on voxels at y = 0;
			for (int x = 0; x < Chunk.SIZE; x++) {
				voxel.position.x = x;
				for (int z = 0; z < Chunk.SIZE; z++) {
					voxel.position.z = z;

					if (!get(voxel) || voxel.isOpaque()) {
						continue;
					}

					topVoxel.position.set(x, 0, z);
					if (top.get(topVoxel)) {
						if (topVoxel.getSunLight() == Voxel.SUN_LIGHT) {
							voxel.setSunLight(Voxel.SUN_LIGHT);
							lightQueue.add(createReference(voxel.position.x, voxel.position.y - 1, voxel.position.z));
						}
					}
				}
			}
		}

		// Lets iterate over light bfs queue;
		while (!lightQueue.isEmpty()) {
			// Get next queue element;
			voxel = lightQueue.poll();

			if (voxel.isTransparent() && voxel.getSunLight() != Voxel.SUN_LIGHT) {
				// Set sunlight and if there is more voxels bellow it, propagate also.
				voxel.setSunLight(Voxel.SUN_LIGHT);
				if (voxel.position.y > 0) {

					// Reuse current voxel obect;
					voxel.position.y -= 1;
					if (get(voxel)) {
						lightQueue.add(voxel);
					}
				}
			}
		}
	}

	/**
	 * Compute sun light reflection on voxels that are surrounded by direct sun light voxels. This method first search
	 * for all voxels that have a neighbor with direct sun light (sunLight = Voxel.SUN_LIGHT), then it set it's own sun
	 * light power (Voxel.SUN_LIGHT - 1 if the sunlight isn't above it) and is added to sunlightPropagationQueue.
	 */
	public void computeSunlightReflection() {
		VoxelReference voxel = new VoxelReference();
		VoxelReference neighbor = new VoxelReference();

		// Search for voxels surrounded by sunLight (15) and propagate this light.
		for (int y = 0; y < Chunk.SIZE; y++) {
			voxel.position.y = y;
			for (int x = 0; x < Chunk.SIZE; x++) {
				voxel.position.x = x;
				for (int z = 0; z < Chunk.SIZE; z++) {
					voxel.position.z = z;
					get(voxel);

					// If this voxel is transparent and haven't the max sunLight
					if (voxel.isTransparent() && voxel.getSunLight() < Voxel.SUN_LIGHT - 1) {
						// Look in all directions for a voxel with direct sun light (sunLight = Voxel.SUN_LIGHT)
						for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
							neighbor.position.set(voxel.position.x + dir.x, voxel.position.y + dir.y,
									voxel.position.z + dir.z);

							getVoxelOnNeighborhood(neighbor);

							if (!neighbor.isReferenceValid())
								continue;

							// If this voxel has direct sun light
							if (neighbor.isTransparent() && neighbor.getSunLight() == Voxel.SUN_LIGHT) {
								// Set sunLight on current voxel and add to propagation queue. If the direct sun light
								// voxel is upwards, then
								// this voxel receives direct sun light, else Voxel.SUN_LIGHT - 1.
								voxel.setSunLight((byte) ((dir == Vec3.UP) ? Voxel.SUN_LIGHT : Voxel.SUN_LIGHT - 1));
								sunlightPropagationQueue.add(cloneReference(voxel));
								break;
							}
						}
					}
				}
			}
		}
	}

	public void removeSunlight() {
		int previousLightLevel, neighborLightLevel;

		VoxelReference neighborVoxel = new VoxelReference();
		LightRemovalNode node;
		Chunk tmpChunk;

		while (!sunlightRemovalQueue.isEmpty()) {
			node = sunlightRemovalQueue.poll();

			previousLightLevel = node.previousLight;

			// If previous light level is equals or less then 1, we can't remove neighbor light.
			if (previousLightLevel <= 1) {
				continue;
			}

			// Look for all neighbors and check for light removal.
			for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
				neighborVoxel.position.set(node.x + dir.x, node.y + dir.y, node.z + dir.z);

				tmpChunk = getVoxelOnNeighborhood(neighborVoxel);

				if (!neighborVoxel.isReferenceValid()) {
					continue;
				}

				neighborLightLevel = neighborVoxel.getSunLight();

				// If this neighbor isn't transparent or is already dark (no sun light), there is nothing to do with it.
				if (neighborVoxel.isTransparent() || neighborLightLevel > 0) {
					if (neighborLightLevel < previousLightLevel
							|| (dir == Vec3.DOWN && previousLightLevel == Voxel.SUN_LIGHT)) {
						// If this neighbor has a light lower then our previousLight OR our previous sunlight is direct
						// sun light (max) and we are looking at our
						// downwards neighbor, we will remove it and propagate it's removal.
						neighborVoxel.setSunLight((byte) 0);
						tmpChunk.sunlightRemovalQueue.add(new LightRemovalNode(neighborVoxel.position.x,
								neighborVoxel.position.y, neighborVoxel.position.z, neighborLightLevel));
					} else if (neighborLightLevel >= previousLightLevel) {
						// Else, if this neighbor has a light equals or higher then our previous light, then we need to
						// propagate it's light,
						// so we may receive it on sunLightPropagation
						tmpChunk.addSunLightPropagationQueue(tmpChunk.cloneReference(neighborVoxel));
					}
				}

				if (tmpChunk != this && tmpChunk.currentState != Chunk.State.LIGHT) {
					are.requestChunkUpdate(tmpChunk, -1);
				}
			}
		}
	}

	/**
	 * Propagate sun light across neigbor voxels. This method uses a Passive Flood Fill algorithm.
	 */
	public void propagateSunlight() {
		int lightLevel;
		byte propagatedLightLevel;

		VoxelReference neighborVoxel = new VoxelReference(), voxel;
		Chunk tmpChunk;

		while (!sunlightPropagationQueue.isEmpty()) {
			voxel = sunlightPropagationQueue.poll();

			lightLevel = voxel.getSunLight();

			// If current light level is equals or less then 1, we can't propagate light.
			if (lightLevel <= 1) {
				continue;
			}

			// Look for all neighbors and check if the light can be propagated.
			for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
				neighborVoxel.position.set(voxel.position.x + dir.x, voxel.position.y + dir.y,
						voxel.position.z + dir.z);

				tmpChunk = getVoxelOnNeighborhood(neighborVoxel);

				if (!neighborVoxel.isReferenceValid()) {
					continue;
				}

				// If current light level is direct sun light (Voxel.SUN_LIGHT) and we are propagating downwards, we
				// don't decrease light level.
				propagatedLightLevel = (byte) ((dir == Vec3.DOWN && lightLevel == Voxel.SUN_LIGHT) ? Voxel.SUN_LIGHT
						: (lightLevel - 1));

				// If this neighbor is transparent and have a light power lower then our, lets propagate it.
				if (neighborVoxel.isTransparent() && neighborVoxel.getSunLight() < propagatedLightLevel) {

					neighborVoxel.setSunLight(propagatedLightLevel);
					// We may propagate light only if it's greater then 1.
					if (lightLevel > 1) {
						tmpChunk.sunlightPropagationQueue.add(tmpChunk.cloneReference(neighborVoxel));
					}

				}
				if (tmpChunk != this && tmpChunk.currentState != Chunk.State.LIGHT) {
					are.requestChunkUpdate(tmpChunk, -1);
				}
			}
		}
	}

	public void removeLight() {
		int previousLightLevel, neighborLightLevel;

		VoxelReference neighborVoxel = new VoxelReference();
		LightRemovalNode node;
		Chunk tmpChunk;

		while (!lightRemovalQueue.isEmpty()) {
			node = lightRemovalQueue.poll();

			previousLightLevel = node.previousLight;

			// If previous light level is equals or less then 1, we can't remove neighbor light.
			if (previousLightLevel <= 1) {
				continue;
			}

			// Look for all neighbors and check for light removal.
			for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
				neighborVoxel.position.set(node.x + dir.x, node.y + dir.y, node.z + dir.z);

				tmpChunk = getVoxelOnNeighborhood(neighborVoxel);

				if (!neighborVoxel.isReferenceValid()) {
					continue;
				}

				neighborLightLevel = neighborVoxel.getLight();

				// If this neighbor isn't transparent or is already dark (no sun light), there is nothing to do with it.
				if (neighborVoxel.isTransparent() || neighborLightLevel > 0) {
					if (neighborLightLevel < previousLightLevel) {
						// If this neighbor has a light lower then our previousLight OR our previous sunlight is direct
						// sun light (max) and we are looking at our
						// downwards neighbor, we will remove it and propagate it's removal.
						neighborVoxel.setLight((byte) 0);
						tmpChunk.lightRemovalQueue.add(new LightRemovalNode(neighborVoxel.position.x,
								neighborVoxel.position.y, neighborVoxel.position.z, neighborLightLevel));
					} else if (neighborLightLevel >= previousLightLevel) {
						// Else, if this neighbor has a light equals or higher then our previous light, then we need to
						// propagate it's light,
						// so we may receive it on sunLightPropagation
						tmpChunk.addLightPropagationQueue(tmpChunk.cloneReference(neighborVoxel));
					}
				}

				if (tmpChunk != this && tmpChunk.currentState != Chunk.State.LIGHT) {
					are.requestChunkUpdate(tmpChunk, -1);
				}
			}
		}
	}

	/**
	 * Propagate light across neigbor voxels. This method uses a Passive Flood Fill algorithm.
	 */
	public void propagateLight() {
		int lightLevel;
		byte propagatedLightLevel;

		VoxelReference neighborVoxel = new VoxelReference(), voxel;
		Chunk tmpChunk;

		while (!lightPropagationQueue.isEmpty()) {
			voxel = lightPropagationQueue.poll();

			lightLevel = voxel.getLight();

			// If current light level is equals or less then 1, we can't propagate light.
			if (lightLevel <= 1) {
				continue;
			}

			// Look for all neighbors and check if the light can be propagated.
			for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
				neighborVoxel.position.set(voxel.position.x + dir.x, voxel.position.y + dir.y,
						voxel.position.z + dir.z);

				tmpChunk = getVoxelOnNeighborhood(neighborVoxel);

				if (!neighborVoxel.isReferenceValid()) {
					continue;
				}

				// If current light level is direct sun light (Voxel.SUN_LIGHT) and we are propagating downwards, we
				// don't decrease light level.
				propagatedLightLevel = (byte) (lightLevel - 1);

				// If this neighbor is transparent and have a light power lower then our, lets propagate it.
				if (neighborVoxel.isTransparent() && neighborVoxel.getLight() < propagatedLightLevel) {

					neighborVoxel.setLight(propagatedLightLevel);
					// We may propagate light only if it's greater then 1.
					if (lightLevel > 1) {
						tmpChunk.lightPropagationQueue.add(tmpChunk.cloneReference(neighborVoxel));
					}

				}
				if (tmpChunk != this && tmpChunk.currentState != Chunk.State.LIGHT) {
					are.requestChunkUpdate(tmpChunk, -1);
				}
			}
		}
	}

	public void unload() {
		mesh = null;

		Chunk neighbor = null;
		for (int side = 0; side < SIDE_COUNT; side++) {
			neighbor = neighborhood[side];

			if (neighbor == null)
				continue;

			// Remove this chunk as neighbor on opposite side.
			neighbor.setNeighbor(Chunk.oppositeSide(side), null);
		}
	}

	public boolean hasVisibleVoxel() {
		return visibleVoxels > 0;
	}

	private void checkVisibleFaces() {
		// Neighbor Voxel.
		this.lightDataArray = new LightData[DATA_LENGTH]; // 4 vertex 6 sides
		VoxelReference neighborVoxel = new VoxelReference();
		VoxelReference voxel = new VoxelReference();
		int side;
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				for (int y = 0; y < SIZE; y++) {
					voxel.position.set(x, y, z);

					if (!get(voxel) || voxel.getType() == VT_NONE) {
						continue;
					}

					for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
						side = Voxel.directionToSide(dir);
						neighborVoxel.position.set(voxel.position.x + dir.x, voxel.position.y + dir.y,
								voxel.position.z + dir.z);

						getVoxelOnNeighborhood(neighborVoxel);

						if (!neighborVoxel.isReferenceValid()) {
							voxel.setSideVisible(side);
						} else if (neighborVoxel.isTransparent()) {
							voxel.setSideVisible(side);
							voxel.setSideLight(neighborVoxel.getFinalLight(), side);
						}
					}

					LightManager.computeSmoothLighting(this, voxel);
				}
			}
		}
	}

	private void mergeVisibleFaces() {
		ChunkSideBuffer buffer = new ChunkSideBuffer();
		mergeFrontFaces(buffer);
		mergeRightFaces(buffer);
		mergeBackFaces(buffer);
		mergeLeftFaces(buffer);
		mergeTopFaces(buffer);
		mergeDownFaces(buffer);

		// At this point we don't need the lightMap anymore.
		this.lightDataArray = null;

		mesh = new ChunkMesh(position, buffer);
	}

	/**
	 * Merge all voxels with a front (Z+) face of same type and visible. This methods uses a simple logic: It just
	 * iterate over X looking for neighbor voxels who can be merged (share the same type and have front face visible
	 * also). When it reaches the at right most voxel, it start looking for neigor at top (Y+) and repeat the proccess
	 * looking into right voxels until it reaches the right most and top most voxels, when it combine all those voxels
	 * on a singles float array and returns it.
	 * 
	 * @param buffer
	 *
	 */
	private void mergeFrontFaces(ChunkSideBuffer buffer) {
		// Voxel and Neightbor Voxel
		VoxelReference v = new VoxelReference();
		VoxelReference nv = new VoxelReference();

		// Merge origin x and y to keep track of merged face bounds.
		int ox, oy;
		boolean done;
		short currentType;
		int side = Voxel.FRONT;
		float[] lightBuffer = new float[4];
		LightData currentLight;

		// When looking for front faces (Z+) we need to check X axis and later Y axis, because this, the iteration
		// occurs this way.
		for (int z = 0; z < SIZE; z++) {
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					v.position.set(x, y, z);

					if (!get(v)) {
						continue;
					}

					currentType = v.getType();

					// If voxel is invalid or is merged already, skip it;
					if (currentType == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)/* || v.isSpecial()*/) {
						continue;
					}

					currentLight = findLightData(v.offset);

					float[] vertices = new float[12];
					ox = x;
					oy = y;
					done = false;

					/*
					 * 
					 *      v7 +-------+ v6	y
					 *      / |      / |	|  
					 *   v3 +-------+v2|	|
					 *      |v4+-------+ v5	+-- X
					 *      | /     | /		 \
					 *      +-------+		  Z
					 *     v0        v1
					 */
					// The front face is composed of v0, v1, v2 and v3.
					System.arraycopy(Voxel.v0(x, y, z), 0, vertices, 0, 3);

					/* 
					 *	Following the counter-clockwise rendering order.
					 * 
					 *	    |
					 *	    +
					 *	    |		    --->
					 *	    +---+---
					 *	  v0			       
					 */
					while (true) {

						// We are on the boundary of chunk. Stop it;
						if (x == SIZE - 1) {
							break;
						}

						// Move to the next voxel on X axis.
						nv.position.set(++x, y, z);

						// If the next voxel is invalid
						if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
								|| (nv.getType() != currentType)
								|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
							// Go back to previous voxel;
							--x;
							break;
						}

						// v = nv; //Set current voxel as next one, so repeat the check until end.
					}

					/*
					 * 
					 *	    |		|
					 *	    +	        +
					 *	    |		|	    
					 *	    +---+---+---+
					 *	  v0		 v1	       
					 * 
					 *	At this moment, we reached the right most X, so set it as v1.
					 */
					System.arraycopy(Voxel.v1(x, y, z), 0, vertices, 3, 3);

					// Go one step on Y direction and repeat the previous logic.
					while (!done) {
						if (y == SIZE - 1) {
							// We are on the boundary of chunk. Stop it;
							break;
						}

						y++;

						for (int k = ox; k <= x; k++) {
							nv.position.set(k, y, z);
							if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
									|| (nv.getType() != currentType)
									|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
								--y; // Go back to previous voxel;
								done = true;
								break;
							}
						}
					}

					/*	  v3		 v2
					 *	    +---+---+---+
					 *	    |		|
					 *	    +	        +
					 *	    |		|	    
					 *	    +---+---+---+
					 *	  v0		 v1	       
					 * 
					 *	At this moment, we reached the right most and top most, so lets track v2 and v3.
					 */
					System.arraycopy(Voxel.v2(x, y, z), 0, vertices, 6, 3);
					System.arraycopy(Voxel.v3(ox, y, z), 0, vertices, 9, 3);

					for (int a = ox; a <= x; a++) {
						for (int b = oy; b <= y; b++) {
							v.position.set(a, b, z);
							if (get(v)) {
								v.setSideMerged(side);
							}
						}
					}

					currentLight.getSide(side, lightBuffer);
					buffer.add(currentType, lightBuffer, VS_FRONT, vertices);
					y = oy; // Set the y back to orignal location, so we can iterate over it again.
				}
			}
		}
	}

	private void mergeBackFaces(ChunkSideBuffer buffer) {
		// Voxel and Neightbor Voxel
		VoxelReference v = new VoxelReference();
		VoxelReference nv = new VoxelReference();

		// Merge origin x and y
		int ox, oy;
		boolean done;
		short currentType;
		int side = Voxel.BACK;
		float[] lightBuffer = new float[4];
		LightData currentLight;

		for (int z = 0; z < SIZE; z++) {
			for (int y = 0; y < SIZE; y++) {
				for (int x = SIZE - 1; x > -1; x--) {
					v.position.set(x, y, z);

					if (!get(v)) {
						continue;
					}

					currentType = v.getType();

					// If vox is invalid or is merged already, skip it;
					if (currentType == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)/* || v.isSpecial()*/) {

						continue;
					}

					currentLight = findLightData(v.offset);
					float[] vertices = new float[12];
					ox = x;
					oy = y;
					done = false;

					// The back face is composed of v5, v4, v7 and v6.
					System.arraycopy(Voxel.v5(x, y, z), 0, vertices, 0, 3);
					while (true) {
						if (x == 0) {
							break; // We are on the boundary of chunk. Stop it;
						}

						// Move to the next voxel on X axis.
						nv.position.set(--x, y, z);

						if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
								|| (nv.getType() != currentType)
								|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
							++x; // Go back to previous voxel;
							break;
						}

						// v = nv; //Set current voxel as next one, so repeat the check until end.
					}

					System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 3, 3);
					while (!done) {
						if (y == SIZE - 1) {
							break; // We are on the boundary of chunk. Stop it;
						}

						y++;

						for (int k = ox; k >= x; k--) {
							nv.position.set(k, y, z);
							if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
									|| (nv.getType() != currentType)
									|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
								--y; // Go back to previous voxel;
								done = true;
								break;
							}
						}
					}

					System.arraycopy(Voxel.v7(x, y, z), 0, vertices, 6, 3);
					System.arraycopy(Voxel.v6(ox, y, z), 0, vertices, 9, 3);

					for (int a = ox; a >= x; a--) {
						for (int b = oy; b <= y; b++) {
							v.position.set(a, b, z);
							if (get(v)) {
								v.setSideMerged(side);
							}
						}
					}

					currentLight.getSide(side, lightBuffer);
					buffer.add(currentType, lightBuffer, VS_BACK, vertices);
					y = oy; // Set the y back to orignal location, so we can iterate over it again.
				}
			}
		}
	}

	private void mergeTopFaces(ChunkSideBuffer buffer) {
		// Voxel and Neightbor Voxel
		VoxelReference v = new VoxelReference();
		VoxelReference nv = new VoxelReference();

		// Merge origin x and y
		int ox, oz;
		boolean done;
		short currentType;
		int side = Voxel.TOP;

		float[] lightBuffer = new float[4];
		LightData currentLight;

		for (int y = SIZE - 1; y > -1; y--) {
			for (int z = SIZE - 1; z > -1; z--) {
				for (int x = 0; x < SIZE; x++) {
					v.position.set(x, y, z);

					if (!get(v)) {
						continue;
					}

					currentType = v.getType();

					// If vox is invalid or is merged already, skip it;
					if (currentType == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)/* || v.isSpecial()*/) {
						continue;
					}

					currentLight = findLightData(v.offset);
					float[] vertices = new float[12];
					ox = x;
					oz = z;
					done = false;

					// The top face is composed of v3, v2, v6 and v7.
					System.arraycopy(Voxel.v3(x, y, z), 0, vertices, 0, 3);
					while (true) {
						if (x == SIZE - 1) {
							break; // We are on the boundary of chunk. Stop it;
						}

						// Move to the next voxel on X axis.
						nv.position.set(++x, y, z);

						if (!get(nv) || nv.getType() == VT_NONE || nv.isSideMerged(side) || !nv.isSideVisible(side)
								|| (nv.getType() != currentType)
								|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
							--x; // Go back to previous voxel;
							break;
						}

						// v = nv; //Set current voxel as next one, so repeat the check until end.
					}

					System.arraycopy(Voxel.v2(x, y, z), 0, vertices, 3, 3);

					while (!done) {
						if (z == 0) {
							break; // We are on the boundary of chunk. Stop it;
						}

						z--;

						for (int k = ox; k <= x; k++) {
							nv.position.set(k, y, z);
							if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
									|| (nv.getType() != currentType)
									|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
								++z; // Go back to previous voxel;
								done = true;
								break;
							}
						}
					}

					System.arraycopy(Voxel.v6(x, y, z), 0, vertices, 6, 3);
					System.arraycopy(Voxel.v7(ox, y, z), 0, vertices, 9, 3);

					for (int a = ox; a <= x; a++) {
						for (int b = oz; b >= z; b--) {
							v.position.set(a, y, b);
							if (get(v)) {
								v.setSideMerged(side);
							}
						}
					}

					currentLight.getSide(side, lightBuffer);
					buffer.add(currentType, lightBuffer, VS_TOP, vertices);
					z = oz; // Set the z back to orignal location, so we can iterate over it again.
				}
			}
		}
	}

	private void mergeDownFaces(ChunkSideBuffer buffer) {
		// Voxel and Neightbor Voxel
		VoxelReference v = new VoxelReference();
		VoxelReference nv = new VoxelReference();

		// Merge origin x and y
		int ox, oz;
		boolean done;
		short currentType;
		int side = Voxel.DOWN;

		float[] lightBuffer = new float[4];
		LightData currentLight;

		for (int y = 0; y < SIZE; y++) {
			for (int z = 0; z < SIZE; z++) {
				for (int x = 0; x < SIZE; x++) {
					v.position.set(x, y, z);

					if (!get(v)) {
						continue;
					}

					currentType = v.getType();

					// If vox is invalid or is merged already, skip it;
					if (currentType == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)/* || v.isSpecial()*/) {
						continue;
					}

					currentLight = findLightData(v.offset);

					float[] vertices = new float[12];
					ox = x;
					oz = z;
					done = false;

					// The down face is composed of v4, v5, v1 and v0.
					System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 0, 3);
					while (true) {
						if (x == SIZE - 1) {
							break; // We are on the boundary of chunk. Stop it;
						}

						// Move to the next voxel on X axis.
						nv.position.set(++x, y, z);

						if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
								|| (nv.getType() != currentType)
								|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
							--x; // Go back to previous voxel;
							break;
						}

						// v = nv; //Set current voxel as next one, so repeat the check until end.
					}

					System.arraycopy(Voxel.v5(x, y, z), 0, vertices, 3, 3);
					while (!done) {
						if (z == SIZE - 1) {
							break; // We are on the boundary of chunk. Stop it;
						}

						z++;

						for (int k = ox; k <= x; k++) {
							nv.position.set(k, y, z);

							if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
									|| (nv.getType() != currentType)
									|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
								--z; // Go back to previous voxel;
								done = true;
								break;
							}
						}
					}

					System.arraycopy(Voxel.v1(x, y, z), 0, vertices, 6, 3);
					System.arraycopy(Voxel.v0(ox, y, z), 0, vertices, 9, 3);

					for (int a = ox; a <= x; a++) {
						for (int b = oz; b <= z; b++) {
							v.position.set(a, y, b);
							if (get(v)) {
								v.setSideMerged(side);
							}
						}
					}

					currentLight.getSide(side, lightBuffer);
					buffer.add(currentType, lightBuffer, VS_DOWN, vertices);
					z = oz; // Set the z back to orignal location, so we can iterate over it again.
				}
			}
		}
	}

	private void mergeRightFaces(ChunkSideBuffer buffer) {
		// Voxel and Neightbor Voxel
		VoxelReference v = new VoxelReference();
		VoxelReference nv = new VoxelReference();

		// Merge origin x and y
		int oz, oy;
		boolean done;
		short currentType;
		int side = Voxel.RIGHT;

		float[] lightBuffer = new float[4];
		LightData currentLight;

		for (int x = SIZE - 1; x > -1; x--) {
			for (int y = 0; y < SIZE; y++) {
				for (int z = SIZE - 1; z > -1; z--) {
					v.position.set(x, y, z);

					if (!get(v)) {
						continue;
					}

					currentType = v.getType();

					// If vox is invalid or is merged already, skip it;
					if (currentType == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)/* || v.isSpecial()*/) {
						continue;
					}

					currentLight = findLightData(v.offset);
					float[] vertices = new float[12];
					oz = z;
					oy = y;
					done = false;

					// The right face is composed of v1, v5, v6 and v2.
					System.arraycopy(Voxel.v1(x, y, z), 0, vertices, 0, 3);
					while (true) {
						if (z == 0) {
							break; // We are on the boundary of chunk. Stop it;
						}

						// Move to the next voxel on X axis.
						nv.position.set(x, y, --z);

						if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
								|| (nv.getType() != currentType)
								|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
							++z; // Go back to previous voxel;
							break;
						}

						// v = nv; //Set current voxel as next one, so repeat the check until end.
					}

					System.arraycopy(Voxel.v5(x, y, z), 0, vertices, 3, 3);
					while (!done) {
						if (y == SIZE - 1) {
							break; // We are on the boundary of chunk. Stop it;
						}

						y++;

						for (int k = oz; k >= z; k--) {
							nv.position.set(x, y, k);
							if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
									|| (nv.getType() != currentType)
									|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
								--y; // Go back to previous voxel;
								done = true;
								break;
							}
						}
					}

					System.arraycopy(Voxel.v6(x, y, z), 0, vertices, 6, 3);
					System.arraycopy(Voxel.v2(x, y, oz), 0, vertices, 9, 3);

					for (int a = oz; a >= z; a--) {
						for (int b = oy; b <= y; b++) {
							v.position.set(x, b, a);
							if (get(v)) {
								v.setSideMerged(side);
							}
						}
					}

					currentLight.getSide(side, lightBuffer);
					buffer.add(currentType, lightBuffer, VS_RIGHT, vertices);
					y = oy; // Set the y back to orignal location, so we can iterate over it again.
				}
			}
		}
	}

	private void mergeLeftFaces(ChunkSideBuffer buffer) {
		// Voxel and Neightbor Voxel
		VoxelReference v = new VoxelReference();
		VoxelReference nv = new VoxelReference();

		// Merge origin x and y
		int oz, oy;
		boolean done;
		short currentType;
		int side = Voxel.LEFT;

		float[] lightBuffer = new float[4];
		LightData currentLight;

		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				for (int z = 0; z < SIZE; z++) {
					v.position.set(x, y, z);

					if (!get(v)) {
						continue;
					}

					currentType = v.getType();

					// If vox is invalid or is merged already, skip it;
					if (currentType == VT_NONE || v.isSideMerged(side) || !v.isSideVisible(side)/* || v.isSpecial()*/) {
						continue;
					}

					currentLight = findLightData(v.offset);
					float[] vertices = new float[12];
					oy = y;
					oz = z;
					done = false;

					// The left face is composed of v4, v0, v3 and v7.
					System.arraycopy(Voxel.v4(x, y, z), 0, vertices, 0, 3);
					while (true) {
						if (z == SIZE - 1) {
							break; // We are on the boundary of chunk. Stop it;
						}

						// Move to the next voxel on Z axis.
						nv.position.set(x, y, ++z);

						if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
								|| (nv.getType() != currentType)
								|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
							--z; // Go back to previous voxel;
							break;
						}
						// v = nv; //Set current voxel as next one, so repeat the check until end.
					}

					System.arraycopy(Voxel.v0(x, y, z), 0, vertices, 3, 3);
					while (!done) {
						if (y == SIZE - 1) {
							break; // We are on the boundary of chunk. Stop it;
						}

						y++;

						for (int k = oz; k <= z; k++) {
							nv.position.set(x, y, k);
							if (!get(nv) || nv.isSideMerged(side) || !nv.isSideVisible(side)
									|| (nv.getType() != currentType)
									|| (!currentLight.compare(findLightData(nv.offset), side))/* || v.isSpecial()*/) {
								--y; // Go back to previous voxel;
								done = true;
								break;
							}
						}
					}

					System.arraycopy(Voxel.v3(x, y, z), 0, vertices, 6, 3);
					System.arraycopy(Voxel.v7(x, y, oz), 0, vertices, 9, 3);

					for (int a = oz; a <= z; a++) {
						for (int b = oy; b <= y; b++) {
							v.position.set(x, b, a);
							if (get(v)) {
								v.setSideMerged(side);
							}
						}
					}

					currentLight.getSide(side, lightBuffer);
					buffer.add(currentType, lightBuffer, VS_LEFT, vertices);
					y = oy; // Set the y back to orignal location, so we can iterate over it again.
				}
			}
		}
	}

	public Vec3 getPosition() {
		return this.position;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + Objects.hashCode(this.position);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Chunk other = (Chunk) obj;
		return Objects.equals(this.position, other.position);
	}

	@Override
	public String toString() {
		return "Chunk{" + "name=" + name + '}';
	}

	public void addSunLightPropagationQueue(VoxelReference voxel) {
		this.sunlightPropagationQueue.add(voxel);
	}

	public void addSunLightRemovalQueue(LightRemovalNode node) {
		this.sunlightRemovalQueue.add(node);
	}

	public void addLightPropagationQueue(VoxelReference voxel) {
		this.lightPropagationQueue.add(voxel);
	}

	public void addLightRemovalQueue(LightRemovalNode node) {
		this.lightRemovalQueue.add(node);
	}

	public void updateVoxelType(VoxelReference voxel, short type) {
		short previousType = voxel.getType();
		int previousSunLight = voxel.getSunLight();
		int previousLight = voxel.getLight();
		byte light = 0;

		voxel.clearVoxelData();
		voxel.setType(type);

		if (previousType == Voxel.VT_NONE) {
			visibleVoxels++;
		} else {
			visibleVoxels--;
		}

		// if (voxel.isSpecial()) {
		// SpecialVoxelData data = new SpecialVoxelData(this, voxel.position);
		// are.requestSpecialVoxelAttach(data);
		//
		// light = SpecialVoxel.getLight(type);
		// voxel.setLight(light);
		// } else if (VoxelReference.isSpecial(previousType)) {
		// SpecialVoxelData data = new SpecialVoxelData(this, voxel.position);
		// are.requestSpecialVoxelDetach(data);
		// }

		LightManager.updateVoxelLight(this, voxel, previousSunLight, previousLight, light, previousType, type);
	}

	public Chunk getVoxelOnNeighborhood(VoxelReference reference) {
		Vec3 pos = reference.position;

		Chunk result;

		if (pos.x < 0) {
			result = neighborhood[SIDE_LEFT];
		} else if (pos.x >= Chunk.SIZE) {
			result = neighborhood[SIDE_RIGHT];
		} else if (pos.y < 0) {
			result = neighborhood[SIDE_DOWN];
		} else if (pos.y >= Chunk.SIZE) {
			result = neighborhood[SIDE_TOP];
		} else if (pos.z < 0) {
			result = neighborhood[SIDE_BACK];
		} else if (pos.z >= Chunk.SIZE) {
			result = neighborhood[SIDE_FRONT];
		} else {
			result = this;
		}

		pos.mod(Chunk.SIZE);

		if (result == null || !result.get(reference)) {
			reference.reset();
		}

		return result;
	}

	public ChunkMesh getMesh() {
		return mesh;
	}
}