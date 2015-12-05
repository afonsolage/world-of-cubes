package com.lagecompany.jme3.state;

import com.jme3.app.Application;
import com.jme3.app.BasicProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.ui.Picture;
import com.lagecompany.jme3.control.CameraFollowControl;
import com.lagecompany.jme3.control.AreFollowControl;
import com.lagecompany.jme3.manager.CameraMan;
import com.lagecompany.manager.Global;
import com.lagecompany.manager.WindowManager;
import com.lagecompany.storage.Are;
import com.lagecompany.storage.Chunk;
import com.lagecompany.storage.Vec3;
import com.lagecompany.storage.voxel.Voxel;
import com.lagecompany.storage.voxel.VoxelReference;
import com.lagecompany.ui.DebugWindow;

public class DebugAppState extends AbstractAppState implements ActionListener, AnalogListener {

    private SimpleApplication app;
    private Node rootNode;
    private Node playerNode;
    private Node guiNode;
    private Are are;
    private AssetManager assetManager;
    private InputManager inputManager;
    private FlyByCamera flyCam;
    private CameraMan cameraMan;
    private Camera cam;
    private DebugWindow debugScreen;
    private CameraFollowControl followControl;
    private AreFollowControl translateControl;
    private BulletAppState bulletState;
    private PhysicsSpace physicsSpace;
    private AppSettings settings;
    private Node compass;
    private Node lightNode;
    public static boolean wireframe;
    public static boolean backfaceCulled;
    public static boolean axisArrowsEnabled;
    public static boolean playerFollow;
    public static DebugAppState instance;
    private static BitmapFont defaultFont;
    private long lastPicking;

    @Override
    public void initialize(AppStateManager stateManager, Application application) {
        super.initialize(stateManager, application);
        this.app = (SimpleApplication) application;
        this.rootNode = app.getRootNode();
        this.inputManager = app.getInputManager();
        this.assetManager = app.getAssetManager();
        this.flyCam = app.getFlyByCamera();
        this.settings = app.getContext().getSettings();
        this.guiNode = app.getGuiNode();
        this.cam = app.getCamera();

        playerNode = stateManager.getState(WorldAppState.class).getPlayerNode();
        cameraMan = stateManager.getState(WorldAppState.class).getCameraMan();
        bulletState = stateManager.getState(BulletAppState.class);
        physicsSpace = bulletState.getPhysicsSpace();
//	stateManager.attach(new BulletDebugAppState(physicsSpace));
        followControl = playerNode.getControl(CameraFollowControl.class);
        translateControl = playerNode.getControl(AreFollowControl.class);

        defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        are = Are.getInstance();

        wireframe = false;
        backfaceCulled = false;
        axisArrowsEnabled = false;
        playerFollow = false;

//	showPlayerNode();
        showAim();
        createCompass();

        this.bindKeys();
        this.initGUI();

        //stateManager.attach(new BasicProfilerState(true));
        DebugAppState.instance = this;
    }

    @Override
    public void update(float tpf) {
        compass.setLocalRotation(cam.getRotation());
        compass.rotate(0, FastMath.DEG_TO_RAD * 180, 0);
        updateGUI();
    }

