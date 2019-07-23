package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;

public class MultiAccepted {
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    // This class is used when the leader is receiving Accepted info from other servers. When the leader receives
    // majority of accepts from other servers, it will send the Decide info to notify all other servers. It will
    // write the message into its own Log DB and perform actions against its local ticket selling DB as well.

    public MultiAccepted(String msg, Connection con){
        conn = con;
        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            long indexLong = (long)message.get("index");
            int index = (int)indexLong;
            // Add the index into the counter
            Control.addIntoAcceptedCounter(index);

            int firstUnchosenLogIndex = Control.getFirstUnchosenIndex();
            if(index == firstUnchosenLogIndex){
                while(Control.checkIfMeetMajority(index)){
                    String firstMsg = Control.getLogFromUnchosenLogs();
                    //Send MultiDecide(index) to all the learners
                    String decideMsg = Command.createMultiDecide(index, firstMsg);
                    Control.getInstance().broadcast(decideMsg);
                    //Get the reply connection
                    Connection replyConn = Control.getConFromUnchosenLogs();
                    //Remove the index from the unchosen log list
                    Control.removeFromUnchosenLogs();
                    //Add the message into the log DB
                    Control.writeIntoLogDB(index,firstMsg);
                    //Operate this message and do operations to its local ticket selling DB.
                    Control.leaderPerformAction(firstMsg,replyConn,index);
                    index++;
                }
            }
        }catch (ParseException e) {
            log.debug(e);
        }
    }

    public boolean getCloseCon() {
        return closeConnection;
    }
}
