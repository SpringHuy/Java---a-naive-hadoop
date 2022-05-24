//package split;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;


public class DaemonImpl extends UnicastRemoteObject implements Daemon, Runnable {
	//properties for threads
	private  WordCount _m ;//= new WordCount();
	private  String _blockin, _blockout;
	private  volatile CallBack _cb;
	private  static volatile boolean end_run = false;
	
	
	// default constructor - no need to define
	public DaemonImpl() throws java.rmi.RemoteException  {	}
	
	//constructor with params
	public DaemonImpl(WordCount m, String blockin, String blockout, CallBack cb) throws RemoteException  {	
		_m = m;
		_blockin = blockin;
		_blockout = blockout;
		_cb = cb;
	}
		
	
	//main func
	public static void main (String[] args) {	
		int port = Integer.parseInt(args[0]);
		System.out.println("Daemon " + Integer.toString(port % 9000) + " listen on port: " + args[0] + "\n");
		
		//read from split and write data to block files in same directory
		System.out.println("************************************************\n" + "Start receiving text block from Split" + "\n************************************************\n");
		read_split(port);		

		//rmi invoke call -> wordcount
		rmi_handle(port);		
		System.out.println("--> In Main: waiting for Launch to rmi invoke call()" );
		
		
	}  // close bracket - main
	

	
	//@Override
	public void call(WordCount m, String blockin, String blockout, CallBack cb) {
		DaemonImpl runnable;
		try {
			runnable = new DaemonImpl(m, blockin, blockout, cb);
			Thread t1 = new Thread(runnable);	
			/*try {
				t1.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			t1.start();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}  // call close bracket
	
	public void rmi_trigger_download(String hostname, int launch_port , String blockout) throws RemoteException {
		boolean loop_flag = true;
		while(loop_flag) {
			if(end_run) {
				loop_flag = false;
				System.out.println("--> In rmi_trigger_download() : start transfering result");
				trans_result(hostname, launch_port, blockout);
				
			}
			else {
				System.out.println("--> In rmi_trigger_download: still running in thread -- wait");
				   String abc = "abc";
				   synchronized(abc) {					 
						try {
							abc.wait(1000);
						} catch (Exception e) {							
							e.printStackTrace();
						}					
				   }
			}
			
		} 
	}   	//close bracket rmi_trigger_download
	
	public void rmi_daemon_die() throws RemoteException{
		System.out.println("In rmi_daemon_die: Daemon terminates");
		System.exit(0);
	}
	
	public void trans_result(String hostname, int launch_port , String blockout) {
		System.out.println("--> In trans_result: Start transfering result file back to Launch");		
		
		//open result file
		String path = System.getProperty("user.dir");
        String fname = path + "/" + blockout;

		try {
			//code to read content in file and split
			FileInputStream fs = new FileInputStream(fname);
			BufferedReader BR = new BufferedReader(new InputStreamReader(fs));
			String line = blockout;
					
			try {
				//socket client to send 	   		
				Socket socket = new Socket(hostname, launch_port);
				OutputStream cos = socket.getOutputStream();
				PrintWriter writer = new PrintWriter(cos, true);	        		         
				System.out.println("*******************************\n" + "Start sending  " + blockout + "\n*******************************\n");	
				
				//sending first line as file name - Launch use this to know result file order
				writer.println(line);
				
				//sending content of result file via TCP socket
				while((line=BR.readLine()) != null ) {				
					writer.println(line);
				}	
				writer.close();
				socket.close();
				System.out.println("*******************************\n" + "Completed sending  " + blockout  + "\n*******************************\n");
			} catch (UnknownHostException e) {	
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	} // close bracket of trans_result
	
	
	// run for the thread
	public void run() {
			//System.out.println("Hello -- this is inside thread t1");
			_m.executeMap(_blockin, _blockout);
			System.out.println("--> In Thread: Thread executing map");
			/*try {
				_cb.completed();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			end_run = true;
			System.out.println("--> In Thread: End of thread -- " + "End_run : " + Boolean.toString(end_run));
			//notify();
	}
	
	//get text from split
	public static void read_split( int port) {
		try {			
			ServerSocket serveur = new ServerSocket(port);
			Socket s = serveur.accept();
			InputStream is = s.getInputStream();			
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			
			//create file writer
			int extension = port % 9000;
			String block_name = "block" + Integer.toString(extension) + ".txt";
			try 
				{
				      FileWriter fileWriter = new FileWriter(block_name);		
				      //read from socket and write to block #i
					String line;			
					while( (line = br.readLine() ) != null) { 
						//System.out.println(line); /// to be remove
						fileWriter.write(line + "\n");
						}
					br.close(); ///be careful
					fileWriter.flush();
					fileWriter.close();
				} 
			catch (IOException e) {
			      e.printStackTrace();
				}
			serveur.close();
			System.out.println("************************************************\n" + "Completed receiving text block from Split" + "\n************************************************\n");			
			} 
		catch (IOException e) {
				System.err.println(e);
			}
	} 	//split close brace

	public static void rmi_handle(int port) {
		try {
			// Launching the naming service – rmiregistry – within the JVM
			Registry registry = LocateRegistry.createRegistry(port);
			
			// Create an instance of the server object
			Daemon obj = new DaemonImpl();
			
			// compute the URL of the server
			String URL = "//localhost:" + port +"/my_server";
			System.out.println("URL: " + URL);
			Naming.rebind(URL, obj);
		}
		catch (Exception exc) { 
			exc.printStackTrace();
		}
	}
	
	
}		//class close brace

	


	
	