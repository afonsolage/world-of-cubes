package com.lagecompany.storage;

public class AreMessage {

    public enum AreMessageType {

	MOVE(0),
	PROCESS_CHUNK(1);

	private AreMessageType(int priority) {
	    this.priority = priority;
	}
	private int priority;

	public int getPriority() {
	    return priority;
	}
    }

    private AreMessageType type;
    private Object data;

    public AreMessage(AreMessageType type) {
	this.type = type;
    }

    public AreMessage(AreMessageType type, Object data) {
	this.type = type;
	this.data = data;
    }

    public AreMessageType getType() {
	return type;
    }

    public void setType(AreMessageType type) {
	this.type = type;
    }

    public Object getData() {
	return data;
    }

    public AreMessage setData(Object data) {
	this.data = data;
	return this;
    }
}