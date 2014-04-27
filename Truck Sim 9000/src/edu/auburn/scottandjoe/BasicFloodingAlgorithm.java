package edu.auburn.scottandjoe;

import java.util.ArrayList;
import java.util.HashMap;

public class BasicFloodingAlgorithm implements FloodingAlgorithm {
	// constants
	// maximum created messages per second
	private final int MESSAGES_PER_SECOND = 50;
	// magical terminating character (should never show up in
	// normal use anywhere in the message)
	private final String TERMINATING_CHAR = "]";

	private float lastMessageTime = 0l;

	@Override
	public void handleMessage(String message, Truck theTruck) {
		// truncate on character that signifies end of message
		String messageTruncated = message.split(TERMINATING_CHAR)[0];
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
			// TODO: prepare Hashmap of message values
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
			forwardedMessage += TERMINATING_CHAR;

			ArrayList<Truck> trucksInRange = new ArrayList<Truck>();
			// determine what trucks are valid and in range
			for (int i = 0; i < theTruck.getTruckCache().length; i++) {
				if (i != previousHop - 1 && i != theTruck.getTruckNumber() - 1
						&& i != messageTruckNumber
						&& theTruck.getTruckInitialized()[i]) {
					trucksInRange.add(theTruck.getTruckCache()[i]);
				}
			}

			// determine whether those messages are going to make it
			// through
			if (trucksInRange.size() > 0) {
				for (int i = 0; i < trucksInRange.size(); i++) {
					// roll the dice
					Truck targetTruck = trucksInRange.get(i);
					if (theTruck.isMessageSuccessful(targetTruck)) {
						theTruck.sendMessage(targetTruck, forwardedMessage);
						theTruck.increaseMessagesForwarded();
					}
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
				+ theTruck.getTruckAddresses()[theTruck.getTruckNumber()] + ","
				+ Controller.PORT + "," // 2 - source port
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
				+ TERMINATING_CHAR;
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
			lastMessageTime = System.nanoTime();
			theTruck.increaseMessagesCreated();

			// find trucks in range
			ArrayList<Truck> trucksInRange = new ArrayList<Truck>();

			// determine what trucks are in range
			for (int i = 0; i < theTruck.getTruckCache().length; i++) {
				if (i != theTruck.getTruckNumber() - 1
						&& theTruck.getTruckInitialized()[i]) {
					trucksInRange.add(theTruck.getTruckCache()[i]);
				}
			}
			if (trucksInRange.size() > 0) {
				for (int i = 0; i < trucksInRange.size(); i++) {
					// roll the dice
					Truck targetTruck = trucksInRange.get(i);
					if (theTruck.isMessageSuccessful(targetTruck)) {
						theTruck.sendMessage(targetTruck, newMessage);
						theTruck.increaseMessagesSent();
						theTruck.setLastCreatedMessage(newMessage);
					}
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