package com.lagecompany.woc.storage;

/**
 *
 * @author Afonso Lage
 */
public abstract class AreQueueListener {

	public enum Type {

		START, FINISH;
	}

	private Chunk.State state;
	private boolean persistent;
	private boolean shouldRemove;

	public AreQueueListener(Chunk.State state, boolean persistent) {
		this.state = state;
		this.persistent = persistent;
	}

	public void checkDoAction(Chunk.State state, int batch) {
		if (state == this.state) {
			doAction(batch);
			shouldRemove = !persistent;
		}
	}

	public boolean shouldRemove() {
		return shouldRemove;
	}

	public abstract void doAction(int batch);
}
