# Distributed-Algorithm
Ticket Selling Distributed System

# Explanations
1. The system is built upon the assignment 2 of COMP90015.
2. It is a java maven project to simulate multiple clients to buy/refund tickets in a distributed ticket selling system.
3. Paxos Algorithm is applied to elect a leader server and re-elect a leader when the current leader crashes.
4. Multi-Paxos Algorithm is applied to deal with concurrent multi-client requests.
5. Each server has a local database for logs and a local database for app-related data, which includes user information, ticket information, purchase history, etc.
6. The goal of the application is to ensure the consistency of log database & ticket database of all the servers in the distributed system.
7. Therefore, when the client connects to any server in the system, it will display consistent ticket information.

# Code Structure:
1. Main running classes are Client.java and Server.java under src/activitystreamer folder.
2. Under activitystreamer/client folder,
- ClientSkeleton.java defines client arguments.
- TextFrame.java covers GUI code.
3. Under activitystreamer/model folder,
- Command.java defines all the commands and command formats we used in the application
- UniqueID.java defines the proposal Id we used in Paxos election
- User.java defines User
4. Under activitystreamer/server folder,
- commands folder covers all kinds of commands. Whenever the server receives a certain command, it will go for corresponding class and do operations.
- Connection.java defines the connection between servers as well as connection between server and client
- Control.java defines when a server receives a message, what it should do.
- Listener.java defines a server listener
- Load.java defines server load
- Message.java defines a message in transit
5. Under activitystreamer/util folder,
- Settings.java defines some config information of this server.

# How to run the code
1. It is suggested to open the project in an IDE (IntelliJ or Eclipse) The two entry points are activitystreamer/Server.java and activitystreamer/Client.java

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

