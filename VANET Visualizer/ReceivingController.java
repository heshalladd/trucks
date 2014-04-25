import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;

public class ReceivingController {

	DataBase mDataBase;
	
	public ReceivingController() {

	}
	
	public void startUDPListener(){
		try {
			System.out.println("Starting UDP Listener...");
			DatagramSocket serverSocket = new DatagramSocket(9876);
			byte[] receiveData = new byte[1024];
			byte[] sendData = new byte[1024];
			StringTokenizer token;
			String id;
			while (true) {

				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				serverSocket.receive(receivePacket);
				String sentence = new String(receivePacket.getData());

				System.out.println("RECEIVED: " + sentence);
				token = new StringTokenizer(sentence,",");
				id = token.nextToken();
				System.out.println(id);
				int loc = Integer.parseInt(token.nextToken());
				if(id.equals("1")){
					mDataBase.x1 = loc;
				}
				if(id.equals("2")){
					mDataBase.x2 = loc;
				}

				InetAddress IPAddress = receivePacket.getAddress();
				int port = receivePacket.getPort();
				
				sendData = sentence.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length, IPAddress, port);
				
				serverSocket.send(sendPacket);
				receiveData = new byte[1024];
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setDataBase(DataBase dataBase) {
		mDataBase = dataBase;
		
	}

}
