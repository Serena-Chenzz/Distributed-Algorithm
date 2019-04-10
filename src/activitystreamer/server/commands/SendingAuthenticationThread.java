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

public class SendingAuthenticationThread extends Thread{
    private static boolean closeConnection=false;
    private final static Logger log = LogManager.getLogger();
    
    public SendingAuthenticationThread() {
    }
    
    public void run() {
        //Monitor the authenticationAckQueue every 5 seconds
        while(!Control.getInstance().getTerm()){
            try{
                HashMap<Connection, Long> authenticationAckQueue = Control.getAuthenticationAckQueue();
                if (!authenticationAckQueue.isEmpty()){
                    for (Connection con: authenticationAckQueue.keySet()){
                        long currentTime = System.currentTimeMillis();
                        long sendingTime = authenticationAckQueue.get(con);
                        if ((sendingTime != -1)&&(currentTime - sendingTime >= 2000)){
                            //if 2s passed but the server hasn't received the msg, it will send the authentication again
                            JSONObject authenticate = Command.createAuthenticate(Settings.getSecret(), Control.getRemoteId());
                            con.writeMsg(authenticate.toJSONString());
                        }
                    }
                }
                Thread.sleep(Settings.getActivityInterval());
            }catch(ConcurrentModificationException e){
                log.info("Block iterating arrays when modifying it");
            }catch (InterruptedException e){
                log.info("This thread is interrupted forcefully.");
            }
        }
        log.info("closing sending Authentication thread....");
        closeConnection=true;
    }
    public static boolean getResponse() {
        return closeConnection;
    }

}
