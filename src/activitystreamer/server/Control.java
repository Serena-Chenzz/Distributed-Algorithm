package activitystreamer.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.server.commands.*;
import activitystreamer.util.Settings;
import activitystreamer.models.*;

public class Control extends Thread {

    protected static final Logger log = LogManager.getLogger();
    //The connections list will record all connections to this server
    protected static HashMap<Connection, Boolean> connections;
    private static ArrayList<Connection> connectionClients;
    //This hashmap is to record status of each connection
    protected static HashMap<String, ArrayList<String>> connectionServers;
    private static ArrayList<Connection> neighbors;
    private static ArrayList<String> pendingNeighbors;
    //Save the list of connected users plus timestamp and its connections
    private static HashMap<Connection, String> userConnections = new HashMap<Connection, String>();
    private static String uniqueId; // unique id for a server
    protected static boolean term = false;
    private static Listener listener;
    private InetAddress ip;
    private static HashMap<Connection, String> activatorMonitor = new HashMap<Connection, String>();
    private static UniqueID accpetedID;
    private static UniqueID promisedID;
    private static String accpetedValue;

    protected static Control control = null;
    protected static Load serverLoad;

    public synchronized UniqueID getAccpetedID() {return accpetedID;}

    public synchronized UniqueID getPromisedID() {return promisedID;}

    public synchronized String getAccpetedValue() {return accpetedValue;}
    
    public synchronized static ArrayList<Connection> getNeighbors(){
        return neighbors;
    }
    
    public synchronized static String getRemoteId(){
        return uniqueId;
    }

    public synchronized static Load getServerLoad() {
        return serverLoad;
    }

    public synchronized static Control getInstance() {
        if (control == null) {
            control = new Control();
        }
        return control;
    }

    public Control() {
    	// initialize ip 
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.error(e);
        }
    	
        // initialize the connections array
        connections = new HashMap<Connection,Boolean>();
        connectionClients = new ArrayList<Connection>();
        //connectionServers is to record all replies from its neighbors and record all connection status.
        connectionServers = new HashMap<String, ArrayList<String>>();
        neighbors = new ArrayList<Connection>();
        pendingNeighbors = new ArrayList<String>();

        serverLoad = new Load();
        if(Settings.getLocalHostname().equals("localhost")) {
        	uniqueId = ip.getHostAddress() + " " + Settings.getLocalPort();
        }else {
        	uniqueId = Settings.getLocalHostname() + " " + Settings.getLocalPort();
        }

