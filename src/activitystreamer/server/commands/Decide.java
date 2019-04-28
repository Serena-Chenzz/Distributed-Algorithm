/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package activitystreamer.server.commands;

import activitystreamer.models.Command;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.Load;
import static activitystreamer.server.commands.ServerAnnounce.doActivity;
import activitystreamer.util.Settings;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author wknje
 */
public class Decide extends Thread{
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private String msg;
    
    public Decide(String decideMsg) {
        msg = decideMsg;
        start();
    }

    @Override
    public void run() { 
        Control.getInstance().broadcast(msg);	
    }
}
