# WebServers-TCPClients
A group of processes act as a system of cooperative Web servers, which interact
- with clients (via HTTP commands) – a client is able to communicate with any process from the group of Web servers
- among themselves to save and to search for objects. Each Web server keeps a copy of each object that Web clients write to them.


Processes implement the following functionalities:
1. Organization of the group of Web servers:
The group is organized like a linked list, where each process knows all the processes in the list and their order in it, but only ever communicates with the next one.
Each new process that wants to be inserted into the list communicates with some (any) process in the list, which undertakes to insert it into it.
At the end of the process, all processes have learned the change in the group.
2. Health check of the group of Web Servers:
The processes of the Web servers are informed of changes in the composition of the group (removal of a process due to its departure from the group).
Each Web Server is responsible for checking the health of its neighbor (next on the list).
3. Updating the group of Web servers:
Whenever a Web client connects to any member of the group (let's call it A) and performs an HTTP PUT of an object, Web server A stores the object locally in its memory (in a HashMap structure), replacing any previous version of the same object in the server A, and then start updating its peer processes in the group, via linked list communication.
4. Search for objects in the group of Web servers:
Whenever a Web client connects to any member of the group (let's call it A) and performs an HTTP GET, if the process it connected to does not find it in its memory, it asks the next one if it has, etc. until the whole group has been asked.
In case any process has the object, it will promote it to the next one, and so on until it reaches A and is given as a response to the Web client.
If any process in the list does not have the object, it stores it.
If the server to which the Web client initially connected has the object, it immediately responds with it (without involving the other processes in the list)
