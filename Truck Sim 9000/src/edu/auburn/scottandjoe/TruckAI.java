package edu.auburn.scottandjoe;

import java.util.HashMap;

public class TruckAI {
	// imported constants
	private static final double MAX_ACCELERATION = Truck.MAX_ACCELERATION;
	private static final double MIN_ACCELERATION = Truck.MIN_ACCELERATION;
	private static final double MAX_REASONABLE_SPEED = Truck.MAX_REASONABLE_SPEED;
	private static final double MIN_REASONABLE_SPEED = Truck.MIN_REASONABLE_SPEED;
	private static final double TRUCK_LENGTH = Truck.TRUCK_LENGTH;
	private static final int TICK_RATE = Controller.TICK_RATE;

	// ai constants
	private static final double STABILIZING_SPEED = 31.3;
	private static final double CATCHING_SPEED = 33.2;
	private static final double MIN_CONVOY_GAP = 10.0;
	private static final double MAX_CONVOY_GAP = 20.0;
	private static final double GAS_PEDAL_ACCEL_VALUE = (0.1 / (double) TICK_RATE);
	private static final double BRAKE_PEDAL_DECEL_VALUE = (0.3 / (double) TICK_RATE);

	// truck state magic number constants
	// ai states:
	// 0 - new truck object. has had no thoughts
	// 1 - just started, is waiting a short time for all trucks to stabilize
	// speed
	// 2 - speeds initially stabilized
	// 3 - is 1st and is leader of convoy (go stabilizing speed)
	// 4 - is in convoy that has person who is 1st (maintain gap)
	// 5 - is in convoy that doesn't have a leader who is 1st (catch others and
	// avoid collisions)
	public static final int NEW_TRUCK = 0;
	public static final int STABILIZING = 1;
	public static final int STABILIZED = 2;
	public static final int FIRST_CONVOY_LEADER = 3;
	public static final int FIRST_CONVOY_MEMBER = 4;
	public static final int NON_FIRST_CONVOY_MEMBER = 5;

	// ai variables
	private int stabilizingCountdown = 0;
	private double startPos;
	private double desiredSpeed = 0;
	private int truckAIState = NEW_TRUCK;
	private boolean fullConvoy = false;

	// always call doAI after you have handled messages for this tick in order
	// to avoid race conditions and inaccurate calculations for distances.

	public TruckAI() {
		// only has a constructor so that it can keep a state
		// if it was just static, this would be cumbersome to work around
	}

