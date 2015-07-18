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
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Texture;
import com.lagecompany.jme3.control.AreFollowControl;
import com.lagecompany.storage.Are;
import com.lagecompany.storage.AreMessage;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * On this stage, the terrain (chunks) will be processed. It means it will be managed by this app state.
 *
 * @author Afonso Lage
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
    private final ConcurrentLinkedQueue<Integer> renderBatchQueue;
    private boolean shouldRender;
    private Material atlas;

    /**
     * Create a new instance of this AppState
     */
    public TerrainAppState() {
	are = Are.getInstance();
	renderBatchQueue = are.getRenderBatchQueue();
    }

    /**
     * Initialize this stage. Is called intenally by JME3.
     *
     * @param stateManager The StateManager used by JME3
     * @param app The application which this stage was attached to
     */
    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	this.app = (SimpleApplication) application;
	this.assetManager = app.getAssetManager();
	this.rootNode = app.getRootNode();
	this.bulletState = stateManager.getState(BulletAppState.class);
	this.physicsSpace = bulletState.getPhysicsSpace();

	initMaterials();

	node = new Node("Chunks Node");

	playerNode = stateManager.getState(WorldAppState.class).getPlayerNode();
	playerNode.addControl(new AreFollowControl());

	Vector3f playerPosition = playerNode.getLocalTranslation();
	node.setLocalTranslation(playerPosition);
	rootNode.attachChild(node);

	are.setPosition((int) playerPosition.getX(), (int) playerPosition.getY(), (int) playerPosition.getZ());
	are.init();
	are.start();
    }

    /**
     * Update loop of this stage. Is called by main loop.
     *
     * @param tpf Time per frame in seconds.
     */
    @Override
    public void update(float tpf) {
	are.tick(tpf);
	processChunks(tpf);
    }

    /**
     * This method is called by JME3 when this stage is detached, so it must be used for cleanup.
     */
    @Override
    public void cleanup() {
	super.cleanup(); //To change body of generated methods, choose Tools | Templates.
	are.interrupt();
    }

    /**
     * Init all default materials to be used by chunks.
     */
    private void initMaterials() {
	atlas = new Material(assetManager, "MatDefs/VoxelLighting.j3md");

//	atlas.setColor("Color", ColorRGBA.Blue);
	atlas.setTexture("DiffuseMap", assetManager.loadTexture("Textures/Elements/atlas.png"));
	atlas.setFloat("TileSize", 1f / 2f);
	atlas.setFloat("MaxTileSize", 1f / Chunk.SIZE);
//	atlas.setBoolean("UseMaterialColors", true);
	atlas.getTextureParam("DiffuseMap").getTextureValue().setWrap(Texture.WrapMode.Clamp);
	atlas.getTextureParam("DiffuseMap").getTextureValue().setMagFilter(Texture.MagFilter.Nearest);
	atlas.getTextureParam("DiffuseMap").getTextureValue().setMinFilter(Texture.MinFilter.NearestLinearMipMap);
//	atlas.setColor("Ambient", ColorRGBA.White);
//	atlas.setColor("Diffuse", ColorRGBA.White);
//	atlas.setColor("Specular", ColorRGBA.White);
//	atlas.setFloat("Shininess", 0f);
    }

    /**
     * Check if this state is can render terrain.
     *
     * @return true if should render, else false.
     */
    public boolean shouldRender() {
	return shouldRender;
    }

    /**
     * Enable or disable the rendering of chunks on this stage.
     *
     * @param should true if should render, else false.
     */
    public void setShouldRender(boolean should) {
	this.shouldRender = should;
    }

    /**
     * Proccess the chunk queue and attach or detach it from scene.
     *
     * @param tpf Time per frame in seconds.
     */
    public void processChunks(float tpf) {
	Integer batch = renderBatchQueue.poll();

	if (batch == null) {
	    return;
	}

	ConcurrentLinkedQueue<AreMessage> queue = are.getAttachQueue(batch);
	if (queue != null) {
	    for (AreMessage message = queue.poll(); message != null; message = queue.poll()) {
		attachChunk(message);
	    }
	    are.finishBatch(AreMessage.AreMessageType.CHUNK_ATTACH, batch);
	}

	queue = are.getDetachQueue(batch);
	if (queue != null) {
	    for (AreMessage message = queue.poll(); message != null; message = queue.poll()) {
		detachChunk(message);
	    }
	    are.finishBatch(AreMessage.AreMessageType.CHUNK_DETACH, batch);
	}

    }

    /**
     * Read an AreMessage and attach the given chunk.
     *
     * @param message an AreMessage of type CHUNK_ATTACH
     */
    private void attachChunk(AreMessage message) {
	Chunk c = (Chunk) message.getData();

	if (!c.hasVertext()) {
	    message.setType(AreMessage.AreMessageType.CHUNK_DETACH);
	    are.postMessage(message);
	    return;
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
	CollisionShape collisionShape;

	try {
	    c.lock();
	    mesh.setBuffer(VertexBuffer.Type.Position, 3, c.getVertexList());
	    mesh.setBuffer(VertexBuffer.Type.Index, 1, c.getIndexList());
	    mesh.setBuffer(VertexBuffer.Type.Normal, 3, c.getNormalList());
	    mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, c.getTextCoord());
	    mesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, c.getTileCoord());
	    mesh.setBuffer(VertexBuffer.Type.Color, 4, c.getTexColor());
	} finally {
	    c.unlock();
	}

	mesh.updateBound();
	geometry.setMesh(mesh);
	geometry.setMaterial(atlas);
	geometry.updateModelBound();

	Vec3 chunkPosition = are.getAbsoluteChunkPosition(v);
	geometry.setLocalTranslation(chunkPosition.getX(), chunkPosition.getY(), chunkPosition.getZ());

	rigidBodyControl = geometry.getControl(RigidBodyControl.class);
	collisionShape = CollisionShapeFactory.createMeshShape(geometry);

	if (rigidBodyControl == null) {
	    rigidBodyControl = new RigidBodyControl(collisionShape, 0);
	    rigidBodyControl.setFriction(1f);
	    geometry.addControl(rigidBodyControl);
	    physicsSpace.add(rigidBodyControl);
	} else {
	    rigidBodyControl.setEnabled(false);
	    rigidBodyControl.setCollisionShape(collisionShape);
	    rigidBodyControl.setEnabled(true);
	}

	if (DebugAppState.backfaceCulled) {
	    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
	} else {
	    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
	}

	if (DebugAppState.wireframe) {
	    geometry.getMaterial().getAdditionalRenderState().setWireframe(true);
	}
    }

    /**
     * Read an AreMessage and detach the given chunk.
     *
     * @param message an AreMessage of type CHUNK_DETACH
     */
    private void detachChunk(AreMessage message) {
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
