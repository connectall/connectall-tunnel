This webapp is used as a webhook listener, ie, it receives a message from a webhook (eg BitBucket, etc), and flattens the json so the message can be received and understood by ConnectALL's Universal Adapter.

To install it:
1) Build the war file using the command: "mvn package"
2) Copy the war to your ConnectAll tomcat webapp folder.
3) Copy the connectall-tunnel.properties to the tomcat conf folder and edit the proprties for your ConnectALL installation.
4) Restart tomcat.
5) Add the webhook to your application that calls this webapp. And example is: http://hostname:8080/connectall-tunnel-1.0/rest/api/push/Bitbucket2DB?apiKey=xxx. Note that BitBucket2DB is the name of the application link to send the data to in ConnectALL.


You can review the catalina.out file to see the input and output json.
