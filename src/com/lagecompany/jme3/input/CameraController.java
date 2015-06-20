package com.lagecompany.jme3.input;

import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;

public class CameraController {
    
    private static FlyByCamera flyCam;

    public static void setup(FlyByCamera cam, InputManager inputManager) {
	flyCam = cam;
	
	clearKeys(inputManager);
	setupKeys(inputManager);

	flyCam.setMoveSpeed(3f);
    }

    public static void setupKeys(InputManager inputManager) {
	// both mouse and button - rotation of cam
	inputManager.addMapping("FLYCAM_Left", new MouseAxisTrigger(MouseInput.AXIS_X, true));
	inputManager.addMapping("FLYCAM_Right", new MouseAxisTrigger(MouseInput.AXIS_X, false));
	inputManager.addMapping("FLYCAM_Up", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
	inputManager.addMapping("FLYCAM_Down", new MouseAxisTrigger(MouseInput.AXIS_Y, true));

	// keyboard only WASD for movement and SPACE/LCONTROL for rise/lower height
	inputManager.addMapping("FLYCAM_StrafeLeft", new KeyTrigger(KeyInput.KEY_A));
	inputManager.addMapping("FLYCAM_StrafeRight", new KeyTrigger(KeyInput.KEY_D));
	inputManager.addMapping("FLYCAM_Forward", new KeyTrigger(KeyInput.KEY_W));
	inputManager.addMapping("FLYCAM_Backward", new KeyTrigger(KeyInput.KEY_S));
	inputManager.addMapping("FLYCAM_Rise", new KeyTrigger(KeyInput.KEY_SPACE));
	inputManager.addMapping("FLYCAM_Lower", new KeyTrigger(KeyInput.KEY_LCONTROL));

	inputManager.addListener(flyCam, new String[]{
	    "FLYCAM_Left",
	    "FLYCAM_Right",
	    "FLYCAM_Up",
	    "FLYCAM_Down",
	    "FLYCAM_StrafeLeft",
	    "FLYCAM_StrafeRight",
	    "FLYCAM_Forward",
	    "FLYCAM_Backward",
	    "FLYCAM_RotateDrag",
	    "FLYCAM_Rise",
	    "FLYCAM_Lower",
	    "FLYCAM_InvertY"
	});
    }

    public static void clearKeys(InputManager inputManager) {
	inputManager.deleteMapping("FLYCAM_Left");
	inputManager.deleteMapping("FLYCAM_Right");
	inputManager.deleteMapping("FLYCAM_Up");
	inputManager.deleteMapping("FLYCAM_Down");

	inputManager.deleteMapping("FLYCAM_ZoomIn");
	inputManager.deleteMapping("FLYCAM_ZoomOut");
	inputManager.deleteMapping("FLYCAM_RotateDrag");

	inputManager.deleteMapping("FLYCAM_StrafeLeft");
	inputManager.deleteMapping("FLYCAM_StrafeRight");
	inputManager.deleteMapping("FLYCAM_Forward");
	inputManager.deleteMapping("FLYCAM_Backward");
	inputManager.deleteMapping("FLYCAM_Rise");
	inputManager.deleteMapping("FLYCAM_Lower");
    }
}
