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

// If the server receives this PREPARE message,
// this means some other server(s) are making a selection.
// Note that during one round of selection (Basic-Paxos),
// each server has three roles: proposer, acceptor and learner
// a proposer is a server that calls the sendSelection() method
// but this server can also be an acceptor and a learner
// This PREPARE message is handled by the acceptor part of a server.
public class Prepare {

	private static Connection conn;
	private static final Logger log = LogManager.getLogger();
	private static boolean closeConnection=false;
	
	public Prepare(String msg, Connection con) {
		Prepare.conn = con;
		try{
			UniqueID proposalID, promisedID;
			String acceptedValue;

			JSONParser parser = new JSONParser();
			JSONObject message = (JSONObject) parser.parse(msg);

			int lamportTimeStamp = Integer.parseInt(message.get("lamportTimeStamp").toString());
			String serverID = message.get("serverID").toString();

			proposalID = new UniqueID(lamportTimeStamp, serverID);
			promisedID = Control.getInstance().getPromisedID();
			acceptedValue = Control.getInstance().getacceptedValue();


			if (promisedID == null || proposalID.largerThan(promisedID)) {
				// it is greater than promisedID, then change the promisedID record to this proposal ID
				log.info("Prepare Phase Larger Than");
				Control.getInstance().setPromisedID(proposalID);
				promisedID = proposalID;
				sendPromise(proposalID, promisedID, acceptedValue);
			}
			else
				// This proposal ID is not large enough.
				sendNack(proposalID);
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
		log.info("Sending PROMISE to " + conn.getRemoteId());
	}
	
	public void sendNack(UniqueID proposalID) {
		String nackMsg = Command.createNack(proposalID.getLamportTimeStamp(), proposalID.getServerID());
		conn.writeMsg(nackMsg);
		log.debug(nackMsg);
		log.info("Sending NACK to " + conn.getRemoteId());
	}

	public boolean getCloseCon() {
		return closeConnection;
	}

}
