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

// A PROMISE message from an acceptor means it will not accept any other proposals with
// a smaller proposal ID than what is attached to this message.
// This message contains a proposal ID, and the last proposal ID (as accepted ID) that it has made a promise to,
// and the value proposed by the latter proposer (as accepted value).
// When receiving this message, a proposer should:
// Check if the message is for itself by compare its own proposal ID with that proposal ID in the message,
// if so, add this <accepted ID, accepted value> pair to its local promiseSet().
// If its own promiseSet.size() == [(number of all other servers) / 2 + 1],
// it will broadcast an ACCEPT message to all other servers, the actually accepted value sent is:
// if no accepter has promised to any value, the accepted value attached to the ACCEPT message is its own proposal value,
// else, the accepted value is the accepted value with the largest accepted ID in its promiseSet().
public class Promise {
    //private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public Promise(String msg) {

        //Promise.conn = con;
        try{

            UniqueID proposalID, promisedID;

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);

            int proposalLamportTimeStamp = Integer.parseInt(message.get("proposalLamportTimeStamp").toString());
            String proposalServerID = message.get("proposalServerID").toString();
            int acceptedLamportTimeStamp = Integer.parseInt(message.get("acceptedLamportTimeStamp").toString());
            String acceptedServerID = message.get("acceptedServerID").toString();
            String acceptedValue = null;
            if (message.get("acceptedValue") != null)
                acceptedValue = message.get("acceptedValue").toString();

            proposalID = new UniqueID(proposalLamportTimeStamp, proposalServerID);
            promisedID = new UniqueID(acceptedLamportTimeStamp,acceptedServerID);

            if (proposalID.equals(Control.getInstance().getProposalID())) {
                Control.getInstance().addToPromiseSet(promisedID,acceptedValue);
                if (Control.getInstance().getPromiseSet().size() == (Control.getInstance().getNeighbors().size() / 2 + 1))
                {
                    String largestAcceptedValue = Control.getInstance().getAcceptedValueWithLargestProposalID(promisedID);
                    if (largestAcceptedValue == null)
                        Control.getInstance().setAcceptedValue(Control.getInstance().getUniqueId());
                    else
                        Control.getInstance().setAcceptedValue(largestAcceptedValue);
                    sendAccept(proposalID,Control.getInstance().getacceptedValue());
                    System.out.println("Proposer Accepted Value " + Control.getInstance().getacceptedValue());
                }
            }
        }catch (ParseException e) {
            log.debug(e);
        }

    }

    public void sendAccept(UniqueID proposalID, String accepteValue) {

        String acceptMsg = Command.createAccept(proposalID.getLamportTimeStamp(), proposalID.getServerID(), accepteValue);
        for(Connection connection:Control.getInstance().getNeighbors()) {
            connection.writeMsg(acceptMsg);
            log.debug(acceptMsg);
            log.info("Sending Accept to " + connection.getRemoteId());
        }
    }
    public boolean getCloseCon() {
        return closeConnection;
    }
}

