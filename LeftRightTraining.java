import com.lobsterman.JavaGrinders.Tracker.OpenCVRecordProc;
import com.lobsterman.JavaGrinders.Tracker.OpenCVTracker;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSetting;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSettingsGroup;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

public class LeftRightTraining 
{
	static int camID = 0;
	static int pixelThreshold = 6;
	static int objectSize = 250;
	static boolean darkObject = true;
	static int secsToRun = 10800;
	static boolean toFile = true;
	static boolean toVector = true;
	static boolean toScreen = false;
	static float fps = 30;

	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	static boolean checkLeft = true;
	static boolean checkRight = false;
	static boolean isValidPath = false;
	static boolean isValidBeginning = false;
	static int currentZone = -1;
	static int oldZone = -1;
	static ArrayList<Integer> flyPath = new ArrayList<Integer>();
	static int channel = -1;
	static boolean isNewChannel = true;
	static ServoControl servo;

	public static void main(String[] args) throws Exception
	{		
		System.out.println("Left Right");
		//To open an instance of the OpenCV library during runtime from a specific path ...
		String pathToLib = "/usr/local/share/OpenCV/java/libopencv_java320.so";
		System.load(pathToLib);

		PrintWriter out = new PrintWriter("Operant Event Log Trial 2");
		//set up video tracking
		TrackingJobSettingsGroup trackJGroup = new TrackingJobSettingsGroup();
		trackJGroup.addTrackingJobSetting(new TrackingJobSetting(new Rectangle(0,0,640,480),pixelThreshold,objectSize,darkObject));

		OpenCVRecordProc aProc = new OpenCVRecordProc();
		aProc.plotExtended = false;			
		//aProc.omitReportOrientedDimensions = true;
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
					//records zone fly was last in and zone fly is now in
					oldZone = currentZone;

					//determine zone
					int actualX = cvTrackerDemo.getXs(0).lastElement();
					int actualY = cvTrackerDemo.getYs(0).lastElement();

					currentZone = ZoneCalculation(actualX, actualY);
					

					//active tracking should only begin once the fly has reached an outside arm
					if (flyPath.size() == 0 && (currentZone <= 12 && currentZone > 6))
					{
						flyPath.add(currentZone);
						isValidBeginning = true;
						isValidPath = true;
					}

					//fly has left an outside arm and changed zones
					else if (isValidBeginning && oldZone != -1 && (oldZone != currentZone))
					{
						//the zone after the outside zone is always the adjacent inner zone
						if (flyPath.size() == 1)
							flyPath.add(currentZone);

						//checks to make sure the fly has not made a right turn, going back to
						//the zone from which it came is fine, but it won't be recorded as going
						//to a new zone
						else if (checkLeft)
						{
							if (flyPath.size() == 2)
							{
								if (currentZone == oldZone + 1 || (currentZone == 1 && oldZone == 6))
									flyPath.add(currentZone);
								else if (currentZone == oldZone - 1 || (currentZone == 6 && oldZone == 1))
								{
									isValidPath = false;
									isValidBeginning = false;
									flyPath.clear();
								}
							}
							else if (flyPath.size() == 3)
							{
								if(currentZone == oldZone + 6)
									flyPath.add(currentZone);
								else if (currentZone == oldZone + 1 || (currentZone == 1 && oldZone == 6))
								{
									isValidPath = false;
									isValidBeginning = false;
									flyPath.clear();
								}
							}
						}

						//essentially same as checkLeft, only the fly must turn right now
						else if (checkRight)
						{
							if (flyPath.size() == 2)
							{
								if (currentZone == oldZone - 1 || (currentZone == 6 && oldZone == 1))
									flyPath.add(currentZone);
								else if (currentZone == oldZone + 1 || (currentZone == 1 && oldZone == 6))
								{
									isValidPath = false;
									isValidBeginning = false;
									flyPath.clear();
								}
							}
							else if (flyPath.size() == 3)
							{
								if (currentZone == oldZone + 6)
									flyPath.add(currentZone);
								else if (currentZone == oldZone - 1 || (currentZone == 6 && oldZone == 1))
								{
									isValidPath = false;
									isValidBeginning = false;
									flyPath.clear();
								}
							}
						}
					}
					//System.out.println("currentZone: " + currentZone + " oldZone: " + oldZone + " flyPath: " + flyPath);
					//if a fly has made 4 valid moves, then it must be in the correct outer arm, so
					//it will be rewarded and the checks start over
					//160 is the angle at which the food circle will have the food available
					//motors 0 and 4 have different angles -> 158 and 162 respectively
					if (flyPath.size() == 4)
					{
						switch(currentZone) 
						{
						case 7:
							servo.move(0, 158);
							Delay(0);
							break;
						case 8:
							servo.move(1, 160);
							Delay(1);
							break;
						case 9:
							servo.move(2, 162);
							Delay(2);
							break;
						case 10:
							servo.move(4, 160);
							Delay(4);
							break;
						case 11:
							servo.move(5, 160);
							Delay(5);
							break;
						case 12:
							servo.move(6, 160);
							Delay(6);
							break;
						default: 
							channel = -1;
							break;

						}
						isValidPath = false;
						isValidBeginning = false;
						flyPath.clear();
						out.write("x: " + actualX + " y: " + actualY + " motor moved in zone " + currentZone + "\n");
						out.flush();
					}
				}
			}
		}
	}

	private static int ZoneCalculation(int actualX, int actualY) 
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

	private static void Delay(final int channel) {
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
