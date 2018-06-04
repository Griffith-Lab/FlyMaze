import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFileChooser;

import com.lobsterman.JavaGrinders.Tracker.OpenCVRecordProc;
import com.lobsterman.JavaGrinders.Tracker.OpenCVTracker;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSetting;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSettingsGroup;

public class YMaze 
{
	// Tracker Settings
	static int camID = 0;
	static int pixelThreshold = 40;
	static int objectSize = 300;
	static boolean darkObject = true;
	static int secsToRun = -1;
	static boolean toFile = true;
	static boolean toVector = true;
	static boolean toScreen = false;
	static float fps = 30;

	// Servo Settings
	static ServoControl servo;
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	final static int servoNeutralPosition = 80;
	final static int servoRewardPosition = 160;
	final static int servoReturnDelay = 5;	// seconds

	// Training Settings
	final static int NUMBER_OF_TRIALS = 200;
	static int EXTINCTION_TRIALS = 0;
	static int REVERSAL_TRIALS = 200;
	
	final static double center_x = 298;	// pixels
	final static double center_y = 255;	// pixels
	final static double RADIUS_ONE = 6;	// mm
	final static int yAxisCutoff = 60;	// pixels
	final static double pxToMm = 11.2; 
	final static double MAZE_Y_DIM = 34; // mm
	final static double MAZE_X_DIM = 36; // mm
	final static long pauseBlockTime = 20; //seconds
	
	final static boolean extinction = false; //maze turns off after 250 trials
	final static boolean pause = false; //maze turns off in the middle for x min
	final static boolean reversal = true; //maze rewards right turns
	
	public static void main(String[] args) throws Exception {

		System.out.println("YMAZE");

		// Declare local variables
		int actualY = -1;
		int actualX = -1;
		double deltX = -1;
		double deltY = -1;
		double dist = -1;

		long frameTime = -1;

		long rewardEntranceTime = 0;
		boolean conditionFailed = false; //condition is turning left
		boolean newZone = false;
		boolean inCenter = false;
		boolean motorHasTurned = false;
		int counter = 0;

		boolean initialMotorTurn = true;
		
		boolean inExtinctionBlock = false;
		boolean inReversalBlock = false;
		boolean inPauseBlock = false;
		
		long pauseBeginTime = -1;

		int originalZone = -1;
		int currentZone = -1;

		String trialOutcome = "";
		
//		if (extinction)
//			EXTINCTION_TRIALS = NUMBER_OF_TRIALS;
//		if (reversal)
//			REVERSAL_TRIALS = NUMBER_OF_TRIALS;

		File eventFile = new File("defaultEventFile");
		DecimalFormat screenFormat = new DecimalFormat("#.0");
		DecimalFormat fileFormat = new DecimalFormat("#.0000");

		// Open an instance of the OpenCV library during runtime from a specific path
		String pathToLib = "/usr/local/share/OpenCV/java/libopencv_java320.so";
		System.load(pathToLib);

		// Set the event log output file
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Choose Event Log Output File");
		if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			eventFile = fileChooser.getSelectedFile();
		}

		PrintWriter out = new PrintWriter(eventFile);
		out.write("Trial\tZone\tTime\tTrialOutcome\n");
		out.flush();

		// Set up video tracking
		TrackingJobSettingsGroup trackJGroup = new TrackingJobSettingsGroup();
		TrackingJobSetting settings = new TrackingJobSetting(new Rectangle(0,0,640,480),pixelThreshold,objectSize,darkObject);
		trackJGroup.addTrackingJobSetting(settings);

		OpenCVRecordProc aProc = new OpenCVRecordProc();
		aProc.plotExtended = false;			
		aProc.isGrayScale = true;			
		aProc.drawObject = true;			

		OpenCVTracker trackerInstance = new OpenCVTracker(camID);
		trackerInstance.initializeTracker(fps,secsToRun,trackJGroup,toFile,toVector,toScreen,null,aProc);
		trackerInstance.setVanishingTrail(20);
		Double corner_x = center_x - (MAZE_X_DIM * pxToMm)/2.0;
		Double corner_y = center_y - (MAZE_Y_DIM * pxToMm)/2.0;
		Double size_x = MAZE_X_DIM * pxToMm;
		Double size_y = MAZE_Y_DIM * pxToMm;
		settings.setAOI(new Rectangle(Math.max(corner_x.intValue(),0), Math.max(corner_y.intValue(),0), size_x.intValue(), size_y.intValue()));

		trackerInstance.setVisible(true);

		servo = new ServoControl();

		// Wait until the analysis starts
		while(!trackerInstance.analysisRunning){
			Thread.sleep(100);
		}

		int totalTrials = NUMBER_OF_TRIALS + EXTINCTION_TRIALS + REVERSAL_TRIALS;

