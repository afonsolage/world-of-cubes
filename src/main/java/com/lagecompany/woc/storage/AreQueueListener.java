package com.lagecompany.woc.storage;

/**
 *
 * @author Afonso Lage
 */
public abstract class AreQueueListener {

    public enum Type {

	START, FINISH;
    }
    private AreMessage.Type messageType;
    private boolean persistent;
    private boolean shouldRemove;

    public AreQueueListener(AreMessage.Type messageType, boolean persistent) {
	this.messageType = messageType;
	this.persistent = persistent;
    }

    public void checkDoAction(AreMessage.Type messageType, int batch) {
	if (messageType == this.messageType) {
	    doAction(batch);
	    shouldRemove = !persistent;
	}
    }

    public boolean shouldRemove() {
	return shouldRemove;
    }

    public abstract void doAction(int batch);
}
