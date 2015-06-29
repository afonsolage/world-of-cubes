package com.lagecompany.jme3.manager;

import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.lagecompany.jme3.control.CameraFollowControl;

/**
 * Camera manager class. This class manage all camera states, key bidings and such.
 *
 * @author Afonso Lage
 */
public class CameraMan {

    public static final String FLYCAM_Left = "FLYCAM_Left";
    public static final String FLYCAM_Right = "FLYCAM_Right";
    public static final String FLYCAM_Up = "FLYCAM_Up";
    public static final String FLYCAM_Down = "FLYCAM_Down";
    public static final String FLYCAM_StrafeLeft = "FLYCAM_StrafeLeft";
    public static final String FLYCAM_StrafeRight = "FLYCAM_StrafeRight";
    public static final String FLYCAM_Forward = "FLYCAM_Forward";
    public static final String FLYCAM_Backward = "FLYCAM_Backward";
    public static final String FLYCAM_Rise = "FLYCAM_Rise";
    public static final String FLYCAM_Lower = "FLYCAM_Lower";
    public static final String PLAYER_NODE_Left = "PLAYER_NODE_Left";
    public static final String PLAYER_NODE_Right = "PLAYER_NODE_Right";
    public static final String PLAYER_NODE_Up = "PLAYER_NODE_Up";
    public static final String PLAYER_NODE_Down = "PLAYER_NODE_Down";
    public static final String PLAYER_NODE_StrafeLeft = "PLAYER_NODE_StrafeLeft";
    public static final String PLAYER_NODE_StrafeRight = "PLAYER_NODE_StrafeRight";
    public static final String PLAYER_NODE_Forward = "PLAYER_NODE_Forward";
    public static final String PLAYER_NODE_Backward = "PLAYER_NODE_Backward";
    public static final String PLAYER_NODE_Rise = "PLAYER_NODE_Rise";
    public static final String PLAYER_NODE_Lower = "PLAYER_NODE_Lower";
    private FlyByCamera flyCam;
    private CameraFollowControl followCam;
    private boolean followCamEnabled;
    private InputManager inputManager;

    /**
     * Instanciate a new CameraMan class.
     *
     * @param flyCam The default FlyByCam to be used on debug.
     * @param followCam The main CameraFollowControl.
     * @param inputManager To register input triggers.
     */
    public CameraMan(FlyByCamera flyCam, CameraFollowControl followCam, InputManager inputManager) {
	this.flyCam = flyCam;
	this.inputManager = inputManager;
	this.followCam = followCam;

	this.flyCam.setMoveSpeed(3f);
    }

    /**
     * Troggle the active camera FollowCamera between FlyByCamera
     *
     * @param followCamEnabled Enable or disable FollowCamera.
     */
    public void toggleCam(boolean followCamEnabled) {
	this.followCamEnabled = followCamEnabled;

	flyCam.setEnabled(!followCamEnabled);
	bindFlyCamKeys(!followCamEnabled);

	followCam.setEnabled(followCamEnabled);
	bindFollowCamKeys(followCamEnabled);
    }

    /**
     * Unbind all camera triggers.
     */
    public void unbind() {
	if (followCamEnabled) {
	    bindFollowCamKeys(false);
	} else {
	    bindFlyCamKeys(false);
	}
    }

    /**
     * Rebind all camera triggers.
     */
    public void bind() {
	bindFlyCamKeys(!followCamEnabled);
	bindFollowCamKeys(followCamEnabled);
    }

