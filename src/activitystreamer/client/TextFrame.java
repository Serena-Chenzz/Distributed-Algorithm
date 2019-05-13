package activitystreamer.client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import activitystreamer.models.Command;
import activitystreamer.util.Settings;

@SuppressWarnings("serial")
public class TextFrame extends JFrame implements ActionListener {
    private JTextField userText;
    private JPasswordField passwordText;
    private JButton loginButton;
    private JButton registerButton;
    private JButton[] purchaseButton;
    private JButton[] refundButton;
    private JButton refreshButton;
    private JButton logoutButton;
    private JButton returnButton;
    private JPanel loginPanel;
    private JPanel ticketPanel;
    private JPanel labelPanel;
    private static final Logger log = LogManager.getLogger();
    private ClientSkeleton clientCon;

    
    public TextFrame(ClientSkeleton clientSkeleton){

        purchaseButton = new JButton[4];
        refundButton = new JButton[4];
        
    	this.clientCon = clientSkeleton;

        this.setSize(1000, 350);
        this.setTitle("Ticket Selling Application");

        loginPanel = new JPanel();
        ticketPanel = new JPanel();
        labelPanel = new JPanel();
        this.add(loginPanel);
        loginPanel.setLayout(null);

        this.addWindowListener(new java.awt.event.WindowAdapter(){
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // When the client closes the window, we assume that the client logs out.
                JSONObject logout = Command.createLogout(Settings.getUsername(), Settings.getSecret());
                ClientSkeleton.getInstance().writeMsg(logout.toJSONString());
                log.info("User is going to log out....");
                ClientSkeleton.getInstance().disconnect();
            }
        });

        /* The following layout is for the initial login panel
        * */

        JLabel userLabel = new JLabel("Username");
        userLabel.setBounds(40, 20, 160, 50);
        userLabel.setFont(new Font("Arial", Font.PLAIN,20));
        loginPanel.add(userLabel);

        userText = new JTextField(20);
        userText.setBounds(200, 20, 360, 50);
        userText.setFont(new Font("Arial", Font.PLAIN,20));
        loginPanel.add(userText);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(40, 80, 160, 50);
        passwordLabel.setFont(new Font("Arial", Font.PLAIN,20));
        loginPanel.add(passwordLabel);

        passwordText = new JPasswordField(20);
        passwordText.setBounds(200, 80, 360, 50);
        passwordText.setFont(new Font("Arial", Font.PLAIN,20));
        loginPanel.add(passwordText);

        loginButton = new JButton("login");
        loginButton.setBounds(40, 160, 160, 50);
        loginButton.setFont(new Font("Arial", Font.PLAIN,20));
        loginPanel.add(loginButton);

        registerButton = new JButton("register");
        registerButton.setBounds(400, 160, 160, 50);
        registerButton.setFont(new Font("Arial", Font.PLAIN,20));
        loginPanel.add(registerButton);

