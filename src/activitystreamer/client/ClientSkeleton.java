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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
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

    public static ClientSkeleton getInstance() {
        if (clientSolution == null) {
            clientSolution = new ClientSkeleton();
        }
        return clientSolution;
    }

    /*When we create a client, we open up a socket!
    The reason is for the client to handle different messages but using the same socket.
    Create buffered reader and writer to receive/write message
    */

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

    public synchronized void disconnect() {
        open = false;
        interrupt();
        System.exit(0);
    }

    //For textframe to write messages
    public synchronized void writeMsg(String s) {
    	outwriter.println(s);
        outwriter.flush();
        log.info(s);
    }

    public void run() {
        try {
            textFrame = new TextFrame(this);
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
                                        //Sending Refresh_req in order to receive the latest info about the ticket info
                                        writeMsg(Command.createRefreshRequest(Settings.getUsername()));
                                        open=true;
                                    }
                                    break;
                                //If the client receives the Register_Failed message, it will close the socket
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
                                //If the client receives the Login_Failed info, it will close the socket
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
                                case REFRESH_INFO:
                                    if(!Command.checkValidRefreshInfo(resObj)){
                                        String invalidRefreshInfo = Command.createInvalidMessage("Invalid RefreshInfo Message Format");
                                        writeMsg(invalidRefreshInfo);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open = true;
                                        log.info("Refreshing Display Panel");
                                        //When the client receives Refresh_Info, it will display the panel with refreshed info on it
                                        JSONObject trainInfo = (JSONObject) resObj.get("ticketInfo");
                                        JSONArray buyingInfo = (JSONArray) resObj.get("purchaseInfo");
                                        textFrame.enterSellingPanel(trainInfo, buyingInfo);
                                    }
                                    break;
                                case PURCHASE_SUCCESS:
                                    if(!Command.checkBuying(resObj)){
                                        String invalidPurchaseSucc = Command.createInvalidMessage("Invalid Purchase Success Message Format");
                                        writeMsg(invalidPurchaseSucc);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open = true;
                                        log.info("Refreshing Display Panel... Purchase Success");
                                        //Displey purchase_success info to the client
                                        textFrame.purchaseSuccessMsg();
                                    }
                                    break;
                                case PURCHASE_FAIL:
                                    if(!Command.checkBuying(resObj)){
                                        String invalidPurchaseFail = Command.createInvalidMessage("Invalid Purchase Fail Message Format");
                                        writeMsg(invalidPurchaseFail);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open = true;
                                        log.info("Refreshing Display Panel... Purchase Failed");
                                        //Display purchase_fail message to the client
                                        textFrame.purchaseFailMsg();
                                    }
                                    break;
                                case REFUND_SUCCESS:
                                    if(!Command.checkRefundTicket(resObj)){
                                        String invalidRefundSucc = Command.createInvalidMessage("Invalid Refund Success Message Format");
                                        writeMsg(invalidRefundSucc);
                                        open=false;
                                        break;
                                    }
                                    else{
                                        open = true;
                                        log.info("Refreshing Display Panel... Refund Success");
                                        //Display refund_success message to the client
                                        textFrame.refundSuccessMsg();
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
        } catch (EOFException e) {
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
                    System.exit(0);

                } catch (IOException e) {
                    System.out.println("close:" + e.getMessage());
                }             
            }
        }

    }

}
