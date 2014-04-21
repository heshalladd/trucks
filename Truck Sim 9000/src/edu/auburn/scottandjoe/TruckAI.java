package edu.auburn.scottandjoe;

public class TruckAI {
	// imported constants
	private static final double MAX_ACCELERATION = Truck.MAX_ACCELERATION;
	private static final double MIN_ACCELERATION = Truck.MIN_ACCELERATION;
	private static final double MAX_REASONABLE_SPEED = Truck.MAX_REASONABLE_SPEED;
	private static final double MIN_REASONABLE_SPEED = Truck.MIN_REASONABLE_SPEED;
	//max lane and min lane aren't used currently because of lack of lane functionality
	@SuppressWarnings("unused")
	private static final int MAX_LANE = Truck.MIN_LANE;
	@SuppressWarnings("unused")
	private static final int MIN_LANE = Truck.MAX_LANE;
	private static final double TRUCK_LENGTH = Truck.TRUCK_LENGTH;
	private static final int TICK_RATE = Controller.TICK_RATE;

	// ai constants
	private static final double STABILIZING_SPEED = 31.3;
	private static final double CATCHING_SPEED = 31.5;
	private static final double SOLO_CATCHING_SPEED = 31.5;
	private static final double MERGING_CATCHING_SPEED = 31.5;
	private static final double MULTI_CATCHING_SPEED = 31.5;
	private static final double MIN_CONVOY_GAP = 15.0;
	private static final double MAX_CONVOY_GAP = 25.0;
	private static final double GAS_PEDAL_ACCEL_VALUE = (0.1 / (double) TICK_RATE);
	private static final double BRAKE_PEDAL_DECEL_VALUE = (0.3 / (double) TICK_RATE);
	// truck state magic number constants
	// ai states:
	// 0 - new truck object. has had no thoughts
	// 1 - just started, is waiting minimum time for all trucks to stabilize
	// speed
	// 2 - speeds initially stabilized
	// 3 - in solo convoy and seeking others
	// 4 - in a multi convoy and seeking others
	// 5 - in a full convoy
	// 6 - collided
	// 7 - merging convoy(trying to join the convoy in front of it)
	public static final int NEW_TRUCK = 0;
	public static final int STABILIZING = 1;
	public static final int STABILIZED = 2;
	public static final int SOLO_CONVOY = 3;
	public static final int MULTI_CONVOY = 4;
	public static final int FULL_CONVOY = 5;
	public static final int COLLIDED = 6;
	public static final int MERGING_CONVOY = 7;

	// ai variables
	private int stabilizingCountdown = 0;
	private double startPos;
	private double desiredSpeed = 0;
	private int truckAIState = NEW_TRUCK;

	// always call doAI after you have handled messages for this tick in order
	// to avoid race conditions and inaccurate calculations for distances.
	// TODO: continue tweaking ai to make it better. it is satisfactory at the
	// moment

	public TruckAI() {
		// only has a constructor so that it can keep a state
		// if it was just static, this would be cumbersome to work around
	}

	public int getAIState() {
		return this.truckAIState;
	}