		while(trackerInstance.analysisRunning && (counter < totalTrials || inPauseBlock))
		{
			if (initialMotorTurn)
			{
				servo.RewardReset();
				initialMotorTurn = false;
			}

			if (reversal && !inReversalBlock && counter > (totalTrials - NUMBER_OF_TRIALS))
			{
				System.out.println("REVERSAL BLOCK\n");
				inReversalBlock = true;
			}

			if (extinction && !inExtinctionBlock && counter > (totalTrials - NUMBER_OF_TRIALS - REVERSAL_TRIALS))
			{
				System.out.println("EXTINCTION BLOCK\n");
				servo.AccReset();
				inExtinctionBlock = true;
			}

			if (pause && !inPauseBlock && counter == (int)(NUMBER_OF_TRIALS+EXTINCTION_TRIALS+REVERSAL_TRIALS)/2)
			{
				System.out.println("PAUSE BLOCK\n");
				servo.AccReset();
				inPauseBlock = true;
				pauseBeginTime = aProc.runTime;
			}

			if (inPauseBlock)
			{
				if ((aProc.runTime - pauseBeginTime)/1000 > pauseBlockTime)
				{
					inPauseBlock = false;
					servo.RewardReset();
					System.out.println("END PAUSEBLOCK\n");
				}
			}
			// If there isn't any data, don't try to process it
			if(trackerInstance.getXs(0).size() == 0){
				Thread.sleep(5);
				continue;
			}

			// If there isn't a new frame to process, don't process this frame again
			if(frameTime == aProc.runTime){
				Thread.sleep(5);
				continue;
			}

			// Acquire position of the fly
			actualX = trackerInstance.getXs(0).lastElement();
			actualY = trackerInstance.getYs(0).lastElement();
			frameTime = aProc.runTime;

			// If there is no tracked object, don't process this frame
			if(actualX == -1){
				Thread.sleep(5);
				continue;
			}

			//Calculate distance from center
			deltX = actualX - center_x;
			deltY = actualY - center_y;

			dist = Math.sqrt(Math.pow(deltY, 2) + Math.pow(deltX, 2)) / pxToMm;

			//Determine zone
			if (dist > RADIUS_ONE)
			{
				originalZone = currentZone;
				if (actualX < center_x)
					currentZone = 1;
				else if (actualY < center_y)
					currentZone = 2;
				else
					currentZone = 3;
				inCenter = false;
			}
			else
			{
				inCenter = true;
				conditionFailed = true;
			}

			if (currentZone != originalZone && currentZone != -1 && originalZone != -1 && !inCenter) //I'm so sorry
				newZone = true;
			else 
				newZone = false;

			//Determine if fly made appropriate turn
			if (newZone)
			{
				newZone = false;
				rewardEntranceTime = aProc.runTime;
				//left turn paradigm
				if (originalZone == 3 && currentZone == 1)
				{
					conditionFailed = false;
				}
				else if (originalZone == 1 && currentZone == 2)
				{
					conditionFailed = false;
				}
				else if (originalZone == 2 && currentZone == 3)
				{
					conditionFailed = false;
				}
				else 
				{
					conditionFailed = true;
				}

				//checking right turn
				if (inReversalBlock)
					conditionFailed = !conditionFailed;

				if (!conditionFailed)
					trialOutcome = "Passed";
				else
					trialOutcome = "Failed";
				
				//this is also bad
				if (!conditionFailed)
				{
					if(originalZone != currentZone)
						counter++;
					
					out.write(counter + "\t" + currentZone + "\t" + aProc.runTime + "\t" + trialOutcome + "\n");
					out.flush();
					
					//hm. this seems dumb.
					if (inPauseBlock && originalZone != currentZone)
						totalTrials++;

					System.out.println("Trial: " + counter + " Current Zone " + currentZone + " Time: " + aProc.runTime + " Trial Outcome: " + trialOutcome + "\n");
				}
			}

			//Move motors
			if (conditionFailed && !inCenter && !motorHasTurned)
			{
				if (!inExtinctionBlock && !inPauseBlock)
				{
					if (currentZone == 1)
						servo.move(0, servoNeutralPosition);
					else if (currentZone == 2)
						servo.move(1, servoNeutralPosition);
					else if (currentZone == 3)
						servo.move(2, servoNeutralPosition);
					motorHasTurned = true;
				}
				else 
					motorHasTurned = true;
				
				if(originalZone != currentZone)
					counter++;
				
				//backtracking not treated as a new zone
				//I should really clean this code.
				//cleaner way would be just using !conditionFailed, not a temp variable
				if (!conditionFailed)
					trialOutcome = "Passed";
				else
					trialOutcome = "Failed";
				
				out.write(counter + "\t" + currentZone + "\t" + aProc.runTime + "\t" + trialOutcome + "\n");
				out.flush();
								
				//again. dumb.
				if (inPauseBlock && originalZone != currentZone)
					totalTrials++;
				
				System.out.println("Trial: " + counter + " Current Zone " + currentZone + " Time: " + aProc.runTime + " trialOutcome: " + trialOutcome + "\n");

				if(!inPauseBlock)
					System.out.println("\nMotor Turn, failed condition\n");

			}

			else if (!inExtinctionBlock && !inPauseBlock && !conditionFailed && !inCenter && aProc.runTime - rewardEntranceTime > 10000 && !motorHasTurned)
			{
				if (currentZone == 1)
					servo.move(0, servoNeutralPosition);
				else if (currentZone == 2)
					servo.move(1, servoNeutralPosition);
				else if (currentZone == 3)
					servo.move(2, servoNeutralPosition);
				motorHasTurned = true;

				System.out.println("\nMotor Turn, time over");
			}

			if (motorHasTurned && inCenter)
			{
				if (!inExtinctionBlock && !inPauseBlock)
					servo.RewardReset();
				motorHasTurned = false;
			}
		}
		// Once the analysis stops
		System.out.println("Complete! Ran " + counter + " trials.");
		out.close();
		if(trackerInstance.analysisRunning){
			trackerInstance.stopAnalysis();
		}

		//System.exit(0);
	}

}
