package edu.auburn.scottandjoe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class TruckDriver {
	private static int tickRate = Controller.TICK_RATE;
	private static boolean running = false;
	private static boolean collision = false;
	private static Truck theTruck;

	/**
	 * @param args
	 * @throws IOException
	 */

	// args[0] - "the controller" ip
	// args[1] - "the controller" port
	// args[2] - truck number (1 - 5)
	// args[3] - start pos
	public static void main(String[] args) throws IOException {
		String airIP = args[0];
		int airPort = Integer.decode(args[1]);

		int truckNumber = Integer.decode(args[2]);
		int lane = Truck.RANDOMIZE_INT;
		double pos = Double.parseDouble(args[3]);
		// double pos = Truck.RANDOMIZE_DOUBLE;
		double speed = Truck.RANDOMIZE_DOUBLE;
		double acceleration = Truck.RANDOMIZE_DOUBLE;
		// TODO:load from config
		try {

			// initialize truck
			theTruck = new Truck(truckNumber, lane, pos, speed, acceleration);
			theTruck.startUDPListener(new DatagramSocket(airPort));

			// open a waiting socket and wait to be started by the server
			InetAddress addr = InetAddress.getByName(airIP);
			System.out.println("[NORMAL] Truck " + truckNumber
					+ ": Air IP address: " + addr);
			Socket airTCPSock = new Socket(addr, airPort);

			// start a listener for the restart signal
			new ControllerListener(airTCPSock).start();

			// start logic loop
			new TruckLogicLooper(airTCPSock).start();

			// start ui thread
			new UIThread().start();

		} catch (FatalTruckException e) {
			System.out.println("[CRITICAL] " + e);
			System.exit(99);
		}
	}

	private static class ControllerListener extends Thread {
		private Socket airSocket;

		public ControllerListener(Socket airSocket) {
			this.airSocket = airSocket;
		}

		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						airSocket.getInputStream()));
				while (true) {
					while (!running) {
						String startMessage = in.readLine();
						if (startMessage.equals("start")) {
							collision = false;
							running = true;
						}
					}

					while (running) {
						String crashMessage = in.readLine();
						if (crashMessage.equals("collision")) {
							running = false;
							collision = true;
							System.out.println("COLLISION! BOOM!");
						}
					}
				}
			} catch (IOException e) {
				System.out
						.println("[SEVERE] IOException in thread that waits for reset signal.");
			}
		}

	}

	private static class TruckLogicLooper extends Thread {
		private Socket airSocket;

		public TruckLogicLooper(Socket airSocket) {
			this.airSocket = airSocket;
		}

		public void run() {
			long tickStart = 0l;
			while (true) {
				try {
					PrintWriter out = new PrintWriter(
							new BufferedWriter(new OutputStreamWriter(
									airSocket.getOutputStream())), true);
					out.println("" + theTruck.getTruckNumber());
					while (!running) {
					}

					// while loop to do tick limited truck updates
					while (running) {
						if (collision) {
							break;
						}
						tickStart = System.nanoTime();
						theTruck.updateDesires();
						theTruck.updatePhysical();
						while (((System.nanoTime() - tickStart) / 1000000000.0) < (1.0 / (double) tickRate)) {
						}
					}
				} catch (FatalTruckException e) {
					PrintWriter out;
					try {
						out = new PrintWriter(new BufferedWriter(
								new OutputStreamWriter(
										airSocket.getOutputStream())), true);
						out.println("collision");
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private static class UIThread extends Thread {
		long UITickStart = 0l;
		int UITickRate = 10;

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
				ArrayList<Truck> truckList = new ArrayList<Truck>();
				for (int i = 0; i < theTruck.getTruckInitialized().length; i++) {
					if (theTruck.getTruckInitialized()[i]) { // make sure you
																// only display
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

				UITickStart = System.nanoTime();
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
				System.out.println("ConvoyID:      " + theTruck.getConvoyID());
				System.out.println("Order:         "
						+ theTruck.getOrderInConvoy());
				System.out
						.println("Convoy Size:   " + theTruck.getConvoySize());
				System.out.println("AIState:       "
						+ AIMap.get(theTruck.getTruckAIState()));
				System.out.println("Am I 1st?:     "
						+ theTruck.getProbablyFirst());
				System.out
						.println("Msgs Forwarded:"
								+ theTruck.getMessagesForwarded()
								+ "  Messages Dropped:"
								+ theTruck.getMessagesDropped());
				System.out.println("===========================");

				// debug
				// System.out.println("Last Forwarded Message:" +
				// theTruck.getLastMessageToForward());

				// Print RoadView
				System.out.println("ROAD VIEW FROM THIS TRUCKS PERSPECTIVE");
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
							+ "       " + df.format(truck.getAcceleration())
							+ "        " + truck.getLane());
				}

				while (((System.nanoTime() - UITickStart) / 1000000000.0) < (2.0 / (double) UITickRate)) {
				}

			}
		}
	}

}
