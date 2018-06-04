import java.awt.Rectangle;

import com.lobsterman.JavaGrinders.Tracker.OpenCVRecordProc;
import com.lobsterman.JavaGrinders.Tracker.OpenCVTracker;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSetting;
import com.lobsterman.JavaGrinders.Tracker.TrackingJobSettingsGroup;

public class Acclimation 
{
	static int camID = 0;
	static int pixelThreshold = 40;
	static int objectSize = 300;
	static boolean darkObject = true;
	static int secsToRun = 300;
	static boolean toFile = true;
	static boolean toVector = true;
	static boolean toScreen = false;
	static float fps = 30;
	
	final static double center_x = 260;	// pixels
	final static double center_y = 222;	// pixels
	final static double pxToMm = 11.2;
	final static double MAZE_Y_DIM = 31; // mm
	final static double MAZE_X_DIM = 36; // mm 
	
	public static void main(String[] args) throws Exception 
	{
		System.out.println("Acclimation");
		//To open an instance of the OpenCV library during runtime from a specific path ...
		String pathToLib = "/usr/local/share/OpenCV/java/libopencv_java320.so";
		System.load(pathToLib);
		
		//set up video tracking
		TrackingJobSettingsGroup trackJGroup = new TrackingJobSettingsGroup();
		TrackingJobSetting settings = new TrackingJobSetting(new Rectangle(0,0,640,480),pixelThreshold,objectSize,darkObject);
		
		trackJGroup.addTrackingJobSetting(settings);

		OpenCVRecordProc aProc = new OpenCVRecordProc();
		aProc.plotExtended = false;			
		aProc.isGrayScale = true;			
		aProc.drawObject = true;
		//aProc.readPNGToRef()
		

		OpenCVTracker trackerInstance = new OpenCVTracker(camID);
		trackerInstance.initializeTracker(fps,secsToRun,trackJGroup,toFile,toVector,toScreen,null,aProc);
		trackerInstance.setVanishingTrail(20);
		Double corner_x = center_x - (MAZE_X_DIM * pxToMm)/2.0;
		Double corner_y = center_y - (MAZE_Y_DIM * pxToMm)/2.0;
		Double size_x = MAZE_X_DIM * pxToMm;
		Double size_y = MAZE_Y_DIM * pxToMm;
		settings.setAOI(new Rectangle(Math.max(corner_x.intValue(),0), Math.max(corner_y.intValue(),0), size_x.intValue(), size_y.intValue()));
		
		trackerInstance.setVisible(true);
				
		final ServoControl servo = new ServoControl();
		servo.AccReset();
	}
}