	public void doAI(Truck theTruck) {
		// grab truck cache and truck initialized cache for less ugly code
		Truck[] truckCache = theTruck.getTruckCache();
		boolean[] truckInitialized = theTruck.getTruckInitialized();
		Truck nextTruck = null;
		double nextTruckPos = 9999999999.0;
		double nextTruckGap = 9999999999.0;

		// check if newest update to cache makes this truck not first
		if (theTruck.getProbablyFirst()
				&& theTruck.getLastMessageMapTime() > 0l) {
			HashMap<Truck.MessageKeys, String> lastMessageMap = theTruck
					.getLastMessageMap();
			double messagePos = Double.parseDouble(lastMessageMap
					.get(Truck.MessageKeys.POSITION));
			if (theTruck.getPos() < messagePos) {
				theTruck.setProbablyFirst(false);
			}
		}

		// calculate the next truck
		for (int i = 0; i < truckInitialized.length; i++) {
			// check for not self, initialized, position greater than self,
			// position less than running next truck dist calc
			if ((i + 1) != theTruck.getTruckNumber() && truckInitialized[i]
					&& truckCache[i].getPos() > theTruck.getPos()
					&& truckCache[i].getPos() < nextTruckPos) {
				nextTruck = truckCache[i];
				nextTruckPos = nextTruck.getPos();
			}
		}
		nextTruckGap = nextTruckPos - theTruck.getPos() - TRUCK_LENGTH;

		// switch statement to enter state logic
		switch (truckAIState) {
		case NEW_TRUCK:
			// save initial stuff for later calculation purposes
			startPos = theTruck.getPos();
			// predict approximate position relative to other trucks based on
			// start pos
			if (startPos == Truck.MAX_INITIAL_POS) {
				theTruck.setProbablyFirst(true);
			} else {
				theTruck.setProbablyFirst(false);
			}

			// set desired speed to stabilizing speed
			desiredSpeed = STABILIZING_SPEED;
			// start stabilizing countdown timer using tick rate for reference
			stabilizingCountdown = TICK_RATE * 10;

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
			// determine which of the three main states the truck is

			// if 1st, set to first convoy leader and set desired speed to
			// stabilizing
			if (theTruck.getProbablyFirst()) {
				truckAIState = FIRST_CONVOY_LEADER;
				break;
			}

			// if in 1st convoy, set to first convoy member
			for (int i = 0; i < truckInitialized.length; i++) {
				// check for not self, initialized, same convoy, and if first
				if ((i + 1) != theTruck.getTruckNumber()
						&& truckInitialized[i]
						&& truckCache[i].getConvoyID().equals(
								theTruck.getConvoyID())
						&& truckCache[i].getProbablyFirst()) {
					truckAIState = FIRST_CONVOY_MEMBER;
					break;
				}
			}

			// if not in 1st convoy, set to non first convoy member, set desired
			// speed to catching speed
			truckAIState = NON_FIRST_CONVOY_MEMBER;
			break;

		case FIRST_CONVOY_LEADER:
			// if full convoy, send end to end message once per second
			if (fullConvoy
					&& (System.nanoTime() - theTruck.getLastEndToEndSendTime()) > 1000000000.0) {
				theTruck.setLastEndToEndSendTime(System.nanoTime());
				theTruck.setEndToEndSequence(theTruck.getEndToEndSequence() + 1);
				System.out.println("[NORMAL] Sending end to end packet.");
				String endToEndMessage = "e2e,"
						+ theTruck.getEndToEndSequence()
						+ Controller.TERMINATING_STRING;
				for (int i = 0; i < theTruck.getTruckPosCache().length; i++) {
					// roll the dice
					if ((i + 1) != theTruck.getTruckNumber()
							&& theTruck.isMessageSuccessful(i + 1)) {
						theTruck.sendMessage((i + 1), endToEndMessage);
						theTruck.setLastCreatedMessage(endToEndMessage);
					}
				}
			}
			desiredSpeed = STABILIZING_SPEED;
			break;

		case FIRST_CONVOY_MEMBER:
			// maintain distance to next truck
			if (nextTruckGap > MAX_CONVOY_GAP) {
				desiredSpeed = CATCHING_SPEED;
			}
			if (nextTruckGap < MIN_CONVOY_GAP
					&& desiredSpeed > MIN_REASONABLE_SPEED) {
				desiredSpeed -= BRAKE_PEDAL_DECEL_VALUE;
			}
			if (nextTruckGap > MIN_CONVOY_GAP && nextTruckGap < MAX_CONVOY_GAP) {
				desiredSpeed = STABILIZING_SPEED;
				if (nextTruckGap > 15) {
					desiredSpeed = STABILIZING_SPEED + 0.1;
				}
			}
			break;

		case NON_FIRST_CONVOY_MEMBER:
			// if too close to next, avoid collision
			if (nextTruckGap < MIN_CONVOY_GAP
					&& desiredSpeed > MIN_REASONABLE_SPEED) {
				desiredSpeed -= BRAKE_PEDAL_DECEL_VALUE;
			}
			// if not, go catching speed.
			if (nextTruckGap > MAX_CONVOY_GAP && nextTruckGap < 100.0) {
				desiredSpeed = CATCHING_SPEED;
			}
			// if in sweetspot
			if (nextTruckGap > MIN_CONVOY_GAP && nextTruckGap < MAX_CONVOY_GAP) {
				if (nextTruck != null) {
					desiredSpeed = nextTruck.getSpeed();
				} else {
					desiredSpeed = STABILIZING_SPEED;
				}
			}

			// if gap is substantial, drive really fast
			if (nextTruckGap > 100) {
				desiredSpeed = MAX_REASONABLE_SPEED;
			}

			// check if truck has become part of 1st convoy
			for (int i = 0; i < truckInitialized.length; i++) {
				// check for not self, initialized, same convoy, and if first
				if ((i + 1) != theTruck.getTruckNumber()
						&& truckInitialized[i]
						&& truckCache[i].getConvoyID().equals(
								theTruck.getConvoyID())
						&& truckCache[i].getProbablyFirst()) {
					truckAIState = FIRST_CONVOY_MEMBER;
					break;
				}
			}
			// TODO: can't find why truck 2 isn't getting updates from truck 1
			// here is a quick fix
			if (fullConvoy) {
				truckAIState = FIRST_CONVOY_MEMBER;
			}
			break;

		default:
			break;
		}
		// adopt convoy id of nearest truck in front of you
		if (nextTruck != null
				&& !nextTruck.getConvoyID().equals(theTruck.getConvoyID())) {
			theTruck.setConvoyID(nextTruck.getConvoyID());
		}

		// adopt (place in convoy + 1) of nearest truck in front of you
		if (nextTruck != null) {
			theTruck.setOrderInConvoy(nextTruck.getOrderInConvoy() + 1);
		}

		// become leader if leader left
		boolean leaderExists = false;
		if (theTruck.getOrderInConvoy() == 1) {
			leaderExists = true;
		}
		for (int i = 0; i < truckInitialized.length; i++) {
			// check for i = not self, initialized, a truck ahead, with same
			// convoy, with order smaller than self
			if ((i + 1) != theTruck.getTruckNumber()
					&& truckInitialized[i]
					&& truckCache[i].getPos() > theTruck.getPos()
					&& truckCache[i].getConvoyID().equals(
							theTruck.getConvoyID())
					&& truckCache[i].getOrderInConvoy() < theTruck
							.getOrderInConvoy()) {
				leaderExists = true;
			}
		}
		if (!leaderExists) {
			theTruck.setOrderInConvoy(1);
		}

		// if convoy size is the same as the simulation population, become full
		if (theTruck.getConvoySize() == theTruck.getDesiredTruckSimPop()) {
			fullConvoy = true;
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
			if (theTruck.getAcceleration() > MIN_ACCELERATION + 0.3) {
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

	public int getAIState() {
		return this.truckAIState;
	}
}
