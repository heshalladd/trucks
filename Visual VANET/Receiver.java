import java.awt.Color;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Receiver {

	TruckDataList mTruckDataList;

	public void setTruckDataList(TruckDataList truckDataList) {
		mTruckDataList = truckDataList;
	}

	public void startUDPListener() {
		try {
			System.out.println("Starting UDP Listener...");
			DatagramSocket serverSocket = new DatagramSocket(10127);
			byte[] receiveData = new byte[1024];
			int number;
			int locationX;
			String platoonId;
			String information;
			HashMap<String,Color> platoonIdMap = mTruckDataList.platoonIdMap;
			while (true) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				serverSocket.receive(receivePacket);
				String data = new String(receivePacket.getData());
				data+="THIS_IS_THE_END";
				StringTokenizer tokenizer = new StringTokenizer(data, ",");
				number = Integer.parseInt(tokenizer.nextToken());
				locationX = Integer.parseInt(tokenizer.nextToken());
				platoonId = tokenizer.nextToken();
				if(!platoonIdMap.containsKey(platoonId)){
					platoonIdMap.put(platoonId, mTruckDataList.getRandomColor());
				}
				information = tokenizer.nextToken("THIS_IS_THE_END");
				mTruckDataList.truckDataList.get(number).updateData(number,
						locationX, platoonId, information);
				receiveData = new byte[1024];
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}