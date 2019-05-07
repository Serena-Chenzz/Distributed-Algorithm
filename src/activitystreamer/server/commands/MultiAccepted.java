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


public class MultiAccepted {
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

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
                    //Send MultiDecide(index)
                    String decideMsg = Command.createMultiDecide(index, firstMsg);
                    Control.getInstance().broadcast(decideMsg);
                    //Get the replyConn
                    Connection replyConn = Control.getConFromUnchosenLogs();
                    //Remove the index from the list
                    Control.removeFromUnchosenLogs();
                    //Add the message into the log DB
                    Control.writeIntoLogDB(index,firstMsg);
                    //Operate this message
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
