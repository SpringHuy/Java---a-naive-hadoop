//package split;
//package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Daemon extends Remote { 
	//WordCount m,
	public void call(WordCount m, String blockin, String blockout, CallBack cb) throws RemoteException;  ///??? Question about callback
	public void rmi_trigger_download(String hostname, int launch_port , String blockout) throws RemoteException;
	public void rmi_daemon_die() throws RemoteException;
}