        // start a listener
        try {
            listener = new Listener();
            initiateConnection();
        } catch (IOException e1) {
            log.fatal("failed to startup a listening thread: " + e1);
            System.exit(-1);
        }
        start();
    }

    public void initiateConnection() {
        // make a connection to another server if remote hostname is supplied
        if (Settings.getRemoteHostname() != null) {
            createServerConnection(Settings.getRemoteHostname(), Settings.getRemotePort());
        }
    }
    
    public void run() {
        // start server announce
        Thread serverAnnouce = new ServerAnnounce();
        serverAnnouce.start();
    }

    
    public synchronized static HashMap<Connection, String> getUserConnections(){
        return userConnections;
    }


  
    
    public synchronized void createServerConnection(String hostname, int port) {
        try {
                Connection con = outgoingConnection(new Socket(hostname, port));
                JSONObject authenticate = Command.createAuthenticate(Settings.getSecret(), uniqueId);
                String remoteId;
                if(hostname == "localhost"){
                    remoteId = ip.getHostAddress() + " " + port;
                }else{
                    remoteId = hostname + " " + port;
                }
                pendingNeighbors.add(remoteId);
                con.writeMsg(authenticate.toJSONString());
                
            } catch (IOException e) {
                log.error("failed to make connection to " + Settings.getRemoteHostname() + ":" + Settings.getRemotePort());
            }
    }

    /*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
     */
    public synchronized boolean process(Connection con, String msg) {
        System.out.println("\n**Receiving: " + msg);
        try {
            JSONParser parser = new JSONParser();
            JSONObject userInput = (JSONObject) parser.parse(msg);
            
            //If the userInput does not have a field for command, it will invoke an invalid message:
            if(!userInput.containsKey("command")){
                String invalidFieldMsg = Command.createInvalidMessage("the received message did not contain a command");
                con.writeMsg(invalidFieldMsg);
                return true;
            }
            else{
                String targetCommand = userInput.get("command").toString();
                if(!Command.contains(targetCommand)){
                    String invalidCommandMsg = Command.createInvalidMessage("the received message did not contain a valid command");
                    con.writeMsg(invalidCommandMsg);
                    return true;
                }
                else{
                    Command userCommand = Command.valueOf(targetCommand);
                    switch (userCommand) {
                        //In any case, if it returns true, it closes the connection.
                        //In any case, we should first check whether it is a valid message format
                        case AUTHENTICATE:
                            if (!Command.checkValidAuthenticate(userInput)){
                                String invalidAuth = Command.createInvalidMessage("Invalid Authenticate Message Format");
                                con.writeMsg(invalidAuth);
                                return true;
                            }
                            //If an old connection sends an authentication message, it means a crashed server recovers within 60s. We need 
                            //to remove the old connections.
                            boolean serverWithOldIpPort = false;
                            Connection closedNei = null;
                            for(Connection neighbor: neighbors){
                                System.out.println(neighbor.getRemoteId());
                                String targetId = (String)userInput.get("remoteId");
                                if (neighbor.getRemoteId().equals(targetId)){
                                    serverWithOldIpPort = true;
                                    closedNei = neighbor;
                                }
                            }
                            if (serverWithOldIpPort&& (closedNei!=null)){
                                connectionClosed(closedNei);
                            }
                            log.debug(connectionServers.toString());
                            Authenticate auth = new Authenticate(msg, con);
                            
                            if (!auth.getResponse()) {
                                // Set remoteId for this connection
                                String remoteId = (String)userInput.get("remoteId");
                                con.setRemoteId(remoteId);
                                // Send all neighbors information to the new server
                                ArrayList<String> neighborInfo = new ArrayList<String>();
                                
                                for(Connection nei : neighbors){
                                    neighborInfo.add( nei.getRemoteId());                                            
                                }
                                String respondMsg = Command.createAuthenticateSuccess(neighborInfo, uniqueId);
                                log.debug("Respond to authentication with message " + respondMsg);
                                con.writeMsg(respondMsg);
                                
                                connectionServers.put(remoteId, new ArrayList<String>());
                                neighbors.add(con);
                                
                                log.debug("Add neighbor: " + con.getRemoteId());
                                
                            }
//                            if (con.getRemoteId().equals("10.0.0.42 3000")){
//                                System.out.println("Here");
//                                return true;
//                            }
                            return auth.getResponse();
                            
                        case AUTHENTICATION_SUCCESS:
                            log.debug("Receive authentication sucess");
                            if (!Command.checkValidCommandFormat2(userInput)){
                                String invalidAuth = Command.createInvalidMessage("Invalid Authenticate Success Message Format");
                                con.writeMsg(invalidAuth);                                
                            }                                        
                            else{
                                // Set remoteId for this connection
                                String remoteId = (String)userInput.get("remoteId");
                                con.setRemoteId(remoteId);
                                // Add connection into neighbor list
                                connectionServers.put(remoteId, new ArrayList<String>());
                                neighbors.add(con);
                                
                                pendingNeighbors.remove(remoteId);
                                log.debug("Add neighbor: " + con.getRemoteId());
                                
                                // create full connection
                                ArrayList<String> neighborInfo = (ArrayList<String>)userInput.get("info");
                                for(String neiId : neighborInfo){
                                    String[] neiDetail = neiId.split(" ");
                                    if (!(this.containsServer(neiId) || pendingNeighbors.contains(neiId))){
                                        String hostname = neiDetail[0];
                                        Integer port = Integer.parseInt(neiDetail[1]);
                                        log.debug("Send connection request to " + hostname + ", port " + port); 
                                        createServerConnection(hostname, port);
                                    }
                                }
//                                if (con.getRemoteId().equals("10.0.0.42 5000")){
//                                    return true;
//                                }
                                return false;
                            }
                            return true;
                        case AUTHENTICATION_FAIL:

                            String remoteId = (String)userInput.get("remoteId");
                            if (!Command.checkValidCommandFormat2(userInput)){
                                String invalidAuth = Command.createInvalidMessage("Invalid Authenticate Fail Message Format");
                                con.writeMsg(invalidAuth);
                            }
                            if (!remoteId.isEmpty()){
                                if (!pendingNeighbors.contains(remoteId)) {
                                    String invalidServer = Command.createInvalidMessage("Authentication Fail Message "
                                            + "From Invalid Server");
                                    con.writeMsg(invalidServer);                                
                                }
                                else{
                                    pendingNeighbors.remove(remoteId);
                                    log.debug("Authentication failed and server is removed.");
                                }
                            }                            
                            return true;
        
                        case SERVER_ANNOUNCE:
                            if (!Command.checkValidServerAnnounce(userInput)){
                                String invalidServer = Command.createInvalidMessage("Invalid ServerAnnounce Message Format");
                                con.writeMsg(invalidServer);
                                return true;
                            }
                            else if (!this.containsServer(con.getRemoteId())) {
                                String invalidServer = Command.createInvalidMessage("The server has not"
                                        + "been authenticated.");
                                con.writeMsg(invalidServer);
                                return true;
                            }
                            // Record the load infomation
                            serverLoad.updateLoad(userInput);
                            return false;
        
                        case REGISTER:
                            if (!Command.checkValidCommandFormat1(userInput)){
                                String invalidReg = Command.createInvalidMessage("Invalid Register Message Format");
                                con.writeMsg(invalidReg);
                                return true;
                            }
                            else{
                                Register reg = new Register(msg, con);
                                return reg.getCloseCon();
                            }
        
                        case LOGIN:
                            if (!Command.checkValidCommandFormat1(userInput)){
                                String invalidLogin = Command.createInvalidMessage("Invalid Login Message Format");
                                con.writeMsg(invalidLogin);
                                return true;
                            }
                            else{
                                Login login = new Login(con, msg);
                                // If login success, check if client need redirecting
                                if(!login.getResponse()){
                                    return serverLoad.checkRedirect(con);                                   
                                }
                                return true;
                            }
                        case LOGOUT:
                            if (!Command.checkValidCommandFormat1(userInput)){
                                String invalidLogout = Command.createInvalidMessage("Invalid Login Message Format");
                                con.writeMsg(invalidLogout);
                                return true;
                            }
                            else{
                                Logout logout = new Logout(con, msg);
                                return logout.getResponse();
                            }
                            
//                        case ACTIVITY_MESSAGE:
//                            if (!Command.checkValidAcitivityMessage(userInput)){
//                                String invalidAc = Command.createInvalidMessage("Invalid PurchasingMessage Message Format");
//                                con.writeMsg(invalidAc);
//                                return true;
//                            }
//                            else{
//                                PurchasingMessage actMess = new PurchasingMessage(con, msg);
//                                return actMess.getResponse();
//                            }
                            
                        case INVALID_MESSAGE:
                            //First, check its informarion format
                            if (!Command.checkValidCommandFormat2(userInput)){
                                String invalidMsg = Command.createInvalidMessage("Invalid InvalidMessage Message Format");
                                con.writeMsg(invalidMsg);
                                return true;
                            }
                            log.info("Receive response: " + Command.getInvalidMessage(userInput));
                            return true;
                            
                        //If it receives the message other than the command above, it will return an invalid message and close the connection
                        default:
                            String invalidMsg = Command.createInvalidMessage("the received message did not contain a correct command");
                            con.writeMsg(invalidMsg);
                            return true;
                        }
                    }
                }
        }  
        catch (ParseException e) {
            //If parseError occurs, the server should return an invalid message and close the connection
            String invalidParseMsg = Command.createInvalidMessage("JSON parse error while parsing message");
            con.writeMsg(invalidParseMsg);
            log.error("msg: " + msg + " has error: " + e);
        }
        return true;
    }
    

    public synchronized boolean containsServer(String remoteId) {
        return connectionServers.containsKey(remoteId);
    }



    public synchronized boolean broadcast(String msg) {     
        for (Connection nei : neighbors) {
            nei.writeMsg(msg);               
        }    
        // need failure model
        return true;
    }
    

    /*
	 * The connection has been closed by the other party.
     */
    public synchronized void connectionClosed(Connection con) {
        if (!term) {
            connections.remove(con);
            connectionClients.remove(con);
            userConnections.remove(con);
            neighbors.remove(con);
            connectionServers.remove(con.getRemoteId());
            activatorMonitor.remove(con);
        }
    }

    /*
	 * A new incoming connection has been established, and a reference is returned to it
     */
    public synchronized Connection incomingConnection(Socket s) throws IOException {
        //log.debug("incomming connection: " + Settings.socketAddress(s));
        Connection c = new Connection(s);
        connections.put(c, true);
        return c;
    }

    /*
	 * A new outgoing connection has been established, and a reference is returned to it
     */
    public synchronized Connection outgoingConnection(Socket s) throws IOException {
        //log.debug("outgoing connection: " + s.toString());
        Connection c = new Connection(s);
        connections.put(c,true);
        return c;

    }

    public synchronized void setAccpetedID(UniqueID ID) {accpetedID = ID;}

    public synchronized void setPromisedID(UniqueID ID) {promisedID = ID;}

    public synchronized void setAccpetedValue(String value) {accpetedValue = value;}

    public final void setTerm(boolean t) {
        term = t;
    }
    public final boolean getTerm() {
    	return term;
    }

    public synchronized final HashMap<Connection, Boolean> getConnections() {
        return connections;
    }

    public HashMap<String, ArrayList<String>> getConnectionServers() {
        return connectionServers;
    }

    public synchronized String getUniqueId() {
        return uniqueId;
    }
    
    public void listenAgain() {
    	listener.setTerm(true);
    }

	public static ArrayList<Connection> getConnectionClients() {
		return connectionClients;
	}

	public static void setConnectionClients(Connection con) {
		log.debug("adding connection client: "+con);
		connectionClients.add(con);
	}

}
