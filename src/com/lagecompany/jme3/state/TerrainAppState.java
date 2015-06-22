package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
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

    private static final int MAX_LOAD_PER_SECOND = 5;
    private Are are;
    private Node node;
    private AssetManager assetManager;
    private Node rootNode;
    private Node playerNode;
    private SimpleApplication app;
    private final ConcurrentLinkedQueue<AreMessage> rendererQueue;
    private Material defaultMat;
    private float elapsedTime;
    private int chunkLoaded;

    public TerrainAppState() {
	rendererQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	this.app = (SimpleApplication) application;
	this.assetManager = app.getAssetManager();
	this.rootNode = app.getRootNode();

	initMaterials();

	are = new Are(rendererQueue);
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
	processChunks(tpf);
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

	    if (message.getType() == AreMessageType.CHUNK_ATTACH) {
		Geometry geometry;

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

		if (DebugAppState.backfaceCulled) {
		    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
		} else {
		    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
		}

		if (DebugAppState.wireframe) {
		    geometry.getMaterial().getAdditionalRenderState().setWireframe(true);
		}
		chunkLoaded++;
		if (chunkLoaded > MAX_LOAD_PER_SECOND) {
		    return;
		}
	    }
	    if (message.getType() == AreMessageType.CHUNK_DETACH) {
		if (spatial != null) {
		    node.detachChild(spatial);
		}
	    }
	}
    }

    @Override
    public void doAction(Vector3f currentLocation) {
	Vec3 moved = new Vec3((int) (currentLocation.getX() / Chunk.WIDTH),
		(int) (currentLocation.getY() / Chunk.HEIGHT),
		(int) (currentLocation.getZ() / Chunk.LENGTH));

	moved.subtract(are.getPosition());
	if (moved.equals(Vec3.ZERO())) {
	    return;
	}

	System.out.println("Moved: " + moved.toString() + " (Are position: " + are.getPosition() + ")");
	AreMessage message = new AreMessage(AreMessage.AreMessageType.ARE_MOVE, moved);
	are.postMessage(message);
    }
}
