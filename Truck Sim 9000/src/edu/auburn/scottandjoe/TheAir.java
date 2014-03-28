package edu.auburn.scottandjoe;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class TheAir 
{

	public static final int TICK_RATE = 10;
	ArrayList<Truck> Trucks = new ArrayList<Truck>();

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		System.out.println("[NORMAL] Launching \"The Air\"");
		int port = 0;
		int totalMessages = 0;
		if(args.length == 0)
		{
			System.out.println("[SEVERE] Error: Please specify a port in the command line.");
			System.exit(0);
		}
		if(Integer.decode(args[0]) >= 1 && Integer.decode(args[0]) <= 65535)
		{
			port = Integer.decode(args[0]);
			System.out.println("[NORMAL] Status: Port successfully selected (" + port + ")");
		}
		else
		{
			System.out.println("[SEVERE] Error in args: Invalid port number. Must be between 1 and 65535.");
			System.exit(0);
		}
		System.out.println("[NORMAL] Status: Running.");
		ServerSocket server = null;
		try{
			server = new ServerSocket(port);
		}
		catch(IOException e){
			//TODO:encase in debug if
			System.out.println("[SEVERE] Failed to open server socket.");
			System.exit(0);
		}


		//TODO:transmit request for status and transmission


		while(true)
		{
			//receive statuses and transmissions
			try
			{
				//TODO:encase this in a debug if
				System.out.println("[NORMAL] Status: Waiting for connections.");
				//end debug if
				while(true)
				{
					//spawn new thread to handle each connection to allow for simultaneous
					//connections and therefore better response times

					new MessageHandler(server.accept(),System.nanoTime()).start();
					totalMessages++;
				}
			}
			catch(IOException e)
			{
				//encase this in a debug if
				System.out.println("[SEVERE] IOException in handling new connection.");
			}
			finally
			{
				try{
					server.close();
				}
				catch(IOException e){
				}
			}
		}
	}

	private static class MessageHandler extends Thread
	{
		private Socket socket;
		private long startTime;

		public MessageHandler(Socket socket, long nanoTime)
		{
			this.socket = socket;
			this.startTime = nanoTime;
		}

		public void run()
		{
			try
			{
				//create input and output tools
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				byte[] reply;
				byte[] receivedMessage = new byte[2048];
				//retrieve message from the client
				in.read(receivedMessage, 0, 2048);
				//grab message length
				int length = receivedMessage[0] * 256 + receivedMessage[1];

				//TODO:encase in debug if
				System.out.println("[NORMAL] Message of length " 
						+ length + " received.");
				System.out.print("[NORMAL] Original Message \"");
				for(int i = 5; i < length; i++)
				{
					System.out.print((char)receivedMessage[i]);
				}
				System.out.println("\"");
				//end debug encasing
				


				//TODO:update status of test environment

				//TODO:determine who wants to send messages

				//TODO:determine whether those messages are going to make it
            
            ////piecewise equation skeleton for determining transmission probability
            //double chanceToSend = 0.0;
            //double distanceApart = Math.abs(truck1pos - truck2pos);
            //if(distanceApart < 70)
            //{
            //   chanceToSend = -0.002142857*distanceApart + 1;
            //}
            //else if(distanceApart >= 70 && distanceApart < 100);
            //{
            //   chanceToSend = -(0.00094*Math.pow(distanceApart-70, 2))+0.85;
            //}
            //else if(distanceApart >= 100)
            //{
            //   chanceToSend = 0.0;
            //}

				//TODO:forward transmissions that qualify to their hosts.
				
			}

			catch(IOException e)
			{
				System.out.println("[SEVERE] Error in Request Handler:" + e);
			}

			finally
			{
				try
				{
					socket.close();
				}
				catch(IOException e)
				{
					System.out.println("[SEVERE] Error in Request Handler: Could not close socket.");
				}
			}
		}

	}

}
