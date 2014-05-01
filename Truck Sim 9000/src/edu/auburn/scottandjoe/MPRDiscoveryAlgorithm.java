package edu.auburn.scottandjoe;

import java.util.ArrayList;
import java.util.HashMap;

public class MPRDiscoveryAlgorithm implements FloodingAlgorithm {
	// constants
	// maximum created messages per second
	private final int MESSAGES_PER_SECOND = 110;
	// updates per second
	private static final int MPR_SELECTOR_TABLE_RATE = 1;
	//
	private static final int SEND_HELLO_RATE = 10;

	// debug rate
	private static final int DEBUG_RATE = 1;
	private long lastDebugMessageTime = 0l;
	// magical terminating character (should never show up in
	// normal use anywhere in the message)
	private final String TERMINATING_STRING = Controller.TERMINATING_STRING;

	private long lastMessageTime = 0l;
	private long lastHelloMessageTime = 0l;

	private ArrayList<ArrayList<Integer>> mNeighborTable;
	private int[] mSequenceNumberCache;
	private ArrayList<Integer> mMPRSelectorTable;
	private ArrayList<Integer> mMPRs;

	// member constant

	// member variable
	private long lastMPRCalcTime = 0l;

	public MPRDiscoveryAlgorithm(int numberOfTrucks) {
		mNeighborTable = new ArrayList<ArrayList<Integer>>(11);
		for (int i = 0; i <= 10; i++) {
			mNeighborTable.add(new ArrayList<Integer>());
		}
		mSequenceNumberCache = new int[numberOfTrucks];
		mMPRSelectorTable = new ArrayList<Integer>();
		mMPRs = new ArrayList<Integer>();
		// constructor needed because static would be hard for states
	}

