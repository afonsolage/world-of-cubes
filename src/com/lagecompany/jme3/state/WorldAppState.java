package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Texture;
import com.lagecompany.jme3.control.CameraFollowControl;
import com.lagecompany.jme3.control.PlayerControl;
import com.lagecompany.jme3.manager.CameraMan;
import com.lagecompany.storage.Are;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.voxel.Voxel;
import com.lagecompany.storage.voxel.VoxelMesh;
import com.lagecompany.ui.ToolbarWindow;
import java.nio.FloatBuffer;

/**
 * On this stage, the world will be processed. By world we mean all content relative to world except Terrain.
 *
 * @author Afonso Lage
 */
public class WorldAppState extends AbstractAppState {

    private SimpleApplication app;
    private Node rootNode;
    private InputManager inputManager;
    private FlyByCamera flyCam;
    private Camera cam;
    private CameraMan cameraMan;
    private Node playerNode;
    private BulletAppState bulletState;
    private Are are;
    private TerrainAppState terrainState;
    private ToolbarWindow toolbar;
    private Node guiNode;
    private Material atlas;
    private AssetManager assetManager;

    /**
     * Initialize this stage. Is called intenally by JME3.
     *
     * @param stateManager The StateManager used by JME3
     * @param app The application which this stage was attached to
     */
    @Override
    public void initialize(AppStateManager stateManager, Application application) {
	super.initialize(stateManager, application);
	this.app = (SimpleApplication) application;
	this.rootNode = app.getRootNode();
	this.guiNode = app.getGuiNode();
	this.inputManager = app.getInputManager();
	this.flyCam = app.getFlyByCamera();
	this.cam = app.getCamera();
	this.bulletState = stateManager.getState(BulletAppState.class);
	this.are = Are.getInstance();
	this.assetManager = app.getAssetManager();
	app.getViewPort().setBackgroundColor(new ColorRGBA(0.5294f, 0.8078f, 0.9215f, 1f));
	terrainState = stateManager.getState(TerrainAppState.class);
	inputManager.setCursorVisible(true);
//	rootNode.setCullHint(Spatial.CullHint.Never); //TODO: Fix frustum culling.

	//Create some lights.
	AmbientLight ambient = new AmbientLight();
	ambient.setColor(ColorRGBA.DarkGray.mult(.25f));
	rootNode.addLight(ambient);
	guiNode.addLight(ambient);
	
	DirectionalLight sun = new DirectionalLight();
	sun.setDirection(new Vector3f(-0.25f, -0.75f, -0.25f).normalizeLocal());

	sun.setColor(ColorRGBA.White);
	rootNode.addLight(sun);
	guiNode.addLight(sun);
	//Create player node.
	playerNode = new Node("Player Node");
	playerNode.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);

	//Add Physics to our node.
	PlayerControl characterControl = new PlayerControl(0.4f, 4f, 60f);
	bulletState.getPhysicsSpace().add(characterControl);
	playerNode.addControl(characterControl);

	//Add a camera follow control to our node.
	CameraFollowControl followControl = new CameraFollowControl(cam, inputManager);
	playerNode.addControl(followControl);

	characterControl.setEnabled(false);
	followControl.setEnabled(false); //For debug reasons.

	cameraMan = new CameraMan(flyCam, followControl, inputManager);
	cameraMan.toggleCam(false);
	cameraMan.unbind();

	rootNode.attachChild(playerNode);

	initMaterials();
	initInterface();
    }

    /**
     * Update loop of this stage. Is called by main loop.
     *
     * @param tpf Time per frame in seconds.
     */
    @Override
    public void update(float tpf) {
	super.update(tpf); //To change body of generated methods, choose Tools | Templates.

	renderSpecialVoxels();
    }

    /**
     * Get the player node
     *
     * @return The player node
     */
    public Node getPlayerNode() {
	return playerNode;
    }

    /**
     * Get the camera manager
     *
     * @return The camera manager
     */
    public CameraMan getCameraMan() {
	return cameraMan;
    }

    void startEnvironment() {
	this.playerNode.getControl(PlayerControl.class).setEnabled(true);
    }

    private void renderSpecialVoxels() {
    }

    private void initInterface() {
	toolbar = new ToolbarWindow(cam.getWidth(), cam.getHeight());

	Mesh mesh = VoxelMesh.getMesh(Voxel.VT_STONE);
	
	FloatBuffer buffer = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Color).getData();
	buffer.rewind();
	
	while(buffer.hasRemaining()) {
	    //Front
	    buffer.put(1.3f); buffer.put(1.3f); buffer.put(1.3f); buffer.put(1.3f);
	    buffer.put(1.3f); buffer.put(1.3f); buffer.put(1.3f); buffer.put(1.3f);
	    buffer.put(1.3f); buffer.put(1.3f); buffer.put(1.3f); buffer.put(1.3f);
	    buffer.put(1.3f); buffer.put(1.3f); buffer.put(1.3f); buffer.put(1.3f);
	    
	    //Right
	    buffer.position(buffer.position() + 4 * 4);
	    
	    //Back
	    buffer.position(buffer.position() + 4 * 4);
	    
	    //Left
	    buffer.position(buffer.position() + 4 * 4);
	    
	    //Top
	    buffer.put(1.8f); buffer.put(1.8f); buffer.put(1.8f); buffer.put(1.8f);
	    buffer.put(1.8f); buffer.put(1.8f); buffer.put(1.8f); buffer.put(1.8f);
	    buffer.put(1.8f); buffer.put(1.8f); buffer.put(1.8f); buffer.put(1.8f);
	    buffer.put(1.8f); buffer.put(1.8f); buffer.put(1.8f); buffer.put(1.8f);
	    
	    //Down
	    buffer.position(buffer.position() + 4 * 4);
	}
	
	Geometry geom = new Geometry("box", mesh);
	geom.setMaterial(atlas);
//	geom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
	geom.setCullHint(Spatial.CullHint.Never);

//	guiNode.attachChild(geom);


	toolbar.show(guiNode);
	toolbar.setSlot(1, geom);
    }

    private void initMaterials() {
	Texture texture = assetManager.loadTexture("Textures/Elements/atlas.png");
	atlas = new Material(assetManager, "MatDefs/VoxelLighting.j3md");
	atlas.setTexture("DiffuseMap", texture);
	atlas.setFloat("TileSize", 1f / (float) (texture.getImage().getWidth() / 128));
	atlas.setFloat("MaxTileSize", 1f / Chunk.SIZE);
	atlas.getTextureParam("DiffuseMap").getTextureValue().setWrap(Texture.WrapMode.Clamp);
	atlas.getTextureParam("DiffuseMap").getTextureValue().setMagFilter(Texture.MagFilter.Nearest);
	atlas.getTextureParam("DiffuseMap").getTextureValue().setMinFilter(Texture.MinFilter.NearestLinearMipMap);
    }
}
