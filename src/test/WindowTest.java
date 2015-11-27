package test;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.lagecompany.manager.Global;
import com.lagecompany.manager.WindowManager;
import com.lagecompany.ui.ToolbarWindow;
import com.simsilica.lemur.GuiGlobals;

/**
 *
 * @author Afonso Lage
 */
public class WindowTest extends SimpleApplication {

    public static void main(String[] args) {
	WindowTest test = new WindowTest();
	test.setShowSettings(false);
	test.start();
    }

    @Override
    public void simpleInitApp() {
	GuiGlobals.initialize(this);
	Global.winMan.setCamera(cam);
	Global.winMan.setGuiNode(guiNode);

	stateManager.detach(stateManager.getState(FlyCamAppState.class));

	ToolbarWindow window = (ToolbarWindow) Global.winMan.get(WindowManager.TOOLBAR);
	window.build();
	window.show();

	Box b = new Box(Vector3f.ZERO, Vector3f.UNIT_XYZ);
	Geometry geom = new Geometry("box", b);
	Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
	mat.setColor("Color", ColorRGBA.Blue);
	geom.setMaterial(mat);

	window.set("1", geom.clone());

	rootNode.attachChild(geom);
    }

    @Override
    public void start(JmeContext.Type contextType) {
	AppSettings sett = new AppSettings(true);
	sett.setResolution(800, 600);
	sett.setVSync(false);
	sett.setFrameRate(-1);
	setSettings(sett);
	super.start(contextType);
    }
}
