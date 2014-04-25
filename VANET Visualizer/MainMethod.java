public class MainMethod{

public static void main(String[] argS) {

		DataBase dataBase = new DataBase();
		GraphicsProgram graphicsProgram = new GraphicsProgram();
		graphicsProgram.setDataBase(dataBase);
		new Thread(graphicsProgram).start();

		ReceivingController receivingController = new ReceivingController();
		receivingController.setDataBase(dataBase);
		SendingController sendingController = new SendingController();
		
		receivingController.startUDPListener();
		// create a new frame to which we will add a canvas
	
	}

}