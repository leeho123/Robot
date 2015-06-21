import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;


/**
 * A single NXT is either in charge of clamping 2 arms or rotating 2 arms
 * @author Le
 *
 */
public class UnsyncedArm {	
	final static String NAME = Bluetooth.getFriendlyName();
	
	
	final static String SLAVEF_NAME = "SLAVEF";
	final static String SLAVER_NAME = "SLAVER";
	final static String SLAVEB_NAME = "SLAVEB";
	final static String SLAVEL_NAME = "SLAVEL";
	//FB
	//RL
	final static int[] SLAVE_RL_ARGS = {126, 3, -125, -4, 
										130, 2, -126, 0};
	final static int[] SLAVE_FB_ARGS = {122, 0, -122, -3, 
										126, 2, -125, -4};
	
	//RLFB
	final static int[] CLAMP_POWS = {800, 600, 800, 600};
	
	final static int[] CLAMP_DEGS = {-110, -99, -110, -98};
	
	final static int[] ONEEIGHTYARGS = {};
	
	final NXTRegulatedMotor[] motors = {Motor.A, Motor.B};
	
	public static final int CLAMP = -1;
	public static final int ROT = -2;
	
	public static final int CLAMPONE = 0;
	public static final int CLAMPTWO = 1;
	public static final int UNCLAMPONE = 2;
	public static final int UNCLAMPTWO = 3;
	public static final int CLAMPBOTH = 4;
	public static final int UNCLAMPBOTH = 5;
	
	public static final int CLOCKONE = 6;
	public static final int CLOCKTWO = 7;
	public static final int ANTIONE = 8;
	public static final int ANTITWO = 9;
	public static final int CLOCKBOTH = 10;
	public static final int ANTIBOTH = 11;
	
	public static final int CLOCK180ONE = 12;
	public static final int CLOCK180TWO = 13;
	public static final int ANTI180ONE = 14;
	public static final int ANTI180TWO = 15;
	
	public static final int HALFUNCLAMPONE = 16;
	public static final int HALFUNCLAMPTWO = 17;
	public static final int HALFUNCLAMPBOTH = 18;
	
	public static final int DONE = -999;
	public static final int END_SEQUENCE = -777;
	
	public static int[] getArgs(String name){
		if(name.equals(SLAVEB_NAME)){
			return SLAVE_FB_ARGS;
		}else if(name.equals(SLAVEL_NAME)){
			return SLAVE_RL_ARGS;
		}
		
		return null;
	}
	
	int[] ARGS = getArgs(NAME);
	
	int[] CLAMP_POW_ARGS = getClampPow(NAME);
	int[] CLAMP_DEG_ARGS = getClampDeg(NAME);
	
	public static int[] getClampDeg(String name){
		if(name.equals(SLAVEF_NAME)){
			return new int[]{CLAMP_DEGS[2], CLAMP_DEGS[3]};
		}else if(name.equals(SLAVER_NAME)){
			return new int[]{CLAMP_DEGS[0], CLAMP_DEGS[1]};
		}
		return null;
	}
	
	public static int[] getClampPow(String name){
		if(name.equals(SLAVEF_NAME)){
			return new int[]{CLAMP_POWS[2], CLAMP_POWS[3]};
		}else if(name.equals(SLAVER_NAME)){
			return new int[]{CLAMP_POWS[0], CLAMP_POWS[1]};
		}
		
		return null;
	}
	
	DataOutputStream output;
	DataInputStream input;
	
	int job = 0;
	boolean[] isCentred = {true, true};
	
	
	public UnsyncedArm(){
		String name = Bluetooth.getFriendlyName();
		
		System.out.println(name);

		System.out.println(Bluetooth.getLocalAddress());
	}
	
	
	public void handleMessage(int message){
		System.out.println("Got message:" + message);
		if(job == CLAMP){
			switch(message){
				case CLAMPONE: clamp(0); break;
				case CLAMPTWO: clamp(1); break;
				case UNCLAMPONE: halfUnclamp(motors[0]); break;
				case UNCLAMPTWO: halfUnclamp(motors[1]); break;
				case CLAMPBOTH: clampBoth(); break;
				case UNCLAMPBOTH: halfUnclampBoth(); break;
				case END_SEQUENCE: reset(); break;
				case HALFUNCLAMPBOTH: halfUnclampBoth(); break;
				case HALFUNCLAMPONE: halfUnclamp(motors[0]); break;
				case HALFUNCLAMPTWO: halfUnclamp(motors[1]); break;
				
				default: break;
			}
		}else if (job == ROT){
			switch(message){
				case CLOCKONE: rotate90(true, 0); break;
				case CLOCKTWO: rotate90(true, 1); break;
	
				
				case ANTIONE: rotate90(false, 0); break;
				case ANTITWO: rotate90(false, 1); break;
				case CLOCKBOTH: rotateBoth(true); break;
				case ANTIBOTH: rotateBoth(false); break;
				case CLOCK180ONE: rotate180(true, 0); break;
				case CLOCK180TWO: rotate180(true, 1); break;
				case ANTI180ONE: rotate180(false,0); break;
				case ANTI180TWO: rotate180(false,1); break;
				case END_SEQUENCE: reset(); break;
				default: break;
			}
		}
	}
	
