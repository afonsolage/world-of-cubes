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
import com.lagecompany.jme3.control.PlayerTranslateControl;
import com.lagecompany.jme3.listener.PlayerTranslateListener;
import com.lagecompany.storage.Are;
import com.lagecompany.storage.AreMessage;
import com.lagecompany.storage.AreMessage.AreMessageType;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author afonsolage
 */
public class TerrainAppState extends AbstractAppState implements PlayerTranslateListener {

    private Are are;
    private Node node;
    private AssetManager assetManager;
    private Node rootNode;
    private Node playerNode;
    private SimpleApplication app;
    private BulletAppState bulletState;
    private PhysicsSpace physicsSpace;
    private final ConcurrentLinkedQueue<AreMessage> rendererQueue;
    private Material defaultMat;
    private float elapsedTime;
    private int chunkLoaded;
    private boolean shouldRender;
    public int maxChunkLoad = -1;

    public TerrainAppState() {
	rendererQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	this.app = (SimpleApplication) application;
	this.assetManager = app.getAssetManager();
	this.rootNode = app.getRootNode();
	this.bulletState = stateManager.getState(BulletAppState.class);
	this.physicsSpace = bulletState.getPhysicsSpace();
	initMaterials();

	Are.instanciate(rendererQueue);
	are = Are.getInstance();
	node = new Node("Chunks Node");
	rootNode.attachChild(node);

	playerNode = stateManager.getState(WorldAppState.class).getPlayerNode();
	playerNode.addControl(new PlayerTranslateControl(this));

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
    
    public int getRendererQueueSize() {
	return rendererQueue.size();
    }
    
    public void processChunks(float tpf) {
	elapsedTime += tpf;

	if (elapsedTime > 0.1) {
	    elapsedTime = 0;
	    chunkLoaded = 0;
	}

	for (AreMessage message = rendererQueue.poll(); message != null; message = rendererQueue.poll()) {
	    Chunk c = (Chunk) message.getData();

	    Vec3 v = c.getPosition();
	    String name = c.getName();
	    Spatial spatial = node.getChild(name);

	    Geometry geometry;
	    RigidBodyControl rigidBodyControl;

	    if (message.getType() == AreMessageType.CHUNK_ATTACH && c.getVertexCount() > 0) {

		if (spatial == null) {
		    geometry = new Geometry(name);
		    node.attachChild(geometry);
		} else {
		    geometry = (Geometry) spatial;
		}

		Mesh mesh = new Mesh();
		mesh.setBuffer(VertexBuffer.Type.Position, 3, c.getVertexList());
		mesh.setBuffer(VertexBuffer.Type.Index, 1, c.getIndexList());
		mesh.setBuffer(VertexBuffer.Type.Normal, 3, c.getNormalList());

		geometry.setMesh(mesh);
		geometry.setMaterial(defaultMat);

		Vec3 chunkPosition = are.getAbsoluteChunkPosition(v);
		geometry.setLocalTranslation(chunkPosition.getX(), chunkPosition.getY(), chunkPosition.getZ());

		rigidBodyControl = geometry.getControl(RigidBodyControl.class);

		CollisionShape collisionShape = null;

		try {
		    collisionShape = CollisionShapeFactory.createMeshShape(geometry);
		} catch (Exception ex) {
		    continue;
		}

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
	    if (message.getType() == AreMessageType.CHUNK_DETACH) {
		if (spatial != null) {
		    geometry = (Geometry) spatial;
		    rigidBodyControl = geometry.getControl(RigidBodyControl.class);
		    physicsSpace.remove(rigidBodyControl);
		    geometry.removeFromParent();
		}
	    }
	}
    }

    @Override
    public void doAction(Vec3 currentLocation) {
	currentLocation.subtract(are.getPosition());

	if (currentLocation.equals(Vec3.ZERO()) || are.isMoving()) {
	    return;
	}

	System.out.println("Moved: " + currentLocation.toString() + " (Are position: " + are.getPosition() + ")");
	AreMessage message = new AreMessage(AreMessage.AreMessageType.ARE_MOVE, currentLocation);
	are.postMessage(message);
	are.setMoving(true);
    }
}
