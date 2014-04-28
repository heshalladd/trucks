package edu.auburn.scottandjoe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class TruckDriver {
	// imported constants
	private static final int TICK_RATE = Controller.TICK_RATE;
	private static final String TERMINATING_STRING = Controller.TERMINATING_STRING;

	// local variables
	private static boolean running = false;
	private static boolean collision = false;
	private static Truck theTruck;

	// variables for reinitializing
	private static int initTruckNumber;
	private static int initLane;
	private static double initPos;
	private static double initSpeed;
	private static double initAcceleration;
	private static FloodingAlgorithm floodingAlgorithm = null;

	/**
	 * @param args
	 * @throws IOException
	 */

	// args[0] - "the controller" ip
	// args[1] - "the controller" port
	// args[2] - truck number (1 - X)
	// args[3] - start pos
	// args[4] - 1 for basic FA, 2 for MPR FA
	public static void main(String[] args) throws IOException {
		String controllerAddress = args[0];
		initTruckNumber = Integer.decode(args[2]);
		initLane = Truck.RANDOMIZE_INT;
		initPos = Double.parseDouble(args[3]);
		if (Integer.parseInt(args[4]) == 1) {
			floodingAlgorithm = new BasicFloodingAlgorithm();
		} else if (Integer.parseInt(args[4]) == 2) {
			// TODO: set to the MPR FA
		}
		// double pos = Truck.RANDOMIZE_DOUBLE;
		initSpeed = Truck.RANDOMIZE_DOUBLE;
		initAcceleration = Truck.RANDOMIZE_DOUBLE;
		// TODO:add feature: load from config

		// let server know you exist by sending truck number
		InetAddress addr = InetAddress.getByName(controllerAddress);
		byte[] outBoundPacketBuf = new byte[4096];
		outBoundPacketBuf = new String("" + initTruckNumber).getBytes();
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

		// start the daemon for receiving requests and responses from controller
		new ControllerDaemon(controllerAddress).start();

		// start logic loop
		new TruckLogicLooper().start();

		// start ui thread
		// NOTE: Disable for performance. Visualizer is the new UI
		// new UIThread().start();
	}

	private static class ControllerDaemon extends Thread {
		private String controllerAddress;

		public ControllerDaemon(String controllerAddress) {
			this.controllerAddress = controllerAddress;
		}

		public void run() {
			try {
				try {
					new ControllerRequester(controllerAddress).start();
					// input for reading requests
					DatagramSocket daemonSocket = new DatagramSocket();
					InetAddress remoteAddress = InetAddress
							.getByName(controllerAddress);
					daemonSocket.connect(remoteAddress, Controller.DAEMON_PORT);

					while (true) {
						String receivedMessageWhole = "";
						byte[] receivedData = new byte[4096];
						DatagramPacket receivedPacket = new DatagramPacket(
								receivedData, receivedData.length);
						daemonSocket.receive(receivedPacket);
						receivedMessageWhole = new String(
								receivedPacket.getData());
						String receivedMessageTerminated = receivedMessageWhole
								.split(TERMINATING_STRING)[0];
						String[] receivedMessage = receivedMessageTerminated
								.split(",");

						int messageType = Integer.decode(receivedMessage[0]);
						int requestType = Integer.decode(receivedMessage[1]);

						if ((int) messageType == (int) Controller.REQUEST) {
							// handle request
							switch (requestType) {
							case Controller.START_SIM:
								running = true;
								// get the number of trucks
								int desiredTruckSimPop = Integer
										.parseInt(receivedMessage[2]);
								// initialize truck
								theTruck = new Truck(initTruckNumber, initLane,
										initPos, initSpeed, initAcceleration,
										floodingAlgorithm, desiredTruckSimPop);
								theTruck.startUDPListener();
								break;
							case Controller.END_SIM:
								running = false;
								theTruck.stopUDPListener();
								break;
							default:
								break;
							}
						} else if ((int) messageType == (int) Controller.RESPONSE) {
							// handle incoming data
							switch (requestType) {
							case Controller.POS_CACHE:

								break;
							case Controller.ADDR_CACHE:
								String[] receivedAddresses = new String[desiredTruckSimPop];
								System.out
										.print("[NORMAL] Receiving Addresses: ");
								for (int i = 0; i < receivedAddresses.length; i++) {
									receivedAddresses[i] = receivedMessage[i + 2];
									System.out.print(i + "-"
											+ receivedAddresses[i] + "\n");
								}
								System.out
										.println("[NORMAL] All addresses received.");
								// set the truck addresses in the truck object
								theTruck.setTruckAddresses(receivedAddresses);
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
			} catch (IOException e) {
				System.out
						.println("[SEVERE] IOException in thread that waits for reset signal.");
			} catch (FatalTruckException e) {
				System.out.println("[CRITICAL] " + e);
			}
		}

	}

	private static class ControllerRequester extends Thread {
		private String controllerAddress;

		public ControllerRequester(String controllerAddress) {
			this.controllerAddress = controllerAddress;
		}

		public void run() {

		}
	}

	private static class TruckLogicLooper extends Thread {

		public TruckLogicLooper() {
		}

		public void run() {
			long tickStart = 0l;
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
					while (running) {
						if (collision) {
							break;
						}
						tickStart = System.nanoTime();
						theTruck.updateMental();
						theTruck.updatePhysical();
						// TODO: request pos cache
						while (((System.nanoTime() - tickStart) / 1000000000.0) < (1.0 / (double) TICK_RATE)) {
						}
					}
				} catch (FatalTruckException e) {
					// TODO: handle
				}
			}
		}
	}

	private static class UIThread extends Thread {
		long UITickStart = 0l;
		private static final int UI_TICK_RATE = 10;

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
			while (true) {
				UITickStart = System.nanoTime();
				// boolean debug = false;
				if (running) {
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

					System.out.println("===========================");
					System.out.println("Truck:         "
							+ theTruck.getTruckNumber());
					System.out.println("Pos:           "
							+ df.format(theTruck.getPos()));
					System.out.println("Next Trk Pos:  "
							+ df.format(theTruck.getNextTruckPos()));
					System.out.println("next Trk Dist: "
							+ df.format(theTruck.getNextTruckPos()
									- theTruck.getPos() - 25));
					System.out.println("Speed          "
							+ df.format(theTruck.getSpeed()));
					System.out.println("Accel:         "
							+ df.format(theTruck.getAcceleration()));
					System.out.println("Lane:          " + theTruck.getLane());
					System.out.println("ConvoyID:      "
							+ theTruck.getConvoyID());
					System.out.println("Order:         "
							+ theTruck.getOrderInConvoy());
					System.out.println("Convoy Size:   "
							+ theTruck.getConvoySize());
					System.out.println("AIState:       "
							+ AIMap.get(theTruck.getTruckAIState()));
					System.out.println("Am I 1st?:     "
							+ theTruck.getProbablyFirst());
					System.out.println("Msgs Forwarded:"
							+ theTruck.getMessagesForwarded()
							+ "  Messages Dropped:"
							+ theTruck.getMessagesDropped()
							+ "  Messages Created:"
							+ theTruck.getMessagesCreated()
							+ "  Messages Sent:" + theTruck.getMessagesSent());
					System.out.println("Last Message sent: "
							+ theTruck.getLastCreatedMessage());
					System.out.println("===========================");

					// debug
					// System.out.println("Last Forwarded Message:" +
					// theTruck.getLastMessageToForward());

					// Print RoadView
					System.out
							.println("ROAD VIEW FROM THIS TRUCKS PERSPECTIVE");
					System.out
							.println("_______________________________________________________");
					for (Truck truck : truckList) {
						System.out.print("-[" + truck.getTruckNumber() + ":"
								+ df.format(truck.getPos()) + "]-");
					}
					System.out.println();
					System.out
							.println("_______________________________________________________");

					// Display truck info (position, speed, acceleration, lane,
					// total messages)
					System.out
							.println("TRUCK     POS            SPEED      ACC      LANE");
					for (Truck truck : truckList) {
						System.out.println("  " + truck.getTruckNumber()
								+ "       " + df.format(truck.getPos())
								+ "        " + df.format(truck.getSpeed())
								+ "       "
								+ df.format(truck.getAcceleration())
								+ "        " + truck.getLane());
					}
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
