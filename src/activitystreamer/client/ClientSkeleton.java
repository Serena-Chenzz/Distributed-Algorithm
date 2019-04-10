package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.*;
import activitystreamer.models.*;

public class ClientSkeleton extends Thread {

    private static final Logger log = LogManager.getLogger();
    private static ClientSkeleton clientSolution;
    private TextFrame textFrame;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedReader inreader;
    private PrintWriter outwriter;
    private JSONParser parser = new JSONParser();
    private boolean open = false;
    private boolean redirect = false;

    public static ClientSkeleton getInstance() {
        if (clientSolution == null) {
            clientSolution = new ClientSkeleton();
        }
        return clientSolution;
    }

    //When we create a client, we open up a socket!
    //The reason is for the client to handle different messages but using the same socket.
    //Create buffered reader and writer to receive/write message
    public ClientSkeleton() {
        try {
            Socket socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            outwriter = new PrintWriter(out, true);
            inreader = new BufferedReader(new InputStreamReader(in));
            open = true;
        } catch (UnknownHostException e) {
            System.out.println("Socket:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        }

        start();
    }

    public JSONObject sendActivityObject(JSONObject activityObj) {
        JSONObject jresponde = new JSONObject();

        try {
            String str = activityObj.toJSONString();
            log.debug("obj: " + str + " - raw: " + activityObj);
            outwriter.println(str);
            outwriter.flush();

            log.info("send activity to server" + Settings.getRemoteHostname()
                    + " " + Settings.getRemotePort() + " : " + str);
            String respond = inreader.readLine();
            jresponde = (JSONObject) parser.parse(respond);
            log.debug(jresponde.get("command"));
        } catch (EOFException e) {
            log.debug("EOF:" + e.getMessage());
        } catch (IOException e) {
            log.debug("readline:" + e.getMessage());
        } catch (ParseException e) {
            log.debug(e);
        }

        //If we don't need to use this socket anymore, we can close it!
        if (!open) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("close:" + e.getMessage());
                }
            }
        }
        return jresponde;
    }

    public void disconnect() {
        open = false;
        interrupt();
        System.exit(0);
    }

    //For textframe to write messages
    public void writeMsg(String s) {
    	outwriter.println(s);
        outwriter.flush();
    }

    public void run() {
        try {
            String username = Settings.getUsername();
            String secret = Settings.getSecret();
            Command userCommand;
            //If user only inputs its username, but not its user secret. It means this user wants to register
            //secret will be set to null after testing!
            if ((!username.equals("anonymous")) && (secret.equals(""))) {
                userCommand = Command.REGISTER;
            } //Otherwise, it means the user wants to log in
            else {
                userCommand = Command.LOGIN;
            }

            switch (userCommand) {
                case REGISTER: //Step 1, generate a secret for this user and print it out
                    String userAutoSecret = UUID.randomUUID().toString();
                    log.info("User:" + username + " Secret:" + userAutoSecret);
                    Settings.setSecret(userAutoSecret);
                    //Step 2, create register message and send it
                    JSONObject messageRegister = Command.createRegister(username, userAutoSecret);
                    log.info("Sending data...."+messageRegister.toJSONString());
                    writeMsg(messageRegister.toJSONString());
                    break;

                case LOGIN:
                    JSONObject messageLogin = Command.createLogin(username, secret);
                    log.info("Sending data...." + messageLogin.toJSONString());
                    writeMsg(messageLogin.toJSONString());
                    break;
                default:
                    break;
            }
            

            //Always listening for input stream
            while (open) {
                String response = inreader.readLine();
                if (response != null) {
                    JSONParser parser = new JSONParser();
                    JSONObject resObj = (JSONObject) parser.parse(response);
                    //If the object does not contain a field named 'command', it will return an invalid message
                    if((!resObj.containsKey("command"))){
                        String invalidFieldMsg = Command.createInvalidMessage("the received message did not contain a command");
                        writeMsg(invalidFieldMsg);
                        open=false;
                    }
                    else{
                        String targetCommand = resObj.get("command").toString();
                        //First check whether it is a valid command
                        if(!Command.contains(targetCommand)){
                            String invalidCommandMsg = Command.createInvalidMessage("the received message did not contain a valid command");
                            writeMsg(invalidCommandMsg);
                            open=false;
                        }
                        else{
                            Command responseCommand = Command.valueOf(targetCommand);
                            log.debug("Processing string: " + response + " from: " + this.socket);
                            
                            switch (responseCommand) {
                                //If the client receives the success message, it will prompt the textframe
                                case REGISTER_SUCCESS:
                                    if(!Command.checkValidCommandFormat2(resObj)){
                                        String invalidRegSuc = Command.createInvalidMessage("Invalid RegisterSuccess Message Format");
                                        writeMsg(invalidRegSuc);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open = true;
                                        JSONObject messageLogin = Command.createLogin(Settings.getUsername(), Settings.getSecret());
                                        writeMsg(messageLogin.toJSONString());
                                        }
                                    break;
                                case LOGIN_SUCCESS:
                                    if(!Command.checkValidCommandFormat2(resObj)){
                                        String invalidLogSuc = Command.createInvalidMessage("Invalid LoginSuccess Message Format");
                                        writeMsg(invalidLogSuc);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open=true;
                                        textFrame = new TextFrame(this);
                                    }
                                    break;
                                //If the client receives the failed message, it will close the socket
                                case REGISTER_FAILED:
                                    if(!Command.checkValidCommandFormat2(resObj)){
                                        String invalidRegFail = Command.createInvalidMessage("Invalid RegisterFailed Message Format");
                                        writeMsg(invalidRegFail);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open = false;
                                        log.info("Register fails, close connection...");
                                    }
                                    break;
                                case LOGIN_FAILED:
                                    if(!Command.checkValidCommandFormat2(resObj)){
                                        String invalidLogFail = Command.createInvalidMessage("Invalid LoginFailed Message Format");
                                        writeMsg(invalidLogFail);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open = false;
                                        log.info("Login failed, close connection...");
                                    }
                                    break;
                                case INVALID_MESSAGE:
                                    if(!Command.checkValidCommandFormat2(resObj)){
                                        String invalidInvMsg = Command.createInvalidMessage("Invalid InvalidMessage Message Format");
                                        writeMsg(invalidInvMsg);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        log.info((String)resObj.get("info"));
                                        open = false;
                                    }
                                    break;
                                // Redirect: close connection, start new socket and process again
                                case REDIRECT:
                                    if(!Command.checkValidRedirect(resObj)){
                                        String invalidRedMsg = Command.createInvalidMessage("Invalid Redirect Message Format");
                                        writeMsg(invalidRedMsg);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open = false;
                                        Settings.setRemoteHostname(resObj.get("hostname").toString());
                                        Settings.setRemotePort(Integer.parseInt(resObj.get("port").toString()));
                                        redirect = true; 
                                    }                          
                                    break;
                                case ACTIVITY_BROADCAST:
                                    if(!Command.checkValidActivityBroadcast(resObj)){
                                        String invalidAcMsg = Command.createInvalidMessage("Invalid ActivityBroadcast Message Format");
                                        writeMsg(invalidAcMsg);
                                        open=false;
                                        break;
                                    }
                                    else{   
                                    	JSONObject onlyActivity = (JSONObject) resObj.get("activity");
                                	    textFrame.setOutputText(onlyActivity);
                                	}
                                	break;
                                //If the command is not in the above command, the client will respond with an invalid message and close the connection
                                default:
                                    String invalidMsg = Command.createInvalidMessage("the received message did not contain an applicable command");
                                    writeMsg(invalidMsg);
                                    open=false;
                                    break;
                                }
                            }
                    }
                } else {
                    log.info("User logged out successfully.");
                    break;
                }
            }
        } 
        catch (EOFException e) {
            log.debug("Closing connection...." + Settings.socketAddress(socket));
        } catch (IOException e) {
            log.error("connection " + Settings.socketAddress(socket) + " closed with exception: " + e);
            this.disconnect();
        } catch (ParseException e) {
            //If the message is not a valid json string, we need to send back an invalid message and close the connection
            String invalidParseMsg = Command.createInvalidMessage("JSON parse error while parsing message");
            writeMsg(invalidParseMsg);
            open=false;
            log.debug("ParseException: " + e);
        }

        if (!open) {
            if (socket != null) {
                try {
                    socket.close();
                    System.out.println("socket is closing...");
                    if(redirect){
                        if(textFrame != null){
                            textFrame.setVisible(false);
                            textFrame.dispose();
                        }
                        // start a new client                         
                        clientSolution = new ClientSkeleton();
                    }else {
                    	System.exit(0);
                    }
                } catch (IOException e) {
                    System.out.println("close:" + e.getMessage());
                }             
            }
        }

    }

}