	public void doAI(Truck theTruck) {
		// grab truck cache and truck initialized cache for less ugly code
		// NOTE: MAKE SURE YOU ONLY USE GETTERS ON THESE. CHANGES ARE NOT
		// GUARANTEED TO BE KEPT BY THE TRUCK THAT CALLED THIS
		Truck[] truckCache = theTruck.getTruckCache();
		boolean[] truckInitialized = theTruck.getTruckInitialized();

		// switch statement to enter state logic
		switch (truckAIState) {
		case NEW_TRUCK:
			// save initial stuff for later calculation purposes
			startPos = theTruck.getPos();
			desiredSpeed = theTruck.getSpeed();
			// predict approximate position relative to other trucks based on
			// start pos
			if (startPos <= 270) {
				theTruck.setProbablyFirst(false);
			}
			if (startPos > 270) {
				theTruck.setProbablyFirst(true);
			}

			// set desired speed to stabilizing speed
			desiredSpeed = STABILIZING_SPEED;
			// start stabilizing countdown timer using tick rate for reference
			stabilizingCountdown = (int) (TICK_RATE * Math.ceil(Math.max(
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
			if (theTruck.getConvoySize() == 5) {
				truckAIState = FULL_CONVOY;
			}
			// search for a truck ahead
			for (int i = 0; i < truckCache.length; i++) {
				// NOTE: this biases forward merging
				if (truckInitialized[i]
						&& theTruck.getTruckNumber() - 1 != i
						&& truckCache[i].getPos() > theTruck.getPos()
						&& !truckCache[i].getConvoyID().equals(
								theTruck.getConvoyID())
						&& truckCache[i].getLane() == theTruck.getLane()) {
					// if a truck has been found ahead, change to merging convoy
					// and join the convoy ahead
					truckAIState = MERGING_CONVOY;
					desiredSpeed = MERGING_CATCHING_SPEED;
					theTruck.setConvoyID(truckCache[i].getConvoyID());
					theTruck.setOrderInConvoy(truckCache[i].getOrderInConvoy() + 1);
					break;
				}
			}

			// based on positional guess, accelerate or decelerate to find more
			// trucks
			// if not first, accelerate
			if (!theTruck.getProbablyFirst()) {
				desiredSpeed = SOLO_CATCHING_SPEED;
			}
			break;

		case MULTI_CONVOY:
			// if there is a truck in this convoy with position 5, become full
			// convoy
			if (theTruck.getConvoySize() == 5) {
				truckAIState = FULL_CONVOY;
			}
			// if the leader of the convoy isn't the first truck,
			// accelerate while maintaining distance
			for (int i = 0; i < truckCache.length; i++) {
				if (truckInitialized[i]
						&& theTruck.getTruckNumber() - 1 == i
						&& !truckCache[i].getProbablyFirst()
						&& truckCache[i].getConvoyID().equals(
								theTruck.getConvoyID())
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
						&& theTruck.getTruckNumber() - 1 != i
						&& truckCache[i].getPos() > theTruck.getPos()
						&& !truckCache[i].getConvoyID().equals(
								theTruck.getConvoyID())
						&& truckCache[i].getPos() - theTruck.getPos() > MAX_CONVOY_GAP
								+ TRUCK_LENGTH
						&& truckCache[i].getLane() == theTruck.getLane()) {
					truckAIState = SOLO_CONVOY;
					break;
				}
			}
			break;

		case FULL_CONVOY:
			// TODO: if in a full convoy, broadcast end to end timing packets and do some calculations
			break;

		case MERGING_CONVOY:
			// if there is a truck in this convoy with position 5, become full
			// convoy
			if (theTruck.getConvoySize() == 5) {
				truckAIState = FULL_CONVOY;
			}

			// try to close the gap to an acceptable range with the truck
			// in front of it
			Truck[] truckCacheFreeze = truckCache.clone();
			Truck nextTruck = null;
			double nextTruckPos = 999999.0;
			for (int i = 0; i < truckCacheFreeze.length; i++) {
				if (truckInitialized[i] && theTruck.getTruckNumber() - 1 != i
						&& truckCacheFreeze[i].getPos() < nextTruckPos
						&& truckCacheFreeze[i].getPos() > theTruck.getPos()) {
					nextTruck = truckCacheFreeze[i];
					nextTruckPos = truckCacheFreeze[i].getPos();
					break;
				}
			}
			if (nextTruck != null) {
				theTruck.setConvoyID(nextTruck.getConvoyID());
				theTruck.setOrderInConvoy(nextTruck.getOrderInConvoy() + 1);
				if (nextTruck.getPos() - theTruck.getPos() < MAX_CONVOY_GAP
						+ TRUCK_LENGTH) {
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
			if (truckInitialized[i] && theTruck.getTruckNumber() - 1 != i
					&& truckCacheFreeze[i].getPos() < nextTruckPos
					&& truckCacheFreeze[i].getPos() > theTruck.getPos()) {
				nextTruck = truckCacheFreeze[i];
				nextTruckPos = truckCacheFreeze[i].getPos();
				break;
			}
		}
		if (nextTruck != null
				&& nextTruck.getPos() - theTruck.getPos() < MIN_CONVOY_GAP
						+ TRUCK_LENGTH && desiredSpeed > MIN_REASONABLE_SPEED) {
			desiredSpeed = desiredSpeed - 0.15;
		}
		if (nextTruck != null
				&& nextTruck.getPos() - theTruck.getPos() > MAX_CONVOY_GAP
						+ TRUCK_LENGTH && desiredSpeed < CATCHING_SPEED) {
			desiredSpeed = desiredSpeed + 0.05;
		}
		if (nextTruck != null
				&& nextTruck.getPos() - theTruck.getPos() > MIN_CONVOY_GAP
						+ TRUCK_LENGTH
				&& nextTruck.getPos() - theTruck.getPos() < MAX_CONVOY_GAP
						+ TRUCK_LENGTH) {
			desiredSpeed = STABILIZING_SPEED;
		}
		if (nextTruck != null && nextTruck.getPos() - theTruck.getPos() < 40) {
			theTruck.setConvoyID(nextTruck.getConvoyID());
			theTruck.setOrderInConvoy(nextTruck.getOrderInConvoy() + 1);
		}
		// logic to try to make truck its desired speed by modifying
		// acceleration
		if (theTruck.getSpeed() < desiredSpeed) {
			if (theTruck.getAcceleration() < MAX_ACCELERATION - 0.1) {
				if (theTruck.getAcceleration() < 0) {
					theTruck.setAcceleration(0);
				} else {
					theTruck.setAcceleration(theTruck.getAcceleration()
							+ GAS_PEDAL_ACCEL_VALUE);
				}
			}
		} else {
			if (theTruck.getAcceleration() > MIN_ACCELERATION + 0.4) {
				if (theTruck.getAcceleration() > 0) {
					theTruck.setAcceleration(0);
				} else {
					theTruck.setAcceleration(theTruck.getAcceleration()
							- BRAKE_PEDAL_DECEL_VALUE);
				}
			}
		}
		if (theTruck.getSpeed() == desiredSpeed) {
			theTruck.setAcceleration(0);
		}

	}
}
