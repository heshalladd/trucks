package edu.auburn.scottandjoe;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TheAir 
{

	public static final int TICK_RATE = 10;
	public static Truck[] theTrucks = new Truck[5];
	public static int totalTrucks = 0;
	public static int[] totalMessages = new int[5];

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		System.out.println("[NORMAL] Launching \"The Air\"");
		int port = 0;
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
		


		while(true)
		{
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
					totalTrucks++;
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
				//OLD:BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
				//OLD:DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
				String reply;
				String[] receivedMessage;
            
				//TODO:transmit request for status and transmission
            
            
				//retrieve message from the client
				receivedMessage = in.readLine().split(",");				
   
				//TODO:scrape location data
            
				//TODO:update status of test environment

				//TODO:determine who the broadcast is in range of

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
