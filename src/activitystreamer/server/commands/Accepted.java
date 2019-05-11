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

// Called when an ACCEPTED message is received from an acceptor
// An ACCEPTED message should contain:
// the Proposal ID of this round's largest proposer that the acceptor knows
// (only in those proposal ID seen by this acceptor, so maybe not the globally largest)
// Each time the proposer with the same proposal ID receives one ACCEPTED,
// it add one to its own ackNumber.
// When its ackNumber record is equal to [(number of all other servers) / 2 + 1],
// this proposer knows it has gathered enough ACKs,
// so the global consensus can be reached, the value is what it has sent to other servers before.
// And this proposer will broadcast a DECIDE message to all learners.
public class Accepted {
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public Accepted(String msg) {

        try{
            UniqueID proposalID;

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);

            int lamportTimeStamp = Integer.parseInt(message.get("lamportTimeStamp").toString());
            String serverID = message.get("serverID").toString();
            proposalID = new UniqueID(lamportTimeStamp, serverID);

            if (proposalID.equals(Control.getInstance().getProposalID())) {
                Control.getInstance().addAckNumber();
                if (Control.getInstance().getAckNumber() == (Control.getInstance().getNeighbors().size() / 2 + 1)) {
                    sendDecide(Control.getInstance().getacceptedValue());
                    Control.getInstance().setAcceptedID(proposalID);

                }
            }

        }catch (ParseException e) {
            log.debug(e);
        }

    }

    public void sendDecide(String acceptedValue) {
        String decideMsg = Command.createDecide(acceptedValue);
        log.info("Now there are " + Integer.toString(Control.getInstance().getNeighbors().size()) + " neighbors");
        Control.getInstance().setAcceptedValue(acceptedValue);
        Control.getInstance().clearAckNumber();
        Control.getInstance().clearPromiseSet();
        for (Connection connection:Control.getInstance().getNeighbors())
        {
            connection.writeMsg(decideMsg);
            log.debug("Sending Decide to " + connection.getRemoteId() + " " + decideMsg);
        }
        Control.getInstance().clearAcceptor();
        Control.setLeaderHasBeenDecided(true);
        Control.setLeaderConnection(null);
    }

    public boolean getCloseCon() {
        return closeConnection;
    }
}