        loginButton.addActionListener(this);
        registerButton.addActionListener(this);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    // Whenever the listener receives an action, it will trigger the processing here.
    public synchronized void actionPerformed(ActionEvent e) {
		if(e.getSource()==loginButton){
			String username = userText.getText().trim().replaceAll("\r","").replaceAll("\n","").replaceAll("\t", "");
			String secret = new String(passwordText.getPassword());
			Settings.setUsername(username);
			Settings.setSecret(secret);

			clientCon.writeMsg(Command.createLogin(username, secret).toJSONString());

		} else if(e.getSource()==registerButton){
            String username = userText.getText().trim().replaceAll("\r","").replaceAll("\n","").replaceAll("\t", "");
            String secret = new String(passwordText.getPassword());
            Settings.setUsername(username);
            Settings.setSecret(secret);

            clientCon.writeMsg(Command.createRegister(username,secret).toJSONString());

		} else if(e.getSource()==refreshButton){
		    String username = Settings.getUsername();
		    clientCon.writeMsg(Command.createRefreshRequest(username));

        } else if(e.getSource()==logoutButton){
		    JSONObject logout = Command.createLogout(Settings.getUsername(), Settings.getSecret());
	        ClientSkeleton.getInstance().writeMsg(logout.toJSONString());
	        log.info("User is going to log out....");
			ClientSkeleton.getInstance().disconnect();

        } else if(e.getSource()==purchaseButton[0]){
            String username = Settings.getUsername();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            clientCon.writeMsg(Command.createBuyTicket(100, username, strDate));

        } else if(e.getSource()==refundButton[0]){
            String username = Settings.getUsername();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            clientCon.writeMsg(Command.createRefundTicket(100, username, strDate));

        } else if(e.getSource()==purchaseButton[1]){
            String username = Settings.getUsername();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            clientCon.writeMsg(Command.createBuyTicket(200, username, strDate));

        } else if(e.getSource()==refundButton[1]){
            String username = Settings.getUsername();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            clientCon.writeMsg(Command.createRefundTicket(200, username, strDate));

        } else if(e.getSource()==purchaseButton[2]){
            String username = Settings.getUsername();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            clientCon.writeMsg(Command.createBuyTicket(300, username, strDate));

        } else if(e.getSource()==refundButton[2]){
            String username = Settings.getUsername();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            clientCon.writeMsg(Command.createRefundTicket(300, username, strDate));

        } else if(e.getSource()==purchaseButton[3]){
            String username = Settings.getUsername();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            clientCon.writeMsg(Command.createBuyTicket(400, username, strDate));

        } else if(e.getSource()==refundButton[3]){
            String username = Settings.getUsername();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String strDate = dateFormat.format(date);
            clientCon.writeMsg(Command.createRefundTicket(400, username, strDate));

        } else if(e.getSource()==returnButton){
            String username = Settings.getUsername();
            clientCon.writeMsg(Command.createRefreshRequest(username));
        }


    }

    /* The following layout is for the ticket selling panel
     * */

	public synchronized void enterSellingPanel(JSONObject ticketInfo, JSONArray buyingInfo){
        ArrayList<Integer> buyingTickets = new ArrayList<>();
        for(int i=0; i<buyingInfo.size(); i++){
            long trainLongId = (long)buyingInfo.get(i);
            buyingTickets.add((int)trainLongId);
        }
        loginPanel.setVisible(false);
        labelPanel.setVisible(false);
        ticketPanel.setVisible(false);
        this.setVisible(false);

        ticketPanel = new JPanel();
        this.add(ticketPanel);
        ticketPanel.setLayout(null);

        JLabel userLabel = new JLabel("Username: " + Settings.getUsername());
        userLabel.setBounds(20, 20, 200, 25);
        userLabel.setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(userLabel);

        JLabel statusLabel = new JLabel("Status: Login");
        statusLabel.setBounds(250, 20, 150, 25);
        statusLabel.setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(statusLabel);

        JLabel train1Label = new JLabel("TrainNo:100 -- From Beijing To Shanghai -- Remaining Tickets:" +
                ticketInfo.get("100"));
        train1Label.setBounds(20, 60, 500, 25);
        train1Label.setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(train1Label);

        purchaseButton[0] = new JButton("Purchase");
        purchaseButton[0].setBounds(550, 60, 100, 25);
        purchaseButton[0].setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(purchaseButton[0]);
        purchaseButton[0].addActionListener(this);

        if(buyingTickets.contains(100)){
            refundButton[0] = new JButton("Refund");
            refundButton[0].setBounds(700, 60, 100, 25);
            refundButton[0].setFont(new Font("Arial", Font.PLAIN,13));
            ticketPanel.add(refundButton[0]);
            refundButton[0].addActionListener(this);
        }

        JLabel train2Label = new JLabel("TrainNo:200 -- From Shanghai To Guangzhou -- Remaining Tickets:" +
                ticketInfo.get("200"));
        train2Label.setBounds(20, 100, 500, 25);
        train2Label.setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(train2Label);

        purchaseButton[1] = new JButton("Purchase");
        purchaseButton[1].setBounds(550, 100, 100, 25);
        purchaseButton[1].setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(purchaseButton[1]);
        purchaseButton[1].addActionListener(this);

        if(buyingTickets.contains(200)){
            refundButton[1] = new JButton("Refund");
            refundButton[1].setBounds(700, 100, 100, 25);
            refundButton[1].setFont(new Font("Arial", Font.PLAIN,13));
            ticketPanel.add(refundButton[1]);
            refundButton[1].addActionListener(this);
        }

        JLabel train3Label = new JLabel("TrainNo:300 -- From Beijing To Tianjin -- Remaining Tickets:" +
                ticketInfo.get("300"));
        train3Label.setBounds(20, 140, 500, 25);
        train3Label.setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(train3Label);

        purchaseButton[2] = new JButton("Purchase");
        purchaseButton[2].setBounds(550, 140, 100, 25);
        purchaseButton[2].setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(purchaseButton[2]);
        purchaseButton[2].addActionListener(this);

        if(buyingTickets.contains(300)){
            refundButton[2] = new JButton("Refund");
            refundButton[2].setBounds(700, 140, 100, 25);
            refundButton[2].setFont(new Font("Arial", Font.PLAIN,13));
            ticketPanel.add(refundButton[2]);
            refundButton[2].addActionListener(this);
        }

        JLabel train4Label = new JLabel("TrainNo:400 -- From Shanghai To Nanjing -- Remaining Tickets:" +
                ticketInfo.get("400"));
        train4Label.setBounds(20, 180, 500, 25);
        train4Label.setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(train4Label);

        purchaseButton[3] = new JButton("Purchase");
        purchaseButton[3].setBounds(550, 180, 100, 25);
        purchaseButton[3].setFont(new Font("Arial", Font.PLAIN,13));
        ticketPanel.add(purchaseButton[3]);
        purchaseButton[3].addActionListener(this);

        if(buyingTickets.contains(400)){
            refundButton[3] = new JButton("Refund");
            refundButton[3].setBounds(700, 180, 100, 25);
            refundButton[3].setFont(new Font("Arial", Font.PLAIN,13));
            ticketPanel.add(refundButton[3]);
            refundButton[3].addActionListener(this);
        }

        // Add two buttons at the end: Refresh & Logout
        refreshButton = new JButton("Refresh");
        refreshButton.setBounds(20, 220, 160, 50);
        refreshButton.setFont(new Font("Arial", Font.PLAIN,20));
        ticketPanel.add(refreshButton);
        refreshButton.addActionListener(this);

        logoutButton = new JButton("Logout");
        logoutButton.setBounds(200, 220, 160, 50);
        logoutButton.setFont(new Font("Arial", Font.PLAIN,20));
        ticketPanel.add(logoutButton);
        logoutButton.addActionListener(this);
        this.setVisible(true);
    }

