package test;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.input.InputState;
import com.simsilica.lemur.input.StateFunctionListener;

/**
 *
 * @author Afonso Lage
 */
public class InputMapperTest extends SimpleApplication implements StateFunctionListener {
    
    private static final String GROUP = "A Group";
    private static final FunctionId F_A = new FunctionId(GROUP, "F A");
    private static final FunctionId F_B = new FunctionId(GROUP, "F B");
    
    public static void main(String[] args) {
	InputMapperTest test = new InputMapperTest();
	test.setShowSettings(false);
	test.start();
    }
    
    @Override
    public void start(JmeContext.Type contextType) {
	AppSettings sett = new AppSettings(true);
	sett.setResolution(800, 600);
	sett.setVSync(true);
	setSettings(sett);
	super.start(contextType);
    }
    
    @Override
    public void simpleInitApp() {
	GuiGlobals.initialize(this);
	flyCam.setEnabled(false);
	inputManager.setCursorVisible(true);
	
	InputMapper mapper = GuiGlobals.getInstance().getInputMapper();
	mapper.map(F_A, KeyInput.KEY_A);
	mapper.map(F_B, KeyInput.KEY_B);
	mapper.addStateListener(this, F_A, F_B);
	mapper.activateGroup(GROUP);
    }
    
    @Override
    public void valueChanged(FunctionId func, InputState value, double tpf) {
	System.out.println("Kilroy was here!");
    }
}
