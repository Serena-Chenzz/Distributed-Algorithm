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
    //private static Connection conn;
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
                    sendDecide(Control.getInstance().getAccpetedValue());
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

