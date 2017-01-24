package network;
// 30/03
import org.apache.log4j.Logger;

import database.DBSet;
import network.message.Message;
import network.message.MessageFactory;
import settings.Settings;

public class Pinger extends Thread
{
	
	private static final Logger LOGGER = Logger.getLogger(Pinger.class);
	private Peer peer;
	//private boolean run;
	private long ping;
	
	public Pinger(Peer peer)
	{
		this.peer = peer;
		//this.run = true;
		this.ping = Long.MAX_VALUE;
		
		this.start();
	}
	
	public long getPing()
	{
		return this.ping;
	}
	/*
	public boolean isRun()
	{
		return this.run;
	}
	*/
	
	public void run()
	{
	
		while(true)
		{
			
			if(!this.peer.isUsed()) {
				try {
					Thread.sleep(100);
				}
				catch (Exception e) {		
				}
				continue;
			}
			
			//CREATE PING
			Message pingMessage = MessageFactory.getInstance().createPingMessage();
						
			//GET RESPONSE
			long start = System.currentTimeMillis();
			Message response = this.peer.getResponse(pingMessage);

			//CHECK IF VALID PING
			if(response == null || response.getType() != Message.PING_TYPE)
			{
				//PING FAILES
				this.peer.onPingFail(response == null?"response == null": "response.getType() != Message.PING_TYPE" );
				try {
					Thread.sleep(100);
				}
				catch (Exception e) {		
				}
				continue;
			}

			try
			{

				//UPDATE PING
				this.ping = System.currentTimeMillis() - start;
				this.peer.addPingCounter();
								
				if(!DBSet.getInstance().isStoped()){
						DBSet.getInstance().getPeerMap().addPeer(this.peer, 0);
				}
			}
			catch(Exception e)
			{
				//PING FAILES
				this.peer.onPingFail(e.getMessage());
				try {
					Thread.sleep(100);
				}
				catch (Exception e1) {		
				}
				continue;
				
			}
			
			//SLEEP
			try 
			{
				Thread.sleep(Settings.getInstance().getPingInterval());
			} 
			catch (InterruptedException e)
			{
				//FAILED TO SLEEP
			}
		}
	}

	/*
	public void stopPing() 
	{
		try
		{
			this.run = false;
			this.goInterrupt();
			this.join();
		}
		catch(Exception e)
		{
			LOGGER.debug(e.getMessage(), e);
		}
		
		try {
			this.wait();
		} catch(Exception e) {
			
		}
	}
	 */
	
	// icreator - wair is DB is busy
	// https://github.com/jankotek/mapdb/search?q=ClosedByInterruptException&type=Issues&utf8=%E2%9C%93
	//
	public void goInterrupt_old()
	{

		DBSet dbSet = DBSet.getInstance(); 
		//int i =0;
		while(dbSet.getBlockMap().isProcessing() || dbSet.isBusy() ) {
			try {
				LOGGER.info(" pinger.goInterrupt wait DB : " + this.peer.getAddress());
				Thread.sleep(50);
			}
			catch (Exception e) {		
			}
			/*
			i++;
			if (i > 20) 
				break;
				*/

		}
		this.interrupt();
	}
}
