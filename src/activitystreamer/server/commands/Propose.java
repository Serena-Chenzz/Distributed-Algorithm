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

// A proposer needs to call this class when it tries to send a proposal.
// The message sent out is called PREPARE.
// In this PREPARE message there is only one Proposal ID, (which has two parts)
// a lamportTimeClock integer and an IP Address + Port string.
public class Propose {
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public Propose(String msg, Connection con) {

        Propose.conn = con;
        try{
            UniqueID proposalID;
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            int lamportTimeStamp = Integer.parseInt(message.get("lamportTimeStamp").toString());
            String serverID = message.get("serverID").toString();
            proposalID = new UniqueID(lamportTimeStamp, serverID);
            Control.getInstance().clearPromiseSet();
            Control.getInstance().clearAckNumber();
            sendPrepare(proposalID);
        }catch (ParseException e) {
            log.debug(e);
        }

    }

    public void sendPrepare(UniqueID prepareID) {
        // Create the command and send back
        String prepareMsg = Command.createPrepare(prepareID.getLamportTimeStamp(), prepareID.getServerID());
        conn.writeMsg(prepareMsg);
        log.debug(prepareMsg);
    }

    public boolean getCloseCon() {
        return closeConnection;
    }
}

