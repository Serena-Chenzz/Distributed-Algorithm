package activitystreamer.server.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ConcurrentModificationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.Message;
import activitystreamer.util.Settings;
import activitystreamer.models.*;
import activitystreamer.server.Load;

public class MsgBufferActivatorThread extends Thread{
    
    private static boolean closeConnection=false;
    private final static Logger log = LogManager.getLogger();
    
    public MsgBufferActivatorThread() {
        //start();
    }
    
    
    @Override
    public void run() {
        log.info("MsgBufferActivatorThread is running");
        
        while(!Control.getInstance().getTerm()){
            //Fetch the latest serverMsgActivaorBufferQueue
            try{
                HashMap<Connection, String> serverActivatorMonitor = Control.getActivatorMonitor();
                for (Connection con: serverActivatorMonitor.keySet()){
                    long lastActTime = Long.parseLong(serverActivatorMonitor.get(con).split(" ")[1]);
                    boolean act = Boolean.parseBoolean(serverActivatorMonitor.get(con).split(" ")[0]);
                    if((!act)&&(System.currentTimeMillis()-lastActTime>500)){
                        Control.getInstance().activateMessageQueue(con.getRemoteId());
                    }
                }
                Thread.sleep(500); 
            }catch(ConcurrentModificationException e){
                log.info("Block iterating arrays when modifying it");
            }catch (InterruptedException e){
                log.info("This thread is interrupted forcefully.");
            }
        }
        
        log.info("closing Activity Server Broadcast thread....");
        closeConnection = true;
      
    }
    
    public static boolean getResponse() {
        return closeConnection;
    }
}

