package edu.auburn.scottandjoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

public class Truck {
	// physical constraints constants
	public static final double MAX_ACCELERATION = 1.0;
	public static final double MIN_ACCELERATION = -3.0;
	public static final int MAX_LANE = 2;
	public static final int MIN_LANE = 1;
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
	private static final int NEW_TRUCK = 0;
	private static final int STABILIZING = 1;
	private static final int STABILIZED = 2;
	private static final int SOLO_CONVOY = 3;
	private static final int MULTI_CONVOY = 4;
	private static final int FULL_CONVOY = 5;
	private static final int COLLIDED = 6;
	private static final int MERGING_CONVOY = 7;

	// ai constants
	private static final double STABILIZING_SPEED = 31.3;
	private static final double CATCHING_SPEED = 33.5;

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
	private static ConcurrentLinkedDeque<String> incomingUDPMessages = new ConcurrentLinkedDeque<String>();
	private boolean changingLanes = false;
	private boolean probablyFirst = false;

	// message meta
	private int sequenceNumber = 1;
	private int messagesPerSecond = 100;
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
				explode("Truck was initialized in an invalid lane (allowable is "
						+ MIN_LANE + " to " + MAX_LANE);
			}
			this.lane = lane;
			this.desiredLane = lane;
			System.out.println("[NORMAL] Truck lane initialized to "
					+ this.lane + ".");
		} else {
			// randomize lane accordingly
			lane = rand.nextInt((MAX_LANE - MIN_LANE) + 1) + MIN_LANE;
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
			if (acceleration > MAX_ACCELERATION
					|| acceleration < MIN_ACCELERATION) {
				this.acceleration = acceleration;
				System.out
						.println("[NORMAL] Truck acceleration initialized to "
								+ this.acceleration + ".");
			} else {
				System.out
						.println("[SEVERE] Truck "
								+ truckNumber
								+ "'s engine has "
								+ "overheated and exploded from excessive acceleration/deceleration.");
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
			speed = speed + (acceleration / (double) tickRate);
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
		this.pos = this.pos + this.speed / tickRate;
	}

	// collisions of trucks is a responsibility of the air to decide
	public int updatePhysical() throws FatalTruckException {
		// update position
		updatePosition();
		// update speed
		accelerate();
		// update lane
		if (desiredLane != lane && intentChangeLane && !changingLanes) {
			new LaneChanger(desiredLane).start();
			changingLanes = true;
		}

		return 1;
	}

	public void updateDesires() {
		// TODO:judge current state of surroundings and figure out next step
		// towards forming a convoy (core ai)

		// TODO: switch statement to enter state logic
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
			if (stabilizingCountdown == 0) {
				truckAIState = STABILIZED;
			}
			stabilizingCountdown--;
			break;

		case STABILIZED:
			// become a solo convoy and move over to lane 1
			desiredLane = 1;
			truckAIState = SOLO_CONVOY;
			// NOTE: this state is here in case something else needs to happen
			// when tweaking AI code
			break;

		case SOLO_CONVOY:
			// based on positional guess, accelerate or decelerate to find more
			// trucks

			// if not first, accelerate
			if (!probablyFirst) {
				desiredSpeed = CATCHING_SPEED;
			}
			// TODO: if a truck has been found ahead, change to multi convoy and
			// change convoy state to merging and join the convoy ahead
			// TODO: bias forward merging
			break;

		case MULTI_CONVOY:
			// TODO: if the leader of the convoy isn't the first truck, accelerate while maintaining distance
			// TODO: if another truck is found in a different convoy, attempt to join their convoy if
			// they are ahead (must disconnect from current convoy and become
			// solo for a tick)
			// TODO: if total trucks in multi convoy is same as max trucks,
			// become full convoy
			break;

		case FULL_CONVOY:
			// TODO: if in a full convoy, broadcast end to end timing packets
			break;

		case MERGING_CONVOY:
			// TODO: try to close the gap to an acceptable range with the truck
			// in front of it
			break;
		case COLLIDED:
			// TODO: if there was a collision, explode appropriately
			break;

		default:
			// TODO: explode appropriately for not being in a recognizeable
			// state
		}

		// TODO: add logic to try to make truck its desired speed by modifying
		// acceleration
		// TODO: if stabilizing countdown != 0 do stuff to stabilize
		// TODO: change lanes if area is clear
		// TODO: if distance to next forward truck is not the margin, try to get
		// there
		// NOTE: only do this if it is solo or multi convoy and is in the same
		// lane

	}

	private void updateCache(String[] message) {
		int messageTruckNumber = Integer.decode(message[7]);
		truckSequenceCache[messageTruckNumber - 1] = Integer.decode(message[0]);
		truckCache[messageTruckNumber - 1].setAcceleration(Double
				.parseDouble(message[4]));
		truckCache[messageTruckNumber - 1].setPos(Double
				.parseDouble(message[5]));
		truckCache[messageTruckNumber - 1].setSpeed(Double
				.parseDouble(message[6]));
		truckCache[messageTruckNumber - 1].setLane(Integer.decode(message[8]));
		truckCache[messageTruckNumber - 1].setDesiredLane(Integer
				.decode(message[9]));
		truckCache[messageTruckNumber - 1].setDesiredPlaceInConvoy(Integer
				.decode(message[10]));
		truckCache[messageTruckNumber - 1].setConvoyID(message[11]);
		truckCache[messageTruckNumber - 1].setOrderInConvoy(Integer
				.decode(message[12]));
	}

	public void startUDPListener(DatagramSocket airUDPSocket) {
		// start listener
		new UDPMessageListener(airUDPSocket).start();
	}

	public ArrayList<String> handleMessage() {
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
		if (((System.nanoTime() - lastMessageTime) / 1000000) > (1 / messagesPerSecond)) {
			// create a message
			outBoundMessages.add(createCSVMessage(truckNumber,
					airUDPSocket.getPort(), "0"));
			// update last message time
			lastMessageTime = System.nanoTime();
		}

		return outBoundMessages;
	}

	public String createCSVMessage(int previousHop, int sourcePort,
			String sourceAddress) {

		DecimalFormat df = new DecimalFormat("#.0000");
		String message = "" + sequenceNumber + "," + sourceAddress + ","
				+ sourcePort + "," + previousHop + ","
				+ df.format(acceleration) + "," + df.format(pos) + ","
				+ df.format(speed) + "," + truckNumber + "," + lane + ","
				+ desiredLane + "," + desiredPlaceInConvoy + "," + convoyID
				+ "," + orderInConvoy;
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
