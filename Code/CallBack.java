//package split;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CallBack extends Remote {
	public void completed() throws RemoteException;
	public void waitforall() throws RemoteException;
}