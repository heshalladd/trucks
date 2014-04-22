package edu.auburn.scottandjoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class Truck {
	// TODO: General: track last status update time for each truck. Disregard
	// stale statuses for collision checks to prevent crashes with "phantoms"

	// physical constraints constants
	public static final double MAX_ACCELERATION = 1.0;
	public static final double MIN_ACCELERATION = -3.0;
	public static final int MAX_LANE = 1;
	public static final int MIN_LANE = 1;
	public static final double TRUCK_LENGTH = 25.0;

	// 80mph is 35.7m/s
	public static final double MAX_REASONABLE_SPEED = 35.7;
	// 55mph is 24.6m/s
	public static final double MIN_REASONABLE_SPEED = 24.6;

	// initialization parameter constants
	public static final int RANDOMIZE_INT = -10000;
	public static final double RANDOMIZE_DOUBLE = -10000.0;

	// imported constants
	private static final int TICK_RATE = Controller.TICK_RATE;
	private static final int PORT = Controller.PORT;

	// truck meta and ai related variables
	private int desiredLane;
	private int desiredPlaceInConvoy;
	private int orderInConvoy = 1; // 1 will signify leader of convoy
	private String convoyID = UUID.randomUUID().toString(); // id of convoy
	private static ConcurrentLinkedQueue<String> incomingUDPMessages = new ConcurrentLinkedQueue<String>();
	private boolean changingLanes = false;
	private boolean probablyFirst = false;
	private TruckAI theAI = new TruckAI();

	// message meta
	private int sequenceNumber = 1;
	private int messagesPerSecond = 50;
	private int messagesForwarded = 0;
	private int messagesDropped = 0;
	private int messagesCreated = 0;
	private int messagesSent = 0;
	private long lastMessageTime = 0l;
	private String lastMessage = "";
	private String lastMessageToForward = "";
	private DatagramSocket airUDPSocket;

	// truck properties
	private double acceleration;
	private double pos;
	private double speed = 0;
	private int truckNumber;
	private int lane;
	private boolean collision = false;

	// intents
	private boolean intentChangeLane = false;

	// caching
	private Truck[] truckCache = new Truck[5];
	private int[] truckSequenceCache = new int[5];
	private String[] truckAddresses = new String[5];

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
			// debug
			// this.speed = 31.3;
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
			this.speed = this.speed + (this.acceleration / (double) TICK_RATE);
		} else {
			System.out
					.println("[SEVERE] Truck "
							+ truckNumber
							+ "'s engine has overheated and exploded from excessive acceleration/deceleration.");
			explode("Excessive acceleration/deceleration (allowable is "
					+ MIN_ACCELERATION + " to " + MAX_ACCELERATION + ")");
		}
	}

	public void checkForCollision() throws FatalTruckException {
		// check for collision
		for (int i = 0; i < truckCache.length - 1; i++) {
			if (truckInitialized[i] && (truckNumber - 1) != i) {
				// check if truck is behind us
				if (pos > truckCache[i].getPos()) {
					// check if truck is crashing into us
					if (pos - TRUCK_LENGTH > truckCache[i].getPos()) {
						// we're good
					} else {
						System.out.println("[COLLISION] Truck " + (i + 1)
								+ " ran into  Truck " + (truckNumber) + "!");
						collision = true;
						explode("COLLISION! BOOOM!");
					}
				} else {
					if (pos < truckCache[i].getPos() - TRUCK_LENGTH) {
						// we're good
					} else {
						System.out.println("[COLLISION] Truck " + truckNumber
								+ " ran into  Truck " + (i + 1) + "!");
						collision = true;
						explode("COLLISION! BOOOM!");
					}
				}
			}
		}
	}

	// determines whether message will make it through using a mathematical
	// model
	public boolean isMessageSuccessful(Truck destinationTruck) {
		double chanceToSend = 0.0;
		double distanceApart = Math.abs(pos - destinationTruck.getPos());
		// piecewise equation for determining
		// transmission
		// probability
		if (distanceApart < 70) {
			chanceToSend = -0.002142857 * distanceApart + 1;
		} else if (distanceApart >= 70 && distanceApart < 100) {
			chanceToSend = -(0.00094 * Math.pow(distanceApart - 70, 2)) + 0.85;
		} else if (distanceApart >= 100) {
			chanceToSend = 0.0;
		}

		// roll the dice
		Random rand = new Random();
		if (chanceToSend >= rand.nextDouble()) {
			return true;
		} else {
			return false;
		}

	}

	public String createCSVMessage(int previousHop, int sourcePort,
			String sourceAddress) {
		String message = "" + sequenceNumber + "," + sourceAddress + ","
				+ sourcePort + "," + previousHop + "," + acceleration + ","
				+ pos + "," + speed + "," + truckNumber + "," + lane + ","
				+ desiredLane + "," + desiredPlaceInConvoy + "," + convoyID
				+ "," + orderInConvoy + "," + probablyFirst + "]";
		sequenceNumber++;
		return message;

	}

	public void explode(String reason) throws FatalTruckException {
		throw new FatalTruckException(reason);
	}

	public double getAcceleration() {
		return acceleration;
	}

	public String getConvoyID() {
		return convoyID;
	}

	public int getConvoySize() {
		int size = orderInConvoy;
		for (int i = 0; i < truckCache.length; i++) {
			if (truckInitialized[i] && truckCache[i].getOrderInConvoy() > size) {
				size = truckCache[i].getOrderInConvoy();
			}
		}
		return size;
	}

	public int getDesiredLane() {
		return desiredLane;
	}

	public int getDesiredPlaceInConvoy() {
		return desiredPlaceInConvoy;
	}

	public int getLane() {
		return lane;
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public String getLastMessageToForward() {
		return lastMessageToForward;
	}

	public int getMessagesCreated() {
		return messagesCreated;
	}

	public int getMessagesDropped() {
		return messagesDropped;
	}

	public int getMessagesForwarded() {
		return messagesForwarded;
	}

	public int getMessagesSent() {
		return messagesSent;
	}

	public double getNextTruckPos() {
		Truck nextTruck = null;
		Truck[] truckCacheLocal = truckCache.clone();
		double nextTruckPos = 999999.0;
		for (int i = 0; i < truckCacheLocal.length; i++) {
			if (truckInitialized[i] && truckNumber - 1 != i
					&& truckCacheLocal[i].getPos() < nextTruckPos
					&& truckCacheLocal[i].getPos() > pos) {
				nextTruck = truckCacheLocal[i];
				nextTruckPos = truckCacheLocal[i].getPos();
				break;
			}
		}
		if (nextTruck == null) {
			return 0;
		} else {
			return nextTruckPos;
		}
	}

	public int getOrderInConvoy() {
		return orderInConvoy;
	}

	public double getPos() {
		return pos;
	}

	public boolean getProbablyFirst() {
		return probablyFirst;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public double getSpeed() {
		return speed;
	}

	public int getTruckAIState() {
		return this.theAI.getAIState();
	}

	public Truck[] getTruckCache() {
		return truckCache;
	}

	public boolean[] getTruckInitialized() {
		return truckInitialized;
	}

	public int getTruckNumber() {
		return truckNumber;
	}

	// TODO: make sure the bug from pre-post-due-dev is gone (random additions)
	// TODO: keep the invalid message checks. those are still good
	public void handleMessages() throws NumberFormatException,
			FatalTruckException {
		// sif this is the first time, let everyone know your starting position
		if (lastMessageTime == 0l) {
			// create a message
			String newMessage = createCSVMessage(truckNumber,
					airUDPSocket.getPort(), new String("0"));
			// update last message time
			lastMessageTime = System.nanoTime();
			sendGenesisBroadcast(newMessage);
			lastMessage = newMessage;
		}

		// check for messages on UDP handler's buffer
		if (!incomingUDPMessages.isEmpty()) {
			String[] messageToProcess;
			String messageToProcessWhole;
			String tempMessage = "";

			while (!incomingUDPMessages.isEmpty()) {
				messageToProcessWhole = incomingUDPMessages.remove();
				messageToProcess = messageToProcessWhole.split(",");
				// truncate last message segment

				// determine if message is new
				int messageTruckNumber = Integer.decode(messageToProcess[7]);
				int messageSequenceNumber = Integer.decode(messageToProcess[0]);
				if (isMessageNew(messageTruckNumber, messageSequenceNumber)) {
					tempMessage = "";
					// update local cache
					updateCache(messageToProcess);
					// check if not first place
					if (pos < Double.parseDouble(messageToProcess[5])) {
						probablyFirst = false;
					}

					// update previous hop
					int previousHop = Integer.decode(messageToProcess[3]);
					messageToProcess[3] = "" + truckNumber;
					if (Integer.decode(messageToProcess[3]) != messageTruckNumber) {
						lastMessageToForward = messageToProcessWhole;
					}
					// form the new message as a string
					for (int i = 0; i < messageToProcess.length; i++) {
						tempMessage += messageToProcess[i];
						if (i != 0 && i != messageToProcess.length - 1) {
							tempMessage += ",";
						}
					}

					ArrayList<Truck> trucksInRange = new ArrayList<Truck>();

					// determine what trucks are valid and in range
					for (int i = 0; i < truckCache.length; i++) {
						if (i != previousHop - 1 && i != truckNumber - 1
								&& i != messageTruckNumber
								&& truckInitialized[i]) {
							trucksInRange.add(truckCache[i]);
						}
					}

					// determine whether those messages are going to make it
					// through
					if (trucksInRange.size() > 0) {
						for (int i = 0; i < trucksInRange.size(); i++) {
							// roll the dice
							Truck targetTruck = trucksInRange.get(i);
							if (isMessageSuccessful(targetTruck)) {
								sendMessage(targetTruck, tempMessage);
								messagesForwarded++;
							}
						}
					}
				} else {
					messagesDropped++;
				}
			}
		}

		// check if it is time to send a message about this truck
		if (((System.nanoTime() - lastMessageTime) / 1000000000.0) > (1.0 / (double) messagesPerSecond)) {
			// create a message
			String newMessage = createCSVMessage(truckNumber,
					airUDPSocket.getPort(), new String("0"));
			// update last message time
			lastMessageTime = System.nanoTime();
			messagesCreated++;

			// find trucks in range
			ArrayList<Truck> trucksInRange = new ArrayList<Truck>();

			// determine what trucks are in range
			for (int i = 0; i < truckCache.length; i++) {
				if (i != truckNumber - 1 && truckInitialized[i]) {
					trucksInRange.add(truckCache[i]);
				}
			}
			if (trucksInRange.size() > 0) {
				for (int i = 0; i < trucksInRange.size(); i++) {
					// roll the dice
					Truck targetTruck = trucksInRange.get(i);
					if (isMessageSuccessful(targetTruck)) {
						sendMessage(targetTruck, newMessage);
						messagesSent++;
						lastMessage = newMessage;
					}
				}
			}
		}
	}

	private boolean isMessageNew(int messageTruckNumber,
			int messageSequenceNumber) {
		return (truckSequenceCache[messageTruckNumber - 1] < messageSequenceNumber && messageTruckNumber != truckNumber);
	}

	private void sendGenesisBroadcast(String message) {
		// get address and port for sending stuff via
		// UDP.
		for (int i = 0; i < truckAddresses.length; i++) {
			byte[] outBoundPacketBuf = new byte[4028];
			outBoundPacketBuf = message.getBytes();
			DatagramSocket forwardUDPSock;
			try {
				forwardUDPSock = new DatagramSocket();
				InetAddress truckDestination = InetAddress
						.getByName(truckAddresses[i]);
				DatagramPacket outBoundUDPPacket = new DatagramPacket(
						outBoundPacketBuf, outBoundPacketBuf.length,
						truckDestination, PORT);

				// forward transmissions (that qualify) to
				// their
				// hosts as UDP.
				forwardUDPSock.send(outBoundUDPPacket);

				// close socket
				forwardUDPSock.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendMessage(Truck targetTruck, String message) {
		// get address and port for sending stuff via
		// UDP.
		byte[] outBoundPacketBuf = new byte[4028];
		outBoundPacketBuf = message.getBytes();
		DatagramSocket forwardUDPSock;
		try {
			forwardUDPSock = new DatagramSocket();
			InetAddress truckDestination = InetAddress
					.getByName(truckAddresses[targetTruck.getTruckNumber() - 1]);
			DatagramPacket outBoundUDPPacket = new DatagramPacket(
					outBoundPacketBuf, outBoundPacketBuf.length,
					truckDestination, PORT);

			// forward transmissions (that qualify) to
			// their
			// hosts as UDP.
			forwardUDPSock.send(outBoundUDPPacket);

			// close socket
			forwardUDPSock.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setAcceleration(double acceleration) {
		this.acceleration = acceleration;
	}

	public void setConvoyID(String convoyID) {
		this.convoyID = convoyID;
	}

	public void setDesiredLane(int desiredLane) {
		this.desiredLane = desiredLane;
	}

	public void setDesiredPlaceInConvoy(int desiredPlaceInConvoy) {
		this.desiredPlaceInConvoy = desiredPlaceInConvoy;
	}

	public void setLane(int lane) {
		this.lane = lane;
	}

	public void setLaneIntent() {
		intentChangeLane = true;
	}

	// set messages per second
	public void setMPS(int mps) {
		this.messagesPerSecond = mps;
	}

	public void setOrderInConvoy(int orderInConvoy) {
		this.orderInConvoy = orderInConvoy;
	}

	public void setPos(double pos) {
		this.pos = pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public void setProbablyFirst(boolean probablyFirst) {
		this.probablyFirst = probablyFirst;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public void setTruckAddresses(String[] addresses) {
		this.truckAddresses = addresses;
	}

	public void setTruckNumber(int truckNumber) {
		this.truckNumber = truckNumber;
	}

	public void startUDPListener(DatagramSocket airUDPSocket) {
		// start listener
		this.airUDPSocket = airUDPSocket;
		new UDPMessageListener().start();
	}

	public void stopUDPListener() {
		// close socket to force the listener to stop blocking and quit to catch
		// statement and end the thread
		airUDPSocket.close();
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
					.parseBoolean(message[13].split("]")[0]));
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
					.parseBoolean(message[13].split("]")[0]));
			truckInitialized[messageTruckNumber - 1] = true;
		}
		truckSequenceCache[messageTruckNumber - 1] = Integer.decode(message[0]);
	}

	public void updateDesires() throws FatalTruckException {
		handleMessages();
		// call the truck AI class
		// NOTE: only call this once the caches are stable for best results
		theAI.doAI(this);
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
		if (!collision) {
			checkForCollision();
		}

		return 1;
	}

	// note: update position before acceleration
	private void updatePosition() {
		// update truck position relative to speed and tick rate
		this.pos = this.pos + (this.speed / TICK_RATE);
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

	private class UDPMessageListener extends Thread {

		public UDPMessageListener() {
		}

		public void run() {
			byte[] receivedData = new byte[4024];
			try {
				while (true) {
					DatagramPacket receivedPacket = new DatagramPacket(
							receivedData, receivedData.length);
					airUDPSocket.receive(receivedPacket);
					// System.out.println("[NORMAL] Received packet:"
					// + new String(receivedPacket.getData()));
					incomingUDPMessages
							.add(new String(receivedPacket.getData())
									.split("\n")[0]);
				}
			} catch (IOException e) {
				System.out
						.println("[SEVERE] IOException in thread that handles message handoff.");
			}
		}
	}
}
