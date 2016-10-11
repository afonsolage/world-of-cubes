/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package com.lagecompany.woc.storage.light;

import com.lagecompany.woc.storage.Are;
import com.lagecompany.woc.storage.Chunk;
import com.lagecompany.woc.storage.Vec3;
import com.lagecompany.woc.storage.voxel.Voxel;
import com.lagecompany.woc.storage.voxel.VoxelReference;

/**
 *
 * @author TI.Afonso
 */
public class LightManager {

	private static Are are;

	public static void setup(Are are) {
		LightManager.are = are;
	}

	public static void updateVoxelLight(Chunk c, VoxelReference voxel, int previousSunLight, int previousLight,
			int newLight, short previousType, short newType) {
		VoxelReference neighbor = new VoxelReference();
		Chunk tmpChunk;

		if (previousType == Voxel.VT_NONE) {
			// Block add
			if (VoxelReference.isOpaque(newType)) {
				c.addSunLightRemovalQueue(
						new LightRemovalNode(voxel.position.x, voxel.position.y, voxel.position.z, previousSunLight));
			}
			if (newLight > 0) {
				c.addLightPropagationQueue(c.cloneReference(voxel));
			} else {
				for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
					neighbor.position.set(voxel.position.x + dir.x, voxel.position.y + dir.y, voxel.position.z + dir.z);

					tmpChunk = c.getVoxelOnNeighborhood(neighbor);

					if (!neighbor.isReferenceValid()) {
						// Oopps, we hit the are bounds, this shouldn't happen outside development.
						continue;
					}
					tmpChunk.addLightRemovalQueue(
							new LightRemovalNode(voxel.position.x, voxel.position.y, voxel.position.z, previousLight));
				}
			}
		} else {
			// Block remove
			for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
				neighbor.position.set(voxel.position.x + dir.x, voxel.position.y + dir.y, voxel.position.z + dir.z);

				tmpChunk = c.getVoxelOnNeighborhood(neighbor);

				if (!neighbor.isReferenceValid()) {
					// Oopps, we hit the are bounds, this shouldn't happen outside development.
					continue;
				}

				tmpChunk.addSunLightPropagationQueue(tmpChunk.cloneReference(neighbor));
				if (previousLight > 0) {
					tmpChunk.addLightRemovalQueue(
							new LightRemovalNode(voxel.position.x, voxel.position.y, voxel.position.z, previousLight));
				} else {
					tmpChunk.addLightPropagationQueue(tmpChunk.cloneReference(neighbor));
				}
			}
		}
	}

	private static byte getFinalLight(Chunk chunk, VoxelReference reference) {
		chunk = are.validateaChunkAndVoxel(chunk, reference.position);
		return (chunk == null) ? 0xF : (chunk.get(reference)) ? reference.getFinalLight() : 0;
	}

	/**
	 * Fills the voxelBuffer with neighbor lighting information.
	 *
	 * @param chunk
	 *            Chunk where voxel is placed.
	 * @param voxel
	 *            The VoxelReference to get it's neighborhood.
	 * @param voxelBuffer
	 *            The output buffer to store the lighting data.
	 */
	private static void getNeighborhoodLighting(Chunk chunk, final VoxelReference voxel, byte[] voxelBuffer) {
		VoxelReference tmpVoxel = new VoxelReference();

		/*
		 -               +-------++-------++-----+
		 -             /   7   //  4    //  2   /|
		 -            +-------++-------++------+ |
		 -          /   6   / ||      /   1   /| +
		 -         +-------++-------++-------+ |/+
		 -       /   5   //    3   //   0   /| +/|
		 -       +-------++-------++-------+ |/+ |
		 -       |       ||       ||       | + | +
		 -       |       ||       ||       |/+ |/+
		 -       +-------++-------++-------+/| +/|
		 -       +-------+  +      +-------+ | + |
		 -       |       | /+------|       | +/| +
		 -       |       |//       |       |/+ |/ 
		 -       +-------+/        +-------+/| +
		 -       +-------++-------++-------+ |/ 
		 -       |       ||       ||       | +  
		 -       |       ||       ||       |/ 
		 -       +-------++-------++-------+
		 - Y
		 - | 
		 - | 
		 - |
		 - +------ X
		 -  \
		 -   \
		 -    Z
		
		 */
		// TOP RIGHT NEIGHBORS
		tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y + 1, voxel.position.z + 1);
		voxelBuffer[0] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y + 1, voxel.position.z);
		voxelBuffer[1] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y + 1, voxel.position.z - 1);
		voxelBuffer[2] = getFinalLight(chunk, tmpVoxel);

		// TOP MIDDLE NEIGHBORS
		tmpVoxel.position.set(voxel.position.x, voxel.position.y + 1, voxel.position.z + 1);
		voxelBuffer[3] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x, voxel.position.y + 1, voxel.position.z - 1);
		voxelBuffer[4] = getFinalLight(chunk, tmpVoxel);

		// TOP LEFT NEIGHBORS
		tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y + 1, voxel.position.z + 1);
		voxelBuffer[5] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y + 1, voxel.position.z);
		voxelBuffer[6] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y + 1, voxel.position.z - 1);
		voxelBuffer[7] = getFinalLight(chunk, tmpVoxel);

		/*
		 -               +-------++-------++-----+
		 -             /       //       //      /|
		 -            +-------++-------++------+ |
		 -          /       / ||      /       /| +
		 -         +-------++-------++-------+ |/
		 -       /       //        //       /| +
		 -       +-------++-------++-------+ |/
		 -       |       ||       ||       | +
		 -       |       ||       ||       |/
		 -       +-------++-------++-------+
		
		 -               +-------          +-----+
		 -             /  11   /|        /   9  /|
		 -            +-------+ |       +------+ |
		 -            |       | +-------|      | +
		 -         +-------+  |/     +-------+ |/+
		 -       /  10    /|--++----/   8   /|-+/|
		 -       +-------+ |/ ||   +-------+ | + |
		 -       |       | |  ||   |       | +/| +
		 -       |       |/+-------|       |/+ |/
		 -       +-------+/        +-------+/| +
		 -       +-------++-------++-------+ |/ 
		 -       |       ||       ||       | +  
		 -       |       ||       ||       |/ 
		 -       +-------++-------++-------+
		 - Y
		 - | 
		 - | 
		 - |
		 - +------ X
		 -  \
		 -   \
		 -    Z
		
		 */
		// MID RIGHT NEIGHBORS
		tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y, voxel.position.z + 1);
		voxelBuffer[8] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y, voxel.position.z - 1);
		voxelBuffer[9] = getFinalLight(chunk, tmpVoxel);

		// MID LEFT NEIGHBORS
		tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y, voxel.position.z + 1);
		voxelBuffer[10] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y, voxel.position.z - 1);
		voxelBuffer[11] = getFinalLight(chunk, tmpVoxel);

		/*
		
		 -               +-------++-------++-----+
		 -             /       //       //      /|
		 -            +-------++-------++------+ |
		 -          /       //||     //       /| +
		 -         +-------++-------++-------+ |/+
		 -       /       //        //       /| +/|
		 -       +-------++-------++-------+ |/+ |
		 -       |       ||       ||       | + | +
		 -       |       ||       ||       |/+ |/
		 -       +-------++-------++-------+/| +
		 -       +-------+  +      +-------+ | 
		 -       |       | /       |       | +  
		 -       |       |/        |       |/ 
		 -       +-------+         +-------+
		
		 -               +-------++-------++-----+
		 -             /  19   //  16   //  14  /|
		 -            +-------++-------++------+ |
		 -          /  18   //||     //  13   /| +
		 -         +-------++-------++-------+ |/
		 -       /   17  //   15   //  12   /| +
		 -       +-------++-------++-------+ |/
		 -       |       ||       ||       | +
		 -       |       ||       ||       |/
		 -       +-------++-------++-------+
		 - Y
		 - | 
		 - | 
		 - |
		 - +------ X
		 -  \
		 -   \
		 -    Z
		
		 */
		// DOWN RIGHT NEIGHBORS
		tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y - 1, voxel.position.z + 1);
		voxelBuffer[12] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y - 1, voxel.position.z);
		voxelBuffer[13] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x + 1, voxel.position.y - 1, voxel.position.z - 1);
		voxelBuffer[14] = getFinalLight(chunk, tmpVoxel);

		// DOWN MIDDLE NEIGHBORS
		tmpVoxel.position.set(voxel.position.x, voxel.position.y - 1, voxel.position.z + 1);
		voxelBuffer[15] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x, voxel.position.y - 1, voxel.position.z - 1);
		voxelBuffer[16] = getFinalLight(chunk, tmpVoxel);

		// DOWN LEFT NEIGHBORS
		tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y - 1, voxel.position.z + 1);
		voxelBuffer[17] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y - 1, voxel.position.z);
		voxelBuffer[18] = getFinalLight(chunk, tmpVoxel);
		tmpVoxel.position.set(voxel.position.x - 1, voxel.position.y - 1, voxel.position.z - 1);
		voxelBuffer[19] = getFinalLight(chunk, tmpVoxel);
	}

	public static void computeSmoothLighting(Chunk chunk, VoxelReference voxel) {
		byte[] neighborhood = new byte[26];
		int sideLighting;

		getNeighborhoodLighting(chunk, voxel, neighborhood);

		LightData data = new LightData(voxel.offset);
		chunk.setLightData(data);

		/*
		 * 
		 *      v7 +-------+ v6         y
		 *      /  |      /|            |  
		 *   v3 +-------+v2|            |
		 *      |v4+-------+ v5         +-- X
		 *      | /     | /              \
		 *      +-------+                 Z 
		 *     v0        v1
		 */
		for (int side = Voxel.FRONT; side <= Voxel.DOWN; side++) {

			if (!voxel.isSideVisible(side)) {
				continue;
			}

			sideLighting = voxel.getSideLight(side);

			switch (side) {
			case Voxel.FRONT: {
				// The front face is composed of v0, v1, v2 and v3.
				data.setVertexData(side, 0,
						ambientOcclusion(sideLighting, neighborhood[15], neighborhood[10], neighborhood[17])); // v0
				data.setVertexData(side, 1,
						ambientOcclusion(sideLighting, neighborhood[15], neighborhood[8], neighborhood[12])); // v1
				data.setVertexData(side, 2,
						ambientOcclusion(sideLighting, neighborhood[3], neighborhood[8], neighborhood[0])); // v2
				data.setVertexData(side, 3,
						ambientOcclusion(sideLighting, neighborhood[3], neighborhood[10], neighborhood[5])); // v3
				break;
			}
			case Voxel.RIGHT: {
				// The right face is composed of v1, v5, v6 and v2.
				data.setVertexData(side, 0,
						ambientOcclusion(sideLighting, neighborhood[13], neighborhood[8], neighborhood[12])); // v1
				data.setVertexData(side, 1,
						ambientOcclusion(sideLighting, neighborhood[13], neighborhood[9], neighborhood[14])); // v5
				data.setVertexData(side, 2,
						ambientOcclusion(sideLighting, neighborhood[1], neighborhood[9], neighborhood[2])); // v6
				data.setVertexData(side, 3,
						ambientOcclusion(sideLighting, neighborhood[1], neighborhood[8], neighborhood[0])); // v2
				break;
			}
			case Voxel.BACK: {
				// The back face is composed of v5, v4, v7 and v6.
				data.setVertexData(side, 0,
						ambientOcclusion(sideLighting, neighborhood[16], neighborhood[9], neighborhood[14])); // v5
				data.setVertexData(side, 1,
						ambientOcclusion(sideLighting, neighborhood[16], neighborhood[11], neighborhood[19])); // v4
				data.setVertexData(side, 2,
						ambientOcclusion(sideLighting, neighborhood[4], neighborhood[11], neighborhood[7])); // v7
				data.setVertexData(side, 3,
						ambientOcclusion(sideLighting, neighborhood[4], neighborhood[9], neighborhood[2])); // v6
				break;
			}
			case Voxel.LEFT: {
				// The left face is composed of v4, v0, v3 and v7.
				data.setVertexData(side, 0,
						ambientOcclusion(sideLighting, neighborhood[18], neighborhood[11], neighborhood[19])); // v4
				data.setVertexData(side, 1,
						ambientOcclusion(sideLighting, neighborhood[18], neighborhood[10], neighborhood[17])); // v0
				data.setVertexData(side, 2,
						ambientOcclusion(sideLighting, neighborhood[6], neighborhood[10], neighborhood[5])); // v3
				data.setVertexData(side, 3,
						ambientOcclusion(sideLighting, neighborhood[6], neighborhood[11], neighborhood[7])); // v7
				break;
			}
			case Voxel.TOP: {
				// The top face is composed of v3, v2, v6 and v7.
				data.setVertexData(side, 0,
						ambientOcclusion(sideLighting, neighborhood[3], neighborhood[6], neighborhood[5])); // v3
				data.setVertexData(side, 1,
						ambientOcclusion(sideLighting, neighborhood[3], neighborhood[1], neighborhood[0])); // v2
				data.setVertexData(side, 2,
						ambientOcclusion(sideLighting, neighborhood[4], neighborhood[1], neighborhood[2])); // v6
				data.setVertexData(side, 3,
						ambientOcclusion(sideLighting, neighborhood[4], neighborhood[6], neighborhood[7])); // v7
				break;
			}
			case Voxel.DOWN: {
				// The down face is composed of v4, v5, v1 and v0.
				data.setVertexData(side, 0,
						ambientOcclusion(sideLighting, neighborhood[16], neighborhood[18], neighborhood[19])); // v4
				data.setVertexData(side, 1,
						ambientOcclusion(sideLighting, neighborhood[16], neighborhood[13], neighborhood[14])); // v5
				data.setVertexData(side, 2,
						ambientOcclusion(sideLighting, neighborhood[15], neighborhood[13], neighborhood[12])); // v1
				data.setVertexData(side, 3,
						ambientOcclusion(sideLighting, neighborhood[15], neighborhood[18], neighborhood[17])); // v0
				break;
			}
			default: {
				System.out.println("Invalid side value!!!");
			}
			}
		}
	}

	/**
	 * Compute the smooth lighting value based of both sides and the corner. This algorithm was based on this blog post:
	 * https://0fps.net/2013/07/03/ambient-occlusion-for-minecraft-like-worlds/
	 * 
	 * @param sideLighting
	 *            The lighting value on this voxel side.
	 * @param side1
	 *            The lighting value of first side of this voxel.
	 * @param side2
	 *            The lighting value of second side of this voxel.
	 * @param corner
	 *            The lighting value of corner of this voxel.
	 * @return The final smoothed light value.
	 */
	private static float ambientOcclusion(int sideLighting, byte side1, byte side2, byte corner) {
		// if side1 and side2 have no light, corner should be darker also, because side1 and side2
		// are blocking corner light.
		if (side1 == 0 && side2 == 0) {
			corner = 0;
		}
		return (sideLighting + side1 + side2 + corner) / 4f;
	}
}
