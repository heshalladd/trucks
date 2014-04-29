public class MainMethod {
	
	public static void main(String[] argS) {
		TruckDataList truckDataList = new TruckDataList();
		Viewer viewer = new Viewer();
		viewer.setTruckDataList(truckDataList);
		new Thread(viewer).start();
		Receiver receiver = new Receiver();
		receiver.setTruckDataList(truckDataList);
		receiver.startUDPListener();
	}

}