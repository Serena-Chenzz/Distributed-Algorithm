package activitystreamer.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import javax.jws.soap.SOAPBinding.Use;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    //This userlist is to store all user information locally in memory
    private static ArrayList<User> localUserList;
    private static ArrayList<String> usernameList;
    //Save the list of connected users plus timestamp and its connections
    private static HashMap<Connection, String> userConnections = new HashMap<Connection, String>();
    //This registerList is a pending list for all registration applications
    private static HashMap<Connection, User> registerPendingList;
    private static String uniqueId; // unique id for a server
    protected static boolean term = false;
    private static Listener listener;
    //Create a pending queue for each neighbor to serve as message buffer
    private static HashMap<Connection, ArrayList<Message>> serverMsgBuffQueue;
    private static HashMap<Connection, Boolean> serverMsgBuffActivator;
    //A buffer queue to store the messages sent to clients
    private static HashMap<Connection, ArrayList<Message>> clientMsgBuffQueue;
    //String will be like "timestamp,senderId,senderPort"
    private static HashMap<Connection, String> serverMsgAckQueue;
    //Create a hashmap to record whether to send Authentication message again. If the timestamp is set to -1, it means 
    //the connection has received a response
    private static HashMap<Connection, Long> authenticationAckQueue;
    //Create a hashmap to record whether to send a lock_requst again. If the timestamp is set to -1, it means the 
    //connection has received a response(acknowledgment)
    private static HashMap<Connection, HashMap<Long,String>> lockAckQueue;
    private InetAddress ip;
    private static HashMap<Connection, String> activatorMonitor = new HashMap<Connection, String>();

    protected static Control control = null;
    protected static Load serverLoad;
    
    public synchronized static ArrayList<Connection> getNeighbors(){
        return neighbors;
    }
    
    public synchronized static String getRemoteId(){
        return uniqueId;
    }
    
    public synchronized static HashMap<Connection, String> getActivatorMonitor(){
        return activatorMonitor;
    }
    
    public synchronized static HashMap<Connection,HashMap<Long,String>> getLockAckQueue(){
        return lockAckQueue;
    }
    
    public synchronized static HashMap<Connection, Long> getAuthenticationAckQueue(){
        return authenticationAckQueue;
    }
    
    public synchronized static HashMap<Connection, Boolean> getServerMsgBuffActivator(){
        return serverMsgBuffActivator;
    }
    
    public synchronized static HashMap<Connection, ArrayList<Message>> getServerMsgBuffQueue(){
        return serverMsgBuffQueue;
    }
    
    public synchronized static HashMap<Connection, ArrayList<Message>> getClientMsgBuffQueue(){
        return clientMsgBuffQueue;
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
        localUserList = new ArrayList<User>();
        usernameList = new ArrayList<String>();
        registerPendingList = new HashMap<Connection, User>();
        serverLoad = new Load();
        if(Settings.getLocalHostname().equals("localhost")) {
        	uniqueId = ip.getHostAddress() + " " + Settings.getLocalPort();
        }else {
        	uniqueId = Settings.getLocalHostname() + " " + Settings.getLocalPort();
        }
        
        serverMsgBuffQueue = new HashMap<Connection, ArrayList<Message>>();
        serverMsgAckQueue = new HashMap<Connection, String>();
        clientMsgBuffQueue = new HashMap<Connection, ArrayList<Message>>();
        serverMsgBuffActivator = new HashMap<Connection, Boolean>();
        authenticationAckQueue = new HashMap<Connection, Long>();
        lockAckQueue = new HashMap<Connection, HashMap<Long,String>>();
        
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
        // start broadcasting messages
        Thread activityServerBrd = new ActivityServerBroadcastThread();
        activityServerBrd.start();
        //start client broadcasting messages
        Thread activityClientBrd = new ActivityClientBroadcastThread();
        activityClientBrd.start();
        //Start sending Authentication Thread
        Thread authenticationSending = new SendingAuthenticationThread();
        authenticationSending.start();
        //Start sending lockRequst Thread
        Thread lockRequestSending = new SendingLockRequestThread();
        lockRequestSending.start();
        
        Thread ackMonitor = new MsgBufferActivatorThread();
        ackMonitor.start();
        //Start userLocalList broadcast Thread
        new UserListBroadCastThread();
    }
    
    //Activator Methods
    public synchronized void deactivateMessageQueue(Connection con){
        serverMsgBuffActivator.put(con, false);
        activatorMonitor.put(con, "false " + System.currentTimeMillis());
    }
    
    public synchronized void activateMessageQueue(String remoteId){
        if (!serverMsgBuffActivator.isEmpty()){
            for(Connection con:serverMsgBuffActivator.keySet()){
                if (con.getRemoteId().equals(remoteId)){
                    serverMsgBuffActivator.put(con, true);
                    activatorMonitor.put(con, "true " + System.currentTimeMillis());
                }
            }
            
        }
    }
    
    //LockAckQueue methods
    public synchronized void setLockAckQueue(Connection con, String lockRequest){
        HashMap<Long,String> targetMap = lockAckQueue.get(con);
        targetMap.put(System.currentTimeMillis(), lockRequest);
    }
    
    public synchronized void setSuspendLockAck(Connection con, String lockRequest){
        HashMap<Long,String> targetMap = lockAckQueue.get(con);
        for(Long time:targetMap.keySet()){
            if (targetMap.get(time).equals(lockRequest)){
                targetMap.put(time, "Suspend");
            }
        }
    }
    
    public synchronized void unsetLockAckQueue(String remoteId, String lockRequest){
        for (Connection con: lockAckQueue.keySet()){
            if (con.getRemoteId().equals(remoteId)){
                HashMap<Long,String> targetMap = lockAckQueue.get(con);
                for(Long time:targetMap.keySet()){
                    if (targetMap.get(time).equals(lockRequest)){
                        targetMap.put(time, "Received Ack");
                    }
                }}
        }
    }
    
    //ClientBufferQueue Methods
    public synchronized void initiateClientMsgBufferQueue(Connection con){
        clientMsgBuffQueue.put(con, new ArrayList<Message>());  
    }
    public synchronized void removeClientFromMsgBuffer(Connection con){
        clientMsgBuffQueue.remove(con);
    }
    
    public synchronized void addToAllClientMsgBufferQueue(Message msg){
        
        for (Connection con: clientMsgBuffQueue.keySet()){
            ArrayList<Message> targetList = clientMsgBuffQueue.get(con);
            targetList.add(msg);
        }
       
    }
    
    public synchronized void removeFromClientMsgBufferQueue(Connection con, Message msg){
        if (clientMsgBuffQueue.containsKey(con)){
            ArrayList<Message> targetList = clientMsgBuffQueue.get(con);
            targetList.remove(msg);
            System.out.println("here");
        }
    }
    
    public synchronized static HashMap<Connection, String> getUserConnections(){
        return userConnections;
    }
    
  //Methods about ackQueue
    public synchronized void updateAckQueue(long timestamp,String senderIp, int senderPort){
        for (Connection con:serverMsgAckQueue.keySet()){
            if (con.getRemoteId().equals(senderIp + " " + senderPort)){
                serverMsgAckQueue.put(con, timestamp + " " +senderIp + " " + senderPort);
            }
        }
        
    }
    
    public synchronized boolean checkAckQueue(long timestamp,String senderIp, int senderPort){
        for (Connection con:serverMsgAckQueue.keySet() ){
            if (con.getRemoteId().equals(senderIp + " " + senderPort)){
                if(serverMsgAckQueue.get(con)!=null){
                    long latestTime = Long.parseLong(serverMsgAckQueue.get(con).split(" ")[0]);
                    if(timestamp <= latestTime){
                        return false;
                    }
                }}
        }
        return true;
    }
    
    //Methods about serverMsgBufferQueue
    public synchronized boolean addMessageToBufferQueue(Message ackMsg){
        log.info("Adding " + ackMsg);
        for (Connection con:  neighbors){
            ArrayList<Message> targetList = serverMsgBuffQueue.get(con);
            targetList.add(ackMsg);
            System.out.println("Server: " + serverMsgBuffQueue);
        }
        return false;
    }
    
    public synchronized boolean checkRegisterPendingList(String username){
        for (User user:registerPendingList.values()){
            if(user.getUsername().equals(username)){
                return true;
            }
        }
        return false;
    }
    
    public synchronized boolean removeMessageFromBufferQueue(long timestamp, String senderIp, int senderPort){
        for (Connection con: serverMsgBuffQueue.keySet()){
            if (con.getRemoteId().equals(senderIp + " " + senderPort)){
                ArrayList<Message> targetList = serverMsgBuffQueue.get(con);
                try{
                    if (targetList != null){
                        for (Message msg:targetList ){
                            if ((msg.getTimeStamp()== timestamp)){
                                targetList.remove(msg);
                                System.out.println(serverMsgBuffQueue);
                                return true;
                            }
                        }
                        return false;
                    } 
                }
                catch (NoSuchElementException e){
                    log.error("Fail to remove the message. " + e.toString());
                }
                
            }
        }
        return false;
        
    }
    
    //Cleanup methods
    public synchronized void cleanMessageBufferQueue(Connection con){
        if (serverMsgBuffQueue.containsKey(con)){
            serverMsgBuffQueue.remove(con);
        }
    }
    
    public synchronized void cleanAckQueue(Connection con){
        if (serverMsgAckQueue.containsKey(con)){
            serverMsgAckQueue.remove(con);
        }
    }
    
    public synchronized void cleanClientMsgBuffQueue(Connection con){
        if (clientMsgBuffQueue.containsKey(con)){
            clientMsgBuffQueue.remove(con);
        }
    }
    
    public synchronized void cleanServerMsgBuffActivator(Connection con){
        if (serverMsgBuffActivator.containsKey(con)){
            serverMsgBuffActivator.remove(con);
        }
    }
    
    public synchronized void cleanAuthenticationAckQueue(Connection con){
        if (authenticationAckQueue.containsKey(con)){
            authenticationAckQueue.remove(con);
        }
    }
    
    public synchronized void cleanLockAckQueue(Connection con){
        if (lockAckQueue.containsKey(con)){
            lockAckQueue.remove(con);
        }
    }
  
    
    public synchronized void createServerConnection(String hostname, int port) {
        try {
                Connection con = outgoingConnection(new Socket(hostname, port));
                JSONObject authenticate = Command.createAuthenticate(Settings.getSecret(), uniqueId);
                //initiate the authenticationAckQueue
                authenticationAckQueue.put(con, System.currentTimeMillis());
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
                                
                                //Initialize message queue
                                serverMsgBuffQueue.put(con, new ArrayList<Message>());
                                serverMsgBuffActivator.put(con, true);
                                activatorMonitor.put(con, "true " + System.currentTimeMillis());
                                serverMsgAckQueue.put(con, null);
                                lockAckQueue.put(con, new HashMap<Long,String>());
                                
                                log.debug("Add neighbor: " + con.getRemoteId());
                                
                                
                                //Send the list of local registered users to new auth server
                                String registerUserList = Command.usersRegisteredList(localUserList);
                                con.writeMsg(registerUserList); 
                                
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
                                //Change the authenticateAckQueue value to -1
                                authenticationAckQueue.put(con, (long)(-1));
                                // Set remoteId for this connection
                                String remoteId = (String)userInput.get("remoteId");
                                con.setRemoteId(remoteId);
                                // Add connection into neighbor list
                                connectionServers.put(remoteId, new ArrayList<String>());
                                neighbors.add(con);
                                
                                //Initialize message queue
                                serverMsgBuffQueue.put(con, new ArrayList<Message>());
                                serverMsgBuffActivator.put(con, true);
                                activatorMonitor.put(con, "true " + System.currentTimeMillis());
                                serverMsgAckQueue.put(con, null);
                                lockAckQueue.put(con, new HashMap<Long,String>());
                                
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
                            //Change the authenticateAckQueue value to -1
                            authenticationAckQueue.put(con, (long)(-1));
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
                            
                        case REGISTER_SUCCESS_BROADCAST:
                            if (!Command.checkValidCommandFormat1(userInput)){
                                String invalidReg = Command.createInvalidMessage("Invalid Register_Success_Broadcast Message Format");
                                con.writeMsg(invalidReg);
                                return true;
                            }
                            else{
                                RegisterSuccessBroadcast reg = new RegisterSuccessBroadcast(msg, con);
                                return reg.getCloseCon();
                            }
        
                        case LOCK_REQUEST:
                            if (!Command.checkLockRequest(userInput)){
                                String invalidLoc = Command.createInvalidMessage("Invalid LockRequest Message Format");
                                con.writeMsg(invalidLoc);
                                return true;
                            }
                            else{
                                Lock lock = new Lock(msg, con);
                                return lock.getCloseCon();
                            }
        
                        case LOCK_DENIED:
                            if (!Command.checkLockRequest(userInput)){
                                String invalidLocD = Command.createInvalidMessage("Invalid LockDenied Message Format");
                                con.writeMsg(invalidLocD);
                                return true;
                            }
                            else{
                                LockDenied lockDenied = new LockDenied(msg, con);
                                return lockDenied.getCloseCon();
                            }
        
                        case LOCK_ALLOWED:
                            if (!Command.checkLockRequest(userInput)){
                                String invalidLocA = Command.createInvalidMessage("Invalid LockAllowed Message Format");
                                con.writeMsg(invalidLocA);
                                return true;
                            }
                            else{
                                LockAllowed lockAllowed = new LockAllowed(msg, con);
                                return lockAllowed.getCloseCon();
                            }
        
                        case LOGIN:
                            if (!Command.checkValidCommandFormat1(userInput)&&!(Command.checkValidAnonyLogin(userInput))){
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
                            if (!Command.checkValidLogout(userInput)){
                                String invalidLogout = Command.createInvalidMessage("Invalid Login Message Format");
                                con.writeMsg(invalidLogout);
                                return true;
                            }
                            else{
                                Logout logout = new Logout(con, msg);
                                return logout.getResponse();
                            }
                            
                        case ACTIVITY_MESSAGE:
                            if (!Command.checkValidAcitivityMessage(userInput)){
                                String invalidAc = Command.createInvalidMessage("Invalid ActivityMessage Message Format");
                                con.writeMsg(invalidAc);
                                return true;
                            }
                            else{
                                ActivityMessage actMess = new ActivityMessage(con, msg);
                                return actMess.getResponse();
                            }
                            
                        case ACTIVITY_SERVER_BROADCAST:
                            if (!Command.checkValidActivityServerBroadcast(userInput)){
                                String invalidAc = Command.createInvalidMessage("Invalid ActivityServerBroadcast Message Format");
                                con.writeMsg(invalidAc);
                                return true;
                            }
                            else{
                                ActivityServerBroadcast actBroad = new ActivityServerBroadcast(con, msg);
                                return actBroad.getResponse();
                            }
                        case ACTIVITY_ACKNOWLEDGEMENT:
                            if (!Command.checkValidActivityAcknowledgment(userInput)){
                                String invalidAc = Command.createInvalidMessage("Invalid ActivityAcknowledgement Message Format");
                                con.writeMsg(invalidAc);
                                return true;
                            }
                            else{
                                ActivityAcknowledgment actAck = new ActivityAcknowledgment(con, msg);
                                return actAck.getResponse();
                            }
                        case USERS_REGISTERED_LIST:
                            if (!Command.checkUsersRegisteredList(userInput)){
                            	String invalidMsg = Command.createInvalidMessage("Invalid UsersRegisteredList Message Format");
                                con.writeMsg(invalidMsg);
                                return true;
                            }
                            else{
                            	Gson gson = new GsonBuilder().create();

                                UserListRequest jsonUserList = gson.fromJson(msg,UserListRequest.class);
                                new UserListReview(jsonUserList.getUserList());
                                
                            	return false;
                            } 
                        case RELAY_MESSAGE:
                            if (!Command.checkRelayMessage(userInput)){
                                String invalidMsg = Command.createInvalidMessage("Invalid RelayMessage Message Format");
                                con.writeMsg(invalidMsg);
                                return true;
                            }
                            else{
                                RelayMessage resRelay = new RelayMessage(msg, con);
                                return resRelay.getCloseCon();
                            }
                            
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
    
    //This method is to send a relay message to a random neighbor 
    public synchronized void sendMessageToRandomNeighbor(String msg){
        if(neighbors.size() > 0){
            int index = (int) (System.currentTimeMillis() % (neighbors.size()));
            System.out.println(index);
            Connection relayConn = neighbors.get(index);
            relayConn.writeMsg(msg);
        }
    }
    
	public synchronized void printRegisteredUsers() {
    	usernameList.clear();
    	try {
			for(User user : localUserList) {
	    		usernameList.add(user.getUsername());	
	    	}
    	}
		catch(Exception e) {
			
		}
    	log.info("Users registered:" +usernameList); 
    }
    public synchronized boolean containsServer(String remoteId) {
        return connectionServers.containsKey(remoteId);
    }
    
  //Given a username and secret, check whether it is correct in this server's local userlist.
    public synchronized boolean checkLocalUserAndSecret(String username,String secret){
        for(User user:localUserList ){
            if (user.getUsername().equals(username)&&user.getSecret().equals(secret)){
                return true;
            }
        } 
        return false;
    }

    //Add a user to the pending list
    public synchronized void addUserToRegistePendingList(String username, String secret, Connection con) {
        User pendingUser = new User(username, secret);
        registerPendingList.put(con, pendingUser);

    }

    //Check whether this user is in this pending list. If so, write messages
    public synchronized boolean changeInPendingList(String username, String secret) {
        Iterator<Map.Entry<Connection, User>> it = registerPendingList.entrySet().iterator();
        User targetUser = new User(username, secret);
        while (it.hasNext()) {
            Map.Entry<Connection, User> mentry = it.next();
            Connection con = mentry.getKey();
            User user = mentry.getValue();
            if (user.equals(targetUser)) {
                JSONObject response = Command.createRegisterSuccess(username);
                if (checkAllLockAllowed(username)){
                    registerPendingList.remove(con);
                }
                System.out.println(registerPendingList);
                con.writeMsg(response.toJSONString());
                // check if it needs redirect
                if(serverLoad.checkRedirect(con)){
                    // Close connection
                    connectionClosed(con);
                    con.closeCon();
                };              
                return true;
            }
        }
        return false;
    }

    //Check whether this user is in this pending list, if so, delete it and write messages
    public synchronized boolean deleteFromPendingList(String username, String secret) {
        Iterator<Map.Entry<Connection, User>> it = registerPendingList.entrySet().iterator();
        User targetUser = new User(username, secret);
        while (it.hasNext()) {
            Map.Entry<Connection, User> mentry = it.next();
            Connection con = mentry.getKey();
            User user = mentry.getValue();
            if (user.equals(targetUser)) {
                JSONObject response = Command.createRegisterFailed(username);
                con.writeMsg(response.toJSONString());
                registerPendingList.remove(con);
                con.closeCon();
                return true;
            }
        }
        return false;
    }

    //Given a username, check whether it is in this server's local userlist.
    public synchronized boolean checkLocalUser(String username) {
        for (User user : localUserList) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    //This function is to add a user to the local userList
    public synchronized void addLocalUser(String username, String secret) {
        User userToAdd = new User(username, secret);
        log.info(username + " " + secret);
        localUserList.add(userToAdd);
    }

    //This function is to delete a user to the local userList
    public synchronized void deleteLocalUser(String username, String secret) {
        boolean flag = false;
        for(User user:localUserList){
            if (user.getUsername().equals(username)){
                flag=true;
            }
        }
        if(flag){
            localUserList.remove(username);
        }
    }
    
    //Change certain connection to status 'Lock_suspend'
    public synchronized void changeLockStatus(Connection con, String username){
        if(connectionServers.containsKey(con.getRemoteId())){
            connectionServers.get(con.getRemoteId()).add("LOCK_SUSPEND " + username);
        }
        System.out.println(connectionServers);
    }
    
    //If return 0, means lock_denied. If 1,means there are some lock_suspend. If 2, means there are all lock_allowed.
    public synchronized void addLockAllowedDenied(String remoteId1, String msg) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            String command = message.get("command").toString();
            String username = message.get("username").toString();
            //Step 1, add this string to the connectionservers list
            for (String remoteId : connectionServers.keySet()) {
                if (remoteId.equals(remoteId1)) {
                    connectionServers.get(remoteId).add(command + " " + username);
                }
                log.debug("Updated hashmap, Con:" + remoteId1 + " Value:" + connectionServers.get(remoteId));
                
            }
            System.out.println(connectionServers);
            
        } catch (ParseException e) {
            log.error(e);
        }
    }
    
    public synchronized boolean checkAllLocks(String username){
        //Step 2, check whether all the connection return a lock allowed/lock_suspend regarding to this user
        //lock_suspend means the connection has problem, and we don't need to wait for the response
        for (String remoteId : connectionServers.keySet()){
            if (!(connectionServers.get(remoteId).contains("LOCK_ALLOWED " + username))){
                return false;
            }
        }
        return true;
        
    }
    
    public synchronized boolean checkAllLockAllowed(String username){
        for (String remoteId : connectionServers.keySet()){
            if (!(connectionServers.get(remoteId).contains("LOCK_ALLOWED " + username))){
                return false;
            }
        }
        return true;
        
    }

    public synchronized boolean broadcast(String msg) {     
        for (Connection nei : neighbors) {
            nei.writeMsg(msg);               
        }    
        // need failure model
        return true;
    }
    
    public synchronized void sendBufferedUsers(){
        //Check all users in registerPendingList
        for (User user: registerPendingList.values()){
            String username = user.getUsername();
            String secret = user.getSecret();

            if (Control.getInstance().checkAllLocks(username)){
                //Writing the user info in local storage
                Control.getInstance().addLocalUser(username, secret);
                //If it has received all lock_allowed from its neighbors, it will continue to check whether it is inside 
                //local register pending list.
                if (Control.getInstance().changeInPendingList(username, secret)){
                    //If the client is registered in this server, it will return back the message
                    //Also, broadcast register success message to all other servers
                    String registerSucMsg = Command.createRegisterSuccessBroadcast(username, secret).toJSONString();
                    Control.getInstance().broadcast(registerSucMsg);
                }
            }
        }
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
            sendBufferedUsers();
            registerPendingList.remove(con);
            cleanMessageBufferQueue(con);
            cleanAckQueue(con);
            cleanClientMsgBuffQueue(con);
            cleanServerMsgBuffActivator(con);
            cleanAuthenticationAckQueue(con);
            cleanLockAckQueue(con);
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
	

	public static ArrayList<User> getLocalUserList() {
		return localUserList;
	}

	public static void setLocalUserList(ArrayList<User> localUserList) {
		Control.localUserList.clear();
		Control.localUserList = localUserList;
	}

}
