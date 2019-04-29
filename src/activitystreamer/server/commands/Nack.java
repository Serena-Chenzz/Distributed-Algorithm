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

public class Nack {
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public Nack(String msg, Connection con) {

        Nack.conn = con;
        try{
            UniqueID proposalID;

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);

            int lamportTimeStamp = Integer.parseInt(message.get("lamportTimeStamp").toString());
            String serverID = message.get("serverID").toString();

            proposalID = new UniqueID(lamportTimeStamp, serverID);
            if (proposalID.equals(Control.getInstance().getProposalID())) {
                abort(proposalID);
            }
        }catch (ParseException e) {
            log.debug(e);
        }

    }


    public void abort(UniqueID proposalID) {
        String nackMsg = Command.createAbort(proposalID.getLamportTimeStamp(), proposalID.getServerID());
        Control.getInstance().clearAckNumber();
        Control.getInstance().clearPromiseSet();
        log.debug(nackMsg);
    }


    public boolean getCloseCon() {
        return closeConnection;
    }
}

