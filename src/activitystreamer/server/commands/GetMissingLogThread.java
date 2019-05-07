package activitystreamer.server.commands;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;

import activitystreamer.models.*;
import activitystreamer.server.Load;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.sql.*;

public class GetMissingLogThread extends Thread{
    private static boolean closeConnection=false;
    private final static Logger log = LogManager.getLogger();
    private static int startIndex;
    private static int endIndex;
    private static int N;
    private static boolean terminate = false;

    public GetMissingLogThread(int startIndex, int endIndex){
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.N = Control.getInstance().getNeighbors().size()+1;
    }

    @Override
    public void run(){
        String getMissingLog = Command.createGetMissingLog(startIndex, endIndex);
        Control.getInstance().broadcast(getMissingLog);
        try {
        while(!terminate){
            Thread.sleep(500);
        }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            //Clean up all the unused memory space
            Control.cleanUpMissingLog();
            log.info("Clean Up All the Unused Memory Space...");
        }
    }

    public static void writeIntoDB(){
        HashMap<Integer, HashMap<String, Integer>> findMissingLog = Control.getFindMissingLog();

        for(int index = startIndex; index<=endIndex; index++){
            HashMap<String, Integer> sCounter = findMissingLog.get(index);
            String majorValue = "";
            for(Object string: sCounter.keySet()){
                if(sCounter.get(string) > N/2){
                    majorValue = string.toString();
                    break;
                }
            }

            //Write into Log DB
            Control.writeIntoLogDB(index, majorValue);

            //Also, perform the correct action to the ticket selling db
            Control.slavePerformAction(majorValue, index);
        }

        terminate = true;
    }


}