	public void halfUnclamp(NXTRegulatedMotor motor){
		motor.rotateTo(-50);
		motor.waitComplete();
	}
	
	public void halfUnclampBoth(){
		Motor.A.rotateTo(-50, true);
		Motor.B.rotateTo(-50, true);
		Motor.A.waitComplete();
		Motor.B.waitComplete();
	
	}
	public void reset(){
		motors[0].setSpeed(300);
		motors[1].setSpeed(300);
		
		if(job == ROT){
			motors[0].rotateTo(100, true);
			motors[1].rotateTo(100, true);
			motors[0].waitComplete();
			motors[1].waitComplete();
		}
		
		motors[0].rotateTo(0,true);
		motors[1].rotateTo(0,true);
		
		motors[0].waitComplete();
		motors[1].waitComplete();
		
		setSpeed();
		
	}
	
	public void rotate90(boolean clockwise, int motor){
		if(clockwise){
			if(isCentred[motor]){
				int argIndex = motor * 4 + 2;
				motors[motor].rotateTo(ARGS[argIndex]);
			}else{
				int argIndex = motor * 4 + 1;
				motors[motor].rotateTo(ARGS[argIndex]);
			}
		}else{
			if(isCentred[motor]){
				int argIndex = motor * 4;
				motors[motor].rotateTo(ARGS[argIndex]);
			}else{
				int argIndex = motor * 4 + 3;
				motors[motor].rotateTo(ARGS[argIndex]);
			}
		}
		motors[motor].waitComplete();
		isCentred[motor] = !isCentred[motor];
	}
	
	public void rotate180(boolean clockwise, int motor){
		if(clockwise){
			motors[motor].rotateTo(-252);
		}else{
			motors[motor].rotateTo(0);
		}
		
		motors[motor].waitComplete();
	}
	
	public void rotateBoth(boolean clockwise){
		int arg1 = 0;
		int arg2 = 0;
		
		if(clockwise){
			arg1 = isCentred[0] ? 2 : 1;
			arg2 = isCentred[1] ? 4 : 7;
		}else{
			arg1 = isCentred[0] ? 0 : 3;
			arg2 = isCentred[1] ? 6 : 5;
		}
		
		motors[0].rotateTo(ARGS[arg1],true);
		motors[1].rotateTo(ARGS[arg2],true);
		motors[0].waitComplete();
		motors[1].waitComplete();
		isCentred[0] = !isCentred[0];
		isCentred[1] = !isCentred[1];
	}
	
	public void unclampBoth(){
		Motor.A.rotateTo(0, true);
		Motor.B.rotateTo(0, true);
		Motor.A.waitComplete();
		Motor.B.waitComplete();
		
	}
	
	public void clampBoth(){
		Motor.A.rotateTo(CLAMP_DEG_ARGS[0], true);
		Motor.B.rotateTo(CLAMP_DEG_ARGS[1], true);
		Motor.A.waitComplete();
		Motor.B.waitComplete();
		Motor.A.lock(100);
		Motor.B.lock(100);
	}
	
	public void unclamp(NXTRegulatedMotor motor){
		motor.rotateTo(0);
		motor.waitComplete();
		
	}
	
	public void clamp(int motor){
		motors[motor].rotateTo(CLAMP_DEG_ARGS[motor]);
		motors[motor].waitComplete();
		motors[motor].lock(100);
	}
	
	
	public void setSpeed(){
		if(job == CLAMP){
			Motor.A.setSpeed(CLAMP_POW_ARGS[0]);
			Motor.B.setSpeed(CLAMP_POW_ARGS[1]);

			Motor.A.resetTachoCount();
			Motor.B.resetTachoCount();
			
			System.out.println("Job is clamp");
		}else if(job == ROT){
			Motor.A.setSpeed(2000);
			Motor.B.setSpeed(2000);

			Motor.A.resetTachoCount();
			Motor.B.resetTachoCount();
			System.out.println("Job is rotate");
		}
	}
	
	public void connect(){
		NXTConnection conn = Bluetooth.waitForConnection();
		conn.setIOMode(NXTConnection.RAW);
		System.out.println("Connected");
		
		input = conn.openDataInputStream();
		output = conn.openDataOutputStream();

		try {
			job = input.readInt();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		setSpeed();
		
		while(true){
			input = conn.openDataInputStream();
			output = conn.openDataOutputStream();
			try {
				handleMessage(input.readInt());
				
				output.writeInt(DONE);
				
				output.flush();
				System.out.println("Sent done");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
}
