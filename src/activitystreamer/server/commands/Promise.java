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
                        Control.getInstance().setAccpetedValue(Control.getInstance().getUniqueId());
                    else
                        Control.getInstance().setAccpetedValue(largestAcceptedValue);
                    sendAccept(proposalID,Control.getInstance().getAccpetedValue());
                    System.out.println("Proposer Accepted Value " + Control.getInstance().getAccpetedValue());
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

