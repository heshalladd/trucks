package edu.auburn.scottandjoe;

public interface FloodingAlgorithm {
	// Does logic for any incoming UDP message for this truck
	// make sure to call updateCache() to keep the truck platooning AI happy.
	void handleMessage(String message, Truck theTruck);

	// Creates a brand new message for broadcasting. Needs to include
	// information about the truck that the AI can use to make decisions.
	// Useful and necessary information includes (but is not limited to):
	// --pos
	// --speed
	// --convoyID
	// --orderInConvoy
	// --probablyFirst
	// --truckNumber
	// --lane
	// etc. For some ideas, see BasicFloodingAlgorithm.java
	String createMessage(Truck theTruck);

	// here is where you do stuff not related to handling each incoming message
	// this usually involves creating new messages and doing logic to create and
	// maintain network links if your protocol involves that
	void doExtra(Truck theTruck);
}
