//package split;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Split {
	public static void main(String[] args) throws IOException {		
		if(args.length != 2) {
			//open file in current directory 
			String path = System.getProperty("user.dir");
			String file_name = "hello.txt";
	
			int nb_daemons = 2;
			int block_size;
			// declaration for socket to daemons
			String hosts[] = {"localhost", "localhost" } ;
			int ports[] = {9000, 9001};
			
			//// Get file name from arg			
	        if (args.length == 1) 
	        { 
	        	file_name = args[0];
	        	System.out.println("Splitting " + args[0] );
	        } 
	        else {
	        	System.out.println("--> There is no or more than one argument.\nSplitting the hello.txt file in current directoty.");
	        }
	        String fname = path + "/" + file_name;
			File f = new File(fname);
			
			//checking file size, block size
			if(f.exists()){
				int fsize = (int)f.length() + 1;
				block_size = fsize / nb_daemons + 1 ; 
				System.out.println("File size to split: " + fsize);
				System.out.println("Block_size = file size / No of block: " + block_size);
				//f.close();
				
				//code to read content in file and split
				FileInputStream fs = new FileInputStream(fname);
				BufferedReader BR = new BufferedReader(new InputStreamReader(fs));
				String line ="\n";
				int sent_size = 0;
				
				//Testing sending via socket
				for(int i=0; i< nb_daemons;i++) {
			        try (Socket socket = new Socket(hosts[i], ports[i])) {	   
			            OutputStream cos = socket.getOutputStream();
			            PrintWriter writer = new PrintWriter(cos, true);	        		         
			            
			            if(i == 0) {
							while((line=BR.readLine()) != null && (sent_size += (int)line.length()) <= block_size) {
								//System.out.println("daemon: "+ Integer.toString(i) + " Sent_size: " + Integer.toString(sent_size) + " " + line );
								writer.println(line);
							}							
							socket.close();							
	            		}
	            		else {
	            			//Make sure the current line move to next block when current block full
	            			writer.println(line);
	            			System.out.println("*******************************\n" + "Completed sending block " + Integer.toString(i-1) + "\n*******************************\n");							
							sent_size = 0; 
							sent_size += line.length();
							//System.out.println("daemon: "+ i  + " Sent_size: " + Integer.toString(sent_size) + " " +line );
													
							while((line=BR.readLine()) != null && (sent_size += (int)line.length()) <= block_size) {
								//System.out.println("daemon: "+ Integer.toString(i) + " Sent_size: " + Integer.toString(sent_size) + " " + line );
								writer.println(line);
							}	
							socket.close();
							System.out.println("*******************************\n" + "Completed sending block " + Integer.toString(i) + "\n*******************************\n");
	            		}							
		 
			        } 
			        catch (UnknownHostException ex) {	System.out.println("Server not found: " + ex.getMessage());		}	 
				    catch (IOException ex) { System.out.println("I/O error: " + ex.getMessage());   }  
			        
				}
				BR.close();
			}
			else{  //else of check if file existed
				System.out.println("File does not exists!");
				}		
		}
		else {
			System.out.println("Require file name and Input only one argument as file name(*)");
		}
	}
	
}
