package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.lagecompany.jme3.control.AreFollowControl;
import com.lagecompany.storage.Are;
import com.lagecompany.storage.AreMessage;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author afonsolage
 */
public class TerrainAppState extends AbstractAppState {

    private Are are;
    private Node node;
    private AssetManager assetManager;
    private Node rootNode;
    private Node playerNode;
    private SimpleApplication app;
    private BulletAppState bulletState;
    private PhysicsSpace physicsSpace;
    private final ConcurrentLinkedQueue<AreMessage> attachQueue;
    private final ConcurrentLinkedQueue<AreMessage> detachQueue;
    private Material defaultMat;
    private float elapsedTime;
    private int chunkLoaded;
    private boolean shouldRender;
    public int maxChunkLoad = -1;

    public TerrainAppState() {
	are = Are.getInstance();
	attachQueue = are.getAttachQueue();
	detachQueue = are.getDetachQueue();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	this.app = (SimpleApplication) application;
	this.assetManager = app.getAssetManager();
	this.rootNode = app.getRootNode();
	this.bulletState = stateManager.getState(BulletAppState.class);
	this.physicsSpace = bulletState.getPhysicsSpace();
	initMaterials();

	node = new Node("Chunks Node");
	rootNode.attachChild(node);

	playerNode = stateManager.getState(WorldAppState.class).getPlayerNode();
	playerNode.addControl(new AreFollowControl());

	Vector3f playerPosition = playerNode.getLocalTranslation().clone();
	node.setLocalTranslation(playerPosition);
	are.setPosition((int) playerPosition.getX(), (int) playerPosition.getY(), (int) playerPosition.getZ());

	are.init();
	are.start();
    }

    @Override
    public void update(float tpf) {
	if (shouldRender) {
	    processChunks(tpf);
	}
    }

    @Override
    public void cleanup() {
	super.cleanup(); //To change body of generated methods, choose Tools | Templates.
	are.interrupt();
    }

    private void initMaterials() {
	defaultMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
	//mat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/Elements/rock.jpg"));
	defaultMat.setColor("Ambient", ColorRGBA.White);
	defaultMat.setColor("Diffuse", ColorRGBA.Gray);
	defaultMat.setColor("Specular", ColorRGBA.White);
	defaultMat.setFloat("Shininess", 0f);
	defaultMat.setBoolean("UseMaterialColors", true);
    }

    public boolean shouldRender() {
	return shouldRender;
    }

    public void setShouldRender(boolean should) {
	this.shouldRender = should;
    }

    public void processChunks(float tpf) {
	elapsedTime += tpf;

	if (elapsedTime > 0.1) {
	    elapsedTime = 0;
	    chunkLoaded = 0;
	}

	for (AreMessage message = attachQueue.poll(); message != null; message = attachQueue.poll()) {
	    Chunk c = (Chunk) message.getData();

	    if (c.getVertexCount() == 0) {
		continue;
	    }

	    Vec3 v = c.getPosition();
	    String name = c.getName();
	    Spatial spatial = node.getChild(name);

	    Geometry geometry;
	    RigidBodyControl rigidBodyControl;

	    if (spatial == null) {
		geometry = new Geometry(name);
		node.attachChild(geometry);
	    } else {
		geometry = (Geometry) spatial;
	    }

	    Mesh mesh = new Mesh();
	    
	    try {
		c.lock();
		mesh.setBuffer(VertexBuffer.Type.Position, 3, c.getVertexList());
		mesh.setBuffer(VertexBuffer.Type.Index, 1, c.getIndexList());
		mesh.setBuffer(VertexBuffer.Type.Normal, 3, c.getNormalList());
	    } finally {
		c.unlock();
	    }
	    geometry.setMesh(mesh);
	    geometry.setMaterial(defaultMat);

	    Vec3 chunkPosition = are.getAbsoluteChunkPosition(v);
	    geometry.setLocalTranslation(chunkPosition.getX(), chunkPosition.getY(), chunkPosition.getZ());

	    rigidBodyControl = geometry.getControl(RigidBodyControl.class);

	    CollisionShape collisionShape;

	    collisionShape = CollisionShapeFactory.createMeshShape(geometry);

	    if (rigidBodyControl == null) {
		rigidBodyControl = new RigidBodyControl(collisionShape, 0);
		geometry.addControl(rigidBodyControl);
		physicsSpace.add(rigidBodyControl);
	    } else {
		rigidBodyControl.setCollisionShape(collisionShape);
	    }

	    if (DebugAppState.backfaceCulled) {
		geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
	    } else {
		geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
	    }

	    if (DebugAppState.wireframe) {
		geometry.getMaterial().getAdditionalRenderState().setWireframe(true);
	    }
	    chunkLoaded++;
	    if (chunkLoaded > maxChunkLoad) {
		return;
	    }
	}
	for (AreMessage message = detachQueue.poll(); message != null; message = detachQueue.poll()) {
	    Chunk c = (Chunk) message.getData();
	    String name;

	    try {
		c.lock();
		name = c.getName();
	    } finally {
		c.unlock();
	    }

	    Spatial spatial = node.getChild(name);

	    if (spatial != null) {
		Geometry geometry = (Geometry) spatial;
		RigidBodyControl rigidBodyControl;
		try {
		    rigidBodyControl = geometry.getControl(RigidBodyControl.class);
		    physicsSpace.remove(rigidBodyControl);
		} catch (Exception ex) {
		    System.out.println("Failed to remove rigidBody from: " + name);
		}
		geometry.removeFromParent();
	    }
	}
    }
}