    /**
     * Bind FlyByCamera triggers and listener.
     *
     * @param enabled Should bind or unbind keys?
     */
    public void bindFlyCamKeys(boolean enabled) {
	if (enabled) {
	    // both mouse and button - rotation of cam
	    inputManager.addMapping(FLYCAM_Left, new MouseAxisTrigger(MouseInput.AXIS_X, true));
	    inputManager.addMapping(FLYCAM_Right, new MouseAxisTrigger(MouseInput.AXIS_X, false));
	    inputManager.addMapping(FLYCAM_Up, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
	    inputManager.addMapping(FLYCAM_Down, new MouseAxisTrigger(MouseInput.AXIS_Y, true));

	    // keyboard only WASD for movement and SPACE/LCONTROL for rise/lower height
	    inputManager.addMapping(FLYCAM_StrafeLeft, new KeyTrigger(KeyInput.KEY_A));
	    inputManager.addMapping(FLYCAM_StrafeRight, new KeyTrigger(KeyInput.KEY_D));
	    inputManager.addMapping(FLYCAM_Forward, new KeyTrigger(KeyInput.KEY_W));
	    inputManager.addMapping(FLYCAM_Backward, new KeyTrigger(KeyInput.KEY_S));
	    inputManager.addMapping(FLYCAM_Rise, new KeyTrigger(KeyInput.KEY_SPACE));
	    inputManager.addMapping(FLYCAM_Lower, new KeyTrigger(KeyInput.KEY_LCONTROL));

	    inputManager.addListener(flyCam, new String[]{
		FLYCAM_Left,
		FLYCAM_Right,
		FLYCAM_Up,
		FLYCAM_Down,
		FLYCAM_StrafeLeft,
		FLYCAM_StrafeRight,
		FLYCAM_Forward,
		FLYCAM_Backward,
		FLYCAM_Rise,
		FLYCAM_Lower
	    });
	} else {
	    inputManager.removeListener(flyCam);
	    inputManager.deleteMapping(FLYCAM_Left);
	    inputManager.deleteMapping(FLYCAM_Right);
	    inputManager.deleteMapping(FLYCAM_Up);
	    inputManager.deleteMapping(FLYCAM_Down);
	    inputManager.deleteMapping(FLYCAM_StrafeLeft);
	    inputManager.deleteMapping(FLYCAM_StrafeRight);
	    inputManager.deleteMapping(FLYCAM_Forward);
	    inputManager.deleteMapping(FLYCAM_Backward);
	    inputManager.deleteMapping(FLYCAM_Rise);
	    inputManager.deleteMapping(FLYCAM_Lower);
	}
    }

    /**
     * Bind FollowCamera triggers and listener.
     *
     * @param enabled Should bind or unbind keys?
     */
    public void bindFollowCamKeys(boolean enabled) {
	if (enabled) {
	    // both mouse and button - rotation of cam
	    inputManager.addMapping(PLAYER_NODE_Left, new MouseAxisTrigger(MouseInput.AXIS_X, true));
	    inputManager.addMapping(PLAYER_NODE_Right, new MouseAxisTrigger(MouseInput.AXIS_X, false));
	    inputManager.addMapping(PLAYER_NODE_Up, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
	    inputManager.addMapping(PLAYER_NODE_Down, new MouseAxisTrigger(MouseInput.AXIS_Y, true));

	    // keyboard only WASD for movement and SPACE/LCONTROL for rise/lower height
	    inputManager.addMapping(PLAYER_NODE_StrafeLeft, new KeyTrigger(KeyInput.KEY_A));
	    inputManager.addMapping(PLAYER_NODE_StrafeRight, new KeyTrigger(KeyInput.KEY_D));
	    inputManager.addMapping(PLAYER_NODE_Forward, new KeyTrigger(KeyInput.KEY_W));
	    inputManager.addMapping(PLAYER_NODE_Backward, new KeyTrigger(KeyInput.KEY_S));
	    inputManager.addMapping(PLAYER_NODE_Rise, new KeyTrigger(KeyInput.KEY_SPACE));
	    inputManager.addMapping(PLAYER_NODE_Lower, new KeyTrigger(KeyInput.KEY_LCONTROL));

	    inputManager.addListener(followCam, new String[]{
		PLAYER_NODE_Left,
		PLAYER_NODE_Right,
		PLAYER_NODE_Up,
		PLAYER_NODE_Down,
		PLAYER_NODE_StrafeLeft,
		PLAYER_NODE_StrafeRight,
		PLAYER_NODE_Forward,
		PLAYER_NODE_Backward,
		PLAYER_NODE_Rise,
		PLAYER_NODE_Lower
	    });
	} else {
	    inputManager.removeListener(followCam);
	    inputManager.deleteMapping(PLAYER_NODE_Left);
	    inputManager.deleteMapping(PLAYER_NODE_Right);
	    inputManager.deleteMapping(PLAYER_NODE_Up);
	    inputManager.deleteMapping(PLAYER_NODE_Down);
	    inputManager.deleteMapping(PLAYER_NODE_StrafeLeft);
	    inputManager.deleteMapping(PLAYER_NODE_StrafeRight);
	    inputManager.deleteMapping(PLAYER_NODE_Forward);
	    inputManager.deleteMapping(PLAYER_NODE_Backward);
	    inputManager.deleteMapping(PLAYER_NODE_Rise);
	    inputManager.deleteMapping(PLAYER_NODE_Lower);
	}
    }
}
