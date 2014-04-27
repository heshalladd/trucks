package edu.auburn.scottandjoe;

import java.util.ArrayList;

public class BasicFloodingAlgorithm implements FloodingAlgorithm {

	@Override
	public void handleMessage(String message, Truck theTruck) {

	}

	@Override
	public synchronized String createMessage(Truck theTruck) {
		// indexes marked after lines
		String message = "" + theTruck.getSequenceNumber() + "," // 0 - sequence number
				+ theTruck.getTruckAddresses()[theTruck.getTruckNumber()] + "," // 1 - source IP address
				+ Controller.PORT + "," // 2 - source port
				+ theTruck.getTruckNumber() + "," // 3 - previous hop
				+ theTruck.getAcceleration() + "," // 4 - acceleration
				+ theTruck.getPos() + "," // 5 - position
				+ theTruck.getSpeed() + "," // 6 - speed
				+ theTruck.getTruckNumber() + "," // 7 - truck number
				+ theTruck.getLane() + "," // 8 - lane
				+ theTruck.getDesiredLane() + "," // 9 - desired lane
				+ theTruck.getDesiredPlaceInConvoy() + "," // 10 - desired place in convoy
				+ theTruck.getConvoyID() + "," // 11 - convoy UUID
				+ theTruck.getOrderInConvoy() + "," // 12 - order in the convoy
				+ theTruck.getProbablyFirst() // 13 - thoughts on being first
				+ "]"; // magical terminating character (should never show up in
						// normal use anywhere in the message)
		theTruck.setSequenceNumber(theTruck.getSequenceNumber() + 1);
		return message;
	}

	@Override
	public void doExtra(Truck theTruck) {
		// truncate on character that signifies end of message
		String messageToProcessTruncated = messageToProcessWhole.split("]")[0];
		String[] messageToProcess = messageToProcessTruncated.split(",");

		// DEBUG: check length of message in terms of elements (should be
		// 14)
		if (messageToProcess.length != 14) {
			malformedMessagesReceived++;
			break;
		}
		// determine if message is new
		int messageTruckNumber = Integer.decode(messageToProcess[7]);
		int messageSequenceNumber = Integer.decode(messageToProcess[0]);
		if (isMessageNew(messageTruckNumber, messageSequenceNumber)) {
			forwardedMessage = "";
			// update local cache
			updateCache(messageToProcess);
			// check if not first place
			if (pos < Double.parseDouble(messageToProcess[5])) {
				probablyFirst = false;
			}

			// update previous hop
			int previousHop = Integer.decode(messageToProcess[3]);
			messageToProcess[3] = "" + truckNumber;
			if (Integer.decode(messageToProcess[3]) != messageTruckNumber) {
				lastMessageToForward = messageToProcessWhole;
			}
			// form the new message as a string
			for (int i = 0; i < messageToProcess.length; i++) {
				forwardedMessage += messageToProcess[i];
				if (i != 0 && i != messageToProcess.length - 1) {
					forwardedMessage += ",";
				}
			}

			ArrayList<Truck> trucksInRange = new ArrayList<Truck>();

			// determine what trucks are valid and in range
			for (int i = 0; i < truckCache.length; i++) {
				if (i != previousHop - 1 && i != truckNumber - 1
						&& i != messageTruckNumber && truckInitialized[i]) {
					trucksInRange.add(truckCache[i]);
				}
			}

			// determine whether those messages are going to make it
			// through
			if (trucksInRange.size() > 0) {
				for (int i = 0; i < trucksInRange.size(); i++) {
					// roll the dice
					Truck targetTruck = trucksInRange.get(i);
					if (isMessageSuccessful(targetTruck)) {
						sendMessage(targetTruck, forwardedMessage);
						messagesForwarded++;
					}
				}
			}
		} else {
			messagesDropped++;
		}

		// check if it is time to send a message about this truck
		if (((System.nanoTime() - lastMessageTime) / 1000000000.0) > (1.0 / (double) messagesPerSecond)) {
			// create a message
			String newMessage = theFA.createMessage(this);
			// update last message time
			lastMessageTime = System.nanoTime();
			messagesCreated++;

			// find trucks in range
			ArrayList<Truck> trucksInRange = new ArrayList<Truck>();

			// determine what trucks are in range
			for (int i = 0; i < truckCache.length; i++) {
				if (i != truckNumber - 1 && truckInitialized[i]) {
					trucksInRange.add(truckCache[i]);
				}
			}
			if (trucksInRange.size() > 0) {
				for (int i = 0; i < trucksInRange.size(); i++) {
					// roll the dice
					Truck targetTruck = trucksInRange.get(i);
					if (isMessageSuccessful(targetTruck)) {
						sendMessage(targetTruck, newMessage);
						messagesSent++;
						lastMessage = newMessage;
					}
				}
			}
		}
	}
}