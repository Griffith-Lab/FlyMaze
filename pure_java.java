package blinker;

import jssc.SerialPort;
import jssc.SerialPortException;

public class pure_java {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws InterruptedException {
		SerialPort serialPort = new SerialPort("/dev/ttyACM0");
		try {
			System.out.println("Port opened: " + serialPort.openPort());
			System.out.println("Params setted: " + serialPort.setParams(9600, 8, 1, SerialPort.PARITY_NONE, false, false));
			Thread.sleep(1000);
			
			serialPort.writeBytes("A".getBytes());
			serialPort.writeBytes("B".getBytes());
			serialPort.writeBytes("0".getBytes());
			serialPort.writeBytes("A".getBytes());
			serialPort.writeBytes("0".getBytes());
			
			serialPort.closePort();
		} catch (SerialPortException ex) {
			System.out.println(ex);
		}
	}


}
