package edu.auburn.scottandjoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import edu.auburn.scottandjoe.Truck;

public class TheAir {

	public static final int TICK_RATE = 10;
	
	public static Truck[] theTrucks = new Truck[5];
	public static int totalTrucks = 0;
	public static int[] totalMessages = new int[5];
	public static int port;
	public static String[] truckAddresses = new String[5];
	public static boolean[] truckInitialized = new boolean[5]; //will initialize to false (desired)

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("[NORMAL] Launching \"The Air\"");
		port = 0;
		if (args.length == 0) {
			System.out
					.println("[SEVERE] Error: Please specify a port in the command line.");
			System.exit(0);
		}
		if (Integer.decode(args[0]) >= 1 && Integer.decode(args[0]) <= 65535) {
			port = Integer.decode(args[0]);
			System.out.println("[NORMAL] Status: Port successfully selected ("
					+ port + ")");
		} else {
			System.out
					.println("[SEVERE] Error in args: Invalid port number. Must be between 1 and 65535.");
			System.exit(0);
		}
		System.out.println("[NORMAL] Status: Running.");
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("[SEVERE] Failed to open server socket.");
			System.exit(0);
		}
		while (true) {
			try {
				System.out.println("[NORMAL] Status: Waiting for connections.");
				//TODO: spawn ui thread (for displaying stuff and also waiting for start and restart keypress
				while (true) {
					// spawn new thread to handle each connection to allow for
					// simultaneous
					// connections and therefore better response times

					new MessageHandler(server.accept()).start();
					totalTrucks++;
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
	
	private static void clearConsole()
	{
	    try
	    {
	        String os = System.getProperty("os.name");
	        if (os.contains("Windows"))
	        {
	            Runtime.getRuntime().exec("cls");
	        }
	        else
	        {
	        	//linux or mac
	            Runtime.getRuntime().exec("clear");
	        }
	    }
	    catch (Exception exception)
	    {
	        System.out.println("[SEVERE] Error in clearing screen.");
	    }
	}

	private static class MessageHandler extends Thread {
		private Socket socket;

		public MessageHandler(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				// create input and output tools
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				String[] receivedMessage;
				String receivedMessageWhole = "";

				// TODO:transmit request for status and transmission

				while (true) {
					// retrieve message from the client
					receivedMessageWhole = in.readLine();
					receivedMessage = receivedMessageWhole.split(",");
					//update sourceAddress in message if it was 0 (unset)
					if(Integer.decode(receivedMessage[1]) == 0) {
						receivedMessage[1] = socket.getRemoteSocketAddress().toString();
					}
					// scrape truck data for air cache
					int messageTruckNumber = Integer.decode(receivedMessage[7]);
					totalMessages[messageTruckNumber - 1]++;
					if(truckInitialized[messageTruckNumber - 1])
					{
					theTrucks[messageTruckNumber - 1].setSequenceNumber(Integer.decode(receivedMessage[0]));
                    theTrucks[messageTruckNumber - 1].setAcceleration(Double.parseDouble(receivedMessage[4]));                    
                    theTrucks[messageTruckNumber - 1].setPos(Double.parseDouble(receivedMessage[5]));
                    theTrucks[messageTruckNumber - 1].setSpeed(Double.parseDouble(receivedMessage[6]));
                    theTrucks[messageTruckNumber - 1].setLane(Integer.decode(receivedMessage[8]));
                    theTrucks[messageTruckNumber - 1].setDesiredLane(Integer.decode(receivedMessage[9]));
                    theTrucks[messageTruckNumber - 1].setDesiredPlaceInConvoy(Integer.decode(receivedMessage[10]));
                    theTrucks[messageTruckNumber - 1].setConvoyID(receivedMessage[11]);
                    theTrucks[messageTruckNumber - 1].setOrderInConvoy(Integer.decode(receivedMessage[12]));
					}
					else
					{
						//add address of truck to air cache of truck addresses and ports
						truckAddresses[messageTruckNumber - 1] = socket.getRemoteSocketAddress().toString();
						//initialize truck for cache
						theTrucks[messageTruckNumber - 1] = new Truck(messageTruckNumber, Integer.decode(receivedMessage[8]), Double.parseDouble(receivedMessage[5]), Double.parseDouble(receivedMessage[6]), Double.parseDouble(receivedMessage[4]));
						//add other data
						theTrucks[messageTruckNumber - 1].setSequenceNumber(Integer.decode(receivedMessage[0]));
	                    theTrucks[messageTruckNumber - 1].setDesiredLane(Integer.decode(receivedMessage[9]));
	                    theTrucks[messageTruckNumber - 1].setDesiredPlaceInConvoy(Integer.decode(receivedMessage[10]));
	                    theTrucks[messageTruckNumber - 1].setConvoyID(receivedMessage[11]);
	                    theTrucks[messageTruckNumber - 1].setOrderInConvoy(Integer.decode(receivedMessage[12]));
	                    truckInitialized[messageTruckNumber - 1] = true;
					}

					// determine who the broadcast is in range of
					ArrayList<Truck> trucksInRange = new ArrayList<Truck>();
                    for(int i = 0; i < theTrucks.length; i++)
                    {
                    	if(i != messageTruckNumber - 1 && truckInitialized[i] && Math.abs(theTrucks[messageTruckNumber - 1].getPos() - theTrucks[i].getPos()) < 100)
                    	{
                    		trucksInRange.add(theTrucks[i]);
                    	}
                    }

					// determine whether those messages are going to make it through
                    if(trucksInRange.size() > 0)
                    {
					for(int i = 0; i < trucksInRange.size(); i++)
					{
					double chanceToSend = 0.0;
					double distanceApart = Math.abs(theTrucks[messageTruckNumber - 1].getPos() - trucksInRange.get(i).getPos());
					//piecewise equation for determining transmission probability
					if(distanceApart < 70)
					{
						chanceToSend = -0.002142857 * distanceApart + 1;
					}
					else if(distanceApart >= 70 && distanceApart < 100)
					{
						chanceToSend = -(0.00094 * Math.pow(distanceApart-70 , 2)) + 0.85;
					}
					else if(distanceApart >= 100)
					{
						chanceToSend = 0.0;
					}
                    
					//roll the dice
					Random rand = new Random();
					if(chanceToSend >= rand.nextDouble())
					{
						byte[] outBoundPacketBuf = new byte[4028];
						outBoundPacketBuf = receivedMessageWhole.getBytes();
						
						//get address and port for sending stuff via UDP.
						DatagramSocket forwardUDPSock = new DatagramSocket();
						InetAddress truckDestination = InetAddress.getByName(truckAddresses[trucksInRange.get(i).getTruckNumber() - 1]);
						DatagramPacket outBoundUDPPacket = new DatagramPacket(outBoundPacketBuf, outBoundPacketBuf.length, truckDestination, port);
						
						// forward transmissions (that qualify) to their hosts as UDP.
						forwardUDPSock.send(outBoundUDPPacket);
						
						//close socket
						forwardUDPSock.close();
					}
					}
                    }
				}

			}

			catch (IOException | NumberFormatException | FatalTruckException e) {
				System.out.println("[SEVERE] Error in Request Handler:" + e);
			}

			finally {
				try {
					socket.close();
				} catch (IOException e) {
					System.out
							.println("[SEVERE] Error in Request Handler: Could not close socket.");
				}
			}
		}

	}

	private class UIThread extends Thread {
		private int newLane;
		
		public UIThread() {
		}

		public void run() {
			//TODO: add ui here
			ArrayList<Truck> truckList = new ArrayList<Truck>();
			for(int i = 0; i<totalTrucks; i++){
				truckList.add(theTrucks[i]);
			}
			
			Collections.sort(truckList, new Comparator<Truck>(){
			     public int compare(Truck t1, Truck t2){
			         if(t1.getPos() == t2.getPos())
			             return 0;
			         return t1.getPos() < t2.getPos() ? -1 : 1;
			     }
			});
			System.out.print("_______________________________________________________");
			for(int i = 0; i<totalTrucks; i++){
				if(truckInitialized[i])
				System.out.print("-  -[" + truckList.get(i).getTruckNumber() + ":" + truckList.get(i).getPos() + "] -   -" );
			}
			System.out.print("_______________________________________________________");
			//________________________________________
			//- - -[1:360][3:390][4:440][2:450][5:550] -   -    -
			//----------------------------------------

			//TODO: display truck info (position, speed, acceleration, lane, total messages)
			//TODO: make sure you only display trucks that have been initialized
			//TODO: busy wait for ui thread tick to finish

			//TIP: use clearConsole() to do so.
			//TODO: clear screen and start the loop over again
			//TODO: display truck info (position, speed, acceleration, lane, total messages)
			//TODO: make sure you only display trucks that have been initialized
			//TODO: busy wait for ui thread tick to finish
			
			//TIP: use clearConsole() to do so.
			//TODO: clear screen and start the loop over again
			
			
		}
	}
}
