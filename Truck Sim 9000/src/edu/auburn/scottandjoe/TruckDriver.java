package edu.auburn.scottandjoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class TruckDriver {
	// imported constants
	private static final int TICK_RATE = Controller.TICK_RATE;
	private static final String TERMINATING_STRING = Controller.TERMINATING_STRING;

	// local variables
	private static boolean running = false;
	private static String visualizerAddress = "";
	private static Truck theTruck;
	private static int desiredTruckSimPop = 0;
	private static long lastTickTime = 0l;
	private static long lastTickInterval = 0l;

	// variables for reinitializing
	private static int initTruckNumber;
	private static int floodingAlgorithmType = 0;
	private static int initLane;
	private static double initPos;
	private static double initSpeed;
	private static double initAcceleration;
	private static boolean initialized = false;
	private static LinkedBlockingQueue<String> requests = new LinkedBlockingQueue<String>();

	/**
	 * @param args
	 * @throws IOException
	 */

	// args[0] - "the controller" ip
	// args[1] - "the viewer" ip
	// args[2] - truck number (1 - X)
	// args[3] - start pos
	// args[4] - 1 for basic FA, 2 for MPR FA
	public static void main(String[] args) throws IOException {
		final String ANSI_CLS = "\u001b[2J";
		final String ANSI_HOME = "\u001b[H";
		System.out.print(ANSI_CLS + ANSI_HOME);
		String controllerAddress = args[0];
		visualizerAddress = args[1];
		System.out.println("[NORMAL] Controller address set to "
				+ controllerAddress);
		initTruckNumber = Integer.decode(args[2]);
		System.out.println("[NORMAL] Truck Number set to " + initTruckNumber);
		initLane = Truck.RANDOMIZE_INT;
		//TEST
		if(Integer.parseInt(args[3]) == -1) {
			initPos = Truck.RANDOMIZE_DOUBLE;
		} else {
			initPos = Double.parseDouble(args[3]);
		}
		System.out.println("[NORMAL] Starting position set to " + initPos);
		floodingAlgorithmType = Integer.parseInt(args[4]);
		// double pos = Truck.RANDOMIZE_DOUBLE;
		initSpeed = Truck.RANDOMIZE_DOUBLE;
		initAcceleration = Truck.RANDOMIZE_DOUBLE;
		// TODO:add feature: load from config

		// let server know you exist by sending truck number
		InetAddress addr = InetAddress.getByName(controllerAddress);
		byte[] outBoundPacketBuf = new byte[4096];
		System.out.println("[NORMAL] Letting Controller know that I exist.");
		outBoundPacketBuf = new String("" + 101 + "," + initTruckNumber
				+ TERMINATING_STRING).getBytes();
		DatagramSocket forwardUDPSock;
		try {
			forwardUDPSock = new DatagramSocket();
			DatagramPacket outBoundUDPPacket = new DatagramPacket(
					outBoundPacketBuf, outBoundPacketBuf.length, addr,
					Controller.DAEMON_PORT);
			forwardUDPSock.send(outBoundUDPPacket);
			forwardUDPSock.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("[NORMAL] Controller poked.");
		// start the daemon for receiving requests and responses from controller
		System.out.println("[NORMAL] Starting Controller Daemon.");
		new ControllerDaemon(controllerAddress).start();

		// start logic loop
		System.out.println("[NORMAL] Starting thread for truck logic looping.");
		new TruckLogicLooper().start();

		// start ui thread
		// NOTE: Disable for performance. Visualizer is the new UI
		// DEBUG: Enabled for testing
		System.out.println("[NORMAL] Preparing UI thread.");
		new UIThread().start();
		System.out.println("[NORMAL] Waiting for start command.");
	}

	private static class ControllerDaemon extends Thread {
		private String controllerAddress;

		public ControllerDaemon(String controllerAddress) {
			this.controllerAddress = controllerAddress;
		}

		public void run() {
			try {
				System.out
						.println("[NORMAL] ControllerDaemon started. Listening for UDP messages from "
								+ controllerAddress);
				new ControllerRequester(controllerAddress).start();
				// input for reading requests
				DatagramSocket daemonSocket = new DatagramSocket(
						Controller.DAEMON_PORT);

				while (true) {
					String receivedMessageWhole = "";
					byte[] receivedData = new byte[4096];
					DatagramPacket receivedPacket = new DatagramPacket(
							receivedData, receivedData.length);
					daemonSocket.receive(receivedPacket);
					receivedMessageWhole = new String(receivedPacket.getData());
					// System.out.print("[DEBUG] Message Received:"
					// + receivedMessageWhole);
					String receivedMessageTerminated = receivedMessageWhole
							.split(TERMINATING_STRING)[0];
					String[] receivedMessage = receivedMessageTerminated
							.split(",");

					int messageType = Integer.decode(receivedMessage[1]);
					int requestType = Integer.decode(receivedMessage[2]);

					if ((int) messageType == (int) Controller.REQUEST) {
						// handle request
						String response = "" + initTruckNumber + ","
								+ Controller.RESPONSE + "," + requestType + ",";
						switch (requestType) {
						case Controller.START_SIM:
							System.out
									.println("[NORMAL] Received start command.");
							running = true;
							// get the number of trucks
							desiredTruckSimPop = Integer
									.parseInt(receivedMessage[3]);
							// initialize truck
							System.out.println("[NORMAL] Creating new truck.");
							theTruck = null;
							FloodingAlgorithm floodingAlgorithm = null;
							if (floodingAlgorithmType == 1) {
								floodingAlgorithm = new BasicFloodingAlgorithm();
							} else if (floodingAlgorithmType == 2) {
								// TODO: set to the MPR FA
							}
							theTruck = new Truck(initTruckNumber, initLane,
									initPos, initSpeed, initAcceleration,
									floodingAlgorithm, desiredTruckSimPop);
							System.out
									.println("[NORMAL] Starting truck object's UDP listener.");
							theTruck.startUDPListener();
							requests.put("" + initTruckNumber + ","
									+ Controller.REQUEST + ","
									+ Controller.ADDR_CACHE
									+ TERMINATING_STRING);
							break;
						case Controller.END_SIM:
							System.out
									.println("[NORMAL] Received end command.");
							running = false;
							System.out
									.println("[NORMAL] Stopping truck object's UDP listener.");
							theTruck.stopUDPListener();
							initialized = false;
							break;
						case Controller.TRUCK_POS:
							if (initialized) {
								response += theTruck.getPos()
										+ TERMINATING_STRING;
								sendMessage(response);
							}
							break;
						default:
							break;
						}
					} else if ((int) messageType == (int) Controller.RESPONSE) {
						// handle incoming data
						switch (requestType) {
						case Controller.POS_CACHE:
							double[] receivedPosCache = new double[desiredTruckSimPop];
							for (int i = 0; i < receivedPosCache.length; i++) {
								receivedPosCache[i] = Double
										.parseDouble(receivedMessage[i + 3]);
							}
							theTruck.setTruckPosCache(receivedPosCache);
							break;
						case Controller.ADDR_CACHE:
							String[] receivedAddresses = new String[desiredTruckSimPop];
							System.out.print("[NORMAL] Receiving Addresses: ");
							for (int i = 0; i < receivedAddresses.length; i++) {
								receivedAddresses[i] = receivedMessage[i + 3];
								System.out.print(i + "-" + receivedAddresses[i]
										+ "\n");
							}
							System.out
									.println("[NORMAL] All addresses received.");
							// set the truck addresses in the truck object
							theTruck.setTruckAddresses(receivedAddresses);
							initialized = true;
							break;
						case Controller.TRUCK_POP:
							desiredTruckSimPop = Integer
									.parseInt(receivedMessage[3]);
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
			} catch (FatalTruckException e) {
				System.out.println("[CRITICAL] " + e);
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
				InetAddress truckDestination = InetAddress
						.getByName(controllerAddress);
				DatagramPacket outBoundUDPPacket = new DatagramPacket(
						outBoundPacketBuf, outBoundPacketBuf.length,
						truckDestination, Controller.DAEMON_PORT);
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

	private static class ControllerRequester extends Thread {
		private String controllerAddress;

		public ControllerRequester(String controllerAddress) {
			this.controllerAddress = controllerAddress;
		}

		public void run() {
			System.out
					.println("[NORMAL] Controller requester started. Targeting: "
							+ controllerAddress);
			while (true) {
				try {
					String message = requests.take();
					sendMessage(message);
					// System.out.print("[DEBUG] Message Sent:" + message);
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
				InetAddress controllerDestination = InetAddress
						.getByName(controllerAddress);
				DatagramPacket outBoundUDPPacket = new DatagramPacket(
						outBoundPacketBuf, outBoundPacketBuf.length,
						controllerDestination, Controller.DAEMON_PORT);
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

	private static class TruckLogicLooper extends Thread {

		public TruckLogicLooper() {
		}

		public void run() {
			long tickStart = 0l;
			long tickEnd = 0l;
			HashMap<Integer, String> AIMap = new HashMap<Integer, String>();
			AIMap.put(0, "NEW_TRUCK");
			AIMap.put(1, "STABILIZING");
			AIMap.put(2, "STABILIZED");
			AIMap.put(3, "SOLO_CONVOY");
			AIMap.put(4, "MULTI_CONVOY");
			AIMap.put(5, "FULL_CONVOY");
			AIMap.put(6, "COLLIDED");
			AIMap.put(7, "MERGING_CONVOY");
			// TODO: add some tools to save and output average or current tick
			// computation time
			// in order to better fine tune the tick rate as low as possible
			while (true) {
				try {
					while (!running) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					// while loop to do tick limited truck updates
					long lastVisMessageTime = 0l;
					
					while (running && initialized) {
						long theTime = System.nanoTime();
						if(((System.nanoTime() - lastVisMessageTime) / 1000000000.0) > (1.0 / (double) Controller.VIS_SEND_RATE)) {
							DecimalFormat df = new DecimalFormat("#0.00");
							String visMessage = "" + theTruck.getTruckNumber() + ","
									+ (int)theTruck.getPos() + ","
									+ theTruck.getConvoyID() + ","
									+ "Truck Number: " + theTruck.getTruckNumber()
									+ "\n" + theTruck.getConvoyID()
									+ "\nOrder: " + theTruck.getOrderInConvoy()
									+ "\nSize: " + theTruck.getConvoySize()
									+ "\nPosition: " + (int)theTruck.getPos()
									+ "\nNext Truck Gap: " + (int)(theTruck.getNextTruckPos() -  theTruck.getPos() - Truck.TRUCK_LENGTH)
									+ "\nSpeed: " + df.format((theTruck.getSpeed()*2.23694)) + "mph"
									+ "\nAcceleration: " + df.format(theTruck.getAcceleration())
									+ "\nAIState: " + AIMap.get(theTruck.getTruckAIState())
									+ TERMINATING_STRING;
							sendVisMessage(visMessage);
							lastVisMessageTime = System.nanoTime();
							//System.out.print("[DEBUG] VIS ADDR:" + visualizerAddress + "||VIS PORT:" + Controller.VISUALIZER_PORT + "||VIS MSG:" + visMessage);
						}
						lastTickInterval = (theTime - tickStart);
						tickStart = theTime;
						theTruck.updateMental();
						theTruck.updatePhysical();
						requests.put("" + initTruckNumber + ","
								+ Controller.REQUEST + ","
								+ Controller.POS_CACHE + TERMINATING_STRING);
						tickEnd = System.nanoTime();
						lastTickTime = (tickEnd - tickStart);
						while (((System.nanoTime() - tickStart) / 1000000000.0) < (1.0 / (double) TICK_RATE)) {
						}
					}
					if (!running) {
						System.out
								.println("[NORMAL] Waiting for start command.");
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} catch (FatalTruckException e) {
					// TODO: handle
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		private void sendVisMessage(String message) {
			byte[] outBoundPacketBuf = new byte[4096];
			outBoundPacketBuf = message.getBytes();
			DatagramSocket visualizerUDPSock;
			try {
				visualizerUDPSock = new DatagramSocket();
				InetAddress visDestination = InetAddress
						.getByName(visualizerAddress);
				DatagramPacket outBoundUDPPacket = new DatagramPacket(
						outBoundPacketBuf, outBoundPacketBuf.length,
						visDestination, Controller.VISUALIZER_PORT);
				visualizerUDPSock.send(outBoundUDPPacket);
				visualizerUDPSock.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static class UIThread extends Thread {
		long UITickStart = 0l;
		private static final int UI_TICK_RATE = 1;

		public UIThread() {
		}

		public void run() {
			HashMap<Integer, String> AIMap = new HashMap<Integer, String>();
			AIMap.put(0, "NEW_TRUCK");
			AIMap.put(1, "STABILIZING");
			AIMap.put(2, "STABILIZED");
			AIMap.put(3, "SOLO_CONVOY");
			AIMap.put(4, "MULTI_CONVOY");
			AIMap.put(5, "FULL_CONVOY");
			AIMap.put(6, "COLLIDED");
			AIMap.put(7, "MERGING_CONVOY");
			DecimalFormat df = new DecimalFormat("#0.00");
			DecimalFormat df2 = new DecimalFormat("#0.000000");
			while (true) {
				UITickStart = System.nanoTime();
				if (running && initialized) {
					ArrayList<Truck> truckList = new ArrayList<Truck>();
					for (int i = 0; i < theTruck.getTruckInitialized().length; i++) {
						if (theTruck.getTruckInitialized()[i]) { // make sure
																	// you
																	// only
																	// display
							// trucks that have been
							// initialized
							truckList.add(theTruck.getTruckCache()[i]);
						}
					}
					// add self
					truckList.add(theTruck);

					// Sort trucks based on position
					Collections.sort(truckList, new Comparator<Truck>() {
						public int compare(Truck t1, Truck t2) {
							if (t1.getPos() == t2.getPos())
								return 0;
							return t1.getPos() < t2.getPos() ? -1 : 1;
						}
					});

					// Clear Console on Linux
					final String ANSI_CLS = "\u001b[2J";
					final String ANSI_HOME = "\u001b[H";
					System.out.print(ANSI_CLS + ANSI_HOME);
					System.out.flush();

					System.out.println("Truck:         "
							+ theTruck.getTruckNumber());
					System.out.println("Pos:           "
							+ df.format(theTruck.getPos()));
					System.out.println("next Trk Dist: "
							+ df.format(theTruck.getNextTruckPos()
									- theTruck.getPos()));
					System.out.print("Speed          "
							+ df.format(theTruck.getSpeed()));
					System.out.println("         Accel:         "
							+ df.format(theTruck.getAcceleration()));
					// System.out.println("Lane:          " +
					// theTruck.getLane());
					System.out.print("Order:         "
							+ theTruck.getOrderInConvoy());
					System.out.println("             Convoy Size:   "
							+ theTruck.getConvoySize());
					System.out.print("AIState:       "
							+ AIMap.get(theTruck.getTruckAIState()));
					System.out.println("   Am I 1st?:     "
							+ theTruck.getProbablyFirst());
					System.out.println("ConvoyID:      "
							+ theTruck.getConvoyID());
					System.out.println("CACHING====================");
					System.out.println("Cache Updates:"
							+ theTruck.getCacheUpdates() + "||Initializations:"
							+ theTruck.getInitializations());
					System.out.print("Pos Cache for Messaging");
					double[] posCache = theTruck.getTruckPosCache();
					for (int i = 0; i < posCache.length; i++) {
						System.out.print("||" + (int)posCache[i]);
					}
					System.out.println();
					System.out.println("MESSAGES===================");
					System.out.println("Forwarded:"
							+ theTruck.getMessagesForwarded() + "||Dropped:"
							+ theTruck.getMessagesDropped() + "||Created:"
							+ theTruck.getMessagesCreated() + "||Sent:"
							+ theTruck.getMessagesSent() + "||Failed:"
							+ theTruck.getMessagesFailed() + "||Bad:"
							+ theTruck.getMalformedMessagesReceived());

					System.out.print("Last Message sent:"
							+ theTruck.getLastCreatedMessage());
					System.out.println("Last Message Recd:"
							+ theTruck.getLastMessageReceived());
					System.out.println("TIMING=====================");
					System.out.println("AI Tick:"
							+ df2.format(theTruck.getLastAIProcessTime())
							+ "||New Msg Interval:"
							+ df2.format(theTruck.getLastMessageInterval()));
					System.out.println("Whole Tick:" + df2.format(lastTickTime)
							+ "||Whole Tick Interval:"
							+ df2.format(lastTickInterval));
					// System.out.println("ROAD VIEW==================");
					//
					// // Print RoadView
					// System.out
					// .println("_______________________________________________________");
					// for (Truck truck : truckList) {
					// System.out.print("-[" + truck.getTruckNumber() + ":"
					// + df.format(truck.getPos()) + "]-");
					// }
					// System.out.println();
					// System.out
					// .println("_______________________________________________________");
					//
					// Display truck info (position, speed, acceleration, lane,
					// total messages)
					// System.out
					// .println("TRUCK     POS            SPEED      ACC      LANE");
					// for (Truck truck : truckList) {
					// System.out.println("  " + truck.getTruckNumber()
					// + "       " + df.format(truck.getPos())
					// + "        " + df.format(truck.getSpeed())
					// + "       "
					// + df.format(truck.getAcceleration())
					// + "        " + truck.getLane());
					// }
				} else {
					try {
						Thread.sleep((long) ((1.0 / (double) UI_TICK_RATE) / 2.0));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				while (((System.nanoTime() - UITickStart) / 1000000000.0) < (1.0 / (double) UI_TICK_RATE)) {
				}
			}

		}
	}

}
