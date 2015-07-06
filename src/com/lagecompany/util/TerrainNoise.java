package com.lagecompany.util;

import com.jme3.math.FastMath;
import com.lagecompany.storage.Chunk;

/**
 *
 * @author afonsolage
 */
public class TerrainNoise {

    private static SimplexNoise plexNoise;

    static {
	plexNoise = new SimplexNoise();
    }

    /**
     * Generate a terrain noise using given parameters. The same parameters give the same result, always.
     *
     * @param x Axis position used on Simplex Noise formula
     * @param y Axis position used on Simplex Noise formula
     * @param z Axis position used on Simplex Noise formula
     * @param startFrequence The inicial frequence of noise. Lower frequency means high variation on noise.
     * @param octaves Number of octaves to be computed. Higher values give better results but at more CPU cost.
     * @param persistence The persistence of amplitude. Higher values means higher variation.
     * @return
     */
    public static double eval(double x, double y, double z, double startFrequence, int octaves, double persistence) {
	double noise = 0;
	double normalizeFactor = 0;

	double frequence = startFrequence;

	double amplitude = 1;

	for (int i = 0; i < octaves; i++) {
	    normalizeFactor += amplitude;

	    noise += amplitude * plexNoise.eval(frequence * x, frequence * y, frequence * z);
	    frequence *= 2;
	    amplitude *= persistence;
	}

	return noise / normalizeFactor;
    }

    public static double getHeight(int x, int z) {
	double result = TerrainNoise.eval(x, z, 0, 0.004, 3, 0.75) * Chunk.HEIGHT * 5;
	result += 5 * Chunk.HEIGHT;
	return result;
    }
}
