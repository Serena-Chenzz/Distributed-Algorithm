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


public class MultiAccept {
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public MultiAccept(String msg, Connection con){
        conn = con;

        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            long leaderFirstUnchosenIndexLong = (long)message.get("firstUnchosenIndex");
            int leaderFirstUnchosenIndex = (int)leaderFirstUnchosenIndexLong;
            String value = message.get("value").toString();
            long indexLong = (long)message.get("index");
            int index = (int) indexLong;

            //First, check if its firstUnchosenIndex < leader's firstUnchosenInde, if so, write the missing logs into the db
            int myFirstUnchosenIndex = Control.getFirstUnchosenIndex();
            if(myFirstUnchosenIndex < leaderFirstUnchosenIndex){
                //Run the getMissingLogThread
                Thread getMissingLog = new GetMissingLogThread(myFirstUnchosenIndex, leaderFirstUnchosenIndex-1);
                getMissingLog.start();
                getMissingLog.join();
            }

            //Append the msg to the UnChosenLogList
            Control.appendUnChosenLogs(value,con);

            //Return accepted msg
            conn.writeMsg(Command.createMultiAccepted(index));
            closeConnection = false;

        }catch (ParseException e) {
            log.debug(e);
        }catch (InterruptedException e){
            log.debug(e);
        }
    }

    public boolean getCloseCon() {
        return closeConnection;
    }

}
