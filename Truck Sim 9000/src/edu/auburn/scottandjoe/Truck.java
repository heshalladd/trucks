package edu.auburn.scottandjoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class Truck {
	// physical constraints constants
	public static final double MAX_ACCELERATION = 1.0;
	public static final double MIN_ACCELERATION = -3.0;
	public static final int MAX_LANE = 1;
	public static final int MIN_LANE = 1;
	public static final double truckLength = 25.0;
	// 80mph is 35.7m/s
	public static final double MAX_REASONABLE_SPEED = 35.7;
	// 55mph is 24.6m/s
	public static final double MIN_REASONABLE_SPEED = 24.6;

	// initialization parameter constants
	public static final int RANDOMIZE_INT = -10000;
	public static final double RANDOMIZE_DOUBLE = -10000.0;

	// truck state magic number constants
	// ai states:
	// 0 - new truck object. has had no thoughts
	// 1 - just started, is waiting minimum time for all trucks to stabilize
	// speed
	// 2 - speeds initially stabilized
	// 3 - in solo convoy and seeking others
	// 4 - in a multi convoy and seeking others
	// 5 - in a full convoy
	// 6 - collided
	// 7 - merging convoy(trying to join the convoy in front of it)
	public static final int NEW_TRUCK = 0;
	public static final int STABILIZING = 1;
	public static final int STABILIZED = 2;
	public static final int SOLO_CONVOY = 3;
	public static final int MULTI_CONVOY = 4;
	public static final int FULL_CONVOY = 5;
	public static final int COLLIDED = 6;
	public static final int MERGING_CONVOY = 7;

	// ai constants
	private static final double STABILIZING_SPEED = 31.3;
	private static final double SOLO_CATCHING_SPEED = 33.5;
	private static final double MERGING_CATCHING_SPEED = 33.0;
	private static final double MULTI_CATCHING_SPEED = 32.4;
	private static final double MIN_CONVOY_GAP = 10.0;
	private static final double MAX_CONVOY_GAP = 20.0;

	// tick rate taken from the air
	private int tickRate = TheAir.TICK_RATE;

	// truck meta and ai variables
	private int desiredLane;
	private int desiredPlaceInConvoy;
	private int orderInConvoy = 1; // 1 will signify leader of convoy
	private int truckAIState = NEW_TRUCK;
	private int stabilizingCountdown = 0;
	private double desiredSpeed;
	private double startPos;
	private String convoyID = UUID.randomUUID().toString(); // id of convoy
	private static ConcurrentLinkedQueue<String> incomingUDPMessages = new ConcurrentLinkedQueue<String>();
	private boolean changingLanes = false;
	private boolean probablyFirst = false;

	// message meta
	private int sequenceNumber = 1;
	private int messagesPerSecond = 10;
	private long lastMessageTime = 0l;
	private static DatagramSocket airUDPSocket;

	// truck properties
	private double acceleration;
	private double pos;
	private double speed = 0;
	private int truckNumber;
	private int lane;

	// intents
	private boolean intentChangeLane = false;

	// caching
	private Truck[] truckCache = new Truck[5];
	private int[] truckSequenceCache = new int[5];
	public static boolean[] truckInitialized = new boolean[5]; // will
																// initialize to
																// false
																// (desired)

	// initializes a truck object. truck numbering conflicts are not handled,
	// and are the truck runners responsibility
	public Truck(int truckNumber, int lane, double pos, double speed,
			double acceleration) throws FatalTruckException {
		Random rand = new Random();
		this.truckNumber = truckNumber;
		if (lane != RANDOMIZE_INT) {
			if (lane > MAX_LANE || lane < MIN_LANE) {
				System.out.println("[SEVERE] Truck " + truckNumber
						+ " is off the road!");
				explode("Truck was initialized in an invalid lane " + lane
						+ " (allowable is " + MIN_LANE + " to " + MAX_LANE);
			}
			this.lane = lane;
			this.desiredLane = lane;
			System.out.println("[NORMAL] Truck lane initialized to "
					+ this.lane + ".");
		} else {
			// randomize lane accordingly
			this.lane = rand.nextInt(MAX_LANE) + MIN_LANE;
			System.out.println("[NORMAL] Truck lane randomly initialized to "
					+ this.lane + ".");
		}

		if (pos != RANDOMIZE_DOUBLE) {
			this.pos = pos;
			System.out.println("[NORMAL] Truck position initialized to "
					+ this.pos + ".");
		} else {
			double MAX_INITIAL_POS = 350;
			// randomize position between 0 and 350
			this.pos = MAX_INITIAL_POS * rand.nextDouble();
			System.out
					.println("[NORMAL] Truck position randomly initialized to "
							+ this.pos + ".");
		}

		if (speed != RANDOMIZE_DOUBLE) {
			this.speed = speed;
			System.out.println("[NORMAL] Truck speed initialized to "
					+ this.speed + ".");
		} else {
			// randomize speed accordingly
			double mean = 30;
			double std = 10;

			int tries = 0;
			while (this.speed < MIN_REASONABLE_SPEED
					|| this.speed > MAX_REASONABLE_SPEED) {
				this.speed = mean + std * rand.nextGaussian();
				tries++;
			}
			System.out.println("[NORMAL] Truck speed randomly initialized to "
					+ this.speed + " after " + tries + " tries.");
		}

		if (acceleration != RANDOMIZE_DOUBLE) {
			if (acceleration < MAX_ACCELERATION
					|| acceleration > MIN_ACCELERATION) {
				this.acceleration = acceleration;
				System.out
						.println("[NORMAL] Truck acceleration initialized to "
								+ this.acceleration + ".");
			} else {
				System.out
						.println("[SEVERE] Truck "
								+ truckNumber
								+ "'s engine has "
								+ "overheated and exploded from excessive acceleration/deceleration.("
								+ acceleration + ")");
				explode("Excessive acceleration/deceleration in initialization (allowable is "
						+ MIN_ACCELERATION + " to " + MAX_ACCELERATION + ")");
			}
		} else {
			// randomize acceleration
			this.acceleration = rand.nextDouble()
					* (MAX_ACCELERATION - MIN_ACCELERATION) + MIN_ACCELERATION;
			System.out
					.println("[NORMAL] Truck acceleration randomly initialized to "
							+ this.acceleration + ".");
		}
	}

	private void accelerate() throws FatalTruckException {
		if (acceleration <= MAX_ACCELERATION
				&& acceleration >= MIN_ACCELERATION) {
			// accelerate relative to tick rate
			this.speed = this.speed + (this.acceleration / (double) tickRate);
		} else {
			System.out
					.println("[SEVERE] Truck "
							+ truckNumber
							+ "'s engine has "
							+ "overheated and exploded from excessive acceleration/deceleration.");
			explode("Excessive acceleration/deceleration (allowable is "
					+ MIN_ACCELERATION + " to " + MAX_ACCELERATION + ")");
		}
	}

	private void explode(String reason) throws FatalTruckException {
		throw new FatalTruckException(reason);
	}

	// note: update position before acceleration
	private void updatePosition() {
		// update truck position relative to speed and tick rate
		this.pos = this.pos + (this.speed / tickRate);
	}

	// collisions of trucks is a responsibility of the air to decide
	public int updatePhysical() throws FatalTruckException {
		// update position
		updatePosition();
		// uncomment update speed
		accelerate();
		// update lane
		if (desiredLane != lane && intentChangeLane && !changingLanes) {
			new LaneChanger(desiredLane).start();
			changingLanes = true;
		}

		return 1;
	}

	public void updateDesires() throws FatalTruckException {

		// switch statement to enter state logic
		switch (truckAIState) {
		case NEW_TRUCK:
			// save initial stuff for later calculation purposes
			startPos = pos;
			desiredSpeed = speed;
			// predict approximate position relative to other trucks based on
			// start pos
			if (startPos <= 270) {
				probablyFirst = false;
			}
			if (startPos > 270) {
				probablyFirst = true;
			}

			// set desired speed to stabilizing speed
			desiredSpeed = STABILIZING_SPEED;
			// start stabilizing countdown timer using tick rate for reference
			stabilizingCountdown = tickRate
					* (int) Math.ceil(Math.max(MAX_REASONABLE_SPEED
							- STABILIZING_SPEED, STABILIZING_SPEED
							- MIN_REASONABLE_SPEED)
							/ Math.min(MAX_ACCELERATION, MIN_ACCELERATION));

			// move to next state
			truckAIState = STABILIZING;
			break;

		case STABILIZING:
			// if done stabilizing, become stabilized
			if (stabilizingCountdown <= 0) {
				truckAIState = STABILIZED;
			}
			stabilizingCountdown--;
			break;

		case STABILIZED:
			// // become a solo convoy and move over to lane 1
			//desiredLane = 1;
			truckAIState = SOLO_CONVOY;
			// NOTE: this state is here in case something else needs to happen
			// when tweaking AI code
			break;

		case SOLO_CONVOY:
			// search for a truck ahead
			for (int i = 0; i < truckCache.length; i++) {
				// NOTE: this biases forward merging
				if (truckInitialized[i] && truckNumber - 1 != i
						&& truckCache[i].getPos() > pos
						&& !truckCache[i].getConvoyID().equals(convoyID)
						&& truckCache[i].getLane() == lane) {
					// if a truck has been found ahead, change to merging convoy
					// and join the convoy ahead
					truckAIState = MERGING_CONVOY;
					convoyID = truckCache[i].getConvoyID();
					orderInConvoy = truckCache[i].getOrderInConvoy() + 1;
					break;
				}
			}

			// based on positional guess, accelerate or decelerate to find more
			// trucks
			// if not first, accelerate
			if (!probablyFirst) {
				desiredSpeed = SOLO_CATCHING_SPEED;
			}
			// if there is a truck in this convoy with position 5, become full
			// convoy
			for (int i = 0; i < truckCache.length; i++) {
				if (truckInitialized[i] && truckNumber - 1 != i
						&& !truckCache[i].getConvoyID().equals(convoyID)
						&& truckCache[i].getOrderInConvoy() == 5) {
					truckAIState = FULL_CONVOY;
					break;
				}
			}
			break;

		case MULTI_CONVOY:
			desiredSpeed = STABILIZING_SPEED;
			// if the leader of the convoy isn't the first truck,
			// accelerate while maintaining distance
			for (int i = 0; i < truckCache.length; i++) {
				if (truckInitialized[i] && truckNumber - 1 == i
						&& truckCache[i].getProbablyFirst()
						&& !truckCache[i].getConvoyID().equals(convoyID)) {
					desiredSpeed = MULTI_CATCHING_SPEED;
					break;
				}
			}
			// if another truck is found in a different convoy, attempt to
			// join their convoy if they are ahead (must disconnect from current
			// convoy and become solo for a tick)
			for (int i = 0; i < truckCache.length; i++) {
				// NOTE: this biases forward merging
				if (truckInitialized[i] && truckNumber - 1 != i
						&& truckCache[i].getPos() > pos
						&& !truckCache[i].getConvoyID().equals(convoyID)
						&& truckCache[i].getLane() == lane) {
					truckAIState = SOLO_CONVOY;
					break;
				}
			}
			// if there is a truck in this convoy with position 5, become full
			// convoy
			for (int i = 0; i < truckCache.length; i++) {
				if (truckInitialized[i] && truckNumber - 1 != i
						&& !truckCache[i].getConvoyID().equals(convoyID)
						&& truckCache[i].getOrderInConvoy() == 5) {
					truckAIState = FULL_CONVOY;
					break;
				}
			}
			break;

		case FULL_CONVOY:
			// TODO: if in a full convoy, broadcast end to end timing packets
			break;

		case MERGING_CONVOY:
			// TODO: try to close the gap to an acceptable range with the truck
			// in front of it
			Truck nextTruck = null;
			for (int i = 0; i < truckCache.length; i++) {
				if (truckInitialized[i]
						&& truckNumber - 1 != i
						&& !truckCache[i].getConvoyID().equals(convoyID)
						&& truckCache[i].getOrderInConvoy() == orderInConvoy - 1) {
					nextTruck = truckCache[i];
					break;
				}
			}
			if (nextTruck != null) {
				if (nextTruck.getPos() - pos < MAX_CONVOY_GAP - 7) {
					// once within acceptable range, become a MULTI_CONVOY
					truckAIState = MULTI_CONVOY;
					convoyID = nextTruck.getConvoyID();
				}
				if (nextTruck.getPos() - pos > MAX_CONVOY_GAP - 7) {
					desiredSpeed = MERGING_CATCHING_SPEED;
				}
			}
			break;

		default:
			break;
		}

		//logic to try to maintain a gap
		Truck nextTruck = null;
		for (int i = 0; i < truckCache.length; i++) {
			if (truckInitialized[i]
					&& truckNumber - 1 != i
					&& !truckCache[i].getConvoyID().equals(convoyID)
					&& truckCache[i].getOrderInConvoy() == orderInConvoy - 1) {
				nextTruck = truckCache[i];
			}
		}
		if (nextTruck.getPos() - pos < MIN_CONVOY_GAP) {
			desiredSpeed = desiredSpeed - 0.5;
		}
		if (nextTruck.getPos() - pos > MAX_CONVOY_GAP) {
			desiredSpeed = desiredSpeed + 0.2;
		}
		// logic to try to make truck its desired speed by modifying
		// acceleration
		if (this.speed < this.desiredSpeed) {
			if (this.acceleration < MAX_ACCELERATION - 0.2) {
				if (this.acceleration < 0) {
					this.acceleration = 0;
				} else {
					this.acceleration += 0.1;
				}
			}
		} else {
			if (this.acceleration > MIN_ACCELERATION + 0.4) {
				if (this.acceleration > 0) {
					this.acceleration = 0;
				} else {
					this.acceleration -= 0.2;
				}
			}
		}
		if (this.speed == this.desiredSpeed) {
			acceleration = 0;
		}
		
		//check for collision
		for (int i = 0; i < truckCache.length; i++) {
            if (truckInitialized[i] && truckNumber - 1 != i){
              	if(this.pos > truckCache[i].pos){ //if front is greater than front of other truck
              		if(this.pos - 25 > truckCache[i].pos){ // then make sure the rear is also greater than the front of other truck
              			//we're good
              		}
              		else{
              			explode("COLLISION! BOOOM!");
              		}
              	}
              	else if(this.pos < truckCache[i].pos)// else if the front is less then the front of the other truck
              		if(this.pos < truckCache[i].pos - 25){ // then make sure its front is less then the rear of the other truck
              			//we're good
              		}
              		else{
              			explode("COLLISION! BOOOM!");
              		}
				}
			}
		
		

		// TODO: change lanes if area is clear

	}

	private void updateCache(String[] message) throws NumberFormatException,
			FatalTruckException {
		int messageTruckNumber = Integer.decode(message[7]);
		if (truckInitialized[messageTruckNumber - 1]) {
			truckCache[messageTruckNumber - 1].setSequenceNumber(Integer
					.decode(message[0]));
			truckCache[messageTruckNumber - 1].setAcceleration(Double
					.parseDouble(message[4]));
			truckCache[messageTruckNumber - 1].setPos(Double
					.parseDouble(message[5]));
			truckCache[messageTruckNumber - 1].setSpeed(Double
					.parseDouble(message[6]));
			truckCache[messageTruckNumber - 1].setLane(Integer
					.decode(message[8]));
			truckCache[messageTruckNumber - 1].setDesiredLane(Integer
					.decode(message[9]));
			truckCache[messageTruckNumber - 1].setDesiredPlaceInConvoy(Integer
					.decode(message[10]));
			truckCache[messageTruckNumber - 1].setConvoyID(message[11]);
			truckCache[messageTruckNumber - 1].setOrderInConvoy(Integer
					.decode(message[12]));
			truckCache[messageTruckNumber - 1].setProbablyFirst(Boolean
					.parseBoolean(message[13]));
		} else {
			// initialize truck for cache
			truckCache[messageTruckNumber - 1] = new Truck(messageTruckNumber,
					Integer.decode(message[8]), Double.parseDouble(message[5]),
					Double.parseDouble(message[6]),
					Double.parseDouble(message[4]));
			// add other data
			truckCache[messageTruckNumber - 1].setSequenceNumber(Integer
					.decode(message[0]));
			truckCache[messageTruckNumber - 1].setDesiredLane(Integer
					.decode(message[9]));
			truckCache[messageTruckNumber - 1].setDesiredPlaceInConvoy(Integer
					.decode(message[10]));
			truckCache[messageTruckNumber - 1].setConvoyID(message[11]);
			truckCache[messageTruckNumber - 1].setOrderInConvoy(Integer
					.decode(message[12]));
			truckCache[messageTruckNumber - 1].setProbablyFirst(Boolean
					.parseBoolean(message[13]));
			truckInitialized[messageTruckNumber - 1] = true;
		}
		truckSequenceCache[messageTruckNumber - 1] = Integer.decode(message[0]);
	}

	public void startUDPListener(DatagramSocket airUDPSocket) {
		// start listener
		new UDPMessageListener(airUDPSocket).start();
	}

	public ArrayList<String> handleMessage() throws NumberFormatException,
			FatalTruckException {
		ArrayList<String> outBoundMessages = new ArrayList<String>();
		// check for messages on UDP from "The Air"

		if (!incomingUDPMessages.isEmpty()) {
			String[] messageToProcess;
			String tempMessage = "";

			while (!incomingUDPMessages.isEmpty()) {
				messageToProcess = incomingUDPMessages.remove().split(",");

				// determine if message is new
				int messageTruckNumber = Integer.decode(messageToProcess[7]);
				int messageSequenceNumber = Integer.decode(messageToProcess[0]);
				if (truckSequenceCache[messageTruckNumber - 1] < messageSequenceNumber
						&& messageTruckNumber != truckNumber) {

					// update local cache
					updateCache(messageToProcess);
					// check if not first place
					if (pos < Double.parseDouble(messageToProcess[5])) {
						probablyFirst = false;
					}

					// update previous hop
					messageToProcess[3] = "" + truckNumber;
					// return a string message to send to the air
					for (int i = 0; i < messageToProcess.length; i++) {
						tempMessage += messageToProcess[i];
						if (i != 0 && i != messageToProcess.length - 1) {
							tempMessage += ",";
						}
					}
					outBoundMessages.add(tempMessage);

				}
			}
		}

		// check if it is time to send a message about this truck
		if (((System.nanoTime() - lastMessageTime) / 1000000000.0) > (1.0 / (double) messagesPerSecond)) {
			// create a message
			outBoundMessages.add(createCSVMessage(truckNumber,
					airUDPSocket.getPort(), new String("0")));
			// update last message time
			lastMessageTime = System.nanoTime();
		}

		return outBoundMessages;
	}

	public String createCSVMessage(int previousHop, int sourcePort,
			String sourceAddress) {
		String message = "" + sequenceNumber + "," + sourceAddress + ","
				+ sourcePort + "," + previousHop + ","
				+ acceleration + "," + pos + ","
				+ speed + "," + truckNumber + "," + lane + ","
				+ desiredLane + "," + desiredPlaceInConvoy + "," + convoyID
				+ "," + orderInConvoy + "," + probablyFirst;
		sequenceNumber++;
		return message;

	}

	public void setLane(int lane) {
		this.lane = lane;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public void setOrderInConvoy(int orderInConvoy) {
		this.orderInConvoy = orderInConvoy;
	}

	public int getOrderInConvoy() {
		return orderInConvoy;
	}
	
	public int getTruckAIState() {
		return this.truckAIState;
	}
	// set messages per second
	public void setMPS(int mps) {
		this.messagesPerSecond = mps;
	}

	public void setAcceleration(double acceleration) {
		this.acceleration = acceleration;
	}

	public void setLaneIntent() {
		intentChangeLane = true;
	}

	public int getDesiredLane() {
		return desiredLane;
	}

	public void setDesiredLane(int desiredLane) {
		this.desiredLane = desiredLane;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public String getConvoyID() {
		return convoyID;
	}

	public void setConvoyID(String convoyID) {
		this.convoyID = convoyID;
	}

	public double getPos() {
		return pos;
	}

	public void setPos(double pos) {
		this.pos = pos;
	}

	public boolean getProbablyFirst() {
		return probablyFirst;
	}

	public void setProbablyFirst(boolean probablyFirst) {
		this.probablyFirst = probablyFirst;
	}

	public int getTruckNumber() {
		return truckNumber;
	}

	public void setTruckNumber(int truckNumber) {
		this.truckNumber = truckNumber;
	}

	public double getAcceleration() {
		return acceleration;
	}

	public double getSpeed() {
		return speed;
	}

	public int getLane() {
		return lane;
	}

	public int getDesiredPlaceInConvoy() {
		return desiredPlaceInConvoy;
	}

	public void setDesiredPlaceInConvoy(int desiredPlaceInConvoy) {
		this.desiredPlaceInConvoy = desiredPlaceInConvoy;
	}

	private static class UDPMessageListener extends Thread {

		public UDPMessageListener(DatagramSocket UDPSocket) {
			airUDPSocket = UDPSocket;
		}

		public void run() {
			byte[] receivedData = new byte[4024];
			try {
				while (true) {
					DatagramPacket receivedPacket = new DatagramPacket(
							receivedData, receivedData.length);
					airUDPSocket.receive(receivedPacket);
					System.out.println("[NORMAL] Received packet:"
							+ new String(receivedPacket.getData()));
					incomingUDPMessages
							.add(new String(receivedPacket.getData())
									.split("\n")[0]);
				}

				// TODO:close the socket
			} catch (IOException e) {
				System.out
						.println("[SEVERE] IOException in thread that handles message handoff.");
			}
		}
	}

	private class LaneChanger extends Thread {
		private int newLane;

		public LaneChanger(int newLane) {
			this.newLane = newLane;
		}

		public void run() {
			// lane changing takes five seconds
			try {
				TimeUnit.SECONDS.sleep(5);
				setLane(newLane);
				changingLanes = false;
			} catch (InterruptedException ex) {
				changingLanes = false;
				Thread.currentThread().interrupt();
			}
		}
	}
}
