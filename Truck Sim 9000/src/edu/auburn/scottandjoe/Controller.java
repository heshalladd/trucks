package edu.auburn.scottandjoe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Controller {
	public static final int TICK_RATE = 10;

	private static int totalTrucks = 0;
	private static boolean allTrucksConnected = false;
	private static boolean runSimulation = false;
	private static boolean collision = false;
	private static String[] truckAddresses = new String[5];

	public static void main(String[] args) {
		System.out.println("[NORMAL] Launching the controller.");

		int port;
		port = parsePortFromArgs(args);

		System.out.println("[NORMAL] Status: Running.");

		ServerSocket server = null;

		new UserInputHandler().start();

		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("[SEVERE] Failed to open server socket.");
			System.exit(0);
		}

		while (true) {
			try {
				System.out.println("[NORMAL] Status: Waiting for connections.");
				while (true) {
					// spawn new thread to handle each connection to allow for
					// simultaneous connections and therefore better response
					// times

					new TruckHandler(server.accept()).start();
					totalTrucks++;
					System.out.println("[NORMAL] " + totalTrucks
							+ " trucks connected.");
					if (totalTrucks == 5) {
						allTrucksConnected = true;
					}
				}
			} catch (IOException e) {
				System.out
						.println("[SEVERE] IOException in handling new connection.");
			} finally {
				try {
					server.close();
				} catch (IOException e) {
				}
			}
		}

	}

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

	private static class TruckHandler extends Thread {
		private Socket socket;

		public TruckHandler(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				// create input and output tools
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				String receivedMessage = "";
				receivedMessage = in.readLine();
				int truckNumber = Integer.decode(receivedMessage);
				truckAddresses[truckNumber - 1] = socket
						.getRemoteSocketAddress().toString().split("/")[1]
						.split(":")[0];

				while (true) {
					while (!runSimulation) {
						Thread.sleep(10);
					}
					System.out
							.println("[NORMAL] Starting simulation for truck "
									+ truckNumber);
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.println("start");

					// listen to this threads truck connection for a collision
					while (runSimulation) {
						receivedMessage = in.readLine();
						if (receivedMessage.equals("collision")) {
							collision = true;
							runSimulation = false;
							out.println("collision");
						}
					}
					System.out
							.println("[SEVERE] Something bad happened to one of the trucks.");
				}
			}

			catch (IOException e) {
				System.out.println("[SEVERE] Error in Request Handler:" + e);
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					System.out
							.println("[SEVERE] Error in Request Handler: Error while closing socket.");
				}
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
					break;
				}
			}
		}

		private void waitForTrucksToConnect() {
			while (!allTrucksConnected) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
