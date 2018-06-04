import com.lobsterman.JavaGrinders.Tracker.OpenCVRecordProc;
import com.lobsterman.JavaGrinders.Tracker.OpenCVTracker;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSetting;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSettingsGroup;
import java.util.concurrent.*;

import java.awt.Rectangle;
import java.io.PrintWriter;

public class RewardTraining 
{	
	//variables for tracking setup
	static int camID = 0;
	static int pixelThreshold = 6;
	static int objectSize = 250;
	static boolean darkObject = true;
	static int secsToRun = 1500;
	static boolean toFile = true;
	static boolean toVector = true;
	static boolean toScreen = false;
	static float fps = 30;
	
	static int currentZone = -1; 	//current zone of the fly
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1); //controls how long the motors wait before they rotate back to neutral position from reward position
	static int channel = -1;	//represents what motor is being activated
	static boolean isNewChannel = true;		//checks to see if the fly has traveled to a new chute to determine if reward should be given
	static ServoControl servo;		//controls motors

	public static void main(String[] args) throws Exception 
	{
		//output to see what is running
		System.out.println("Reward Training");
		
		//output for motor turns
		PrintWriter out = new PrintWriter("Reward Event Log Trial 2");
		
		//To open an instance of the OpenCV library during runtime from a specific path
		String pathToLib = "/usr/local/share/OpenCV/java/libopencv_java320.so";
		System.load(pathToLib);
		
		//set up video tracking
		TrackingJobSettingsGroup trackJGroup = new TrackingJobSettingsGroup();
		trackJGroup.addTrackingJobSetting(new TrackingJobSetting(new Rectangle(0,0,640,480),pixelThreshold,objectSize,darkObject));

		OpenCVRecordProc aProc = new OpenCVRecordProc();
		aProc.plotExtended = false;	
		aProc.isGrayScale = true;			
		aProc.drawObject = true;			

		OpenCVTracker cvTrackerDemo = new OpenCVTracker(camID);
		cvTrackerDemo.initializeTracker(fps,secsToRun,trackJGroup,toFile,toVector,toScreen,null,aProc);
		cvTrackerDemo.setVanishingTrail(20);

		cvTrackerDemo.setVisible(true);

		//sets up servos
		servo = new ServoControl();
		servo.RewardReset();

		//this is bad code don't do this
		while(true)
		{
			Thread.sleep(1000);
			while(cvTrackerDemo.analysisRunning)
			{
				if(!(cvTrackerDemo.getXs(0).size() == 0))
				{
					//determine zone
					int actualX = cvTrackerDemo.getXs(0).lastElement();
					int actualY = cvTrackerDemo.getYs(0).lastElement();
					currentZone = ZoneCalc(actualX, actualY);
					
					if (currentZone > 6)
					{

						switch(currentZone) 
						{
						case 7:
							if (channel != 0) 
							{
								channel = 0;
								servo.move(channel, 158);
								isNewChannel = true;
								Delay(0);
							}
							else
								isNewChannel = false;
							break;
						case 8:
							if (channel != 1)
							{
								channel = 1;
								servo.move(channel, 160);
								isNewChannel = true;
								Delay(1);
							}
							else
								isNewChannel = false;
							break;
						case 9:
							if (channel != 2)
							{
								channel = 2;
								servo.move(channel, 160);
								isNewChannel = true;
								Delay(2);
							}
							else
								isNewChannel = false;
							break;
						case 10:
							if (channel != 4)
							{
								channel = 4;
								servo.move(channel, 162);
								isNewChannel = true;
								Delay(4);
							}
							else
								isNewChannel = false;
							break;
						case 11:
							if (channel != 5)
							{
								channel = 5;
								servo.move(channel, 160);
								isNewChannel = true;
								Delay(5);
							}
							else
								isNewChannel = false;
							break;
						case 12:
							if (channel != 6)
							{
								channel = 6;
								servo.move(channel, 160);
								isNewChannel = true;
								Delay(6);
							}
							else
								isNewChannel = false;
							break;
						default: 
							channel = -1;
							isNewChannel = false;
							break;
						}


						//eventlog
						if (isNewChannel)
						{
							out.write("x: " + actualX + " y: " + actualY + " motor moved in zone " + currentZone + "\n");
							out.flush();
						}

						currentZone = -1;
					}
				}
			}
		}
	}

	private static int ZoneCalc(int actualX, int actualY) 
	{
		int zone = -1;
		double calcY_L2 = (-83/141.5) * (actualX - 373.5) + 167.5;
		double calcY_L3 = (83/141.5) * (actualX - 373.5) + 250.5;

		if (actualX < 302.5)
			if (actualY < calcY_L3)
				zone = 2;
			else if (actualY < calcY_L2)
				zone = 1;
			else 
				zone = 6; //actualY > calcY_L2
		else
			if (actualY < calcY_L2)
				zone = 3;
			else if (actualY < calcY_L3)
				zone = 4;
			else
				zone = 5; //actualY > calcY_L3

		//determines whether or not fly is on inside or outside
		double distance = Math.sqrt(Math.pow(302.5-actualX, 2) + Math.pow(209-actualY, 2));
		double centerOutDistance = Math.sqrt(25169);

		if (distance >= centerOutDistance)
			zone+=6;

		if (actualX == -1 && actualY == -1)
			zone = -1;
		return zone;
	}

	public static void Delay(final int channel)
	{
		ScheduledFuture<?> countdown = scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					servo.move(channel, 80);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		},5,TimeUnit.SECONDS);
	}
}
