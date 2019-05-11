package activitystreamer.server.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



import activitystreamer.models.*;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;

// Called when an ACCEPT message is received from a Proposer
// An ACCEPT message should contain:
// the Proposal ID of this proposer and the proposed value
public class Accept {

	private static Connection conn;
	private static final Logger log = LogManager.getLogger();
	private static boolean closeConnection=false;

	
	public Accept(String msg, Connection con) {

		Accept.conn = con;
        try{
			UniqueID proposalID, promisedID;

			JSONParser parser = new JSONParser();
			JSONObject message = (JSONObject) parser.parse(msg);

			// These two parts form the Proposal ID.
			int lamportTimeStamp = Integer.parseInt(message.get("lamportTimeStamp").toString());
			String serverID = message.get("serverID").toString();

			// The proposed value
			String value = message.get("value").toString();

			proposalID = new UniqueID(lamportTimeStamp, serverID);
			promisedID = Control.getInstance().getPromisedID();

			// The algorithm works here
			// If the local promised ID is greater, send NACK back
			// else change local promised ID to this proposal ID, send ACCEPTED back
			if (promisedID == null || proposalID.largerThan(promisedID)
					|| proposalID.equals(promisedID)) {

				Control.getInstance().setPromisedID(proposalID);
				Control.getInstance().setAcceptedID(proposalID);
				Control.getInstance().setAcceptedValue(value);
				sendAccepted(proposalID);
			}
			else {
				sendNack(proposalID);
			}
		}catch (ParseException e) {
			log.debug(e);
		}

	}
	
	public void sendAccepted(UniqueID acceptedID) {
		// Create ACCEPTED and send back
		String acceptedMsg = Command.createAccepted(acceptedID.getLamportTimeStamp(), acceptedID.getServerID());
		conn.writeMsg(acceptedMsg);
		log.debug(acceptedMsg);
		log.info("Sending ACCEPTED to " + conn.getRemoteId());
	}

	public void sendNack(UniqueID proposalID) {
		// Create NACK and send back
		String nackMsg = Command.createNack(proposalID.getLamportTimeStamp(), proposalID.getServerID());
		conn.writeMsg(nackMsg);
		log.debug(nackMsg);
		log.info("Sending NACK to " + conn.getRemoteId());

	}

	public boolean getCloseCon() {
		return closeConnection;
	}
}
