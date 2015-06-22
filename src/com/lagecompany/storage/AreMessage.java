package com.lagecompany.storage;

public class AreMessage {

    public enum AreMessageType {
	ARE_MOVE,
	CHUNK_SETUP,
	CHUNK_LOAD,
	CHUNK_RELOAD,
	CHUNK_UNLOAD,
	CHUNK_ATTACH,
	CHUNK_DETACH;
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