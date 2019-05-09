package activitystreamer.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

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
    private static String serverID = UUID.randomUUID().toString();
    private static int lamportTimeStamp = 0;
    // added in the evening of 04-28
    private static UniqueID proposalID = new UniqueID(0,serverID);
    private static HashMap<UniqueID, String> promiseSet = new HashMap<>();
    private static int ackNumber = 0;
    private static String proposedValue;
    private static int neighbors_num = -1;

    private static String leaderAddress;

    private static Connection leader = null;
    private static HashMap<Integer,HashMap<String, Integer>> findMissingLog;
    private static HashMap<Integer, Integer> acceptedCounter;
    private static int missingAckCounter = 0;

    //Unchosen Log List
    private static LinkedList<String> unChosenLogs;
    //Corresponding Connections
    private static LinkedList<Connection> unChosenConnection;
    private static int firstUnchosenIndex;
    private static boolean leaderHasBeenDecided;

    private static final HashSet<Command> clientCommands = new HashSet<Command>() {{
        add(Command.LOGIN);
        add(Command.BUY_TICKET);
        add(Command.PURCHASE_SUCCESS);
        add(Command.INVALID_MESSAGE);
        add(Command.LOGIN_SUCCESS);
        add(Command.LOGIN_FAILED);
        add(Command.LOGOUT);
        add(Command.ACTIVITY_MESSAGE);
        add(Command.ACTIVITY_BROADCAST);
        add(Command.REFRESH_INFO);
        add(Command.REFRESH_REQUEST);
        add(Command.REGISTER);
        add(Command.REGISTER_FAILED);
        add(Command.REGISTER_SUCCESS);
        add(Command.BUY_TICKET);
        add(Command.REFUND_TICKET);
        add(Command.REFUND_SUCCESS);
        add(Command.PURCHASE_FAIL);
        add(Command.PURCHASE_SUCCESS);
    }};
    public static void setLeaderAddress(String leaderAddress) { Control.leaderAddress = leaderAddress; }
    public String getLeaderAddress() { return leaderAddress; }

    private static LinkedList<Integer> DBIndexList;
    private static int myLargestDBIndex;

    public static int getMyLargestDBIndex(){return myLargestDBIndex;}
    public synchronized String getproposedValue(){ return proposedValue; }
    public synchronized void setProposedValue(String value){ proposedValue = value;}
    public static int getMissingAckCounter(){ return missingAckCounter; }
    public static HashMap<Integer,HashMap<String, Integer>> getFindMissingLog(){return findMissingLog;}

    public synchronized int getAckNumber(){ return ackNumber; }

    public synchronized void addAckNumber() {ackNumber += 1;}

    private static int acceptedNum = 0; // need to be initiate whenver start new proposal

    protected static Control control = null;
    protected static Load serverLoad;

    public synchronized UniqueID getAccpetedID() {return accpetedID;}

    public synchronized UniqueID getPromisedID() {return promisedID;}

    public synchronized String getAccpetedValue() {return accpetedValue;}
    public synchronized int getLamportTimeStamp() {return lamportTimeStamp;}


    public synchronized UniqueID getProposalID() {return proposalID;}
    
    public synchronized ArrayList<Connection> getNeighbors(){
        return neighbors;
    }

    public synchronized HashMap<UniqueID,String> getPromiseSet(){
        return promiseSet;
    }

    public synchronized void addToPromiseSet(UniqueID promiseID,String promiseValue){
        promiseSet.put(promiseID,promiseValue);
    }

    public synchronized void clearPromiseSet(){ promiseSet.clear();
    }


    public synchronized void clearAckNumber(){ ackNumber = 0; }

    public void addLamportTimeStamp(){ lamportTimeStamp++;}



    public void sendSelection(int tempStamp){
        lamportTimeStamp++;
        proposalID = new UniqueID(tempStamp,uniqueId);
        setPromisedID(proposalID);
        String msg = Command.createPropose(tempStamp,uniqueId);
        for (Connection connection:neighbors){
            Propose propose = new Propose(msg,connection);
        }
        log.info("Start Selection on " + uniqueId);
    }

    public synchronized static void appendUnChosenLogs(String s, Connection con){
        log.info("Appending UnchosenLog: " + s);
        unChosenLogs.add(s);
        unChosenConnection.add(con);
    }

    public synchronized static void cleanUnChosenLogs(){
        unChosenLogs = new LinkedList<String>();
        unChosenConnection = new LinkedList<Connection>();
    }

    public static int getFirstUnchosenIndex(){return firstUnchosenIndex;}

    public synchronized static void setFirstUnchosenIndex(int index){firstUnchosenIndex = index;}

    public static synchronized void recordMissingLog(int index, String s){
        missingAckCounter++;
        if(findMissingLog.containsKey(index)){
            int currCount = 1;
            if(findMissingLog.get(index).containsKey(s)){
                currCount = findMissingLog.get(index).get(s)+1;
                findMissingLog.get(index).put(s,currCount);
            }
            else{
                findMissingLog.get(index).put(s,currCount);
            }
        }
        else{
            HashMap<String, Integer> newhm = new HashMap<String, Integer>();
            newhm.put(s,1);
            findMissingLog.put(index, newhm);
        }
    }

    public static synchronized void addIntoAcceptedCounter(int index){
        if(acceptedCounter.containsKey(index)){
            int prevVal = acceptedCounter.get(index);
            acceptedCounter.put(index, prevVal+1);
        }
        else{
            acceptedCounter.put(index, 1);
        }
    }

    public static synchronized boolean checkIfMeetMajority(int index){
        if(acceptedCounter.containsKey(index)){
            int count = acceptedCounter.get(index)+1;
            int N = getInstance().getNeighbors().size()+1;
            if(count > N/2) {return true;}
        }
        return false;
    }

    public static synchronized void removeFromUnchosenLogs(){
        unChosenLogs.removeFirst();
        unChosenConnection.removeFirst();
    }

    public static synchronized String getLogFromUnchosenLogs(){
        return unChosenLogs.getFirst();
    }

    public static synchronized Connection getConFromUnchosenLogs(){
        return unChosenConnection.getFirst();
    }

    public static synchronized void writeIntoLogDB(int index, String msg){
        try{
            String sqlLogUrl =Settings.getSqlLogUrl();
            java.sql.Connection sqlLogConnection = DriverManager.getConnection(sqlLogUrl);

            //Write into Log DB
            String sqlInsert = "INSERT INTO Log(LogId, Value) VALUES(?,?)";
            PreparedStatement pstmt = sqlLogConnection.prepareStatement(sqlInsert);
            pstmt.setInt(1, index);
            pstmt.setString(2, msg);
            pstmt.executeUpdate();
            log.info("Adding the Log into the database");
            myLargestDBIndex++;
            firstUnchosenIndex++;
            sqlLogConnection.close();

        }catch (SQLException e){
            log.debug(e);
        }
    }

    public static synchronized void slavePerformAction(String msg, int index){
        //Three main actions: Register, BuyTicket, RefundTicket
        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            Command slaveCommand;

            if(message.get("command").toString().equals("RELAY_MESSAGE")){
                String relayMessage = message.get("message").toString();
                message= (JSONObject)parser.parse(relayMessage);
                slaveCommand = Command.valueOf(message.get("command").toString());
            }
            else{
                slaveCommand = Command.valueOf(message.get("command").toString());
            }

            switch(slaveCommand){
                case REGISTER:
                    Register rg = new Register(message.toJSONString(), null, 3, index);
                    return;
                case BUY_TICKET:
                    BuyTicket bt = new BuyTicket(message.toJSONString(), null, 3);
                    return;
                case REFUND_TICKET:
                    RefundTicket rt = new RefundTicket(message.toJSONString(), null, 3);
                    return;
            }

        }catch (ParseException e) {
            log.debug(e);
        }


    }

    public static synchronized void leaderPerformAction(String msg, Connection con, int index){
        //Three main actions: Register, BuyTicket, RefundTicket
        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);

            if(message.get("command").toString().equals("RELAY_MESSAGE")){
                String relayMessage = message.get("message").toString();
                JSONObject relay = (JSONObject)parser.parse(relayMessage);

                Command relayCommand = Command.valueOf(relay.get("command").toString());

                switch(relayCommand){
                    case REGISTER:
                        Register rg = new Register(message.toJSONString(), con, 2, index);
                        return;
                    case BUY_TICKET:
                        BuyTicket bt = new BuyTicket(message.toJSONString(), con, 2);
                        return;
                    case REFUND_TICKET:
                        RefundTicket rt = new RefundTicket(message.toJSONString(), con, 2);
                        return;
                }
            }

            else{
                Command leaderCommand = Command.valueOf(message.get("command").toString());

                switch(leaderCommand){
                    case REGISTER:
                        Register rg = new Register(message.toJSONString(), con, 1, index);
                        return;
                    case BUY_TICKET:
                        BuyTicket bt = new BuyTicket(message.toJSONString(), con, 1);
                        return;
                    case REFUND_TICKET:
                        RefundTicket rt = new RefundTicket(message.toJSONString(), con, 1);
                        return;
                }
            }

        }catch (ParseException e) {
            log.debug(e);
        }
    }

    public synchronized static void appendDBIndexList(int index){
        DBIndexList.add(index);
    }

    public synchronized static boolean checkDBIndexListSize(){
        int N = getInstance().getNeighbors().size();
        if(DBIndexList.size() == N){
            return true;
        }
        return false;
    }

    public synchronized static boolean checkDBIndexList(){
        for(int id: DBIndexList){
            if(id > myLargestDBIndex){
                return false;
            }
        }
        return true;
    }

    public synchronized static void cleanDBIndexList(){
        DBIndexList = new LinkedList<Integer>();
    }

    public synchronized static void cleanUpMissingLog(){
        missingAckCounter = 0;
        findMissingLog = new HashMap<Integer,HashMap<String, Integer>>() ;
        acceptedCounter = new HashMap<Integer, Integer>();
    }

    public synchronized static void setLeaderHasBeenDecided(boolean flag){leaderHasBeenDecided = flag;}
    public synchronized static boolean getLeaderHasBeenDecided(){return leaderHasBeenDecided;}
    
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
        unChosenLogs = new LinkedList<String>();
        firstUnchosenIndex=0;
        findMissingLog = new HashMap<Integer,HashMap<String, Integer>>();
        acceptedCounter = new HashMap<Integer, Integer>();
        unChosenConnection = new LinkedList<Connection>();
        DBIndexList = new LinkedList<Integer>();
        leaderHasBeenDecided = false;

        serverLoad = new Load();
        if(Settings.getLocalHostname().equals("localhost")) {
        	uniqueId = ip.getHostAddress() + " " + Settings.getLocalPort();
        }else {
        	uniqueId = Settings.getLocalHostname() + " " + Settings.getLocalPort();
        }

        //Also initiate myLargestDBIndex
        AskDBIndex myAsk = new AskDBIndex("", null, 3);
        myLargestDBIndex = myAsk.getMyLargestDBIndex();
        firstUnchosenIndex = myLargestDBIndex+1;

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
                    addLamportTimeStamp();
                    Command userCommand = Command.valueOf(targetCommand);
