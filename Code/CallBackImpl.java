//package split;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CallBackImpl extends UnicastRemoteObject implements CallBack {
	int nbnode;
	
	public CallBackImpl(int n) throws RemoteException { nbnode = n; }
	
	public synchronized void completed() throws RemoteException {
		notify();
	}
	
	public synchronized void waitforall() throws RemoteException {
			for (int i=0; i<nbnode; i++) 			
			{	
				try {
					wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}
}