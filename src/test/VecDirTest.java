/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;
import com.lagecompany.storage.voxel.Voxel;

/**
 *
 * @author Afonso Lage
 */
public class VecDirTest {

    public static void main(String args[]) {
	Vec3 tmpVec = new Vec3();
	for (Vec3 dir : Vec3.ALL_DIRECTIONS) {
	    tmpVec.set(10 + dir.getX(), 10 + dir.getY(), 10 + dir.getZ());
	    System.out.println(String.format("[%s] x: %d, y: %d, z: %d, light: %d", dir.toString(),
		    (dir.getX() < 0) ? Chunk.SIZE - 1 : (dir.getX() > 0) ? 0 : tmpVec.getX(),
		    (dir.getY() < 0) ? Chunk.SIZE - 1 : (dir.getY() > 0) ? 0 : tmpVec.getY(),
		    (dir.getZ() < 0) ? Chunk.SIZE - 1 : (dir.getZ() > 0) ? 0 : tmpVec.getZ(),
		    (dir.equals(Vec3.DOWN) && 15 == Voxel.LIGHT_SUN) ? Voxel.LIGHT_SUN : 15 - 1));
	}
    }
}
