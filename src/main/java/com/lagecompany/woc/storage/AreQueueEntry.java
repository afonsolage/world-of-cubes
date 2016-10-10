package com.lagecompany.woc.storage;

class AreQueueEntry {

	private Chunk.State state;
	private Chunk chunk;
	private int batch = -1;

	public AreQueueEntry(Chunk.State state) {
		this.state = state;
	}

	public AreQueueEntry(Chunk.State state, Chunk chunk) {
		this.state = state;
		this.chunk = chunk;
	}

	public AreQueueEntry(Chunk.State state, Chunk chunk, int batch) {
		this(state, chunk);
		this.batch = batch;
	}

	public Chunk.State getState() {
		return state;
	}

	public void setState(Chunk.State state) {
		this.state = state;
	}

	public Chunk getChunk() {
		return chunk;
	}

	public AreQueueEntry setChunk(Chunk chunk) {
		this.chunk = chunk;
		return this;
	}

	public int getBatch() {
		return batch;
	}

	public void setBatch(int batch) {
		this.batch = batch;
	}

}