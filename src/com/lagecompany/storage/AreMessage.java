package com.lagecompany.storage;

import java.util.Objects;

public class AreMessage {

    public enum Type {

	ARE_MOVE,
	CHUNK_SETUP,
	CHUNK_LIGHT,
	CHUNK_LOAD,
	//	CHUNK_UPDATE,
	CHUNK_UNLOAD,
	CHUNK_ATTACH,
	CHUNK_DETACH;
    }
    private Type type;
    private Object data;
    private int batch = -1;

    public AreMessage(Type type) {
	this.type = type;
    }

    public AreMessage(Type type, Object data) {
	this.type = type;
	this.data = data;
    }

    public AreMessage(Type type, Object data, int batch) {
	this(type, data);
	this.batch = batch;
    }

    public Type getType() {
	return type;
    }

    public void setType(Type type) {
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

    @Override
    public int hashCode() {
	int hash = 3;
	hash = 83 * hash + Objects.hashCode(this.type);
	hash = 83 * hash + Objects.hashCode(this.data);
	hash = 83 * hash + this.batch;
	return hash;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final AreMessage other = (AreMessage) obj;
	if (this.type != other.type) {
	    return false;
	}
	if (!Objects.equals(this.data, other.data)) {
	    return false;
	}
	if (this.batch != other.batch) {
	    return false;
	}
	return true;
    }
}