# Assignment 4 Activity 2
## Description:
This program will start up a new simple Battleship game on a server, which then multiple clients can join in order to complete the game.

## Purpose:
Demonstrate simple Client and Server communication using `SocketServer` and `Socket`classes.

Here a simple protocol is defined which uses protobuf. The client reads in a json file and then creates a protobuf object from it to send it to the server. The server reads it and sends back the calculated result. 

The response is also a protobuf but only with a result string. 

To see the proto file see: src/main/proto which is the default location for proto files. 

Gradle is already setup to compile the proto files. 

### The procotol
Request:
- NAME: a name is sent to the server
	- name
	Response: GREETING
			- message
- LEADER: client wants to get leader board
	- no further data
	Response: LEADER
			- leader
- NEW: client wants to enter a game
	- no further data
	Response: TASK
			- image
			- task
- ANSWER: client sent an answer to a server task
	- answer
	Response: TASK
			- image
			- task
			- eval
	OR
	Response: WON
			- image
- QUIT: clients wants to quit connection
	- no further data
	Response: BYE
		- message

Response ERROR: anytime there is an error you should send the ERROR response and give an appropriate message. Client should act appropriately
	- message

## How to run it 


### Default 
Server is Java
Per default on 9099
` runServer `


Clients runs per default on 
host localhost, port 9099
Run Java:
` runClient `


### With parameters:
Java
`gradle runClient -Pport=9099 -Phost='localhost'`
`gradle runServer -Pport=9099`


### Requirements Fulfilled
* Menu gives user 3 options:
  1. Leaderboard - Displays scoreboard of current scores of all players
  2. Play game - Joins a game started on the server
  3. Quit - Quits the client
* Multiple clients are able to join the same game 
* Game will ask user to enter a row and col (In same line, format: <int> <int>) then return updated board to user with a message if it was a hit
* Game closes gracefully through option 3
* User can exit the client during game play to exit
* Server will not crash when a client unexpectedly disconnects
 
### [ScreenCast demo](https://youtu.be/YS7VL9RKZsM)

