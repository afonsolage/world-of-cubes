package com.lagecompany.storage;

public class AreMessage {

    public enum AreMessageType {

	ARE_MOVE,
	CHUNK_SETUP,
	CHUNK_LIGHT,
	CHUNK_LOAD,
	CHUNK_UPDATE,
	CHUNK_UNLOAD,
	CHUNK_ATTACH,
	CHUNK_DETACH;
    }
    private AreMessageType type;
    private Object data;
    private int batch = -1;

    public AreMessage(AreMessageType type) {
	this.type = type;
    }

    public AreMessage(AreMessageType type, Object data) {
	this.type = type;
	this.data = data;
    }

    public AreMessage(AreMessageType type, Object data, int batch) {
	this(type, data);
	this.batch = batch;
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

    public int getBatch() {
	return batch;
    }

    public void setBatch(int batch) {
	this.batch = batch;
    }
}