/**
 * FloorSubsystem.java
 * 
 * The FloorSubsystem thread will read in events from an input file and will try to put a request 
 * in the scheduler and then peek ahead to the next request and sleep until time elapsed to send 
 * the next floor request to the scheduler
 *
 * @author Emma Boulay
 * 
 * SYSC 3303 L2 Group 1
 * @version 1.0
 */

package ElevatorProject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Scanner;

public class FloorSubsystem extends Network implements Runnable {
	// The input file that contains all the floor requests
	private File inputFile = new File(System.getProperty("user.dir") + "/src/ElevatorProject/floorRequest.txt");
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

	private int schedulerPort;
	private int timeout;
	private int nFloors;
	private int nShafts;
	private Floor[] floors;

	/**
	 * The constructor method creates a floor subsystem with nFloors and each floor
	 * has nShafts. It will communicate to the scheduler with the specified port and
	 * timeout
	 * 
	 * @param nFloors       The number of floors the the elevator system spans
	 * @param nShafts       The number of elevator shafts
	 * @param schedulerPort The port the scheduler is configured to listen on
	 *                      (Usually 23)
	 * @param timeout       The time in milliseconds, if a packet is not ACK it will
	 *                      send again
	 */
	public FloorSubsystem(int nFloors, int nShafts, int schedulerPort, int timeout) {
		this.schedulerPort = schedulerPort;
		this.timeout = timeout;
		this.nFloors = nFloors;
		this.nShafts = nShafts;

	}

	/**
	 * This method creates each floor in the floor subsystem and communicates which
	 * port it is listening on for this session to the scheduler
	 * 
	 * @param nFloors The number of floors the system spans
	 * @param nShafts The number of elevator shafts the system has
	 */
	public void createFloorThreads(int nFloors, int nShafts) {

		floors = new Floor[nFloors];

		for (int i = 0; i < nFloors; i++) {
			floors[i] = new Floor(i + 1, nFloors, nShafts, this.schedulerPort);
			floors[i].setUp(); // Communicate listening port to scheduler
			new Thread(floors[i], "Floor " + (i + 1)).start();
		}
	}

	/**
	 * Method used to parse the request file and store the requests in an array list
	 * (requestList)
	 * 
	 * @param file the input file that contains the list of elevator requests to be
	 *             performed.
	 * @return requestList the arraylist containing all the requests to be performed
	 */
	@SuppressWarnings({ "resource" })
	public ArrayList<String> getRequestFromFile(File file) {
		ArrayList<String> requestList = new ArrayList<String>();
		try {
			Scanner scanner = new Scanner(inputFile);

			// Scan each line from the txt file and store it to the requestList
			while (scanner.hasNextLine()) {
				requestList.add(scanner.nextLine());
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return requestList;
	}

	/**
	 * This method turns on the lamp of the floorButton after being pressed
	 * 
	 * @param request The request read from the input file
	 */
	public void turnOnLamp(String request) {
		String[] reqArr = request.split(" ");
		int floor = Integer.parseInt(reqArr[1]);
		String dir = reqArr[2];
		floors[floor].turnOnOffLamp(dir, true);
	}

	/**
	 * This method converts the given time in the format "HH:mm:ss.SSS" to
	 * milliseconds
	 * 
	 * @param time A string representation of time in the format "HH:mm:ss.SSS"
	 * @return milliseconds The given time in milliseconds
	 */
	public int getMilli(String time) {
		try {
			return (int) sdf.parse(time).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return -1; // Error
	}

	/**
	 * The run method for the floorSubsystem. It will read in the input file and
	 * send them to the scheduler. After sending a request it will turn on the
	 * designate floor direction lamp
	 */
	@Override
	public void run() {
		// Parse input file
		ArrayList<String> requests = getRequestFromFile(inputFile);

		// Instantiate and setup floors
		createFloorThreads(nFloors, nShafts);

		// This lets the scheduler know that all floors have been initialized
		byte[] initMsg = null;
		try {
			initMsg = ("floorInitEnd").getBytes(pac.getEncoding());
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		rpc_send(schedulerPort, initMsg, 500);

		// Send the next request in the array
		while (!requests.isEmpty()) {
			String curRequest = requests.remove(0);
			turnOnLamp(curRequest); // Button has been pressed, turn on lamp

			try {
				rpc_send(schedulerPort, ("floorRequest " + curRequest).getBytes(pac.getEncoding()), timeout);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			// If the last request sent was not the last request sleep until next request is
			// read to be sent
			if (requests.size() > 0) {
				try {
					int now = getMilli(curRequest.split(" ")[0]);
					int nextTime = getMilli(requests.get(0).split(" ")[0]);
					Thread.sleep(nextTime - now);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

	}

	/**
	 * The main method of FloorSubsystem. It creates the floorSubystem and starts
	 * all threads
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		int nFloors = 7;
		int nShafts = 1;
		int schedulerPort = 23;
		int timeout = 500; // in milliseconds

		new Thread(new FloorSubsystem(nFloors, nShafts, schedulerPort, timeout), "FloorSubsystem").start();

	}
}
