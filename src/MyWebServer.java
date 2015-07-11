/*--------------------------------------------------------

1. Name / Date: Garrett Krueger/May 9, 2014

2. Java version used, if not the official version for the class:

build 1.8.0-b132

3. Precise command-line compilation examples / instructions:

> javac MyWebServer.java

4. Precise examples / instructions to run this program:

> java MyWebServer

It then listens to port 2540. I will only serve files out in its current
working directory.

5. List of files needed for running the program.

MyWebServer.java
any other txt or html files you would like to serve
favicon.ico

5. Notes:

It does not handle unknown locations or files other than txt and html.
I tried to get a 404 page working, and it does kind of, but not well.

----------------------------------------------------------*/



import java.io.*;
import java.net.*;
//import java.util.*;

class Server extends Thread {
	Socket sock;

	Server (Socket s)	{sock = s;}

	public void run(){
		DataOutputStream out = null; //out is the DataOutputStream sending bytes to the browser/requester/client
		BufferedReader in = null;//in is the reader of the input from wherever its coming from. Assumed browser
		String cWDir = System.getProperty("user.dir");//gets the current working directory of the server
		boolean tempCreated = false;//used to delete temp files at end
		String path;//will be gathered from requested file to help build header
		File requested;//the file thats gonna get served up

		try {
			in = new BufferedReader(new InputStreamReader (sock.getInputStream())); //getting the stream info to get stuff from browser
			out = new DataOutputStream (sock.getOutputStream());//getting the stream info to send stuff to the browser

			String fromBrowser = in.readLine();//reads the GET/POST/HEADER request, but will only handle GET
			System.out.println("Request read from browser " + fromBrowser);
			String delims = "[ ]+";									//These lines are how i parsed out the request
			String[] statment = fromBrowser.split(delims);			//maybe not the best way, or cleanest

			if (statment[1].contains("%20")) {statment[1] = statment[1].replace("%20", " ");} //sometimes files have names with spaces, this cleans that up

			path = cWDir +  statment[1];					//gets path for generic file
			requested = new File(cWDir +  statment[1]);		//and grabs file of generic file

			if (statment[1].contains(".fake-cgi")) {		//if it has fake-cgi its going to do addnums and get everythin ready for later
				File cgiRequest = new File(cWDir);
				requested = addnums(statment[1], cgiRequest);
				path = requested.getPath();
            	tempCreated = true;}

            if (requested.isDirectory ()) { 				//if its a directory it creates a temp html page of the directory listing
            	requested = createTempDir(requested, statment[1]);
            	path = requested.getPath();
            	tempCreated = true;}

             out.writeBytes(BuildHeader(path, statment[0])); //like it says below, it builds the header and sends it
             System.out.println("Sending header");
             System.out.println("Serving up " + requested +"\n");
             FileInputStream pageToServe = new FileInputStream(requested); //getting ready to read the file requested (or temp)

             byte [] buffer = new byte[1024]; //this peice reads the file 1024 bytes at a time
             while (true) {
            	 int b = pageToServe.read(buffer, 0,1024);
            	 if (b == -1) {break;}
            	 out.write(buffer,0,b); //sends the 1024 bytes at a time off to the browser
            	}
             pageToServe.close(); //closes the FileInputStream
             if (tempCreated){requested.delete();} //if a temp file was created, delete it
             } catch (IOException x) {
            	 try {notFound(cWDir, out);	//my attempt at filenotfound handling. doesnt work really, but doesnt screw up the whole thing either
				} catch (IOException e) {e.printStackTrace();}
            	 }
	}
	private File createTempDir(File dir, String parent) throws IOException{
		File temp = File.createTempFile("tmpDir", ".html", dir);
		FileWriter out = new FileWriter(temp);
        File[] strFilesDirs = dir.listFiles();//array of files in directory
        String fileName; //used to hold names of each file/directory in current directoy
        //String path = dir.getParent(); //parent directory path
        String fileNameDira;
        try {		//builds a temp html file of the requested directory
        	String dirName = dir.getName();  //gets name of the current directory
        	out.write("<html><body>");
        	out.write("<h1>"+ dirName+ "</h1><hr>");
        	out.write("<pre>");
        	for ( int i = 0 ; i < strFilesDirs.length ; i ++ ) {
        		fileName = strFilesDirs[i].getName();

        		if (!parent.equals("/")){fileNameDira = parent + "/" + fileName;}
        		else {fileNameDira = fileName;}
        		if (!strFilesDirs[i].getName().startsWith(".")){//if it starts with a dot it doesnt show up
        			if ( strFilesDirs[i].isDirectory ( ) ){
        				out.write ( "Directory:	<a href='" + fileNameDira +
        						"'>" + fileName + "</a>\n" ) ;}
        			else if (strFilesDirs[i].isFile() &&
        					!(strFilesDirs[i].getName().startsWith("tmpDir"))){//also doesnt show temp files that were created
        				out.write ( "File:		<a href='" + fileNameDira +
        						"'>" + fileName + "</a>"	+	"	(" + strFilesDirs[i].length() + ")\n" ) ;}
            }}
        	out.write("</pre><hr>");
        	out.write("</body></html>");
        out.close(); //closes the file writing stuff

        } catch (IOException e) {e.printStackTrace();}
		return temp; //returns the temp html file
	}


