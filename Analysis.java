import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.lang.Math;

public class Analysis 
{
	public static void main(String[] args) throws Exception
	{
		//TODO
		//implement some way of browsing for the file rather than copying it
		//into src - something about FileChooser?
		
		//replace text with the name of the text files to be analyzed
		File rewardFile = new File("Reward Training Trial 1");
		File acclimationFile = new File("Acclimation Trial 1");
		
		ArrayList<Integer> rewardTimes = new ArrayList<Integer>();
		ArrayList<Integer> rewardZones = new ArrayList<Integer>();
		ArrayList<Integer> acclimationTimes = new ArrayList<Integer>();
		ArrayList<Integer> acclimationZones = new ArrayList<Integer>();
		int rewardInside = 0;
		int rewardOutside = 0;
		int acclimationInside = 0;
		int acclimationOutside = 0;
		
		//parses the raw coordinates from the original text files into times and zones
		Scanner text = new Scanner(rewardFile);
		Parser(rewardTimes, rewardZones, text);				
		text = new Scanner(acclimationFile);
		Parser(acclimationTimes, acclimationZones, text);
		
		//goes through the zones to determine whether the fly was in the inside or outside zones
		acclimationInside = InsideCheck(acclimationZones);
		acclimationOutside = OutsideCheck(acclimationZones);
		rewardInside = InsideCheck(rewardZones);
		rewardOutside = OutsideCheck(rewardZones);
		
		//simple calculation to look at percentage spent in the inside arms
		System.out.println("Acclimation - Percent spent inside loop: " + (double)acclimationInside/(acclimationInside+acclimationOutside));
		System.out.println("Reward - Percent spent inside loop: " + (double)rewardInside/(rewardInside + rewardOutside));
		
		//prints out the files to text files found in the source file of this program
		PrintWriter out = new PrintWriter("Acclimation 7-11 Trial 1 Times and Zones");
		Printer(acclimationTimes, acclimationZones, out);
		out = new PrintWriter("Reward 7-11 Trial 1 Times and Zones");
		Printer(rewardTimes, rewardZones, out);
		out.close();
	}
	
	/**
	 * This method goes through the passed zone array list and counts the number of times the 
	 * fly was in an inside zone
	 * @param zones
	 * @return number of times the fly was in an inside zone (1-6)
	 */
	public static int InsideCheck (ArrayList<Integer> zones)
	{
		int inside = 0;
		
		for (int i = 0; i < zones.size(); i++)
			if (zones.get(i) > 0 && zones.get(i) < 7)
				inside++;
		
		return inside;
	}
	
	/**
	 * This method goes through the passed zones array list and counts the number of times the 
	 * fly was in an outside zone
	 * @param zones
	 * @return number of times the fly was in an outside zone (7-12)
	 */
	public static int OutsideCheck (ArrayList<Integer> zones)
	{
		int outside = 0;
		
		for (int i = 0; i < zones.size(); i++)
			if (zones.get(i) > 6)
				outside++;
		
		return outside;
	}
	
	/**
	 * This method prints out the times and zones into a single text file, found in the program folder
	 * @param times
	 * @param zones
	 * @param out
	 */
	public static void Printer (ArrayList<Integer> times, ArrayList<Integer> zones, PrintWriter out)
	{
		for (int i = 0; i < zones.size(); i++)
		{
			out.println(times.get(i) + " " + zones.get(i));
			out.flush();
		}
	}
	
	/**
	 * This method uses the raw data files to take out the fly's coordinates and its corresponding
	 * timestamps
	 * @param times
	 * @param zones
	 * @param text
	 */
	public static void Parser(ArrayList<Integer> times, ArrayList<Integer> zones, Scanner text)
	{
		while (text.hasNextLine())
		{
			times.add(text.nextInt());
			zones.add(ZoneCalculation(text.nextInt(), text.nextInt()));
			text.next();
			text.next();
		}
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return currentZone
	 */
	public static int ZoneCalculation(int x, int y)
	{
		int currentZone = -1;
		int actualX = x;
		int actualY = y;
		
		double calcY_L2 = (-83/141.5) * (actualX - 373.5) + 167.5;
		double calcY_L3 = (83/141.5) * (actualX - 373.5) + 250.5;
		
		if (actualX < 302.5)
			if (actualY < calcY_L3)
				currentZone = 2;
			else if (actualY < calcY_L2)
				currentZone = 1;
			else 
				currentZone = 6; //actualY > calcY_L2
		else
			if (actualY < calcY_L2)
				currentZone = 3;
			else if (actualY < calcY_L3)
				currentZone = 4;
			else
				currentZone = 5; //actualY > calcY_L3
		
		double distance = Math.sqrt(Math.pow(302.5-actualX, 2) + Math.pow(209-actualY, 2));
		int centerOutDistance = 25169;
		if (distance >= Math.sqrt(centerOutDistance))
			currentZone+=6;
		
		if (actualX == -1 && actualY == -1)
			currentZone = -1;
		return currentZone;
	}
}