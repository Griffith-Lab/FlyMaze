import java.awt.Rectangle;
import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;

import com.lobsterman.JavaGrinders.Tracker.OpenCVRecordProc;
import com.lobsterman.JavaGrinders.Tracker.OpenCVTracker;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSetting;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSettingsGroup;

public class SlowDownTraining {
	// Tracker Settings
	static int camID = 0;
	static int pixelThreshold = 12;
	static int objectSize = 200;
	static boolean darkObject = true;
	static int secsToRun = -1;
	static boolean toFile = true;
	static boolean toVector = true;
	static boolean toScreen = false;
	static float fps = 30;

	// Servo Settings
	static ServoControl servo;
	private static final ScheduledExecutorService scheduler = Executors
			.newScheduledThreadPool(1);
	final static int servoNeutralPosition = 80;
	final static int servoRewardPosition = 160;
	final static int servoReturnDelay = 5; // seconds

	// Training Settings
	final static int NUMBER_OF_TRIALS = 210;
	final static int BASELINE_TRIALS = 10;
	final static double TARGET_SPEED = 3;
	final static double center_x = 282; // pixels
	final static double center_y = 211; // pixels
	final static double RADIUS_ONE = 20; // mm
	final static double RADIUS_TWO = 25; // mm
	final static double pxToMm = 6.75;
	final static int yAxisCutoff = 60; // pixels
	final static double MAZE_Y_DIM = 63; // mm
	final static double MAZE_X_DIM = 73; // mm

	public static void main(String[] args) throws Exception {
		System.out.println("Slow Down Training");

		// Declare local variables
		int actualY;
		int actualX;
		double deltX;
		double deltY;
		double dist;
		int zone = -1;

		long frameTime = -1;
		long time1 = 0;
		long time2 = 0;
		// *******
		long rewardEntranceTime = 0;
		boolean motorHasTurned = false;
		boolean conditionFailed = false;
		boolean failureOutput = false;
		// *******
		boolean outRad1 = false;
		boolean outRad2 = false;
		double currentSpeed = -1;

		boolean initialMotorTurn = true;

		ArrayList<Double> speedsByOrder = new ArrayList<Double>();
		ArrayList<Double> speedsBySpeed = new ArrayList<Double>();
		Double removedElement = -1.0;

		boolean isNewApproach = false;
		boolean turnMotor = false;
		String trialOutcome = " ";
		int counter = 0;

		File eventFile = new File("defaultEventFile");
		DecimalFormat screenFormat = new DecimalFormat("#.0");
		DecimalFormat fileFormat = new DecimalFormat("#.0000");

		// Open an instance of the OpenCV library during runtime from a specific
		// path
		String pathToLib = "/usr/local/share/OpenCV/java/libopencv_java320.so";
		System.load(pathToLib);

		// Set the event log output file
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Choose Event Log Output File");
		if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			eventFile = fileChooser.getSelectedFile();
		}

		PrintWriter out = new PrintWriter(eventFile);
		out.write("Trial\tTime_Enter\tTime_Exit\tZone\tCurrent_Speed\tOutcome\n");
		out.flush();

		// Set up video tracking
		TrackingJobSettingsGroup trackJGroup = new TrackingJobSettingsGroup();
		TrackingJobSetting settings = new TrackingJobSetting(new Rectangle(0,
				0, 640, 480), pixelThreshold, objectSize, darkObject);
		trackJGroup.addTrackingJobSetting(settings);

		OpenCVRecordProc aProc = new OpenCVRecordProc();
		aProc.plotExtended = false;
		aProc.isGrayScale = true;
		aProc.drawObject = true;

		OpenCVTracker trackerInstance = new OpenCVTracker(camID);
		trackerInstance.initializeTracker(fps, secsToRun, trackJGroup, toFile,
				toVector, toScreen, null, aProc);
		trackerInstance.setVanishingTrail(20);
		Double corner_x = center_x - (MAZE_X_DIM * pxToMm) / 2.0;
		Double corner_y = center_y - (MAZE_Y_DIM * pxToMm) / 2.0;
		Double size_x = MAZE_X_DIM * pxToMm;
		Double size_y = MAZE_Y_DIM * pxToMm;
		settings.setAOI(new Rectangle(Math.max(corner_x.intValue(), 0), Math
				.max(corner_y.intValue(), 0), size_x.intValue(), size_y
				.intValue()));

		trackerInstance.setVisible(true);

		servo = new ServoControl();

		// Wait until the analysis starts
		while (!trackerInstance.analysisRunning) {
			Thread.sleep(100);
		}