    private void bindKeys() {
        inputManager.addMapping("TOGGLE_WIREFRAME", new KeyTrigger(KeyInput.KEY_F1));
        inputManager.addMapping("TOGGLE_CURSOR", new KeyTrigger(KeyInput.KEY_F2));
        inputManager.addMapping("TOGGLE_CULLING", new KeyTrigger(KeyInput.KEY_F3));
        inputManager.addMapping("TOGGLE_AXISARROWS", new KeyTrigger(KeyInput.KEY_F4));
        inputManager.addMapping("UPDATE_GUI", new KeyTrigger(KeyInput.KEY_F6));
        inputManager.addMapping("CUSTOM_FUNCTION", new KeyTrigger(KeyInput.KEY_F7));
        inputManager.addMapping("TOGGLE_CAM_VIEW", new KeyTrigger(KeyInput.KEY_TAB));
        inputManager.addMapping("SET_VOXEL", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("REMOVE_VOXEL", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addMapping("MOVESPEED_UP", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("MOVESPEED_DOWN", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addListener(this, "TOGGLE_WIREFRAME", "TOGGLE_CURSOR", "TOGGLE_CULLING", "TOGGLE_AXISARROWS",
                "MOVESPEED_UP", "MOVESPEED_DOWN", "UPDATE_GUI", "CUSTOM_FUNCTION", "TOGGLE_CAM_VIEW", "SET_VOXEL",
                "REMOVE_VOXEL");

    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ("TOGGLE_WIREFRAME".equals(name) && !isPressed) {
            wireframe = !wireframe;
            toggleWireframe(rootNode, wireframe);
        } else if ("TOGGLE_CURSOR".equals(name) && !isPressed) {
            toggleCursor();
        } else if ("TOGGLE_CULLING".equals(name) && !isPressed) {
            backfaceCulled = !backfaceCulled;
            toggleBackfaceCulling(rootNode);
        } else if ("TOGGLE_AXISARROWS".equals(name) && !isPressed) {
            toggleAxisArrows();
        } else if ("UPDATE_GUI".equals(name) && !isPressed) {
            // updateGUI();
        } else if ("TOGGLE_CAM_VIEW".equals(name) && !isPressed) {
            toggleCamView();
        } else if ("CUSTOM_FUNCTION".equals(name) && !isPressed) {
//	    playerNode.getControl(PlayerControl.class).setEnabled(true);
            showLights();
            //customFunction();
        } else if ("REMOVE_VOXEL".equals(name) && !isPressed) {
            cursorPicking(true);
        } else if ("SET_VOXEL".equals(name) && !isPressed) {
            cursorPicking(false);
        }

    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        switch (name) {
            case "MOVESPEED_UP": {
                speedUpMove(value);
                break;
            }
            case "MOVESPEED_DOWN": {
                speedDownMove(value);
                break;
            }
        }

    }

    private void toggleWireframe(Node node, boolean enabled) {
        for (Spatial spatial : node.getChildren()) {
            if (spatial instanceof Geometry) {
                Geometry geometry = (Geometry) spatial;

                geometry.getMaterial().getAdditionalRenderState().setWireframe(enabled);
            } else if (spatial instanceof Node) {
                toggleWireframe((Node) spatial, enabled);
            }
        }
    }

    private void toggleCursor() {
        boolean isEnabled = inputManager.isCursorVisible();

        isEnabled = !isEnabled;

        inputManager.setCursorVisible(isEnabled);
    }

    private void toggleBackfaceCulling(Node node) {
        toggleBackfaceCulling(node, backfaceCulled);
    }

    private void toggleBackfaceCulling(Node node, boolean cull) {
        for (Spatial spatial : node.getChildren()) {
            if (spatial instanceof Geometry) {
                Geometry geometry = (Geometry) spatial;

                if (cull) {
                    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                } else {
                    geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
                }

            } else if (spatial instanceof Node) {
                toggleBackfaceCulling((Node) spatial);
            }
        }
    }

    private void toggleAxisArrows() {
        if (axisArrowsEnabled) {
            for (Spatial spatial : rootNode.getChildren()) {
                if (spatial instanceof Geometry && spatial.getName().equals("Grid")) {
                    rootNode.detachChild(spatial);
                }
            }
        } else {
            attachGrid();
        }

        axisArrowsEnabled = !axisArrowsEnabled;
    }

    private void attachGrid() {
        Geometry g = new Geometry("Grid", new Grid(200, 200, 1f));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.DarkGray);
        g.setMaterial(mat);
        g.setLocalTranslation(new Vector3f(-100, 0, -100));
        rootNode.attachChild(g);
    }

    private void speedUpMove(float value) {
        float speed = flyCam.getMoveSpeed() + value;
        flyCam.setMoveSpeed(speed);
    }

    private void speedDownMove(float value) {
        float speed = flyCam.getMoveSpeed() - value;
        flyCam.setMoveSpeed((speed < 2f) ? 2f : speed); //Minimum walk speed.
    }

    private void toggleCamView() {
        playerFollow = !playerFollow;

        //characterController.setEnabled(playerFollow);
        cameraMan.toggleCam(playerFollow);
    }

    private void initGUI() {
        debugScreen = (DebugWindow) Global.winMan.get(WindowManager.DEBUG);
        debugScreen.build();
        debugScreen.show();

    }

    private void updateGUI() {
        Vector3f v = translateControl.getPlayerPosition();
        String pos = String.format("%.2f, %.2f, %.2f", v.x, v.y, v.z);
        debugScreen.set(DebugWindow.PLAYER_ARE_POSITION, translateControl.getArePosition().toString());
        debugScreen.set(DebugWindow.PLAYER_POSITION, pos);
    }

    private void customFunction() {
        //
    }

    private void showPlayerNode() {
        Box b = new Box(0.5f, 2f, 0.5f);
        Geometry geom = new Geometry("PlayerNode Box", b);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.DarkGray);
        geom.setMaterial(mat);
        geom.setUserData("skipRayCast", true);
        playerNode.attachChild(geom);
        geom.move(0, 2f, 0);
    }

    private void showAim() {
        Picture pic = new Picture("Crosshair");
        pic.setImage(assetManager, "Interface/Icons/crosshair.png", true);
        pic.setWidth(30);
        pic.setHeight(30);
        pic.setPosition((settings.getWidth() / 2) - 15, (settings.getHeight() / 2) - 15);
        guiNode.attachChild(pic);
    }

    private void cursorPicking(boolean remove) {
        if (System.currentTimeMillis() - lastPicking < 100) {
            return;
        }

        lastPicking = System.currentTimeMillis();

        CollisionResults results = new CollisionResults();

        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        rootNode.collideWith(ray, results);

        CollisionResult collision = null;
        for (CollisionResult result : results) {
            if (result.getGeometry().getUserData("skipRayCast") != null) {
                continue;
            }
            collision = result;
            break;
        }

        if (collision == null) {
            return;
        }

        Vector3f point;
        short type;
        if (remove) {
            point = collision
                    .getContactPoint()
                    .subtractLocal(collision.getContactNormal().mult(FastMath.ZERO_TOLERANCE));
            type = Voxel.VT_NONE;
        } else {
            point = collision
                    .getContactPoint()
                    .addLocal(collision.getContactNormal().mult(FastMath.ZERO_TOLERANCE));
            type = Voxel.VT_TORCH;

        }
        Vec3 v = new Vec3(point.x, point.y, point.z);
        System.out.println("Picking: (" + point + ") - (" + v + ") - (" + collision.getContactPoint() + ")");
        are.setVoxel(v, type);

    }

    public void showPoint(Vector3f p, ColorRGBA color) {
        Sphere point = new Sphere(10, 10, 0.03f);
        Geometry geoPoint = new Geometry("DebugCollisionPoint", point);

        Material matPoint = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matPoint.setColor("Color", color);
        geoPoint.setMaterial(matPoint);

        geoPoint.setLocalTranslation(p);

        rootNode.attachChild(geoPoint);
    }

    public void showCollisingPoint(CollisionResult collision, ColorRGBA color) {
        Sphere point = new Sphere(10, 10, 0.03f);
        Geometry geoPoint = new Geometry("DebugCollisionPoint", point);
        geoPoint.setUserData("skipRayCast", true);

        Material matPoint = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matPoint.setColor("Color", color);
        geoPoint.setMaterial(matPoint);

        Arrow normal = new Arrow(collision.getContactNormal().mult(0.3f));
        Geometry geoNormal = new Geometry("DebugCollisionArrow", normal);
        geoNormal.setUserData("skipRayCast", true);

        Material matNormal = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matNormal.setColor("Color", color);
        geoNormal.setMaterial(matNormal);

        Node node = new Node("DebugPoint");
        node.attachChild(geoPoint);
        node.attachChild(geoNormal);

        node.setLocalTranslation(collision.getContactPoint());

        rootNode.attachChild(node);
    }

    private void createCompass() {
        compass = new Node("Compass Node");

        Arrow arrow = new Arrow(Vector3f.UNIT_X);
        arrow.setLineWidth(4);
        Geometry g = new Geometry("Axis Arrow X", arrow);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        g.setMaterial(mat);

        compass.attachChild(g);

        arrow = new Arrow(Vector3f.UNIT_Y);
        arrow.setLineWidth(4);
        g = new Geometry("Axis Arrow Y", arrow);
        mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        g.setMaterial(mat);

        compass.attachChild(g);

        arrow = new Arrow(Vector3f.UNIT_Z);
        arrow.setLineWidth(4);
        g = new Geometry("Axis Arrow Z", arrow);
        mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        g.setMaterial(mat);

        compass.attachChild(g);

        compass.scale(40);
        compass.setLocalTranslation(settings.getWidth() - 60, settings.getHeight() - 60, 0f);
        guiNode.attachChild(compass);
    }

    public void showLights() {
        if (lightNode == null) {
            lightNode = new Node("LightingNode");
            Vector3f playerPosition = playerNode.getLocalTranslation();
            playerPosition.addLocal((Are.DATA_WIDTH / 2), (8 * Chunk.SIZE), (Are.DATA_LENGHT / 2));
            Vec3 playerChunkPos = are.toChunkPosition((int) playerPosition.x, (int) playerPosition.y, (int) playerPosition.z);

            Chunk c;

            for (int x = -100; x <= 100; x++) {
                for (int y = -100; y <= 100; y++) {
                    for (int z = -100; z <= 100; z++) {
                        c = are.get(Vec3.copyAdd(playerChunkPos, x, y, z));
                        showChunkLight(c, lightNode);
                        toggleBackfaceCulling(lightNode, true);
                    }
                }
            }

            rootNode.attachChild(lightNode);
        } else {
            lightNode.removeFromParent();
            lightNode = null;
        }
    }

    public void showChunkLight(Chunk chunk, Node node) {
        BitmapText text;

        if (chunk == null) {
            return;
        }

        VoxelReference voxel = new VoxelReference();
        int light;
        Vec3 cPos = are.getAbsoluteChunkPosition(chunk.getPosition());
        for (int x = 0; x < Chunk.SIZE; x++) {
            voxel.position.x = x;
            for (int y = 0; y < Chunk.SIZE; y++) {
                voxel.position.y = y;
                for (int z = 0; z < Chunk.SIZE; z++) {
                    voxel.position.z = z;
                    if (!chunk.get(voxel)) {
                        continue;
                    }

                    light = voxel.getSunLight();

                    if (light == 0 || light == 15) {
                        continue;
                    }

                    text = getText(chunk.getName() + " - light " + x + "," + y + "," + z, "" + light, 0.1f, ColorRGBA.Red);
                    text.setLocalTranslation(cPos.x + x + 0.5f, cPos.y + y + 0.5f, cPos.z + z + 0.5f);
                    node.attachChild(text);
                }
            }
        }
    }

    public void removeChunkLight(Chunk chunk, Node node) {
        for (int i = 0; i < Chunk.DATA_LENGTH; i++) {
            node.detachChildNamed(chunk.getName() + " - light " + i);
        }
    }

    private BitmapText getText(String name, String text, float size, ColorRGBA color) {
        BitmapText result = new BitmapText(defaultFont);
        result.setName(name);
        result.setText(text);
        result.setSize(size);
        result.setColor(color);
        result.addControl(new BillboardControl());
        return result;
    }
}
