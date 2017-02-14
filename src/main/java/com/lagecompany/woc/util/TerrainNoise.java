package com.lagecompany.woc.util;

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
	 * Generate a terrain noise using given parameters. By using the same parameters, the same results are given.
	 *
	 * @param x
	 *            Axis position used on Simplex Noise formula
	 * @param y
	 *            Axis position used on Simplex Noise formula
	 * @param z
	 *            Axis position used on Simplex Noise formula
	 * @param startFrequence
	 *            The initial frequency of noise. Lower frequency means high variation on noise.
	 * @param octaves
	 *            Number of octaves to be computed. Higher values give better results but at more CPU cost.
	 * @param persistence
	 *            The persistence of amplitude. Higher values means higher variation of terrain height.
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
}
