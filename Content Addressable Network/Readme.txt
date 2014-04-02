Bootstrap.java is to be present and run on the machine which should run the bootstrap server.

The files, Peer.java, PeerOperations.java, neighbour.java and TakeOver.java are to be present in the nodes that are going to act as peers. PeerOperations.java is to be run in order to check the below implemented operations.

1. Join: (command - join)

=> Each time PeerOperations.java is executed in a node, "join" is the first command it expects. This is followed by the entering of bootstrap ip. The following operations can be performed after giving the "join" command.

2. View (command view)

=> The current peer information is displayed. The information include...

	Ip address and port of the peer.
	Zone of the peer. (Low x, Low y, Hi x and Hi y)

	The zones that it took over (due to it not being able to form rectangle or square with any of the neighbours)
		TakeOver's Low x, Low y, Hi x and Hi y are displayed

	Immediate neighbours.
		Neighbour's ip, port, low x, low y, high x and high y.
		
		If a neighbour has takeover region(s), they are stored in this neighbour as well so that greedy search can be performed for search of files.

	Takeover's neighbours.
		 Neighbours' ip, port, low x, low y, high x and high y. These are maintained so that when a new peer joins, minimal operations can be 		 		performed in assigning its neighbours.

	Data items stored.
		File names are displayed.

3. Search keyword (example: search peter)

=> Implements greedy search to search for the node that has the filename. Displays "Failure" if not found and the path in the format "Ip:port->Ip:port...->Success" if found.

4. Insert keyword(example: insert peter)

=> Inserts the filename in the appropriate peer and displays "Success" and the path if insertion is successful. 

5. Leave

=> When a leave is performed, it checks to see if it can form a rectangle or a square with any of its neighbours, if so, that neighbour is updated with the expanded zone and the neighbours are updated as well.

=> If a square or a rectangle cannot be formed, it gets taken over by a random peer and the neighbours are updated of this information. So whenever, a node 'x' gets taken over by 'y', the neighbours that have 'x' as their neighbour will now have 'y'.

=> The takeover information is present in the neighbours as well so that it doesn't look at just 'y' but also the takeover regions of 'y'.

=> If a new node joins whose random x and y coordinates are present in a takenover region, that region is entirely given to the new peer and the neighbours are updated accordingly.
