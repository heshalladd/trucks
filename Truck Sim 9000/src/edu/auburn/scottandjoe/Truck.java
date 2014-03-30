package edu.auburn.scottandjoe;

import java.util.Random;
import java.util.ArrayList;
import java.util.UUID;

public class Truck{
   static final double MAX_ACCELERATION = 1.0;
   static final double MIN_ACCELERATION = -3.0;
   static final int MAX_LANE = 2;
   static final int MIN_LANE = 1;
	
   public static final int RANDOMIZE_INT = -10000;
   public static final double RANDOMIZE_DOUBLE = -10000.0;
	
   private int tickRate = TheAir.TICK_RATE;
   
   //truck meta
   private int desiredLane;
   private int sequenceNumber = 1;
   private int messagesPerSecond;
   private int orderInConvoy = 1;  //1 will signify leader of convoy
   private String convoyID = UUID.randomUUID().toString(); //id of convoy
   
   //truck properties
   private double acceleration;
   private double pos;
   private double speed = 0;
   private int truckNumber;
   private int lane;
   
   //intents
   private boolean intentChangeLane = false;
   
   //caching
   private Truck[] truckCache = new Truck[5];
   private int[] truckSequenceCache = new int[5]; 
	
	//initializes a truck object. truck numbering conflicts are not handled, and are the truck runners responsibility
   public Truck(int truckNumber, int lane, double pos, double speed, double acceleration) throws FatalTruckException
   {
      Random rand = new Random();
      this.truckNumber = truckNumber;
      if(lane != RANDOMIZE_INT)
      {
         if(lane > MAX_LANE || lane < MIN_LANE)
         {
            System.out.println("[SEVERE] Truck " + truckNumber + " is off the road!");
            explode("Truck was initialized in an invalid lane (allowable is " + MIN_LANE + " to " + MAX_LANE);
         }
         this.lane = lane;
         this.desiredLane = lane;
         System.out.println("[NORMAL] Truck lane initialized to " + this.lane + ".");
      }
      else
      {
      	//randomize lane accordingly
         lane = rand.nextInt((MAX_LANE - MIN_LANE) + 1) + MIN_LANE;
         System.out.println("[NORMAL] Truck lane randomly initialized to " + this.lane + ".");
      }
   	
      if(pos != RANDOMIZE_DOUBLE)
      {
         this.pos = pos;
         System.out.println("[NORMAL] Truck position initialized to " + this.pos + ".");
      }
      else
      {
         double MAX_INITIAL_POS = 350;
      	//randomize position between 0 and 350
         this.pos = MAX_INITIAL_POS*rand.nextDouble();
         System.out.println("[NORMAL] Truck position randomly initialized to " + this.pos + ".");
      }
   	
      if(speed != RANDOMIZE_DOUBLE)
      {
         this.speed = speed;
         System.out.println("[NORMAL] Truck speed initialized to " + this.speed + ".");
      }
      else
      {
         //randomize speed accordingly
         //55mph is 24.6m/s
         double MIN_REASONABLE_SPEED = 24.6;
         //80mph is 35.7m/s
         double MAX_REASONABLE_SPEED = 35.7;
         //these will mark the discard boundaries
         double mean = 30;
         double std = 10;
         
         int tries = 0;
         while(this.speed < MIN_REASONABLE_SPEED || this.speed > MAX_REASONABLE_SPEED)
         {
            this.speed = mean + std*rand.nextGaussian();
            tries++;
         }
         System.out.println("[NORMAL] Truck speed randomly initialized to " + this.speed + " after " + tries + " tries.");
      }
   	
      if(acceleration != RANDOMIZE_DOUBLE)
      {
         if(acceleration > MAX_ACCELERATION || acceleration < MIN_ACCELERATION)
         {
            this.acceleration = acceleration;
            System.out.println("[NORMAL] Truck acceleration initialized to " + this.acceleration + ".");
         }
         else
         {
            System.out.println("[SEVERE] Truck " + truckNumber + "'s engine has " +
               	"overheated and exploded from excessive acceleration/deceleration.");
            explode("Excessive acceleration/deceleration in initialization (allowable is " + MIN_ACCELERATION + " to " + MAX_ACCELERATION + ")");
         }
      }
      else
      {
         //randomize acceleration
         this.acceleration = rand.nextDouble()*(MAX_ACCELERATION - MIN_ACCELERATION) + MIN_ACCELERATION;
         System.out.println("[NORMAL] Truck acceleration randomly initialized to " + this.acceleration + ".");
      }
   }
	
   private void accelerate() throws FatalTruckException
   {
      if(acceleration <= MAX_ACCELERATION && acceleration >= MIN_ACCELERATION)
      {
      	//accelerate relative to tick rate
         speed = speed + (acceleration / (double)tickRate);
      }
      else
      {
         System.out.println("[SEVERE] Truck " + truckNumber + "'s engine has " +
            	"overheated and exploded from excessive acceleration/deceleration.");
         explode("Excessive acceleration/deceleration (allowable is " + MIN_ACCELERATION + " to " + MAX_ACCELERATION + ")");
      }
   }
	
   private void explode(String reason) throws FatalTruckException
   {
      throw new FatalTruckException(reason);
   }
	
   //note: update position before acceleration
   private void updatePosition()
   {
   	//update truck position relative to speed and tick rate
      this.pos = this.pos + this.speed/tickRate;
   }
	
	//collisions of trucks is a responsibility of the air to decide
   public int updatePhysical() throws FatalTruckException
   {
   	//update position
      updatePosition();
   	//update acceleration
      accelerate();
   	//TODO:update lane
   	
      return 1;
   }
	
   public int updateDesires()
   {
   	//TODO:(maybe)update countdown to sending a broadcast to other trucks
   	//TODO:judge current state of surroundings and figure out next step towards forming a convoy
      return 1;
   }

   public String handleMessage()
   {
	   //TODO: receive messages on UDP from "The Air"
	   
	   //TODO: determine if message is new
	   
	   //TODO: update local cache
	   
	   //TODO: return a string message to send to the air
	   
	   return "";
   }
   
   public void setLane(int lane)
   {
      this.lane = lane;
   }

   public void setPos(int pos)
   {
      this.pos = pos;
   }

   public void setSpeed(double speed)
   {
      this.speed = speed;
   }

	//set messages per second
   public void setMPS(int mps)
   {
      this.messagesPerSecond = mps;
   }
	
   public void setAcceleration(double acceleration)
   {
      this.acceleration = acceleration;
   }
	
   public void setLaneIntent(int desiredLane)
   {
      intentChangeLane = true;
      this.desiredLane = desiredLane;
   }
}













