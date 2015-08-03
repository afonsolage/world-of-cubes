package test;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.DefaultRangedValueModel;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.LayerComparator;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.Slider;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.CursorMotionEvent;
import com.simsilica.lemur.event.DefaultCursorListener;
import com.simsilica.lemur.event.DragHandler;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.StyleLoader;
import com.simsilica.lemur.style.Styles;

/**
 *
 * @author Afonso Lage
 */
public class LemurTest extends SimpleApplication {
    
    private VersionedReference<Double> redRef;
    private VersionedReference<Double> greenRef;
    private VersionedReference<Double> blueRef;
    private VersionedReference<Double> alphaRef;
    private VersionedReference<Boolean> showStatsRef;
    private VersionedReference<Boolean> showFpsRef;
    private ColorRGBA boxColor = ColorRGBA.Blue.clone();
    
    public static void main(String[] args) {
	LemurTest test = new LemurTest();
	test.start();
    }
    
    @Override
    public void simpleInitApp() {
	GuiGlobals.initialize(this);
	
	stateManager.detach(stateManager.getState(FlyCamAppState.class));

	//Setting styles
	Styles styles = GuiGlobals.getInstance().getStyles();
	styles.getSelector(Slider.THUMB_ID, "glass").set("text", "[]", false);
	styles.getSelector(Panel.ELEMENT_ID, "glass").set("background",
		new QuadBackgroundComponent(new ColorRGBA(0, 0.25f, 0.25f, 0.5f)));
	styles.getSelector(Checkbox.ELEMENT_ID, "glass").set("background",
		new QuadBackgroundComponent(new ColorRGBA(0f, 0.5f, 0.5f, 0.5f)));
	styles.getSelector("spacer", "glass").set("background",
		new QuadBackgroundComponent(new ColorRGBA(1f, 0.0f, 0.0f, 0.0f)));
	styles.getSelector("header", "glass").set("background",
		new QuadBackgroundComponent(new ColorRGBA(0f, 0.75f, 0.75f, 0.5f)));
	styles.getSelector("header", "glass").set("shadowColor", new ColorRGBA(1f, 0f, 0f, 1f));

	//Create base container with our "glass" style
	Container hudPanel = new Container("glass");
	hudPanel.setLocalTranslation(5f, cam.getHeight() - 50f, 0f);
	guiNode.attachChild(hudPanel);

	//Create a panel container with our "glass" style
	Container panel = new Container("glass");
	hudPanel.addChild(panel);

	//Setup top panel
	panel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0.5f, 0.5f, 0.5f), 5, 5, 0.02f, false));
	panel.addChild(new Label("Stats Settings", new ElementId("header"), "glass"));
	panel.addChild(new Panel(2, 2, ColorRGBA.Cyan, "glass")).setUserData(LayerComparator.LAYER, 2);
	
	Checkbox temp = panel.addChild(new Checkbox("Show Stats"));
	temp.setChecked(true);
	showStatsRef = temp.getModel().createReference();
	
	temp = panel.addChild(new Checkbox("Show FPS"));
	temp.setChecked(true);
	showFpsRef = temp.getModel().createReference();

	//Add spacer
	hudPanel.addChild(new Panel(10f, 10f, new ElementId("spacer"), "glass"));

	//Create bottom panel
	panel = new Container("glass");
	panel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0.5f, 0.5f, 0.5f), 5, 5f, 0.02f, false));
	panel.addChild(new Label("Cube Settings", new ElementId("header"), "glass"));
	panel.addChild(new Panel(2, 2, ColorRGBA.Cyan, "glass")).setUserData(LayerComparator.LAYER, 2);
	panel.addChild(new Label("Red:"));
	final Slider redSlider = new Slider("glass");
	redSlider.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0.5f, 0.5f, 0.5f), 5, 5, 0.02f, false));
	redRef = panel.addChild(redSlider).getModel().createReference();
	CursorEventControl.addListenersToSpatial(redSlider, new DefaultCursorListener() {
	    @Override
	    public void cursorMoved(CursorMotionEvent event, Spatial target, Spatial capture) {
		System.out.println("Event: " + event);
		Vector3f cp = event.getCollision().getContactPoint();
		cp = redSlider.worldToLocal(cp, null);
		System.out.println("Range value: " + redSlider.getValueForLocation(cp));
	    }
	});
	
	panel.addChild(new Label("Green:"));
	greenRef = panel.addChild(new Slider("glass")).getModel().createReference();
	panel.addChild(new Label("Blue:"));
	blueRef = panel.addChild(new Slider(new DefaultRangedValueModel(0, 100, 100), "glass")).getModel().createReference();
	panel.addChild(new Label("Alpha:"));
	alphaRef = panel.addChild(new Slider(new DefaultRangedValueModel(0, 100, 100), "glass")).getModel().createReference();
	hudPanel.addChild(panel);
	
	hudPanel.addChild(new Panel(10f, 10f, new ElementId("spacer"), "glass"));
	
	panel = new Container("glass");
	panel.addChild(new Label("Test Entry:", "glass"));
	panel.addChild(new TextField("", "glass"));
	hudPanel.addChild(panel);
	
	hudPanel.setPreferredSize(new Vector3f(200, 0, 0).maxLocal(hudPanel.getPreferredSize()));
	
	Box box = new Box(Vector3f.ZERO, Vector3f.UNIT_XYZ);
	Geometry geom = new Geometry("Box", box);
	Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
	mat.setColor("Color", boxColor);
	mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
	geom.setMaterial(mat);
	rootNode.attachChild(geom);
	
	Container testPanel = new Container();
	testPanel.setPreferredSize(new Vector3f(200, 200, 0));
	testPanel.setBackground(TbtQuadBackgroundComponent.create("Textures/border.png",
		1, 2, 2, 3, 3, 0, false));
	Label test = testPanel.addChild(new Label("Border Test"));
	test.setShadowColor(ColorRGBA.Red);
	test.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));
	testPanel.setLocalTranslation(400, 400, 0);
	
	CursorEventControl.addListenersToSpatial(testPanel, new DragHandler());
	
	guiNode.attachChild(testPanel);
    }
    
    @Override
    public void simpleUpdate(float tpf) {
	if (showStatsRef.update()) {
	    setDisplayStatView(showStatsRef.get());
	}
	
	if (showFpsRef.update()) {
	    setDisplayFps(showFpsRef.get());
	}
	
	if (redRef.update() || greenRef.update() || blueRef.update() || alphaRef.update()) {
	    boxColor.set((float) (redRef.get() / 100.0),
		    (float) (greenRef.get() / 100.0),
		    (float) (blueRef.get() / 100.0),
		    (float) (alphaRef.get() / 100.0));
	}
    }
}