	private String BuildHeader(String requested, String requestDo){
		int toDo = 0;
		long fileLength = 0;
		String header;
		String responseCode;

		if (requestDo.equals("GET")){toDo = 1;}       //I was going to, and still might, get this working for different types of requests
		else if (requestDo.equals("POST")){toDo = 2;} //It doesnt really do anything with POST right now

		if (toDo == 0) {responseCode = "501 Not Implemented";}	//I was going to have it do all these awesome error returns and stuff
		else {responseCode = "200 OK";} //sometimes that 501 does end up in the header, but from a user standpoint its useless

		String fileType;

		if (requested.endsWith(".txt")) {fileType = "plain";}
		else if (requested.endsWith(".html") || requested.endsWith(".htm")) {fileType = "html";}
		else {fileType = "plain";
				responseCode = "415 Unsupported Media Type";}
		if (!requested.equals("HTTP")){
			File file = new File(requested);
			if(file.exists()){fileLength = file.length();}
		}
		else fileLength = 0;
		header = "HTTP/1.1 " + responseCode + "\r\n";
		header = header + "Content-Length: " + fileLength + " \r\n";
		header = header + "Content-Type: text/" + fileType + "\r\n";
		header = header + "Connection: close";
		header = header + "\r\n\r\n";

		return header;
	}

	private File addnums(String domath, File dir) throws IOException{
		dir = File.createTempFile("tmpAddNum", ".html", dir);
		FileWriter out = new FileWriter(dir);
		String delims = "[?=&]+"; //used to parse out the string send from the cgi stuff
		String[] input = domath.split(delims);
		String name = input[2]; //it only really works for a very very specific request. where Name is and nums are are key to this working.
		int num1 = Integer.parseInt(input[4]);
		int num2 = Integer.parseInt(input[6]);
		int num3 = num1 + num2;
		out.write("<html><body>");
    	out.write("<h1> Hello, " + name + "!</h1>");
    	out.write("<pre>");
		out.write("you sent me " + num1 + " " + num2 + "\n");
		out.write("That adds up to <b>" + num3 + "<b>!\n");
    	out.write("</pre><hr>");
    	out.write("</body></html>");
    	out.close();
    	return (dir); //returns the temp file created
	}
	private void notFound(String cWDIR, DataOutputStream toBrowser) throws IOException{ //I tried to make a 404 handler. it will work for
		File saveTo = new File (cWDIR);													//localhost:2540/..... but not very well after that
		File dir = File.createTempFile("404error", ".html", saveTo);
		FileWriter out = new FileWriter(dir);
		out.write("<html><body>");
    	out.write("<h1> Hello, This is an error 404</h1><hr>");
    	out.write("<pre>");
		out.write("you tried to go to a page that doesnt exisit\n");
		out.write("or maybe I messed something up.\n");
		out.write("either way, oops.\n");
    	out.write("</pre><hr>");
    	out.write("</body></html>");
    	out.close();
    	toBrowser.writeBytes(BuildHeader(dir.getPath(), "GET"));

        FileInputStream pageToServe = new FileInputStream(dir);

        byte [] buffer = new byte[1024];
        while (true) {
       	 int b = pageToServe.read(buffer, 0,1024);
       	 if (b == -1) {break;}
       	 toBrowser.write(buffer,0,b);
       	}

        pageToServe.close();
        dir.delete();}

}


public class MyWebServer {

		public static boolean shouldRun = true;

		public static void main(String a[]) throws IOException {

			int q_len = 6;
			int port = 2540;
			Socket sock;

			ServerSocket servsock = new ServerSocket (port, q_len);
			System.out.println("Garrett's WebServer is listening at port " + port + "\n");

			while (shouldRun) {
				sock = servsock.accept ();
				if (shouldRun) {new Server (sock).start();}
			}
		}
}