//                    if (leaderAddress == null && clientCommands.contains(userCommand)){
//                        sendSelection(lamportTimeStamp);
//                    }
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
                            if (leaderAddress != null) {
                                String decideMsg = Command.createDecide(leaderAddress);
                                con.writeMsg(decideMsg);
                                log.info("Sending Decide to " + con.getRemoteId());
                            }

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

                                boolean initiateElection = Settings.getInitiateElection();
                                if(initiateElection && (neighborInfo.size() + 1) == neighbors.size()){
                                    sendSelection(lamportTimeStamp);
                                }
                                return false;
                            }

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
                            else if (leader == null){
                                appendUnChosenLogs(msg, con);
                                int currIndex = firstUnchosenIndex + unChosenLogs.size() -1;
                                String acceptMsg = Command.createMultiAccept(currIndex, msg, firstUnchosenIndex);
                                broadcast(acceptMsg);
                                log.info("Broadcasting AcceptMsg: "+acceptMsg);
                                return false;
                            }
                            else{
                                String clientConnection = con.getSocket().getInetAddress() + ":" + con.getSocket().getPort();
                                leader.writeMsg(Command.createRelayMsg(clientConnection, msg));
                                return false;
                            }
        
                        case LOGIN:
                            if (!Command.checkValidCommandFormat1(userInput)){
                                String invalidLogin = Command.createInvalidMessage("Invalid Login Message Format");
                                con.writeMsg(invalidLogin);
                                return true;
                            }
                            else{
                                Login login = new Login(con, msg);
                                return login.getResponse();
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

                        case ACCEPT:
                            if (!Command.checkValidAccept(userInput)){
                                String invalidAccept = Command.createInvalidMessage("Invalid Accept Message Format");
                                con.writeMsg(invalidAccept);
                                return true;
                            }
                            else{
                                Accept accept = new Accept(msg, con);
                                return false;
                            }

                        case ACCEPTED:
                            if (!Command.checkValidAccepted(userInput)){
                                String invalidAccepted = Command.createInvalidMessage("Invalid Accepted Message Format");
                                con.writeMsg(invalidAccepted);
                                return true;
                            }
                            else{
                                Accepted accepted = new Accepted(msg);
                                return false;
                            }

                        case REFRESH_REQUEST:
                            if (!Command.checkValidRefreshReq(userInput)){
                                String invalidRefreshReq = Command.createInvalidMessage("Invalid RefreshRequest Message Format");
                                con.writeMsg(invalidRefreshReq);
                                return true;
                            }
                            else{
                                RefreshRequest rq = new RefreshRequest(msg,con);
                                return rq.getCloseCon();
                            }

                        case BUY_TICKET:
                            if (!Command.checkBuying(userInput)){
                                String invalidBuying = Command.createInvalidMessage("Invalid BuyingTicket Message Format");
                                con.writeMsg(invalidBuying);
                                return true;
                            }
                            else if (leader == null){
                                appendUnChosenLogs(msg, con);
                                int currIndex = firstUnchosenIndex + unChosenLogs.size() -1;
                                broadcast(Command.createMultiAccept(currIndex, msg, firstUnchosenIndex));
                                return false;
                            }
                            else{
                                String clientConnection = con.getSocket().getInetAddress() + ":" + con.getSocket().getPort();
                                leader.writeMsg(Command.createRelayMsg(clientConnection, msg));
                                return false;
                            }

                        case RELAY_MESSAGE:
                            if (!Command.checkValidRelayMessage(userInput)){
                                String invalidRelayMsg = Command.createInvalidMessage("Invalid RelayMsg Message Format");
                                con.writeMsg(invalidRelayMsg);
                                return true;
                            }
                            else if(leader != null){
                                JSONObject message = (JSONObject) parser.parse(msg);
                                String relayResponse = message.get("message").toString();
                                Command responseCommand = Command.valueOf(message.get("command").toString());
                                Connection clientConnection = getClient(message.get("clientConnection").toString());
                                //Find out the client Connection
                                clientConnection.writeMsg(relayResponse);

                                if(responseCommand == Command.REGISTER_FAILED){
                                    clientConnection.closeCon();
                                }
                                return false;
                            }
                            else if(leader == null){
                                appendUnChosenLogs(msg, con);
                                int currIndex = firstUnchosenIndex + unChosenLogs.size() -1;
                                broadcast(Command.createMultiAccept(currIndex, msg, firstUnchosenIndex));
                                return false;
                            }

                        case REFUND_TICKET:
                            if (!Command.checkRefundTicket(userInput)){
                                String invalidRefunding = Command.createInvalidMessage("Invalid RefundingTicket Message Format");
                                con.writeMsg(invalidRefunding);
                                return true;
                            }
                            else if (leader == null){
                                appendUnChosenLogs(msg, con);
                                int currIndex = firstUnchosenIndex + unChosenLogs.size() -1;
                                broadcast(Command.createMultiAccept(currIndex, msg, firstUnchosenIndex));
                                return false;
                            }
                            else{
                                String clientConnection = con.getSocket().getInetAddress() + ":" + con.getSocket().getPort();
                                leader.writeMsg(Command.createRelayMsg(clientConnection, msg));
                                return false;
                            }
                        case PROMISE:
                            if (!Command.checkValidPromise(userInput)){
                                String invalidPromise = Command.createInvalidMessage("Invalid Promise Message Format");
                                con.writeMsg(invalidPromise);
                                return true;
                            }
                            else{
                                Promise promise = new Promise(msg);
                                return false;
                            }

                        case NACK:
                            if (!Command.checkValidNack(userInput)){
                                String invalidNack = Command.createInvalidMessage("Invalid Nack Message Format");
                                con.writeMsg(invalidNack);
                                return true;
                            }

                            else{
                                Control.getInstance().clearAckNumber();
                                Control.getInstance().clearPromiseSet();
                                log.info("Received NACK for " + proposalID.getServerID() + " " + Integer.toString(proposalID.getLamportTimeStamp()));
                                return false;
                            }

                        case PREPARE:
                            // So receiving a selection is equal to receiving a PREPARE message
                            // And the response is a PROMISE message
                            if (!Command.checkValidPrepare(userInput)){
                                String invalidPrepare = Command.createInvalidMessage("Invalid Propose Message Format");
                                con.writeMsg(invalidPrepare);
                                return true;
                            }
                            else{
                                Prepare prepare = new Prepare(msg, con);
                                return false;
                            }

                        case DECIDE:
                            if (!Command.checkValidDecide(userInput)){
                                String invalidDecide = Command.createInvalidMessage("Invalid Decide Message Format");
                                con.writeMsg(invalidDecide);
                                return true;
                            }
                            else{
                                Decide decide = new Decide(msg,con);
                                // Other server asks for leader's DB index
                                if(leader!=null){
                                    leader.writeMsg(Command.createAskLeaderDBIndex());
                                }
                                return decide.getCloseCon();
                            }

                        case MULTI_ACCEPT:
                            if (!Command.checkValidMultiAccept(userInput)){
                                String invalidMultiAccept = Command.createInvalidMessage("Invalid Multi-Accept Message Format");
                                con.writeMsg(invalidMultiAccept);
                                return true;
                            }
                            else{
                                MultiAccept multiAccept = new MultiAccept(msg,con);
                                return multiAccept.getCloseCon();
                            }

                        case MULTI_ACCEPTED:
                            if (!Command.checkValidMultiAccepted(userInput)){
                                String invalidMultiAccepted = Command.createInvalidMessage("Invalid Multi-Accepted Message Format");
                                con.writeMsg(invalidMultiAccepted);
                                return true;
                            }
                            else{
                                MultiAccepted multiAccepted = new MultiAccepted(msg,con);
                                return multiAccepted.getCloseCon();
                            }

                        case MULTI_DECIDE:
                            if (!Command.checkValidMultiDecide(userInput)){
                                String invalidMultiDecide = Command.createInvalidMessage("Invalid Multi-Decide Message Format");
                                con.writeMsg(invalidMultiDecide);
                                return true;
                            }
                            else{
                                MultiDecide multiDecide = new MultiDecide(msg,con);
                                return multiDecide.getCloseCon();
                            }

                        case GET_MISSING_LOG:
                            if (!Command.checkValidGetMissingLog(userInput)){
                                String invalidGetMissingLog= Command.createInvalidMessage("Invalid Get Missing Log Message Format");
                                con.writeMsg(invalidGetMissingLog);
                                return true;
                            }
                            else{
                                GetMissingLog getMissingLog = new GetMissingLog(msg,con);
                                return getMissingLog.getCloseCon();
                            }

                        case MISSING_LOG_INFO:
                            if (!Command.checkValidMissingLogInfo(userInput)){
                                String invalidMissingLogInfo = Command.createInvalidMessage("Invalid Missing Log Info Message Format");
                                con.writeMsg(invalidMissingLogInfo);
                                return true;
                            }
                            else{
                                MissingLogInfo missingLogInfo = new MissingLogInfo(msg,con);
                                return missingLogInfo.getCloseCon();
                            }

                        case ASK_DB_INDEX:
                            if (!Command.checkValidAskDBIndex(userInput)){
                                String invalidAskDBIndexMsg= Command.createInvalidMessage("Invalid Ask DB Index Message Format");
                                con.writeMsg(invalidAskDBIndexMsg);
                                return true;
                            }
                            else{
                                AskDBIndex askDBIndex = new AskDBIndex(msg,con,1);
                                return askDBIndex.getCloseCon();
                            }

                        case ASK_LEADER_DB_INDEX:
                            if (!Command.checkValidAskDBIndex(userInput)){
                                String invalidAskDBIndexMsg= Command.createInvalidMessage("Invalid Ask Leader DB Index Message Format");
                                con.writeMsg(invalidAskDBIndexMsg);
                                return true;
                            }
                            else{
                                AskDBIndex askDBIndex = new AskDBIndex(msg,con,2);
                                return askDBIndex.getCloseCon();
                            }

                        case REPLY_DB_INDEX:
                            if (!Command.checkValidReplyDBIndex(userInput)){
                                String invalidReplyDBIndexMsg= Command.createInvalidMessage("Invalid Reply DB Index Message Format");
                                con.writeMsg(invalidReplyDBIndexMsg);
                                return true;
                            }
                            else{
                                ReplyDBIndex replyDBIndex = new ReplyDBIndex(msg,con);
                                return replyDBIndex.getCloseCon();
                            }

                        case REPLY_LEADER_DB_INDEX:
                            if (!Command.checkValidReplyDBIndex(userInput)){
                                String invalidReplyDBIndexMsg= Command.createInvalidMessage("Invalid Reply Leader DB Index Message Format");
                                con.writeMsg(invalidReplyDBIndexMsg);
                                return true;
                            }
                            else{
                                JSONObject message = (JSONObject) parser.parse(msg);
                                long addIndexLong = (long)message.get("index");
                                int leaderIndex = (int)addIndexLong;

                                if(myLargestDBIndex < leaderIndex){
                                    //Start the GetMissingLog..
                                    log.info("Start Broadcasting Asking Missing Log Msg...");
                                    String getMissingLog = Command.createGetMissingLog(myLargestDBIndex+1, leaderIndex);
                                    Control.getInstance().broadcast(getMissingLog);
                                }
                                return false;
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
//        catch (InterruptedException e){
//            log.debug(e);
//        }
        return true;
    }
    

    public synchronized boolean containsServer(String remoteId) {
        return connectionServers.containsKey(remoteId);
    }

    public synchronized Connection getClient(String clientConnection){
        for(Connection client: connections.keySet()){
            if((client.getSocket().getInetAddress() + ":" + client.getSocket().getPort()).equals(clientConnection)){
                return client;
            }
        }
        return null;
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

    public synchronized String getAcceptedValueWithLargestProposalID(UniqueID uniqueID)
    {
        String value = promiseSet.get(uniqueID);
        UniqueID largestID = uniqueID;
        for (UniqueID key : promiseSet.keySet()){
            if (value == null)
            {
                if (promiseSet.get(key) != null)
                {
                    largestID = key;
                    value = promiseSet.get(key);
                }
            }
            else if (key.largerThan(largestID) && promiseSet.get(key) != null){
                largestID = key;
                value = promiseSet.get(largestID);
            }
        }
        return value;
    }

    public synchronized void setAcceptedID(UniqueID ID) {accpetedID = new UniqueID(ID.getLamportTimeStamp(),ID.getServerID());}

    public synchronized void setProposalID(UniqueID ID) {proposalID = new UniqueID(ID.getLamportTimeStamp(),ID.getServerID());}

    public synchronized void clearAcceptor()
    {
        promisedID = null;
        accpetedID = null;
        accpetedValue = null;
    }

    public synchronized void clearProposer()
    {
        promisedID = null;
        accpetedID = null;
    }

    public synchronized void setPromisedID(UniqueID ID) {

        promisedID = new UniqueID(ID.getLamportTimeStamp(),ID.getServerID());
        log.info("PromisedID Here " + promisedID.getServerID());
    }

    // Define the leader connection as well
    public synchronized void setAcceptedValue(String value) {
        accpetedValue = value;
        leaderAddress = value;
    }

    public synchronized static void setLeaderConnection(Connection con){
        leader = con;
        log.info("The leader connection has been set to: " + leaderAddress);
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

}
