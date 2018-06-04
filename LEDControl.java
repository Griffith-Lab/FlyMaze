import com.lobsterman.JavaGrinders.JavaGrinders;
import com.lobsterman.JavaGrinders.control.ArduinoInterface;
import com.lobsterman.JavaGrinders.control.ControlDeviceOperator;

public class LEDControl {
	public static final void main(String args[]) throws Exception {
		JavaGrinders.itsStringer.tellln("Arduino LED Controller");
		JavaGrinders.listDetail = true;
		JavaGrinders.listAllVersions();
		ArduinoInterface relayIntf = new ArduinoInterface(40);
		
		ControlDeviceOperator relay0 = new ControlDeviceOperator(32);
		
//		ControlDeviceOperator relay1 = new ControlDeviceOperator(12);
		relayIntf.addControlDeviceOperator(relay0);
		
//		relayIntf.addControlDeviceOperator(relay1);
//		relayIntf.listDeviceInterface();
		
		relay0.pullHigh();
		Thread.sleep(5000);
		relay0.pullLow();
		Thread.sleep(500);
		relay0.pullHigh();

		relayIntf.close();
	}
}
