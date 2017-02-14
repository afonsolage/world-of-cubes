package com.lagecompany.woc.storage;

import java.util.ArrayList;
import java.util.List;

import com.lagecompany.woc.util.TerrainNoise;

public class Biome {

	class Layer {
		private int minSize;
		private int maxSize;
		private short type;
		private int minHeight;
		private int maxHeight;
		private float persistence;
		private int smoothness;
		private int variance;

		public Layer(int minSize, int maxSize, short type, int variance, int smoothness, float persistence) {
			this.minSize = minSize;
			this.maxSize = maxSize;
			this.type = type;
			this.variance = variance;
			this.smoothness = smoothness;
			this.persistence = persistence;
		}

		public int getMinSize() {
			return minSize;
		}

		public int getMaxSize() {
			return maxSize;
		}

		public short getType() {
			return type;
		}

		public int getMinHeight() {
			return minHeight;
		}

		public void setMinHeight(int minHeight) {
			this.minHeight = minHeight;
		}

		public int getMaxHeight() {
			return maxHeight;
		}

		public void setMaxHeight(int maxHeight) {
			this.maxHeight = maxHeight;
		}

		public int getSmoothness() {
			return smoothness;
		}

		public void setSmoothness(int smoothness) {
			this.smoothness = smoothness;
		}

		public int getVariance() {
			return variance;
		}

		public void setVariance(int variance) {
			this.variance = variance;
		}

		public float getPersistence() {
			return persistence;
		}

		public void setPersistence(float persistence) {
			this.persistence = persistence;
		}
	}

	private List<Layer> layers = new ArrayList<>();

	public void AddLayer(short type, int minSize, int maxSize, int variance, int smoothness, float persistence) {
		layers.add(new Layer(minSize, maxSize, type, variance, smoothness, persistence));
	}

	public List<Layer> getHeightLayer(int x, int z) {
		int lastMaxHeight = 0;
		for (Layer layer : layers) {
			layer.setMinHeight(lastMaxHeight);

			double noiseHeight = TerrainNoise.eval(x, 0, z, layer.getVariance() / 1000.0, layer.getSmoothness(), layer.getPersistence());

			noiseHeight *= layer.getMaxSize();
			noiseHeight += layer.getMinSize();

			layer.setMaxHeight((int) noiseHeight);
			lastMaxHeight = layer.getMaxHeight();
		}
		return layers;
	}
}