    public synchronized void purchaseSuccessMsg(){
        ticketPanel.setVisible(false);
        this.setVisible(false);
        labelPanel = new JPanel();
        this.add(labelPanel);

        JLabel userLabel = new JLabel("User: " + Settings.getUsername() + " has successfully purchased the ticket.");
        userLabel.setBounds(40, 20, 800, 50);
        userLabel.setFont(new Font("Arial", Font.PLAIN,18));
        labelPanel.add(userLabel);

        returnButton = new JButton("Return");
        returnButton.setBounds(450, 80, 100, 25);
        returnButton.setFont(new Font("Arial", Font.PLAIN,13));
        labelPanel.add(returnButton);
        returnButton.addActionListener(this);
        this.setVisible(true);
    }

    public synchronized void purchaseFailMsg(){
        ticketPanel.setVisible(false);
        this.setVisible(false);
        labelPanel = new JPanel();
        this.add(labelPanel);

        JLabel userLabel = new JLabel("User: " + Settings.getUsername() + " failed to purchase the ticket.");
        userLabel.setBounds(40, 20, 800, 50);
        userLabel.setFont(new Font("Arial", Font.PLAIN,18));
        labelPanel.add(userLabel);

        returnButton = new JButton("Return");
        returnButton.setBounds(450, 80, 100, 25);
        returnButton.setFont(new Font("Arial", Font.PLAIN,13));
        labelPanel.add(returnButton);
        returnButton.addActionListener(this);
        this.setVisible(true);
    }

    public synchronized void refundSuccessMsg(){
        ticketPanel.setVisible(false);
        this.setVisible(false);
        labelPanel = new JPanel();
        this.add(labelPanel);

        JLabel userLabel = new JLabel("User: " + Settings.getUsername() + " has successfully refunded the ticket.");
        userLabel.setBounds(40, 20, 800, 50);
        userLabel.setFont(new Font("Arial", Font.PLAIN,18));
        labelPanel.add(userLabel);

        returnButton = new JButton("Return");
        returnButton.setBounds(450, 80, 100, 25);
        returnButton.setFont(new Font("Arial", Font.PLAIN,13));
        labelPanel.add(returnButton);
        returnButton.addActionListener(this);
        this.setVisible(true);
    }


}