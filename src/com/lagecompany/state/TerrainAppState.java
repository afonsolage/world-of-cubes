package com.lagecompany.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.lagecompany.storage.Are;
import com.lagecompany.storage.AreMessage;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author afonsolage
 */
public class TerrainAppState extends AbstractAppState {

    private static final float FPS_FRACTION = 1f / 60f;
    private Are are;
    private Node node;
    private AssetManager assetManager;
    private Node rootNode;
    private SimpleApplication app;

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	this.app = (SimpleApplication) application;
	this.assetManager = app.getAssetManager();
	this.rootNode = app.getRootNode();

	are = new Are();
	node = new Node("Chunks Node");

	rootNode.attachChild(node);
	are.start();
	are.init();
    }

    @Override
    public void update(float tpf) {
	detachChunks();
	attachChunks();
    }

    @Override
    public void cleanup() {
	super.cleanup(); //To change body of generated methods, choose Tools | Templates.
	are.interrupt();
    }

    public void detachChunks() {
	if (are.getQueueSize(Are.IT_DETACH) == 0) {
	    return;
	}

	for (Iterator<Map.Entry<Vec3, Chunk>> it = are.iterator(Are.IT_DETACH); it.hasNext();) {
	    Map.Entry<Vec3, Chunk> entry = it.next();
	    Vec3 v = entry.getKey();
	    Chunk chunk = entry.getValue();
	    String name = String.format("Chunk %s", v);
	    Spatial spatial = node.getChild(name);

	    // If chunk is null or there is no mesh data, skip it
	    if (chunk == null || chunk.getFlag() != Chunk.FLAG_DETACH) {
		it.remove();
		continue;
	    }

	    if (spatial == null) {
		System.err.println(String.format("Invalid detach flag on Chunk (%s): Spatial is null.", v));
	    } else {
		chunk.setFlag(Chunk.FLAG_UNLOAD);
		are.unload(v, chunk);
		System.out.println(String.format("Removed (%s) child at: %d", v, node.detachChild(spatial)));
	    }
	    it.remove();
	}
    }

    public void attachChunks() {
	if (are.getQueueSize(Are.IT_ATTACH) == 0) {
	    return;
	}

	for (Iterator<Map.Entry<Vec3, Chunk>> it = are.iterator(Are.IT_ATTACH); it.hasNext();) {
	    Map.Entry<Vec3, Chunk> entry = it.next();
	    Vec3 v = entry.getKey();
	    Chunk chunk = entry.getValue();
	    String name = String.format("Chunk %s", v);
	    Spatial spatial = node.getChild(name);

	    // If chunk is null or there is no mesh data, skip it
	    if (chunk == null || chunk.getFlag() != Chunk.FLAG_ATTACH) {
		it.remove();
		continue;
	    }

	    Geometry geometry;

	    if (spatial == null) {
		geometry = new Geometry(name);
		node.attachChild(geometry);
	    } else {
		geometry = (Geometry) spatial;
	    }

	    Mesh mesh = new Mesh();
	    mesh.setBuffer(VertexBuffer.Type.Position, 3, chunk.getVertexList());
	    mesh.setBuffer(VertexBuffer.Type.Index, 1, chunk.getIndexList());
	    mesh.setBuffer(VertexBuffer.Type.Normal, 3, chunk.getNormalList());

	    Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
	    //mat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/Elements/rock.jpg"));
	    material.setColor("Ambient", ColorRGBA.White);
	    material.setColor("Diffuse", ColorRGBA.Gray);
	    material.setColor("Specular", ColorRGBA.White);
	    material.setFloat("Shininess", 0f);
	    material.setBoolean("UseMaterialColors", true);

	    geometry.setMesh(mesh);
	    geometry.setMaterial(material);

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

	    chunk.setFlag(Chunk.FLAG_NONE);
	    it.remove();
	}
    }

    public void move() {
	AreMessage message = AreMessage.MOVE;
	message.setData(new Vec3(1, 0, 0));
	are.postMessage(message);
    }
}
