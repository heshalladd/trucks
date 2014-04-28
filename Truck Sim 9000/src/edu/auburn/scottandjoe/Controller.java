package edu.auburn.scottandjoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class Controller {
	public static final int TICK_RATE = 100;
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

	private static int totalTrucks = 0;
	// pseudo constant. will be later controlled by args
	private static int desiredTruckSimPop = 5;
	private static boolean allTrucksConnected = false;
	private static boolean runSimulation = false;
	private static boolean collision = false;
	private static boolean[] collisionForTruck;
	private static int[] truckPosCache;
	private static long[] truckPosCacheTime;
	private static String[] truckAddresses;
	private static LinkedBlockingQueue<String> requests = new LinkedBlockingQueue<String>();

	public static void main(String[] args) {
		System.out.println("[NORMAL] Launching the controller.");
		collisionForTruck = new boolean[desiredTruckSimPop];
		truckAddresses = new String[desiredTruckSimPop];
		truckPosCache = new int[desiredTruckSimPop];
		truckPosCacheTime = new long[desiredTruckSimPop];

		// NOTE: port is constant, because there is not
		// much need for variable port at this time.
		// int port;
		// port = parsePortFromArgs(args);

		System.out.println("[NORMAL] Status: Running.");

		DatagramSocket server = null;

		new UserInputHandler().start();

		System.out.println("[NORMAL] Status: Waiting for connections.");
		ArrayList<String> connectedTrucks = new ArrayList<String>();
		try {
			while (true) {
				server = new DatagramSocket(DAEMON_PORT);
				byte[] receivedData = new byte[4096];
				DatagramPacket receivedPacket = new DatagramPacket(
						receivedData, receivedData.length);
				server.receive(receivedPacket);
				String truckNumberString = new String(receivedPacket.getData());
				if (!connectedTrucks.contains(truckNumberString)) {
					// spawn new thread to handle each connection to allow for
					// simultaneous connections and therefore better response
					// times
					int truckNumber = Integer.parseInt(truckNumberString);
					String address = receivedPacket.getAddress().toString()
							.split("/")[1].split(":")[0];
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
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

		public TruckRequester(String address) {
			this.address = address;
		}

		public void run() {
			while (true) {
				try {
					String message = requests.take();
					sendMessage(message);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		private void sendMessage(String message) {
			byte[] outBoundPacketBuf = new byte[4096];
			outBoundPacketBuf = message.getBytes();
			DatagramSocket forwardUDPSock;
			try {
				forwardUDPSock = new DatagramSocket();
				InetAddress truckDestination = InetAddress.getByName(address);
				DatagramPacket outBoundUDPPacket = new DatagramPacket(
						outBoundPacketBuf, outBoundPacketBuf.length,
						truckDestination, DAEMON_PORT);
				forwardUDPSock.send(outBoundUDPPacket);
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

	private static class TruckDaemon extends Thread {
		private String address;
		private int truckNumber;

		public TruckDaemon(String address, int truckNumber) {
			this.address = address;
			this.truckNumber = truckNumber;
		}

		public void run() {
			try {
				new TruckRequester(address).start();
				// input for reading requests
				DatagramSocket daemonSocket = new DatagramSocket();
				InetAddress remoteAddress = InetAddress.getByName(address);
				daemonSocket.connect(remoteAddress, DAEMON_PORT);

				while (true) {
					String receivedMessageWhole = "";
					byte[] receivedData = new byte[4096];
					DatagramPacket receivedPacket = new DatagramPacket(
							receivedData, receivedData.length);
					daemonSocket.receive(receivedPacket);
					receivedMessageWhole = new String(receivedPacket.getData());
					String receivedMessageTerminated = receivedMessageWhole
							.split(TERMINATING_STRING)[0];
					String[] receivedMessage = receivedMessageTerminated
							.split(",");

					int messageType = Integer.decode(receivedMessage[0]);
					int requestType = Integer.decode(receivedMessage[1]);

					if ((int) messageType == (int) REQUEST && runSimulation) {
						// handle request
						String message = "";
						switch (requestType) {
						case ADDR_CACHE:
							message = "" + RESPONSE + "," + requestType;
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
							message = "" + RESPONSE + "," + requestType;
							for (int i = 0; i < truckPosCache.length; i++) {
								message += truckPosCache[i];
								if (i != truckPosCache.length - 1) {
									message += ",";
								}
							}
							message += TERMINATING_STRING;
							sendMessage(message);
							break;
						default:
							break;
						}
					} else if ((int) messageType == (int) RESPONSE
							&& runSimulation) {
						// handle incoming data
						switch (requestType) {
						case TRUCK_POS:
							truckPosCache[truckNumber - 1] = Integer
									.parseInt(receivedMessage[2]);
							truckPosCacheTime[truckNumber - 1] = System
									.currentTimeMillis();
							break;
						default:
							break;
						}
					}
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void sendMessage(String message) {
			byte[] outBoundPacketBuf = new byte[4096];
			outBoundPacketBuf = message.getBytes();
			DatagramSocket forwardUDPSock;
			try {
				forwardUDPSock = new DatagramSocket();
				InetAddress truckDestination = InetAddress.getByName(address);
				DatagramPacket outBoundUDPPacket = new DatagramPacket(
						outBoundPacketBuf, outBoundPacketBuf.length,
						truckDestination, DAEMON_PORT);
				forwardUDPSock.send(outBoundUDPPacket);
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
					System.out
							.println("[UIH] There was a collision. Type \"start\" to start a new simulation.");
					waitForStartFromUser();
				}

			}
		}

		private void waitForCollisionFromTruck() {
			while (!collision) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private void waitForStartFromUser() {
			while (true) {
				userInput = scanner.nextLine();
				if (userInput.equals("start")) {
					collision = false;
					runSimulation = true;
					for (int i = 0; i < collisionForTruck.length; i++) {
						collisionForTruck[i] = false;
					}
					break;
				}
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
}
