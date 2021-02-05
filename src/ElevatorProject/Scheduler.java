/**
 * Scheduler.java
 * 
 * The scheduler is used to pass floorRequests from the floor subsystem to the
 * elevator subsystem
 *
 * @author Emma Boulay
 * @author Abeer Rafiq
 * 
 *         SYSC 3303 L2
 * @version 1.0
 */
package ElevatorProject;

import java.util.ArrayList;

public class Scheduler implements Runnable {
	
	private ArrayList<Object> workRequests = new ArrayList<>();

	/**
	 * The default constructor.
	 */
	public Scheduler() {
	}

	/**
	 * This method puts a floor request into the scheduler. This method will return
	 * when there is space for the floor request in the scheduler.
	 * 
	 * @param floorRequest An object representing the floor request from the floor
	 *                     subsystem
	 */
	public synchronized void putRequest(Object elevatorRequests) {
		while (workRequests.size() != 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.err.println(e);
			}
		}

			this.workRequests.add(elevatorRequests);
			notifyAll();
			return;
		
	}

	/**
	 * 
	 * @return floorRequest
	 */
	public synchronized Object getRequest() {
		while (!isWork()) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.err.println(e);
			}
		}
		
		Object floorRequest = workRequests.remove(0);
		notifyAll();
		return floorRequest;
	}

	/**
	 * 
	 * @return isWork
	 */
	public synchronized boolean isWork() {
		return !workRequests.isEmpty();
	}

	@Override
	public void run() {
		while (true) {

		}
	}

}
