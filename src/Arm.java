import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.bluetooth.RemoteDevice;

import lejos.nxt.Button;
import lejos.nxt.Motor;
import lejos.nxt.NXT;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.RegulatedMotorListener;


public class Arm {
	boolean isCentred = true;
	long RTT = 0;
	long timeDiff = 0;
	DataOutputStream output;
	DataInputStream input;
	final static String NAME = Bluetooth.getFriendlyName();
	
	
	final static String SLAVEF_NAME = "SLAVEF";
	final static String SLAVER_NAME = "SLAVER";
	final static String SLAVEB_NAME = "SLAVEB";
	final static String SLAVEL_NAME = "SLAVEL";
	
	final static int[] SLAVE_F_ARGS = {123, 4, -123, -7};
	final static int[] SLAVE_R_ARGS = {122, 3, -122, -3};
	final static int[] SLAVE_B_ARGS = {123, 4, -123, -2};
	final static int[] SLAVE_L_ARGS = {123, 3, -121, 0};
	
	public static int[] getArgs(String name){
		if(name.equals(SLAVEB_NAME)){
			return SLAVE_B_ARGS;
		}else if(name.equals(SLAVEF_NAME)){
			return SLAVE_F_ARGS;
		}else if(name.equals(SLAVER_NAME)){
			return SLAVE_R_ARGS;
		}else if(name.equals(SLAVEL_NAME)){
			return SLAVE_L_ARGS;
		}
		
		return null;
	}
	
	final static int[] ARGS = getArgs(NAME);
	
	final static int PERFORM_DELAYED = -6;
	final static int FINISH_SETUP = -5;
	
	final static int END_SEQUENCE = -4;
	final static int START_SEQUENCE = -3;
	
	final static int TIME_SYNC = -2;
	final static int PING = -1;
	
	
	final static int CLAMP = 0;
	final static int UNCLAMP = 1;
	final static int CLOCKWISE = 2;
	final static int ANTICLOCKWISE = 3;
	final static int CLOCKWISE_ROT = 4;
	final static int ANTICLOCKWISE_ROT = 5;
	final static int CLOCK180 = 6;
	final static int ANTI180 = 7;
	
	final static int DONE = -999;
	
	public void handleMessage(int message){
		System.out.println("Got message:" + message);
		switch(message){
			case CLAMP: clamp(); break;
			case UNCLAMP: unclamp(); break;
			case CLOCKWISE: clockwiseface(); break;
			case ANTICLOCKWISE: anticlockwiseface(); break;
			case CLOCKWISE_ROT: clockwiseRotation(); break;
			case ANTICLOCKWISE_ROT: anticlockwiseRotation(); break;
			case CLOCK180: clockwise180(); break;
			case PERFORM_DELAYED: performDelayed(); break;
			default: break;
		}
	}
	
	public void performDelayed(){
		try {
			String info = input.readUTF();
			int split = info.indexOf(",");
			
			long timeToSleep = Long.parseLong(info.substring(split+1, info.length())) - currentTime();
			if(timeToSleep > 0)
				Thread.sleep(timeToSleep);
			
			int command = Integer.parseInt(info.substring(0, split));
			handleMessage(command);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public long currentTime(){
		return System.currentTimeMillis() + timeDiff + RTT/2;
		
	}

	
	public void clamp(){
		System.out.println("DOING CLAMP");
		Motor.A.rotateTo(-100);
		System.out.println("CLAMP");
		Motor.A.waitComplete();
		System.out.println("DONE");
	}
	
	public void unclamp(){
		Motor.A.rotateTo(0);
		Motor.A.waitComplete();
	}
	
	public void clockwiseRotation(){
		if(isCentred){
			Motor.B.rotateTo(ARGS[2]);
		}else{
			Motor.B.rotateTo(ARGS[1]);
		}
		Motor.B.waitComplete();
		isCentred = !isCentred;
	}
	
	public void anticlockwiseRotation(){
		if(isCentred){
			Motor.B.rotateTo(ARGS[0]);
		}else{
			Motor.B.rotateTo(ARGS[3]);
		}
		Motor.B.waitComplete();
		isCentred = !isCentred;
	}
	
	public void anticlockwiseface(){
		System.out.println("ANTI");
		Motor.B.rotateTo(ARGS[0]);
		Motor.B.waitComplete();
		unclamp();
		Motor.B.rotateTo(ARGS[1]);
		Motor.B.waitComplete();
		clamp();
	}
	
	public void clockwiseface(){
		System.out.println("CLOCKWISE");
		Motor.B.rotateTo(ARGS[2]);
		Motor.B.waitComplete();
		unclamp();
		Motor.B.rotateTo(ARGS[3]);
		Motor.B.waitComplete();
		clamp();
	}
	
	public void clockwise180(){
		System.out.println("CLOCKWISE180");
		Motor.B.rotateTo(-252);
		Motor.B.waitComplete();
		unclamp();
		Motor.B.rotateTo(0);
		Motor.B.waitComplete();
		clamp();
	}
	
	public Arm(){
		String name = Bluetooth.getFriendlyName();
		
		System.out.println(name);
		
		Motor.A.setSpeed(500);
		Motor.B.setSpeed(900);

		Motor.A.resetTachoCount();
		Motor.B.resetTachoCount();
		System.out.println(Bluetooth.getLocalAddress());
	}
	
	public long measureRTT(NXTConnection conn){
		System.out.println("Measuring RTT");
		DataInputStream in = conn.openDataInputStream();
		DataOutputStream out = conn.openDataOutputStream();
		long before = 0;
		long after = 0;
		try {
			before = System.currentTimeMillis();
			out.writeInt(PING);
			out.flush();
			System.out.println("Sent PING");
			in.readInt();
			after = System.currentTimeMillis();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return after - before;		
	}
	
	public void syncProtocol(){
		try {
			output.writeInt(TIME_SYNC);
			output.flush();
			long masterTime = input.readLong();
			long myTime = System.currentTimeMillis();
			
			timeDiff = masterTime - myTime;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void connect(){
		
		NXTConnection conn = Bluetooth.waitForConnection();
		conn.setIOMode(NXTConnection.RAW);
		System.out.println("Connected");
		
		input = conn.openDataInputStream();
		output = conn.openDataOutputStream();
		
		//Sync clock with master
		RTT = measureRTT(conn);
		syncProtocol();
		
		try {
			output.writeInt(FINISH_SETUP);
			output.flush();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
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
	
	public static void main(String[] args){
		UnsyncedArm arm = new UnsyncedArm();
		arm.connect();	
	}
}
