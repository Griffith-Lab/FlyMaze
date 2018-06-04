import com.lobsterman.JavaGrinders.Tracker.OpenCVRecordProc;
import com.lobsterman.JavaGrinders.Tracker.OpenCVTracker;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSetting;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSettingsGroup;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

import com.mathworks.engine.*;

public class SpeedTraining 
{
	static int camID = 0;
	static int pixelThreshold = 12;
	static int objectSize = 200;
	static boolean darkObject = true;
	static int secsToRun = -1;
	static boolean toFile = true;
	static boolean toVector = true;
	static boolean toScreen = false;
	static float fps = 30;

	static double center_x = -1;
	static double center_y = -1;
	static double dist1 = 0;
	static double dist2 = 0;
	static long time1 = 0;
	static long time2 = 0;
	static boolean outRad1 = false;
	static boolean outRad2 = false;
	static double currentSpeed = -1;
	static ArrayList <Double> speeds = new ArrayList<Double>();
	static boolean turnMotor = false;
	static boolean checkSpeed = false;
	static ServoControl servo;
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	static boolean isNewApproach = true;
	static int counter = 0;
	static boolean begin = true;
	static long initialTime = 0;
	
	public static void main(String[] args) throws Exception
	{
		System.out.println("Speed");

		//To open an instance of the OpenCV library during runtime from a specific path ...
		String pathToLib = "/usr/local/share/OpenCV/java/libopencv_java320.so";
		System.load(pathToLib);
		PrintWriter out = new PrintWriter("Speed Event Log Trial 1");

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

		servo = new ServoControl();
		servo.RewardReset();

		//determine actual center from acclimation data
		//		MatlabEngine eng = MatlabEngine.startMatlab();
		//		double[] coords = eng.feval("grinders_find_centerxy", "/home/griffith_lab/Desktop/Fly Coordinates/7-27/Acclimation Trial 1");
		//		System.out.println("\n\n\n\n\n\n\n x: " + coords[0] + " y: " + coords[1] + "\n\n\n\n\n\n");
		center_x = 300;
		center_y = 211;

		
		while (true)
		{
			//System.out.println("!!!!!!while true!!!!!");
			Thread.sleep(1000);
			while(cvTrackerDemo.analysisRunning)
			{
				if(!(cvTrackerDemo.getXs(0).size() == 0))
				{
					if (begin)
					{
						initialTime = System.nanoTime();
						begin = false;
					}

					int actualX = cvTrackerDemo.getXs(0).lastElement();
					int actualY = cvTrackerDemo.getYs(0).lastElement();
					//System.out.println(aProc.runTime);

					double deltX = (95.7/640)*(actualX - center_x);
					double deltY = (71.9/480)*(actualY - center_y);

					double dist = Math.pow(deltY, 2) + Math.pow(deltX, 2);

					if (dist >= 400)
					{
						if (!outRad1)
						{
							outRad1 = true;
							time1 = System.nanoTime();
						}
					}
					else
						outRad1 = false;

					if (outRad1)
					{
						if (dist >= 676)
						{
							outRad2 = true;
							time2 = System.nanoTime();
						}
						else
							outRad2 = false;
					}

					if (!outRad1)
						isNewApproach = true;

					if (outRad1 && outRad2 && isNewApproach && actualX != -1 && actualY != -1)
					{
						isNewApproach = false;

						//calc speed in mm/s
						currentSpeed = (6./(time2-time1)) * 1000000000;

						//add to arraylist
						if (speeds.size() == 11)
						{
							speeds.remove(0);
							speeds.add(currentSpeed);
						}

						else
							speeds.add(currentSpeed);

						ArrayList <Double> temp = new ArrayList<Double>();
						for (int i = 0; i < speeds.size(); i++)
							temp.add(speeds.get(i));

						Collections.sort(temp);

						//check speed
						if (speeds.size() == 11 && currentSpeed < temp.get(5))
							turnMotor = true;
						else
							turnMotor = false;

						//turn motor
						if (speeds.size() < 11 || turnMotor)
						{
							//zonecalc, motor turn, delay
							if (actualX < 190 && actualY > 115 && actualY < 311)
							{
								servo.move(0, 158);
								Delay(0);
							}
							else if (actualX < 248 && actualY < 115)
							{
								servo.move(1, 160);
								Delay(1);
							}
							else if (actualX > 345 && actualY < 115)
							{
								servo.move(2, 160);
								Delay(2);
							}
							else if (actualX > 406 && actualY < 311 && actualY > 115)
							{
								servo.move(4, 162);
								Delay(4);
							}
							else if (actualX > 345 && actualY > 311)
							{
								servo.move(5, 160);
								Delay(5);
							}
							else if (actualY > 311 && actualX< 248)
							{
								servo.move(6, 160);
								Delay(6);
							}
						}

						counter++;
						if (counter == 300)
							System.exit(0);
						
						long timeElapsed = (System.nanoTime()-initialTime) * 1000000000;
						
						out.write("Trial number: " + counter + " Time Elapsed: " + timeElapsed + " current speed: " + currentSpeed);
						out.flush();
						
						if (turnMotor || speeds.size() < 11)
							out.write(" Rewarded\n");
						else
							out.write(" Not rewarded\n");
						out.flush();
						
						System.out.println("Trial number: " + counter);
						
					}
				}
			}
		}
	}

	private static void Delay(final int i) 
	{
		ScheduledFuture<?> countdown = scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					servo.move(i, 80);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		},5,TimeUnit.SECONDS);

	}
}
