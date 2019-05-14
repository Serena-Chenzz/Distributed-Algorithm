# Distributed-Algorithm
Ticket Selling Distributed System

# Explanations
The system is built upon the assignment 2 of COMP90015.
It is a java maven project to simulate multiple clients to buy/refund tickets in a distributed ticket selling system.
Paxos Algorithm is applied to elect a leader server and also re-elect a leader when the current leader crashes.
Multi-Paxos Algorithm is applied to deal with concurrent multi-client responses.
Each server has a local database for logs and a local database for app-related data, including user information, ticket information, purchase history, etc.
The goal of the application is to ensure the consistency of log database & ticket database of all the servers in the distributed system.
Therefore, when the client connect to any server, it will display consistent ticket information.

# How to run the code
1. It is suggested to open the project in an IDE (IntelliJ or Eclipse) Otherwise, you can package the application into jar files and run them. The two entry points are activitystreamer.Server and activitystreamer.Client

2. To start the very first server
Parameter: -lh <Your_IP_Address> -lp <Your_Port_Number>

3. To start the following servers:
Parameter: -lh <Your_IP_Address> -lp <Your_Port_Number> -rh <Your_Destination_IP> -rp<Your_Destination_Port>

4. To start a server and start the very first election as well
Parameter: -lh <Your_IP_Address> -lp <Your_Port_Number> -rh <Your_Destination_IP> -rp<Your_Destination_Port> -ifStartElection true

5. To start a client
Parameter: -rh <Server_IP_Address> -rp <Server_Port_Number>

# Attention!
1. If you start two or more servers in one machine, you need to change your database file urls in the util/Settings.java file. Because different server should use a different DB url in a single machine.
2. If you want to simulate the situation when a server crashes, you have to terminate the server on the Windows machine. The reason is that only Windows system can trigger SocketException, which is designed in our application code.

