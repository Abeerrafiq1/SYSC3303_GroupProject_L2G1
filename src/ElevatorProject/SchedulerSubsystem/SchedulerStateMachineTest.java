package ElevatorProject.SchedulerSubsystem;


import static org.junit.Assert.assertEquals;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ElevatorProject.Information;
import ElevatorProject.Network;
/**
 * This is the JUnit test class for the SchedulerStateMachine class.
 * It exercises the setup process and how the scheduler handles receiving
 * arrivalSensor and floorRequest messages, as well as fault timer triggers.
 * 
 * @author Emma Boulay [Iteration 3], Abeer Rafiq
 *
 */
class SchedulerStateMachineTest extends Network{
	private SchedulerStateMachine underTest;
	private Thread active, listening;
	private Scheduler scheduler;
	
	/**
	 * This method will setup the active and listening thread of the scheduler. It will then
	 * send mock floorInit and elevatorInit messages to the scheduler to exercise its setup 
	 * process
	 * 
	 * @param port The port the listening thread is listening on
	 */
	public void setUpThreads(int port) {
		int receiverPort = port;
		
		scheduler = new Scheduler("Scheduler", receiverPort, 500);
		underTest = new SchedulerStateMachine(scheduler);
		active = new Thread(underTest, "activeThread");
		listening = new Thread(underTest, "listeningThread");
		
		listening.start();
		active.start();
		
		
		try {
			byte[] returnData;
			byte[] elevatorPort = "elevatorInit 1 123".getBytes(pac.getEncoding());
			byte[] floorPort1 = "floorInit 1 2".getBytes(pac.getEncoding());
			byte[] floorPort2 = "floorInit 2 3".getBytes(pac.getEncoding());
			byte[] floorInitEnd = "floorInitEnd".getBytes(pac.getEncoding());
			
			returnData = rpc_send(receiverPort, elevatorPort, 2000);
			assertEquals(Arrays.equals(returnData, "ACK".getBytes(pac.getEncoding())), true);
			
			returnData = rpc_send(receiverPort, floorPort1, 2000);
			assertEquals(Arrays.equals(returnData, "ACK".getBytes(pac.getEncoding())), true);
			
			returnData = rpc_send(receiverPort, floorPort2, 2000);
			assertEquals(Arrays.equals(returnData, "ACK".getBytes(pac.getEncoding())), true);
			
			returnData = rpc_send(receiverPort, floorInitEnd, 2000);
			assertEquals(Arrays.equals(returnData, "ACK".getBytes(pac.getEncoding())), true);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		//Check that scheduler starts in WAIT_FOR_REQUEST state
		assertEquals(underTest.getState().equals(SchedulerStateMachine.State.WAIT_FOR_REQUEST),true);
		
		
	}

	/**
	 * After each test interrupt the active and listening threads in the scheduler
	 * @throws InterruptedException 
	 */
	@AfterEach
	public void tearDownThreads() throws InterruptedException {
		
		underTest.interrupt();
		Thread.sleep(500);		
	}
	/**
	 * This tests if after a arrivalSensor message is received by the scheduler that
	 * it stays in WAIR_FOR_REQUEST state.
	 * 
	 * @throws Throwable byte encoding exception
	 */
	@Test
	public void testUpdateArrivals() throws Throwable{
		System.out.println("========================================");
		System.out.println("\t\tTest 1 - Update Arrivals Test");
		System.out.println("========================================");
		
		//Set up threads
		int receiverPort = 24;
		setUpThreads(receiverPort);
		//Create arrival sensor notification to send to scheduler
		byte[] arrivalSensorData = "arrivalSensor hh:mm:ss.mmm 5 1".getBytes();
		//Send arrival sensor notification to scheduler and wait for ACK
		byte[] returnData = rpc_send(receiverPort, arrivalSensorData, 2000);
		
		//Once an ACK is received, assert that it is as expected
		assertEquals(Arrays.equals(returnData, "ACK".getBytes(pac.getEncoding())), true);
		
		
	}
	
	/**
	 * At start up the scheduler state machine is expected to be in the 
	 * WAIT_FOR_REQUEST state
	 * @throws Throwable
	 */
	@Test
	public void testStartUp() throws Throwable{
		
		System.out.println("========================================");
		System.out.println("\t\tTest 2 - Start Up Test");
		System.out.println("========================================");
		
		//Test that all threads startup as expected
		int receiverPort = 23;
		setUpThreads(receiverPort);
	}
	
	/**
	 * This test checks the state after a floorRequest is placed in the scheduler and
	 * ensures that the workRequests array is expected after the scheduler receives
	 * the request
	 * 
	 * @throws Throwable byte encoding exception
	 */
	@Test
	public void testPutWorkRequest() throws Throwable{
		
		System.out.println("========================================");
		System.out.println("\t\tTest 3 - Put Request Test");
		System.out.println("========================================");
		
		//Set up threads
		int receiverPort = 26;
		setUpThreads(receiverPort);
		
		//Create floorRequest to send to scheduler
		String[] floorString = "floorRequest hh:mm:ss.mmm 5 UP 7".split(" ");
		
		//Send floorRequest to scheduler and wait for ACK
		byte[] floorRequest = "floorRequest hh:mm:ss.mmm 5 UP 7".getBytes();
		byte[] returnData = rpc_send(receiverPort, floorRequest, 2000);
		
		//floorRequest has been acknowledged. Check that its been added to the scheduler properly
		assertEquals(Arrays.equals(returnData, "ACK".getBytes(pac.getEncoding())), true);
		assertEquals(scheduler.getAllRequest().size() == 1, true);
		assertEquals(Arrays.equals(floorString, scheduler.getAllRequest().get(0)), true);
		scheduler.getAllRequest().clear();
		

	}
	
	/**
	 * The scheduler sends a request to the elevator and if the scheduler doesn't 
	 * receive a arrivalSensor message, then fault timer is supposed to be triggered. 
	 * 
	 * Therefore, this method tests that in this situation, the elevator reference 
	 * in the scheduler should have a state change to Out_of_Order.
	 * 
	 * @throws Throwable
	 */
	@Test
	public void testFaultTimer() throws Throwable{
		
		System.out.println("========================================");
		System.out.println("\t\tTest 4 - Fault Timer Timeout Test");
		System.out.println("========================================");
		int receiverPort = 121;
		
		// Set up threads
		setUpThreads(receiverPort);
		int elevatorPort = 123;//random
		
		// Create a Mock elevator 
		new Thread()
		{
		    public void run() {
		        try {
					DatagramSocket socket = new DatagramSocket(elevatorPort);
					System.out.println("Mock elevator subystem listening on port: " + elevatorPort);
					//Elevator receives Scheduler Request at port 123 and sends ack
					ReturnData data = receive(socket);
					System.out.println("Sent from port " + data.getPort());
					send(data.getPort(), createACK(), socket);
				} catch (SocketException e) {
					e.printStackTrace();
				}
		        
		    }
		}.start();
		
		//Send floorRequest to scheduler and wait for ACK
		byte[] floorRequest = "floorRequest hh:mm:ss.mmm 5 UP 7".getBytes();
		byte[] returnData = rpc_send(121, floorRequest, 100);
		
		// Sleep waitTime*2 to give some extra time before checking state
		Thread.sleep((int) 2 * (Information.TIME_CLOSE_DOOR + Information.TIME_OPEN_DOOR + Information.TRAVEL_TIME_PER_FLOOR));
		
		// Ensure that the elevator reference in scheduler is now Out_of_Order state
		// since the scheduler didn't receive a arrivalSensor message after sending a
		// request to the elevator
		assertEquals(scheduler.elevators.get(1).getFloor() == -1, true);
		assertEquals(scheduler.elevators.get(1).getState().equals("Out_of_Order"), true);
		
	}
	
}
