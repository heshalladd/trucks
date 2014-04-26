import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;

public class ReceivingController {

	DataBase mDataBase;
	
	public ReceivingController() {

	}
	
	public void connectDataBase(DataBase dataBase){
		mDataBase = dataBase;
	}
	
	public void startUDPListener(){
		try {
			System.out.println("Starting UDP Listener...");
			DatagramSocket serverSocket = new DatagramSocket(9876);
			byte[] receiveData = new byte[1024];
			//byte[] sendData = new byte[1024];

			while (true) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				serverSocket.receive(receivePacket);
				mDataBase.queuePacket(receivePacket);
				receiveData = new byte[1024];
			
				//String sentence = new String(receivePacket.getData());
				//InetAddress IPAddress = receivePacket.getAddress();
				//int port = receivePacket.getPort();
				//sendData = sentence.getBytes();
				//DatagramPacket sendPacket = new DatagramPacket(sendData,
				//sendData.length, IPAddress, port);
				//serverSocket.send(sendPacket);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setDataBase(DataBase dataBase) {
		mDataBase = dataBase;
		
	}

}