		while (trackerInstance.analysisRunning && counter < NUMBER_OF_TRIALS) {
			if (initialMotorTurn) {
				servo.AccReset();
				Thread.sleep(500);
				servo.RewardReset();
				initialMotorTurn = false;
			}
			// If there isn't any data, don't try to process it
			if (trackerInstance.getXs(0).size() == 0) {
				Thread.sleep(5);
				continue;
			}

			// If there isn't a new frame to process, don't process this frame
			// again
			if (frameTime == aProc.runTime) {
				Thread.sleep(5);
				continue;
			}

			// Acquire position of the fly
			actualX = trackerInstance.getXs(0).lastElement();
			actualY = trackerInstance.getYs(0).lastElement();
			frameTime = aProc.runTime;

			// If there is no tracked object, don't process this frame
			if (actualX == -1) {
				Thread.sleep(5);
				continue;
			}

			// Record frame times when the fly crosses a distance threshold
			deltX = actualX - center_x;
			deltY = actualY - center_y;
			dist = Math.sqrt(Math.pow(deltY, 2) + Math.pow(deltX, 2)) / pxToMm;

			if (dist >= RADIUS_TWO && outRad1 && !outRad2) {
				outRad2 = true;
				time2 = frameTime;
			}

			if (dist >= RADIUS_ONE && !outRad1 && dist < RADIUS_TWO) {
				outRad1 = true;
				time1 = frameTime;
			}

			if (dist < RADIUS_TWO) {
				outRad2 = false;
			}

			if (dist < RADIUS_ONE) {
				outRad1 = false;
				isNewApproach = true;
			}

			// If the fly went down a chute
			if (outRad1 && outRad2 && isNewApproach) {
				// *******
				rewardEntranceTime = aProc.runTime;
				// ********
				isNewApproach = false;

				// Determine which zone the fly is in
				if (actualX > center_x) { // Right side of the maze
					if (actualY > (center_y + yAxisCutoff)) {
						zone = 5; // upper right
					} else if (actualY < (center_y - yAxisCutoff)) {
						zone = 2; // lower right
					} else {
						zone = 4; // middle right
					}
				} else { // Left side of the maze
					if (actualY > (center_y + yAxisCutoff)) {
						zone = 6; // upper left
					} else if (actualY < (center_y - yAxisCutoff)) {
						zone = 1; // lower left
					} else {
						zone = 0; // middle left
					}
				}

				// Add speed to ArrayLists
				currentSpeed = (RADIUS_TWO - RADIUS_ONE)
						* (1.0 / (time2 - time1)) * 1000.0;
				if (speedsByOrder.size() == 11) {
					removedElement = speedsByOrder.remove(0);
					speedsBySpeed.remove(removedElement);
					speedsByOrder.add(currentSpeed);
					speedsBySpeed.add(currentSpeed);
				} else {
					speedsByOrder.add(currentSpeed);
					speedsBySpeed.add(currentSpeed);
				}
				Collections.sort(speedsBySpeed);

				// Check speed, determine if the fly should be rewarded
				if (speedsBySpeed.size() < 11) {
					turnMotor = true;
				} else if (currentSpeed < speedsBySpeed.get(5)
						|| currentSpeed < TARGET_SPEED) {
					turnMotor = true;
				}

				// Reward the first trials
				if (counter < BASELINE_TRIALS) {
					turnMotor = true;
				}
				// ***************
				// Turn the motor, turn it back after a delay
				// if (turnMotor){
				// servo.move(zone, servoRewardPosition);
				// Delay(zone);
				// trialOutcome = "TURN";
				// }
				// else{
				// trialOutcome = "SKIP";
				// }
				// ***************
				if (turnMotor) {
					trialOutcome = "PASSED";
				} else {
					trialOutcome = "FAILED";
					conditionFailed = true;
				}
				//
				// Record the trial
				counter++;
				out.write(counter + "\t" + time1 + "\t" + time2 + "\t" + zone
						+ "\t" + fileFormat.format(currentSpeed) + "\t"
						+ trialOutcome + "\n");
				out.flush();
				// turnMotor = false;

				System.out.println("Trial:" + counter + " Zone:" + zone
						+ " Speed:" + screenFormat.format(currentSpeed)
						+ " Outcome:" + trialOutcome);
				System.out.print("Speed Array: ");

				for (double elem : speedsBySpeed) {
					System.out.print(screenFormat.format(elem));
					System.out.print(" , ");
				}
				System.out.print("\n\n");
			}
			// if conditions are passed
			if (turnMotor && outRad1 && outRad2
					&& (aProc.runTime - rewardEntranceTime > 10000)) {
				servo.move(zone, servoNeutralPosition);
				System.out.println("MOTOR TURN Time Over \n\n");
				turnMotor = false;
				motorHasTurned = true;
			} else if (turnMotor && !outRad2) {
				turnMotor = false;
				motorHasTurned = false;
			}
			if (motorHasTurned && !outRad1) {
				servo.move(zone, servoRewardPosition);
				System.out.println("MOTOR TURN Return to default\n\n");
				motorHasTurned = false;
			}

			// if conditions are not passed
			if (conditionFailed && !failureOutput) {
				servo.move(zone, servoNeutralPosition);
				System.out.println("MOTOR TURN Failed Condition \n\n");
				failureOutput = true;
			}
			if (conditionFailed && !outRad1) {
				servo.move(zone, servoRewardPosition);
				System.out.println("MOTOR TURN Return to default \n\n");
				conditionFailed = false;
				failureOutput = false;
			}
		}

		// Once the analysis stops
		System.out.println("Complete! Ran " + counter + " trials.");
		out.close();
		if (trackerInstance.analysisRunning) {
			trackerInstance.stopAnalysis();
		}
		servo.RewardReset();
		System.exit(0);
	}

	private static void Delay(final int i) {
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					servo.move(i, servoNeutralPosition);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, servoReturnDelay, TimeUnit.SECONDS);

	}
}
