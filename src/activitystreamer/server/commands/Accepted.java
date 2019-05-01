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

public class Accepted {
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public Accepted(String msg, Connection con) {

        Accepted.conn = con;
        try{
            UniqueID proposalID;

            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);

            int lamportTimeStamp = Integer.parseInt(message.get("lamportTimeStamp").toString());
            String serverID = message.get("serverID").toString();
            String value = message.get("value").toString();
            proposalID = new UniqueID(lamportTimeStamp, serverID);

            if (proposalID.equals(Control.getInstance().getProposalID())) {
                Control.getInstance().addAckNumber();
                if (Control.getInstance().getAckNumber() == (Control.getInstance().getNeighbors().size() / 2 + 1))
                    sendDecide(Control.getInstance().getAccpetedValue());
            }

        }catch (ParseException e) {
            log.debug(e);
        }

    }

    public void sendDecide(String acceptedValue) {
        String decideMsg = Command.createDecide(acceptedValue);
        conn.writeMsg(decideMsg);
        log.debug(decideMsg);
    }

    public boolean getCloseCon() {
        return closeConnection;
    }
}

