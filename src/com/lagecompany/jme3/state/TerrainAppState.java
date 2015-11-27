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
import com.lagecompany.storage.voxel.SpecialVoxel;
import com.lagecompany.storage.voxel.SpecialVoxelData;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * On this stage, the terrain (chunks) will be processed. It means it will be managed by this app state.
 *
 * @author Afonso Lage
 */
public class TerrainAppState extends AbstractAppState {

    private static final String CHUNK_NODE_PREFIX = "Node-";
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
    protected Material voxelAtlas;
    private Texture texture;

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
     * @param application The application which this stage was attached to
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
	processMessages(tpf);
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
	texture = assetManager.loadTexture("Textures/Elements/atlas.png");

	voxelAtlas = new Material(assetManager, "MatDefs/VoxelLighting.j3md");
	voxelAtlas.setTexture("DiffuseMap", texture);
	voxelAtlas.setFloat("TileSize", 1f / (float) (texture.getImage().getWidth() / 128));
	voxelAtlas.setFloat("MaxTileSize", 1f / Chunk.SIZE);
	voxelAtlas.getTextureParam("DiffuseMap").getTextureValue().setWrap(Texture.WrapMode.Clamp);
	voxelAtlas.getTextureParam("DiffuseMap").getTextureValue().setMagFilter(Texture.MagFilter.Nearest);
	voxelAtlas.getTextureParam("DiffuseMap").getTextureValue().setMinFilter(Texture.MinFilter.NearestLinearMipMap);
	//size: 0,125 x 0,625
	//offset: 0,4375 x 0,375

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
    public void processMessages(float tpf) {
	Integer batch = renderBatchQueue.poll();

	if (batch == null) {
	    return;
	}

	ConcurrentLinkedQueue<AreMessage> queue = are.getQueue(batch, AreMessage.Type.CHUNK_ATTACH);
	if (queue != null) {
	    for (AreMessage message = queue.poll(); message != null; message = queue.poll()) {
		attachChunk(message);
	    }
	    are.finishBatch(AreMessage.Type.CHUNK_ATTACH, batch);
	}

	queue = are.getQueue(batch, AreMessage.Type.CHUNK_DETACH);
	if (queue != null) {
	    for (AreMessage message = queue.poll(); message != null; message = queue.poll()) {
		detachChunk(message);
	    }
	    are.finishBatch(AreMessage.Type.CHUNK_DETACH, batch);
	}

	queue = are.getQueue(batch, AreMessage.Type.SPECIAL_VOXEL_ATTACH);
	if (queue != null) {
	    for (AreMessage message = queue.poll(); message != null; message = queue.poll()) {
		attachSpecialVoxel(message);
	    }
	    are.finishBatch(AreMessage.Type.SPECIAL_VOXEL_ATTACH, batch);
	}

	queue = are.getQueue(batch, AreMessage.Type.SPECIAL_VOXEL_DETACH);
	if (queue != null) {
	    for (AreMessage message = queue.poll(); message != null; message = queue.poll()) {
		detachSpecialVoxel(message);
	    }
	    are.finishBatch(AreMessage.Type.SPECIAL_VOXEL_DETACH, batch);
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
	    message.setType(AreMessage.Type.CHUNK_DETACH);
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
	    Node chunkNode = new Node(CHUNK_NODE_PREFIX + name);
	    chunkNode.attachChild(geometry);
	    node.attachChild(chunkNode);

	    Vec3 chunkPosition = are.getAbsoluteChunkPosition(v);
	    chunkNode.setLocalTranslation(chunkPosition.x, chunkPosition.y, chunkPosition.z);
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
	geometry.setMaterial(voxelAtlas);
	geometry.updateModelBound();

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

    private void attachSpecialVoxel(AreMessage message) {
	SpecialVoxelData data = (SpecialVoxelData) message.getData();
	String voxelName = data.toString();
	String chunkNodeName = CHUNK_NODE_PREFIX + data.chunk.getName();

	Spatial chunkNodeSpatial = node.getChild(chunkNodeName);

	if (chunkNodeSpatial == null) {
	    throw new RuntimeException("Failed to find chunk node " + chunkNodeName);
	}

	Node chunkNode = (Node) chunkNodeSpatial;

	Spatial voxelSpatial = chunkNode.getChild(voxelName);
	Geometry geometry;

	if (voxelSpatial == null) {
	    geometry = new Geometry(voxelName);
	    chunkNode.attachChild(geometry);

	    geometry.setLocalTranslation(data.x, data.y, data.z);
	} else {
	    geometry = (Geometry) voxelSpatial;
	}

	short specialType = data.chunk.get(data.x, data.y, data.z).getType();

	Mesh mesh = new Mesh();
	mesh.setBuffer(VertexBuffer.Type.Position, 3, SpecialVoxel.getVertices(specialType));
	mesh.setBuffer(VertexBuffer.Type.Index, 1, SpecialVoxel.getIndexes(specialType));
	mesh.setBuffer(VertexBuffer.Type.Normal, 3, SpecialVoxel.getNormals(specialType));
	mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, SpecialVoxel.getTextCoord(specialType));
	mesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, SpecialVoxel.getTileCoord(specialType));
	mesh.setBuffer(VertexBuffer.Type.Color, 4, SpecialVoxel.getTextColor(specialType));

	mesh.updateBound();
	geometry.setMesh(mesh);
	geometry.setMaterial(voxelAtlas);
	geometry.updateModelBound();

	if (DebugAppState.backfaceCulled) {
	    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
	} else {
	    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
	}

	if (DebugAppState.wireframe) {
	    geometry.getMaterial().getAdditionalRenderState().setWireframe(true);
	}
    }

    private void detachSpecialVoxel(AreMessage message) {
	SpecialVoxelData data = (SpecialVoxelData) message.getData();
	String voxelName = data.toString();
	String chunkNodeName = CHUNK_NODE_PREFIX + data.chunk.getName();

	Spatial chunkNodeSpatial = node.getChild(chunkNodeName);

	if (chunkNodeSpatial != null) {
	    Node chunkNode = (Node) chunkNodeSpatial;
	    Spatial voxelSpatial = chunkNode.getChild(voxelName);

	    if (voxelSpatial == null) {
		return;
	    }

	    Geometry voxelGeometry = (Geometry) voxelSpatial;
	    voxelGeometry.removeFromParent();
	}
    }
}
