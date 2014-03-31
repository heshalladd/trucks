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
import java.util.ArrayList;

public class TruckRunner {
	private static int tickRate = TheAir.TICK_RATE;
	private static boolean restarted = false;
	private static String startMessage = "";
	private static Truck theTruck;

	/**
	 * @param args
	 * @throws IOException
	 */

	// args[0] - "the air" ip
	// args[1] - "the air" port
	// args[2] - truck number (1 - 5)
	// args[3] - config file
	public static void main(String[] args) throws IOException {
		String airIP = args[0];
		int airPort = Integer.decode(args[1]);

		long tickStart = 0l;

		int truckNumber = Integer.decode(args[2]);
		int lane = Truck.RANDOMIZE_INT;
		double pos = Truck.RANDOMIZE_DOUBLE;
		double speed = Truck.RANDOMIZE_DOUBLE;
		double acceleration = Truck.RANDOMIZE_DOUBLE;
		// TODO:load from config
		try {
			while (true) {
				// initialize truck
				theTruck = new Truck(truckNumber, lane, pos, speed,
						acceleration);
				theTruck.startUDPListener(new DatagramSocket(airPort));

				// open a waiting socket and wait to be started by the server
				InetAddress addr = InetAddress.getByName(airIP);
				System.out.println("[NORMAL] Truck " + truckNumber
						+ ": Air IP address: " + addr);
				Socket airTCPSock = new Socket(addr, airPort);
				try {
					// wait for start
					startMessage = "";
					BufferedReader in = new BufferedReader(new InputStreamReader(airTCPSock.getInputStream()));
					while (!startMessage.equals("start")) {
						
						startMessage = in.readLine();
					}
					in.close();
					
					// start a listener for the restart signal
					new RestartListener(airTCPSock).start();
					// start thread for doing message handoffs to air, and also
					// telling the truck to listen for broadcasts
					new MessageHandoffHandler(airTCPSock).start();

					// while loop to do tick limited truck updates
					while (!restarted) {
						tickStart = System.nanoTime();
						theTruck.updateDesires();
						theTruck.updatePhysical();
						while (((System.nanoTime() - tickStart) / 1000000) < (1 / tickRate)) {
						}
					}
				} finally {
					System.out.println("[NORMAL] Closing tcp socket...");
					airTCPSock.close();
				}
			}
		} catch (FatalTruckException e) {
			System.out.println("[CRITICAL] " + e);
		}
	}

	private static class RestartListener extends Thread {
		private Socket airSocket;

		public RestartListener(Socket airSocket) {
			this.airSocket = airSocket;
		}

		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						airSocket.getInputStream()));
				String restartMessage = in.readLine();
				if (restartMessage.equals("restart")) {
					restarted = true;
				}

			} catch (IOException e) {
				System.out
						.println("[SEVERE] IOException in thread that waits for reset signal.");
			}
		}

	}

	private static class MessageHandoffHandler extends Thread {
		private Socket airSocket;
		private ArrayList<String> truckMessages;

		public MessageHandoffHandler(Socket airSocket) {
			this.airSocket = airSocket;
		}

		public void run() {
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(airSocket.getOutputStream())),
						true);
				while (!restarted) {
					truckMessages = new ArrayList<String>();
					// tell truck to receive response
					theTruck.handleMessage();
					// send messages to the air for appropriate forwarding
					if (truckMessages.size() != 0) {
						for (int i = 0; i < truckMessages.size(); i++) {
							out.println(truckMessages.get(i));
						}
					}
				}
			} catch (IOException e) {
				System.out
						.println("[SEVERE] IOException in thread that handles message handoff.");
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (FatalTruckException e) {
				e.printStackTrace();
			}
		}
	}

}
