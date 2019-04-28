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

public class Prepare {

	private static Connection conn;
	private static final Logger log = LogManager.getLogger();
	private static boolean closeConnection=false;
	
	public Prepare(String msg, Connection con) {

		try{
			UniqueID proposalID, promisedID,acceptedID;
			String acceptedValue;

			JSONParser parser = new JSONParser();
			JSONObject message = (JSONObject) parser.parse(msg);

			int lamportTimeStamp = Integer.parseInt(message.get("lamportTimeStamp").toString());
			int serverID = Integer.parseInt(message.get("serverID").toString());

			proposalID = new UniqueID(lamportTimeStamp, serverID);
			promisedID = Control.getInstance().getPromisedID();
			acceptedID = Control.getInstance().getAccpetedID();
			acceptedValue = Control.getInstance().getAccpetedValue();

			if (promisedID != null && proposalID.equals(promisedID)) { // it is a duplicate message
				sendPromise(proposalID, acceptedID, acceptedValue);
			}
			else if (promisedID == null || proposalID.largerThan(promisedID)) { // it is greater than promisedID, then change the promisedID
				Control.getInstance().setPromisedID(proposalID);
				sendPromise(proposalID, acceptedID, acceptedValue);
			}
			else {
				sendNack(proposalID);
			}
		}
		catch (ParseException e) {
			log.debug(e);
		}
	}
	
	public void sendPromise(UniqueID proposalID, UniqueID acceptedID, String acceptedValue) {

		String promiseMsg = Command.createPromise(proposalID.getLamportTimeStamp(), proposalID.getServerID(),
				acceptedID.getLamportTimeStamp(), acceptedID.getServerID(),acceptedValue);
		conn.writeMsg(promiseMsg);
		log.debug(promiseMsg);
	}
	
	public void sendNack(UniqueID proposalID) {
		String nackMsg = Command.createNack(proposalID.getLamportTimeStamp(), proposalID.getServerID());
		conn.writeMsg(nackMsg);
		log.debug(nackMsg);
	}

	public boolean getCloseCon() {
		return closeConnection;
	}

}
