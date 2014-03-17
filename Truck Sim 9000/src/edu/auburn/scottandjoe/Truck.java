package edu.auburn.scottandjoe;

public class Truck {
	static final double MAX_ACCELERATION = 1.0;
	static final double MIN_ACCELERATION = -3.0;
	static final int MAX_LANE = 2;
	static final int MIN_LANE = 1;
	
	public static final int RANDOMIZE_INT = -10000;
	public static final double RANDOMIZE_DOUBLE = -10000.0;
	
	int tickRate = TheAir.TICK_RATE;
	int lane;
	int truckNumber;
	int messagesPerSecond;
	int desiredLane;
	double acceleration;
	double pos;
	double speed;
	boolean hasGuns = false;
	boolean intentChangeLane = false;
	
	//initializes a truck object. truck numbering conflicts are not handled, and are the truck runners responsibility
	public Truck(int truckNumber, int lane, double pos, double speed, double acceleration)
	{
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
		}
		else
		{
			//randomize lane accordingly
			lane = MIN_LANE + (int)(Math.random() * ((MAX_LANE - MIN_LANE) + 1));
		}
		
		if(pos != RANDOMIZE_DOUBLE)
		{
			this.pos = pos;
		}
		else
		{
			//TODO:randomize position accordingly
		}
		
		if(speed != RANDOMIZE_DOUBLE)
		{
			this.speed = speed;
		}
		else
		{
			//TODO:randomize speed accordingly
		}
		
		if(acceleration != RANDOMIZE_DOUBLE)
		{
			if(acceleration > MAX_ACCELERATION || acceleration < MIN_ACCELERATION)
			{
				this.acceleration = acceleration;
			}
			else
			{
				System.out.println("[SEVERE] Truck " + truckNumber + "'s engine has " +
						"overheated and exploded from excessive acceleration/deceleration.");
				explode("Excessive acceleration/deceleration in initialization (allowable is " + MIN_ACCELERATION + " to " + MAX_ACCELERATION + ")");
			}
		}
	}
	
	private void accelerate()
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
	
	private void explode(String reason)
	{
		throw new FatalTruckException(reason);
	}
	
	private void updatePosition()
	{
		//TODO:update truck position relative to speed and tick rate
	}
	
	//collisions of trucks is a responsibility of the air to decide
	public int updatePhysical()
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
		//update countdown to sending a broadcast to other trucks
		//judge current state of surroundings and figure out next step towards forming a convoy
		return 1;
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

	public void setGuns(boolean guns)
	{
		this.hasGuns = guns;
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
	
	
	
	
	//TODO:messages to be broadcasted. will update after reading project requirements again
	private class Message
	{
		public Message()
		
	}
}













