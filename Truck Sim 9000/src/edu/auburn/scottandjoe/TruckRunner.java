package edu.auburn.scottandjoe;

public class TruckRunner {
   static int tickRate = TheAir.TICK_RATE;
	/**
	 * @param args
	 */
   public static void main(String[] args)
   {
      boolean restarted = false;
      int truckNumber = 0;
      int lane = Truck.RANDOMIZE_INT;
      double pos = Truck.RANDOMIZE_DOUBLE;
      double speed = Truck.RANDOMIZE_DOUBLE;
      double acceleration = Truck.RANDOMIZE_DOUBLE;
   	//TODO:interpret arguments
      try
      {
         while(true)
         {
         //initialize truck
            Truck theTruck = new Truck(truckNumber, lane, pos, speed, acceleration);
         
         //TODO:open a waiting socket and wait to be started by the server
         
         
            while(!restarted)
            {
            //TODO:send status and desired transmissions
            
            //TODO:receive response
            
            //TODO:do truck updates if messages are received
            
            //TODO:update restarted flag if the air has mandated it.
            }
         }
      }
      catch(FatalTruckException e)
      {
         System.out.println("[CRITICAL] " + e );
      }
   }

}
