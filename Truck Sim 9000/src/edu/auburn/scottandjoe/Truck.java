package edu.auburn.scottandjoe;

public class Truck {
	static final double MAX_ACCELERATION = 1.0;
	static final double MIN_ACCELERATION = -1.0;
	int tickRate = TheAir.TICK_RATE;
	int lane;
	int truckNumber;
	int messagesPerSecond;
	double pos;
	double speed;
	boolean hasGuns = false;

	public Truck(int truckNumber)
	{
		
	}
	
	private void accelerate(double rate)
	{
		if(rate <= MAX_ACCELERATION && rate >= MIN_ACCELERATION)
		{
			//accelerate relative to tick rate
		}
		else
		{
			System.out.println("[SEVERE] Truck " + truckNumber + "'s engine has " +
					"overheated and exploded from excessive acceleration/deceleration.");
			explode();
		}
	}
	
	private void explode()
	{
		
	}
	private void updatePosition()
	{

	}
	
	public int updatePhysical()
	{
		//update position
		//update acceleration
		//update lane
		
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
}