	@Override
	public void handleMessage(String message, Truck theTruck) {
		// truncate on character that signifies end of message
		String messageTruncated = message.split(TERMINATING_STRING)[0];
		if ((Math.abs(System.nanoTime() - lastDebugMessageTime) / 1000000000.0) > (1.0 / (double) DEBUG_RATE)) {
			lastDebugMessageTime = System.nanoTime();
			theTruck.setLastMessageReceived(messageTruncated);
		}
		String messageType = messageTruncated.substring(0, 14);
		messageTruncated = messageTruncated.substring(14,
				messageTruncated.length());

		if (messageType.equals("MSG_TYPE_FLOOD")) {
			String[] messageSplit = messageTruncated.split(",");
			// DEBUG: check length of message in terms of elements (should be
			// 14)
			if (messageSplit.length != 14) {
				theTruck.increaseMalformedMessagesReceived();
				return;
			}
			// determine if message is new
			int messageTruckNumber = Integer.decode(messageSplit[7]);
			int messageSequenceNumber = Integer.decode(messageSplit[0]);
			if (isMessageNew(messageTruckNumber, messageSequenceNumber,
					theTruck)) {
				String forwardedMessage = "";
				// prepare Hashmap of message values
				HashMap<Truck.MessageKeys, String> messageMap = new HashMap<Truck.MessageKeys, String>();

				messageMap.put(Truck.MessageKeys.SEQUENCE_NUMBER,
						messageSplit[0]);
				messageMap.put(Truck.MessageKeys.ACCELERATION, messageSplit[4]);
				messageMap.put(Truck.MessageKeys.POSITION, messageSplit[5]);
				messageMap.put(Truck.MessageKeys.SPEED, messageSplit[6]);
				messageMap.put(Truck.MessageKeys.LANE, messageSplit[8]);
				messageMap.put(Truck.MessageKeys.DESIRED_LANE, messageSplit[9]);
				messageMap.put(Truck.MessageKeys.DESIRED_PIC, messageSplit[10]);
				messageMap.put(Truck.MessageKeys.CONVOY_ID, messageSplit[11]);
				messageMap.put(Truck.MessageKeys.ORDER_IN_CONVOY,
						messageSplit[12]);
				messageMap.put(Truck.MessageKeys.PROBABLY_FIRST,
						messageSplit[13]);

				// update local cache
				try {
					theTruck.updateCache(messageMap, messageTruckNumber);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				// check if not first place
				if (theTruck.getPos() < Double.parseDouble(messageSplit[5])) {
					theTruck.setProbablyFirst(false);
				}

				// update previous hop
				int previousHop = Integer.decode(messageSplit[3]);
				messageSplit[3] = "" + theTruck.getTruckNumber();
				if (Integer.decode(messageSplit[3]) != messageTruckNumber) {
					theTruck.setLastForwardedMessage(message);
				}
				// form the new message as a string
				for (int i = 0; i < messageSplit.length; i++) {
					forwardedMessage += messageSplit[i];
					if (i != messageSplit.length - 1) {
						forwardedMessage += ",";
					}
				}
				forwardedMessage += TERMINATING_STRING;

				/*
				 * new condition added. This truck will only forward messages
				 * received from trucks in the MPR selector table
				 */
				if (this.mMPRSelectorTable.contains(messageTruckNumber)) {
					// determine whether those messages are going to make it
					// through
					for (int i = 0; i < theTruck.getTruckPosCache().length; i++) {
						// roll the dice
						if (i != messageTruckNumber && i != previousHop
								&& i != theTruck.getTruckNumber()
								&& theTruck.isMessageSuccessful(i + 1)) {
							theTruck.sendMessage((i + 1), forwardedMessage);
							theTruck.increaseMessagesForwarded();
						}
					}
				}

			} else {
				theTruck.increaseMessagesDropped();
			}

		}

		else if (messageType.equals("MSG_TYPE_HELLO")) {

			// TODO: Check sequence number

			// imagine a hello packet with the contents
			// {SEQUENCENUMBER,6,1,5,7,8,-1,4,8,\n}

			// create a list for the first set (before the -1)
			ArrayList<Integer> neighborList = new ArrayList<Integer>();

			// split everything
			String[] messageSplit = messageTruncated.split(",");

			int helloMessageSequenceNumber = Integer.parseInt(messageSplit[0]);

			// first element is who it is from (i.e. in this case it is from
			// truck #6)
			int receivedFrom = Integer.parseInt(messageSplit[1]);

			// neighbor table uses first element to specify who this
			// neighborList belongs too (i.e. 6)
			neighborList.add(receivedFrom);

			// now we will load up the neighbor list which stops at -1
			int i = 1;
			while (Integer.parseInt(messageSplit[i]) != -1) {
				neighborList.add(Integer.parseInt(messageSplit[i]));
				i++;
			}

			// let's update mNeighborTable with the most recent list from the
			// transmitting truck
			mNeighborTable.set(receivedFrom, neighborList);

			// we will now parse the second half of the packet that is after the
			// -1 (i.e. {4,8}) - this part of the packet notifies all receivers
			// of this packet that
			// it should forward packets received from the transmitter.
			i++;
			ArrayList<Integer> receivedMPRRequests = new ArrayList<Integer>();
			while (messageSplit.length > i) {
				receivedMPRRequests.add(Integer.parseInt(messageSplit[i]));
				i++;
			}

			/*
			 * if this truck is in the request list, it will add the
			 * receivedFrom number to the selector table and will only forward
			 * packets from this truck
			 */
			if (receivedMPRRequests.contains(theTruck.getTruckNumber())) {
				this.mMPRSelectorTable.add(receivedFrom);
			}

		}

	}

	@Override
	public synchronized String createMessage(Truck theTruck) {
		// indexes marked after lines
		String message = "" + "MSG_TYPE_FLOOD"
				// 0 - sequence number
				+ theTruck.getSequenceNumber() + ","
				// 1 - source IP address
				+ theTruck.getTruckAddresses()[theTruck.getTruckNumber() - 1]
				+ "," + Controller.TRUCK_PORT + "," // 2 - source port
				+ theTruck.getTruckNumber() + "," // 3 - previous hop
				+ theTruck.getAcceleration() + "," // 4 - acceleration
				+ theTruck.getPos() + "," // 5 - position
				+ theTruck.getSpeed() + "," // 6 - speed
				+ theTruck.getTruckNumber() + "," // 7 - truck number
				+ theTruck.getLane() + "," // 8 - lane
				+ theTruck.getDesiredLane() + "," // 9 - desired lane
				+ theTruck.getDesiredPlaceInConvoy() + "," // 10 - desired place
															// in convoy
				+ theTruck.getConvoyID() + "," // 11 - convoy UUID
				+ theTruck.getOrderInConvoy() + "," // 12 - order in the convoy
				+ theTruck.getProbablyFirst() // 13 - thoughts on being first
				+ TERMINATING_STRING;
		theTruck.setSequenceNumber(theTruck.getSequenceNumber() + 1);
		return message;
	}

	// #1
	public synchronized String createHelloMessage(Truck theTruck) {
		// indexes marked after lines
		String message = ""
				+ "MSG_TYPE_HELLO"
				// 0 - sequence number
				+ mSequenceNumberCache[theTruck.getTruckNumber() - 1] + ","
				+ theTruck.getTruckNumber() + getOneHopList(theTruck) + ","
				+ "-1" + ",";

		if (mMPRs.size() > 0) {
			for (int i = 0; i < mMPRs.size(); i++) {
				message += mMPRs.get(i) + ",";
			}
		}
		message += TERMINATING_STRING;
		mSequenceNumberCache[theTruck.getTruckNumber() - 1]++;
		return message;
	}

	public String getOneHopList(Truck theTruck) {
		StringBuffer oneHopNeighbors = new StringBuffer();
		for (int i = 0; i < theTruck.getTruckPosCache().length; i++) {
			// roll the dice
			if ((i + 1) != theTruck.getTruckNumber()
					&& theTruck.getPos() + 100 > theTruck.getTruckPosCache()[i]
					&& theTruck.getPos() - 100 < theTruck.getTruckPosCache()[i]) {
				int truckNumber = i + 1;
				oneHopNeighbors.append("," + truckNumber);
			}
		}
		return oneHopNeighbors.toString();
	}

	public ArrayList<Integer> getOneHopArrayList(Truck theTruck) {
		ArrayList<Integer> oneHops = new ArrayList<Integer>();
		oneHops.add(theTruck.getTruckNumber());
		for (int i = 0; i < theTruck.getTruckPosCache().length; i++) {
			// roll the dice
			if ((i + 1) != theTruck.getTruckNumber()
					&& theTruck.getPos() + 100 > theTruck.getTruckPosCache()[i]
					&& theTruck.getPos() - 100 < theTruck.getTruckPosCache()[i]) {
				int truckNumber = i + 1;
				oneHops.add(truckNumber);
			}
		}
		return oneHops;
	}

	@Override
	public void doExtra(Truck theTruck) {
		// check if it is time to send a message about this truck
		if (((System.nanoTime() - lastMessageTime) / 1000000000.0) > (1.0 / (double) MESSAGES_PER_SECOND)) {
			// create a message
			String newMessage = createMessage(theTruck);
			// update last message time
			long theTime = System.nanoTime();
			theTruck.setLastMessageInterval((theTime - lastMessageTime));
			lastMessageTime = theTime;
			theTruck.increaseMessagesCreated();

			for (int i = 0; i < theTruck.getTruckPosCache().length; i++) {
				// roll the dice
				if ((i + 1) != theTruck.getTruckNumber()
						&& theTruck.isMessageSuccessful(i + 1)) {
					theTruck.sendMessage((i + 1), newMessage);
					theTruck.increaseMessagesSent();
					theTruck.setLastCreatedMessage(newMessage);
				}
			}

			/*
			 * the following condition will run every 1000ms and will update who
			 * this trucks MPR's are
			 */
			if ((Math.abs(System.nanoTime() - lastMPRCalcTime) / 1000000000.0) > (1.0 / (double) MPR_SELECTOR_TABLE_RATE)) {
				ArrayList<ArrayList<Integer>> preparedNeighborTable = new ArrayList<ArrayList<Integer>>();
				for (ArrayList<Integer> currentList : mNeighborTable) {
					if (!currentList.isEmpty()) {
						preparedNeighborTable.add(currentList);
					}
				}
				this.mMPRs = MPRAlgorithm.getMPR(getOneHopArrayList(theTruck),
						preparedNeighborTable);
				long theTime2 = System.nanoTime();
				lastMPRCalcTime = theTime2;
			}

		}
		// check if it is time to send a hello message
		if (((System.nanoTime() - lastHelloMessageTime) / 1000000000.0) > (.0 / (double) SEND_HELLO_RATE)) {
			lastHelloMessageTime = System.nanoTime();
			String newHelloMessage = createHelloMessage(theTruck);
			theTruck.increaseHelloMessagesCreated();

			for (int i = 0; i < theTruck.getTruckPosCache().length; i++) {
				// roll the dice
				if ((i + 1) != theTruck.getTruckNumber()
						&& theTruck.isMessageSuccessful(i + 1)) {
					theTruck.sendMessage((i + 1), newHelloMessage);
					theTruck.increaseHelloMessagesSent();
					theTruck.setLastCreatedMessage(newHelloMessage);
				}
			}
		}
	}

	private boolean isMessageNew(int messageTruckNumber,
			int messageSequenceNumber, Truck theTruck) {
		return (theTruck.getTruckSequenceCache()[messageTruckNumber - 1] < messageSequenceNumber && messageTruckNumber != theTruck
				.getTruckNumber());
	}
}