//package split;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

public class Launch implements Runnable {	
	public static int dae_ports[] = {9000, 9001};
	private  static volatile boolean thread_end_run = false;
	public static volatile String final_res[] = new String[2];
	public static String launch_final_result = "launch_final_result.txt";
	public static String reduce_result = "final_reduce_result.txt";
	
	
	public static void main(String args[]) {
		if(args.length != 2 ) {
			System.out.println("--> In receive_result(): Lanch start listening on port " + args[0]);
			Launch runnable;			
				try {
					runnable = new Launch();
					Thread t1 = new Thread(runnable);	
					t1.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			int launch_port = Integer.parseInt(args[0]);			
			WordCount m = new WordCount();
			
			try {	
				CallBack cb = new CallBackImpl(2);		
				for(int i = 0; i<2; i++) {
				
					// get the stub of the server object from the rmiregistry
					Daemon obj = (Daemon) Naming.lookup("//localhost:" + Integer.toString(dae_ports[i]) + "/my_server");				
					String blockin = "block" + Integer.toString(i) + ".txt" ;
					String blockout = "result" + Integer.toString(i) + ".txt" ;
							
					obj.call(m, blockin, blockout, cb);
					System.out.println("--> In Main: RMI call() to Daemon" + i);
					
					//rmi call daemon to check if can start transferring result back
					String hostname = "localhost";
					obj.rmi_trigger_download(hostname, launch_port, blockout);
					}
				//cb.waitforall();
				//System.out.println("--> In Main: Daemons returned result file via TCP socket");
				} 
			catch (Exception exc) { 
				System.out.println(exc);
				}				
			
			boolean loop_flag = true;
			while(loop_flag) {				
				if(thread_end_run) {	
					loop_flag = false;
					//merging results files
					join_files(final_res);
					
					//executing reduce
					System.out.println("--> In Main: Executing reduce");
					Collection<String> blocks = new ArrayList<String>();
					blocks.add(launch_final_result);
					m.executeReduce(blocks, reduce_result);
					System.out.println("--> In Main: Done reducing");
					/*
					System.out.println("--> Exit Doemons");
					for(int i = 0; i<2; i++) {
						// get the stub of the server object from the rmiregistry
						try {
							Daemon obj = (Daemon) Naming.lookup("//localhost:" + Integer.toString(dae_ports[i]) + "/my_server");		
							obj.rmi_daemon_die();
						} catch (MalformedURLException e) {
							//e.printStackTrace();
							System.out.println("");
						} catch (RemoteException e) {
							//e.printStackTrace();
							System.out.println("");
						} catch (NotBoundException e) {
							//e.printStackTrace();
							System.out.println("");
						}
					}
					*/
					System.out.println("--> Launch terminates");
					System.exit(0);
					
				}
				else {
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
		}
		else {
			System.out.println("--> In Main: Launch require a port number to run");
		}		
		
	}
	

	@Override
	public void run() {
		try {
		    ServerSocket serveur = new ServerSocket(9002);
		    
		    boolean listen = true;
		    int client_counter = 0;
		    
		    
			while(listen) {				
				Socket s = serveur.accept();
				InputStream is = s.getInputStream();			
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				
				//create file writer
				String res_fname = "launch_" + br.readLine();
				try 
					{
					      FileWriter fileWriter = new FileWriter(res_fname);		
					      //read from socket and write to block #i
						String line;			
						while( (line = br.readLine() ) != null) { 
							//System.out.println(line); /// to be remove
							fileWriter.write(line + "\n");
							}
						fileWriter.close();
					} 
				catch (IOException e) {
				      e.printStackTrace();
					}
				s.close();

				client_counter = client_counter + 1;
				final_res[client_counter - 1] = res_fname;
				if(client_counter == 2 ) {
					listen = false;
					System.out.println("--> In Thread: Daemons returned result file via TCP socket");
				}															
			}				
			
			//return to main thread to do reducing
			thread_end_run = true;
			serveur.close();
			}
		catch (IOException e) {
				System.err.println(e);
		}
		
		System.out.println("--> In Thread: Terminate Doemons");
		for(int i = 0; i<2; i++) {
			// get the stub of the server object from the rmiregistry
			try {
				Daemon obj = (Daemon) Naming.lookup("//localhost:" + Integer.toString(dae_ports[i]) + "/my_server");		
				obj.rmi_daemon_die();
			} catch (MalformedURLException e) {
				//e.printStackTrace();
				//System.out.println("");
			} catch (RemoteException e) {
				//e.printStackTrace();
				//System.out.println("");
			} catch (NotBoundException e) {
				//e.printStackTrace();
				//System.out.println("");
			}
		}	
	}	/// close bracket run()	
	

	public static void join_files(String final_res[]) {
		System.out.println("--> In join_file: Start merging result files");
		String path = System.getProperty("user.dir");
		
        try {
			PrintWriter pw = new PrintWriter(launch_final_result );  
			
			for(int i = 0; i < final_res.length ; i++) {
				BufferedReader br = new BufferedReader(new FileReader(path + "/" + final_res[i])); 
			    String line = br.readLine(); 
			    while (line != null) 
			    {       pw.println(line); 
			            line = br.readLine();                     
			    } 
			    br.close(); 
			}              
			pw.flush();          
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
          
        System.out.println("--> In join_file(): Done merging result files");
		
	}
	
} // class close brace


