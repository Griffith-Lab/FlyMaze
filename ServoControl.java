import com.lobsterman.JavaGrinders.JavaGrinders;
import com.lobsterman.JavaGrinders.control.ControlDeviceOperator;
import com.lobsterman.JavaGrinders.control.PhidgetAdvancedServoInterface;
import com.phidgets.Phidget;

public class ServoControl 
{

	PhidgetAdvancedServoInterface servoIntf;
	ControlDeviceOperator relay0;
	ControlDeviceOperator relay1;
	ControlDeviceOperator relay2;
	ControlDeviceOperator relay3;
	ControlDeviceOperator relay4;
	ControlDeviceOperator relay5;


	public ServoControl() throws Exception
	{
		servoIntf = new PhidgetAdvancedServoInterface(8);
		relay0 = new ControlDeviceOperator(0);
		relay1 = new ControlDeviceOperator(1);
		relay2 = new ControlDeviceOperator(2);
		relay3 = new ControlDeviceOperator(4);
		relay4 = new ControlDeviceOperator(5);
		relay5 = new ControlDeviceOperator(6);

		servoIntf.addControlDeviceOperator(relay0);
		servoIntf.addControlDeviceOperator(relay1);
		servoIntf.addControlDeviceOperator(relay2);
		servoIntf.addControlDeviceOperator(relay3);
		servoIntf.addControlDeviceOperator(relay4);
		servoIntf.addControlDeviceOperator(relay5);
		servoIntf.listDeviceOperators();
	}

	public void move(int channel, int degrees) throws Exception
	{
		engage();

		servoIntf.setValue(channel, degrees);

//		Thread.sleep(5000);
//
//		servoIntf.setValue(channel, 120);
		
		Thread.sleep(1000);
		
		disengage();

	}

	public void AccReset() throws Exception
	{
		engage();

		for (int i = 0; i < 7; i++)
		{
			if (i != 3)
				try {
					servoIntf.setValue(i, 80);
					} catch (Exception e) {e.printStackTrace();}
		}
		Thread.sleep(1000);

		disengage();
	}
	
	public void RewardReset() throws Exception
	{
		engage();

		for (int i = 0; i < 7; i++)
		{
			if (i != 3)
				try {
					servoIntf.setValue(i, 160);
					} catch (Exception e) {e.printStackTrace();}
		}
		Thread.sleep(1000);

		disengage();
	}
	
	public void engage() throws Exception
	{
		relay0.setEngaged(true);
		relay1.setEngaged(true);
		relay2.setEngaged(true);
		relay3.setEngaged(true);
		relay4.setEngaged(true);
		relay5.setEngaged(true);
	}
	
	public void disengage() throws Exception
	{
		relay0.setEngaged(false);
		relay1.setEngaged(false);
		relay2.setEngaged(false);
		relay3.setEngaged(false);
		relay4.setEngaged(false);
		relay5.setEngaged(false);
	}
	
}
