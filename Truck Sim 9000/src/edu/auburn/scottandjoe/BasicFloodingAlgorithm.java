package edu.auburn.scottandjoe;

import java.util.HashMap;

public class BasicFloodingAlgorithm implements FloodingAlgorithm {
	// constants
	// maximum created messages per second
	private final int MESSAGES_PER_SECOND = 110;
	// magical terminating character (should never show up in
	// normal use anywhere in the message)
	private final String TERMINATING_STRING = Controller.TERMINATING_STRING;

	private long lastMessageTime = 0l;

	public BasicFloodingAlgorithm() {
		// constructor needed because static would be hard for states
	}

	@Override
	public void handleMessage(String message, Truck theTruck) {
		// truncate on character that signifies end of message
		theTruck.increaseMessagesReceived();
		String messageTruncated = message.split(TERMINATING_STRING)[0];
		theTruck.setLastMessageReceived(messageTruncated);
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
		if (isMessageNew(messageTruckNumber, messageSequenceNumber, theTruck)) {
			String forwardedMessage = "";
			// prepare Hashmap of message values
			HashMap<Truck.MessageKeys, String> messageMap = new HashMap<Truck.MessageKeys, String>();
			messageMap.put(Truck.MessageKeys.SEQUENCE_NUMBER, messageSplit[0]);
			messageMap.put(Truck.MessageKeys.ACCELERATION, messageSplit[4]);
			messageMap.put(Truck.MessageKeys.POSITION, messageSplit[5]);
			messageMap.put(Truck.MessageKeys.SPEED, messageSplit[6]);
			messageMap.put(Truck.MessageKeys.LANE, messageSplit[8]);
			messageMap.put(Truck.MessageKeys.DESIRED_LANE, messageSplit[9]);
			messageMap.put(Truck.MessageKeys.DESIRED_PIC, messageSplit[10]);
			messageMap.put(Truck.MessageKeys.CONVOY_ID, messageSplit[11]);
			messageMap.put(Truck.MessageKeys.ORDER_IN_CONVOY, messageSplit[12]);
			messageMap.put(Truck.MessageKeys.PROBABLY_FIRST, messageSplit[13]);
			// update local cache
			try {
				theTruck.updateCache(messageMap, messageTruckNumber);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (FatalTruckException e) {
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

		} else {
			theTruck.increaseMessagesDropped();
		}
	}

	@Override
	public synchronized String createMessage(Truck theTruck) {
		// indexes marked after lines
		String message = ""
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

		}
	}

	private boolean isMessageNew(int messageTruckNumber,
			int messageSequenceNumber, Truck theTruck) {
		return (theTruck.getTruckSequenceCache()[messageTruckNumber - 1] < messageSequenceNumber && messageTruckNumber != theTruck
				.getTruckNumber());
	}
}