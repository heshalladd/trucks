package edu.auburn.scottandjoe;

public class TruckAI {
	// ai constants
	private static final double STABILIZING_SPEED = 31.3;
	private static final double CATCHING_SPEED = 31.5;
	private static final double SOLO_CATCHING_SPEED = 31.5;
	private static final double MERGING_CATCHING_SPEED = 31.5;
	private static final double MULTI_CATCHING_SPEED = 31.5;
	private static final double MIN_CONVOY_GAP = 15.0;
	private static final double MAX_CONVOY_GAP = 25.0;
	
	//ai variables
	private int stabilizingCountdown = 0;
	private double desiredSpeed;
	private double startPos;
	
	//TODO: a method that accepts a clone of a truck (avoids race conditions) and does ai work
	//TODO: a method for returning new desired speed
	//TODO: a method for returning new acceleration
	//TODO: continue coming up with TODO's for this class.
	public void dummyMethod(){
		// switch statement to enter state logic
				switch (truckAIState) {
				case NEW_TRUCK:
					// save initial stuff for later calculation purposes
					startPos = pos;
					desiredSpeed = speed;
					// predict approximate position relative to other trucks based on
					// start pos
					if (startPos <= 270) {
						probablyFirst = false;
					}
					if (startPos > 270) {
						probablyFirst = true;
					}

					// set desired speed to stabilizing speed
					desiredSpeed = STABILIZING_SPEED;
					// start stabilizing countdown timer using tick rate for reference
					stabilizingCountdown = (int) (tickRate * Math.ceil(Math.max(
							MAX_REASONABLE_SPEED - STABILIZING_SPEED, STABILIZING_SPEED
									- MIN_REASONABLE_SPEED)
							/ Math.min(MAX_ACCELERATION, MIN_ACCELERATION)));

					// !debug set stabilizing countdown to what it should be
					stabilizingCountdown = 67;

					// move to next state
					truckAIState = STABILIZING;
					break;

				case STABILIZING:
					// if done stabilizing, become stabilized
					if (stabilizingCountdown <= 0) {
						truckAIState = STABILIZED;
					}
					stabilizingCountdown--;
					break;

				case STABILIZED:
					// // become a solo convoy and move over to lane 1
					// desiredLane = 1;
					truckAIState = SOLO_CONVOY;
					// NOTE: this state is here in case something else needs to happen
					// when tweaking AI code
					break;

				case SOLO_CONVOY:
					// if there is a truck in this convoy with position 5, become full
					// convoy
					if (getConvoySize() == 5) {
						truckAIState = FULL_CONVOY;
					}
					// search for a truck ahead
					for (int i = 0; i < truckCache.length; i++) {
						// NOTE: this biases forward merging
						if (truckInitialized[i] && truckNumber - 1 != i
								&& truckCache[i].getPos() > pos
								&& !truckCache[i].getConvoyID().equals(convoyID)
								&& truckCache[i].getLane() == lane) {
							// if a truck has been found ahead, change to merging convoy
							// and join the convoy ahead
							truckAIState = MERGING_CONVOY;
							desiredSpeed = MERGING_CATCHING_SPEED;
							convoyID = truckCache[i].getConvoyID();
							orderInConvoy = truckCache[i].getOrderInConvoy() + 1;
							break;
						}
					}

					// based on positional guess, accelerate or decelerate to find more
					// trucks
					// if not first, accelerate
					if (!probablyFirst) {
						desiredSpeed = SOLO_CATCHING_SPEED;
					}
					break;

				case MULTI_CONVOY:
					// if there is a truck in this convoy with position 5, become full
					// convoy
					if (getConvoySize() == 5) {
						truckAIState = FULL_CONVOY;
					}
					// if the leader of the convoy isn't the first truck,
					// accelerate while maintaining distance
					for (int i = 0; i < truckCache.length; i++) {
						if (truckInitialized[i] && truckNumber - 1 == i
								&& !truckCache[i].getProbablyFirst()
								&& truckCache[i].getConvoyID().equals(convoyID)
								&& truckCache[i].getOrderInConvoy() == 1
								&& desiredSpeed == STABILIZING_SPEED) {
							desiredSpeed = MULTI_CATCHING_SPEED;
							break;
						}
					}
					// if another truck is found in a different convoy, attempt to
					// join their convoy if they are ahead (must disconnect from current
					// convoy and become solo for a tick)
					for (int i = 0; i < truckCache.length; i++) {
						// NOTE: this biases forward merging
						if (truckInitialized[i]
								&& truckNumber - 1 != i
								&& truckCache[i].getPos() > pos
								&& !truckCache[i].getConvoyID().equals(convoyID)
								&& truckCache[i].getPos() - pos > MAX_CONVOY_GAP
										+ truckLength
								&& truckCache[i].getLane() == lane) {
							truckAIState = SOLO_CONVOY;
							break;
						}
					}
					break;

				case FULL_CONVOY:
					// TODO: if in a full convoy, broadcast end to end timing packets
					break;

				case MERGING_CONVOY:
					// if there is a truck in this convoy with position 5, become full
					// convoy
					if (getConvoySize() == 5) {
						truckAIState = FULL_CONVOY;
					}

					// try to close the gap to an acceptable range with the truck
					// in front of it
					Truck[] truckCacheFreeze = truckCache.clone();
					Truck nextTruck = null;
					double nextTruckPos = 999999.0;
					for (int i = 0; i < truckCacheFreeze.length; i++) {
						if (truckInitialized[i] && truckNumber - 1 != i
								&& truckCacheFreeze[i].getPos() < nextTruckPos
								&& truckCacheFreeze[i].getPos() > pos) {
							nextTruck = truckCacheFreeze[i];
							nextTruckPos = truckCacheFreeze[i].getPos();
							break;
						}
					}
					if (nextTruck != null) {
						convoyID = nextTruck.getConvoyID();
						orderInConvoy = nextTruck.getOrderInConvoy() + 1;
						if (nextTruck.getPos() - pos < MAX_CONVOY_GAP + truckLength) {
							// once within acceptable range, become a MULTI_CONVOY
							truckAIState = MULTI_CONVOY;
							desiredSpeed = STABILIZING_SPEED;
						}
					}
					break;

				default:
					break;
				}

				// logic to try to maintain a gap
				Truck[] truckCacheFreeze = truckCache.clone();
				Truck nextTruck = null;
				double nextTruckPos = 999999.0;
				for (int i = 0; i < truckCacheFreeze.length; i++) {
					if (truckInitialized[i] && truckNumber - 1 != i
							&& truckCacheFreeze[i].getPos() < nextTruckPos
							&& truckCacheFreeze[i].getPos() > pos) {
						nextTruck = truckCacheFreeze[i];
						nextTruckPos = truckCacheFreeze[i].getPos();
						break;
					}
				}
				if (nextTruck != null
						&& nextTruck.getPos() - pos < MIN_CONVOY_GAP + truckLength
						&& desiredSpeed > MIN_REASONABLE_SPEED) {
					desiredSpeed = desiredSpeed - 0.15;
				}
				if (nextTruck != null
						&& nextTruck.getPos() - pos > MAX_CONVOY_GAP + truckLength
						&& desiredSpeed < CATCHING_SPEED) {
					desiredSpeed = desiredSpeed + 0.05;
				}
				if (nextTruck != null
						&& nextTruck.getPos() - pos > MIN_CONVOY_GAP + truckLength
						&& nextTruck.getPos() - pos < MAX_CONVOY_GAP + truckLength) {
					desiredSpeed = STABILIZING_SPEED;
				}
				if (nextTruck != null && nextTruck.getPos() - pos < 40) {
					convoyID = nextTruck.getConvoyID();
					orderInConvoy = nextTruck.getOrderInConvoy() + 1;
				}
				// logic to try to make truck its desired speed by modifying
				// acceleration
				if (this.speed < this.desiredSpeed) {
					if (this.acceleration < MAX_ACCELERATION - 0.1) {
						if (this.acceleration < 0) {
							this.acceleration = 0;
						} else {
							this.acceleration += 0.01;
						}
					}
				} else {
					if (this.acceleration > MIN_ACCELERATION + 0.4) {
						if (this.acceleration > 0) {
							this.acceleration = 0;
						} else {
							this.acceleration -= 0.03;
						}
					}
				}
				if (this.speed == this.desiredSpeed) {
					acceleration = 0;
				}

	}
}
