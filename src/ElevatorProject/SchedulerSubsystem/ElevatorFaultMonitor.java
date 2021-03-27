package ElevatorProject.SchedulerSubsystem;

import java.util.Timer;
import java.util.TimerTask;
import java.awt.Toolkit;

/**
 * This class is responsible for starting a timer each time a elevator services a request.
 * The timer will be triggered after a certain time since there was no arrival sensor
 * message received that would have cancelled the timer. The corresponding elevator 
 * will go out of service to ensure that the issue can be reviewed before operating again.
 * 
 * @author Emma Boulay, Abeer Rafiq
 *
 */
public class ElevatorFaultMonitor {
	// The amount of time to wait before triggering the timer
	private int waitTime;
	private Scheduler scheduler;
	private int elevator;
	// Used to implement the fault timer
	private Timer timer;

	/**
	 * This is the constructor for the ElevatorFaultMonitor. 
	 * It initializes the amount of time to wait before triggering the timer.
	 * It initializes the scheduler and elevator so the scheduler's elevator reference 
	 * goes out of order for that elevator. 
	 * @param scheduler
	 * @param waitTime
	 * @param elevator
	 */
	public ElevatorFaultMonitor(Scheduler scheduler, int waitTime, int elevator) {
		this.waitTime = waitTime;
		this.scheduler = scheduler;
		this.elevator = elevator;
	}
	
	/**
	 * This method is used to cancel the timer which will be when a arrival sensor 
	 * message has been received by the scheduler for the elevator. 
	 * It means the elevator has operated properly.
	 */
	public void cancelTimer() {
		System.out.println("Arrival notification received. Stoping fault timer for elevator " + elevator);
		timer.cancel();
	}
	
	/**
	 * This method is used to start the timer for a specific elevator.
	 * If that elevator doesn't reach in time and send a arrival sensor notification,
	 * it means that the elevator must have had some issue.
	 * Therefore the scheduler's elevator reference goes to Out_of_Order.
	 */
	public void startTimer() {
		// Instantiate a Timer
		timer = new Timer();
		System.out.println("Starting fault timer for elevator " + elevator);
		// Assign Timer Task
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
					// If timer triggered, make elevator reference in scheduler Out_of_Order
					System.out.println("Arrival sensor timed out. Elevator " + elevator
							+ " out of order.\nMaintenance has been notified and is on its way!");
					int elevatorFloor = scheduler.elevators.get(elevator).getFloor();
					//scheduler.elevators.remove(elevator);
					scheduler.elevators.put(elevator, scheduler.createNewElevatorReference(elevatorFloor, "Out_of_Order"));
					Toolkit.getDefaultToolkit().beep();
					// End timer
					timer.cancel();
			}
		};
		// Schedule Timer to wait waitTime before being triggered
		timer.schedule(task, (int) 1.15 * waitTime);
	}

}




