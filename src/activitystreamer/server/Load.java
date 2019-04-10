/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package activitystreamer.server;

import activitystreamer.models.Command;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author wknje
 */
public class Load {

    class ServerInfo {

        private String hostname;
        private String portStr;

        public ServerInfo(String hostname, String portStr) {
            this.hostname = hostname;
            this.portStr = portStr;
        }

    }

    // If the server has more than limited clients, it should consider 
//    private static final int upperLimit = 5;
    private static final Logger log = LogManager.getLogger();
    private Map<String, Integer> loadMap;
    private Map<String, ServerInfo> serverList;
    private Map<String, Long> serverTime;

    public Load() {
        this.loadMap = new HashMap();
        this.serverList = new HashMap();
        this.serverTime = new HashMap();
    }

    public synchronized void updateLoad(JSONObject userInput) {
        String hostname = userInput.get("hostname").toString();
        String portStr = userInput.get("port").toString();
        String id = userInput.get("id").toString();
        // If this is a new server
        if (!serverList.containsKey(id)) {
            ServerInfo si = new ServerInfo(hostname, portStr);
            serverList.put(id, si);
        }
        // Update loadmap
        loadMap.put(id, Integer.parseInt(userInput.get("load").toString()));
        serverTime.put(id, System.nanoTime());
        String output = "";
        for (Map.Entry<String, Long> entry : serverTime.entrySet()) {
            output = output + entry.getKey() + ':' + entry.getValue() + '\n';
        }
        //log.debug("Current Server Time Infomation: " + output);
    }

    // Check if 
    public synchronized boolean checkRedirect(Connection clientCon) {
        int ownLoad = getOwnLoad();
        Long currentTime = System.nanoTime();
        // search for the server that is at least 1 clients less than its own
        for (Map.Entry<String, Integer> entry : loadMap.entrySet()) {
            // check the expiration of servers
            String id = entry.getKey();
            if (!checkServerExpired(currentTime, id)) {
                Integer load = entry.getValue();
                if (load < ownLoad - 1) {
                    String hostname = serverList.get(id).hostname;
                    String portStr = serverList.get(id).portStr;
                    // send redirect infomation to client
                    log.debug("Redirect to " + 
                        Connection.generateRemoteId(hostname, portStr));
                    clientCon.writeMsg(Command.createRedirect(hostname, portStr));
                    // Then the connection needs to be terminated
                    return true;
                }
            }else{
                log.debug("Server " + id + " expired.");
            }
        }
        // the connection needn't to be terminated
        return false;
    }

    public synchronized boolean checkServerExpired(Long currentTime, String serverId) {
        long duration = (currentTime - serverTime.get(serverId))/1000000000;
        log.debug("Server " + serverId + "'s duration is: " + duration);
        // if the server has no update within 6 seconds, return server expired;
        if (duration > 20) {
            return true;
        }
        // if the server has no update within 60 seconds, remove the server and return server expired;
        if (duration > 60) {
            loadMap.remove(serverId);
            serverList.remove(serverId);
            serverTime.remove(serverId);
            return true;
        }
        return false;
    }

    public synchronized static int getOwnLoad() {
        return Control.getUserConnections().size();

    }

}
