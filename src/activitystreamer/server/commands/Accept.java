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

//Called when an Accept message is received from a Proposer
public class Accept {

	private static Connection conn;
	private static final Logger log = LogManager.getLogger();
	private static boolean closeConnection=false;

	
	public Accept(String msg, Connection con) {

		Accept.conn = con;
        try{
			UniqueID proposalID, promisedID,acceptedID;
			String acceptedValue;

			JSONParser parser = new JSONParser();
			JSONObject message = (JSONObject) parser.parse(msg);

			int lamportTimeStamp = Integer.parseInt(message.get("LamportTimeStamp").toString());
			int serverID = Integer.parseInt(message.get("ServerID").toString());
			String value = message.get("Value").toString();

			proposalID = new UniqueID(lamportTimeStamp, serverID);
			promisedID = Control.getInstance().getPromisedID();
			acceptedID = Control.getInstance().getAccpetedID();
			acceptedValue = Control.getInstance().getAccpetedValue();

			if (promisedID == null || proposalID.largerThan(promisedID)
					|| proposalID.equals(promisedID)) {
				Control.getInstance().setPromisedID(proposalID);
				Control.getInstance().setAccpetedID(proposalID);
				Control.getInstance().setAccpetedValue(value);

				sendAccepted(proposalID, acceptedID, acceptedValue);
			}
			else {
				sendNack(proposalID);
			}
		}catch (ParseException e) {
			log.debug(e);
		}

	}
	
	public void sendAccepted(UniqueID proposalID, UniqueID acceptedID, String acceptedValue) {
		// Create the command and send back
		proposer.acceptedID = proposalID;
	}
	
	public void sendNack(UniqueID proposalID) {
		// Create the command and send back
		proposer.proposalID = proposalID;

	}

	public boolean getCloseCon() {
		return closeConnection;
	}
}
