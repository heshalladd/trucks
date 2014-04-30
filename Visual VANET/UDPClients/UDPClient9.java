

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class UDPClient9 {
	
		  private static int mAcceleration;
		  private static int mSpeed;
	
	public static void main(String args[]) throws Exception
	   {
	      int count = 0;
	      mSpeed = 1;
	      Random rand = new Random();
	      System.out.println("Starting UDPClient...");
	      DatagramSocket clientSocket = new DatagramSocket();
	      InetAddress IPAddress = InetAddress.getByName("localhost");
	      byte[] sendData = new byte[1024];
	      byte[] receiveData = new byte[1024];

	      while(count>-1){
		  String sentence = "9,"+count +","+29291831+",Truck #9\nSpeed:55mph\nAcceleration:2mph";
//		  if(count%100 == 0){
//			  //mSpeed =  rand.nextInt((2 - (1)) + 1) + (1);
//			  System.out.println("Speed is " +mSpeed);
//		  }
		  
		  count++;
	      sendData = sentence.getBytes();
	      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
	      clientSocket.send(sendPacket);
	      //DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	      //clientSocket.receive(receivePacket);
	      //String modifiedSentence = new String(receivePacket.getData());
	      //System.out.println("FROM SERVER:" + modifiedSentence);
	      Thread.sleep(10);
	      }
	      
	      clientSocket.close();
	   }
}
