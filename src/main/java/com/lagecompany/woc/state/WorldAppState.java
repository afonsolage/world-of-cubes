package com.lagecompany.woc.state;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
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
import com.jme3.texture.Texture;
import com.lagecompany.woc.manager.CameraMan;
import com.lagecompany.woc.storage.Chunk;
import com.lagecompany.woc.storage.voxel.Voxel;
import com.lagecompany.woc.storage.voxel.VoxelMesh;
import com.lagecompany.woc.ui.ToolbarWindow;

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
	// private Are are;
	// private TerrainAppState terrainState;
	private ToolbarWindow toolbar;
	private Node guiNode;
	private Material atlas;
	private AssetManager assetManager;

	/**
	 * Initialize this stage. Is called intenally by JME3.
	 *
	 * @param stateManager
	 *            The StateManager used by JME3
	 * @param application
	 *            The application which this stage was attached to
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
		// this.are = Are.getInstance();
		this.assetManager = app.getAssetManager();
		app.getViewPort().setBackgroundColor(new ColorRGBA(0.5294f, 0.8078f, 0.9215f, 1f));
		// terrainState = stateManager.getState(TerrainAppState.class);
		inputManager.setCursorVisible(true);

		// Create some lights.
		AmbientLight ambient = new AmbientLight();
		ambient.setColor(ColorRGBA.DarkGray.mult(.25f));
		// rootNode.addLight(ambient);
		guiNode.addLight(ambient);

		DirectionalLight sun = new DirectionalLight();
		sun.setDirection(new Vector3f(-0.25f, -0.75f, -0.25f).normalizeLocal());

		sun.setColor(ColorRGBA.White);
		rootNode.addLight(sun);
		guiNode.addLight(sun);
		// Create player node.
		playerNode = new Node("Player Node");
		playerNode.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);


		cameraMan = new CameraMan(flyCam, cam, inputManager);

		rootNode.attachChild(playerNode);

		initMaterials();
		initInterface();
	}

	/**
	 * Update loop of this stage. Is called by main loop.
	 *
	 * @param tpf
	 *            Time per frame in seconds.
	 */
	@Override
	public void update(float tpf) {
		super.update(tpf); // To change body of generated methods, choose Tools | Templates.

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

	private void renderSpecialVoxels() {
	}

	private void initInterface() {

	}

	public void loadToolbar() {
		Mesh mesh = VoxelMesh.getMesh(Voxel.VT_STONE);

		Geometry geom = new Geometry("box", mesh);
		geom.setMaterial(atlas);
		geom.setCullHint(Spatial.CullHint.Never);
		toolbar.setSlot(1, geom);
	}

	private void initMaterials() {
		Texture texture = assetManager.loadTexture("Textures/Elements/atlas.png");
		atlas = new Material(assetManager, "MatDefs/VoxelLighting.j3md");
		atlas.setTexture("DiffuseMap", texture);
		atlas.setFloat("TileSize", 1f / (float) (texture.getImage().getWidth() / 128));
		atlas.setFloat("MaxTileSize", 1f / Chunk.SIZE);
		atlas.getTextureParam("DiffuseMap").getTextureValue().setWrap(Texture.WrapMode.EdgeClamp);
		atlas.getTextureParam("DiffuseMap").getTextureValue().setMagFilter(Texture.MagFilter.Nearest);
		atlas.getTextureParam("DiffuseMap").getTextureValue().setMinFilter(Texture.MinFilter.NearestLinearMipMap);
	}
}
