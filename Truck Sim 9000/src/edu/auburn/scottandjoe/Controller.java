package edu.auburn.scottandjoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class Controller {
	public static final int TICK_RATE = 200;
	public static final int TRUCK_PORT = 10125;
	public static final int DAEMON_PORT = 10126;
	public static final int MAX_BUFFER_SIZE = 4096;
	public static final String TERMINATING_STRING = "\n";
	// constants for daemon message types
	public static final int REQUEST = 1;
	public static final int RESPONSE = 2;
	// constants for daemon request/response types
	public static final int ADDR_CACHE = 1;
	public static final int POS_CACHE = 2;
	public static final int TRUCK_POS = 3;
	public static final int START_SIM = 4;
	public static final int END_SIM = 5;
	public static final int TRUCK_POP = 6;

	private static int totalTrucks = 0;
	// pseudo constant. will be later controlled by args
	private static int desiredTruckSimPop = 3;
	private static boolean allTrucksConnected = false;
	private static boolean collision = false;
	private static double[] truckPosCache;
	private static long[] truckPosCacheTime;
	private static String[] truckAddresses;
	private static LinkedBlockingQueue<String>[] requests;
	private static LinkedBlockingQueue<String>[] receivedMessages;
	private static LinkedBlockingQueue<String> newConnections = new LinkedBlockingQueue<String>();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		final String ANSI_CLS = "\u001b[2J";
		final String ANSI_HOME = "\u001b[H";
		System.out.print(ANSI_CLS + ANSI_HOME);
		System.out.println("[NORMAL] Launching the controller.");
		requests = new LinkedBlockingQueue[desiredTruckSimPop];
		for (int i = 0; i < desiredTruckSimPop; i++) {
			requests[i] = new LinkedBlockingQueue<String>();
		}
		receivedMessages = new LinkedBlockingQueue[desiredTruckSimPop];
		for (int i = 0; i < desiredTruckSimPop; i++) {
			receivedMessages[i] = new LinkedBlockingQueue<String>();
		}
		truckAddresses = new String[desiredTruckSimPop];
		truckPosCache = new double[desiredTruckSimPop];
		Arrays.fill(truckPosCache, 0.0);
		truckPosCacheTime = new long[desiredTruckSimPop];

		// NOTE: port is constant, because there is not
		// much need for variable port at this time.
		// int port;
		// port = parsePortFromArgs(args);

		System.out.println("[NORMAL] Status: Running.");
		new UDPListener().start();

		new UserInputHandler().start();

		System.out.println("[NORMAL] Status: Waiting for connections.");
		ArrayList<String> connectedTrucks = new ArrayList<String>();
		try {
			while (!allTrucksConnected) {
				// TODO: adapt to standard
				String newConnection = newConnections.take();
				String truckNumberString = newConnection.split(",")[1];

				if (!connectedTrucks.contains(truckNumberString)) {
					// spawn new thread to handle each connection to allow for
					// simultaneous connections and therefore better response
					// times
					String address = newConnection.split(",")[2];
					int truckNumber = Integer.parseInt(truckNumberString);
					truckAddresses[truckNumber - 1] = address;
					new TruckDaemon(address, truckNumber).start();
					totalTrucks++;
					System.out.println("[NORMAL] " + totalTrucks
							+ " trucks connected.");
					if (totalTrucks == desiredTruckSimPop) {
						allTrucksConnected = true;
					}
				}

			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static boolean checkForCollision() {
		// check for collision
		for (int i = 0; i < truckPosCache.length - 1; i++) {
			for (int j = i + 1; j < truckPosCache.length; j++) {
				if (truckPosCache[i] != 0 && truckPosCache[j] != 0) {
					if (truckPosCache[j] < (truckPosCache[i] + Truck.TRUCK_LENGTH)
							&& truckPosCache[j] > (truckPosCache[i] - Truck.TRUCK_LENGTH)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private static int parsePortFromArgs(String[] arguments) {
		int port = 0;
		if (arguments.length == 0) {
			System.out
					.println("[SEVERE] Error: Please specify a port in the command line.");
			System.exit(0);
		}
		if (Integer.decode(arguments[0]) >= 1
				&& Integer.decode(arguments[0]) <= 65535) {
			port = Integer.decode(arguments[0]);
			System.out.println("[NORMAL] Status: Port successfully selected ("
					+ port + ")");
		} else {
			System.out
					.println("[SEVERE] Error in args: Invalid port number. Must be between 1 and 65535.");
			System.exit(0);
		}
		return port;
	}

	private static class TruckRequester extends Thread {
		private String address;
		private int truckNumber;

		public TruckRequester(String address, int truckNumber) {
			this.address = address;
			this.truckNumber = truckNumber;
		}

		public void run() {
			System.out
					.println("[NORMAL] TruckRequester for TruckDriver driving Truck "
							+ truckNumber + " started. Targeting: " + address);
			while (true) {
				try {
					String message = requests[truckNumber - 1].take();
					sendMessage(message);
					//System.out.print("[DEBUG] Message Sent:" + message);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		private void sendMessage(String message) {
			byte[] outBoundPacketBuf = new byte[4096];
			outBoundPacketBuf = message.getBytes();
			DatagramSocket daemonUDPSock;
			try {
				daemonUDPSock = new DatagramSocket();
				InetAddress truckDestination = InetAddress.getByName(address);
				DatagramPacket outBoundUDPPacket = new DatagramPacket(
						outBoundPacketBuf, outBoundPacketBuf.length,
						truckDestination, DAEMON_PORT);
				daemonUDPSock.send(outBoundUDPPacket);
				daemonUDPSock.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static class TruckDaemon extends Thread {
		private String address;
		private int truckNumber;

		public TruckDaemon(String address, int truckNumber) {
			this.address = address;
			this.truckNumber = truckNumber;
		}

		public void run() {
			try {
				System.out
						.println("[NORMAL] TruckDaemon for TruckDriver driving Truck "
								+ truckNumber
								+ " started. Listening To UDP messages from "
								+ address);
				new TruckRequester(address, truckNumber).start();

				while (true) {
					String receivedMessageWhole = receivedMessages[truckNumber - 1]
							.take();
					String receivedMessageTerminated = receivedMessageWhole
							.split(TERMINATING_STRING)[0];
					String[] receivedMessage = receivedMessageTerminated
							.split(",");

					int messageType = Integer.decode(receivedMessage[1]);
					int requestType = Integer.decode(receivedMessage[2]);

					if ((int) messageType == (int) REQUEST) {
						// handle request
						String message = "" + truckNumber + "," + RESPONSE
								+ "," + requestType + ",";
						switch (requestType) {
						case ADDR_CACHE:
							for (int i = 0; i < truckAddresses.length; i++) {
								message += truckAddresses[i];
								if (i != truckAddresses.length - 1) {
									message += ",";
								}
							}
							message += TERMINATING_STRING;
							sendMessage(message);
							break;
						case POS_CACHE:
							for (int i = 0; i < truckPosCache.length; i++) {
								message += truckPosCache[i];
								if (i != truckPosCache.length - 1) {
									message += ",";
								}
							}
							message += TERMINATING_STRING;
							sendMessage(message);
							break;
						case TRUCK_POP:
							message += desiredTruckSimPop + TERMINATING_STRING;
							sendMessage(message);
							break;
						default:
							break;
						}
					} else if ((int) messageType == (int) RESPONSE) {
						// handle incoming data
						switch (requestType) {
						case TRUCK_POS:
							double position = Double.parseDouble(receivedMessage[3]);
							if (position > truckPosCache[truckNumber - 1]) {
								truckPosCache[truckNumber - 1] = position;
								truckPosCacheTime[truckNumber - 1] = System
										.currentTimeMillis();
							}
							break;
						default:
							break;
						}
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void sendMessage(String message) {
			byte[] outBoundPacketBuf = new byte[4096];
			outBoundPacketBuf = message.getBytes();
			DatagramSocket daemonUDPSock;
			try {
				daemonUDPSock = new DatagramSocket();
				InetAddress truckDestination = InetAddress.getByName(address);
				DatagramPacket outBoundUDPPacket = new DatagramPacket(
						outBoundPacketBuf, outBoundPacketBuf.length,
						truckDestination, DAEMON_PORT);
				daemonUDPSock.send(outBoundUDPPacket);
				daemonUDPSock.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static class UserInputHandler extends Thread {
		private static Scanner scanner = new Scanner(System.in);
		private static String userInput = "";

		public UserInputHandler() {
		}

		public void run() {
			while (true) {
				waitForTrucksToConnect();
				System.out
						.println("[UIH] All trucks connected. Type \"start\" to begin the simulation.");
				waitForStartFromUser();

				while (true) {
					waitForCollisionFromTruck();
					Arrays.fill(truckPosCache, 0.0);
					System.out
							.println("[UIH] There was a collision. Type \"start\" to start a new simulation.");
					waitForStartFromUser();
				}

			}
		}

		private void waitForCollisionFromTruck() {
			while (!collision) {
				try {
					if (checkForCollision()) {
						System.out
								.println("[WARNING] Collision between trucks detected. Sending stop command.");
						for (int i = 0; i < requests.length; i++) {
							requests[i].put("" + (i+1) + "," 
									+ REQUEST + "," 
									+ END_SIM
									+ TERMINATING_STRING);
						}
						break;
					} else {
						for (int i = 0; i < requests.length; i++) {
							requests[i].put("" + (i+1) 
									+ "," + REQUEST 
									+ "," + TRUCK_POS
									+ TERMINATING_STRING);
						}
					}

					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private void waitForStartFromUser() {
			try {
				while (true) {
					userInput = scanner.nextLine();
					if (userInput.equals("start")) {
						for (int i = 0; i < requests.length; i++) {
							requests[i].put("" + (i+1) + "," 
									+ REQUEST + "," 
									+ START_SIM + "," 
									+ desiredTruckSimPop
									+ TERMINATING_STRING);
							System.out
									.println("[NORMAL] Sending start request to TruckDriver for Truck "
											+ (i + 1) + ".");
						}
						break;
					}
					System.out
							.println("[DEBUG] you didn't enter start or this loop is broken.");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void waitForTrucksToConnect() {
			while (!allTrucksConnected) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static class UDPListener extends Thread {
		public UDPListener() {

		}

		public void run() {
			// input for reading requests
			try {
				DatagramSocket daemonSocket = new DatagramSocket(DAEMON_PORT);
				while (true) {
					String receivedMessageWhole = "";
					byte[] receivedData = new byte[4096];
					DatagramPacket receivedPacket = new DatagramPacket(
							receivedData, receivedData.length);
					daemonSocket.receive(receivedPacket);
					receivedMessageWhole = new String(receivedPacket.getData());
					//System.out.print("[DEBUG] Message Received:"
					//		+ receivedMessageWhole);
					int truckNumber = Integer.parseInt(receivedMessageWhole
							.split(TERMINATING_STRING)[0].split(",")[0]);
					if (truckNumber == 101) {
						String address = receivedPacket.getAddress().toString()
								.split("/")[1].split(":")[0];
						System.out.println("[NORMAL] New connection detected.");
						newConnections.put(receivedMessageWhole
								.split(TERMINATING_STRING)[0] + "," + address);
					} else {
						receivedMessages[truckNumber - 1]
								.put(receivedMessageWhole);
					}
				}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
