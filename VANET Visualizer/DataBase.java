import java.net.DatagramPacket;
import java.util.concurrent.ArrayBlockingQueue;


public class DataBase {
	 int x1 = 0;
	 int x2 = 0;
	 
	 ArrayBlockingQueue<DatagramPacket> mIncomingPackets;
	 
	 public DataBase(){
		 mIncomingPackets = new ArrayBlockingQueue<DatagramPacket>(5096);
	 }
	 protected void queuePacket(DatagramPacket packet){
		 try {
			mIncomingPackets.put(packet);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	 }

}
