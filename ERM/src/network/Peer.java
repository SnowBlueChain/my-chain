package network;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
 import org.apache.log4j.Logger;

import controller.Controller;
import database.DBSet;
import lang.Lang;
import network.message.Message;
import network.message.MessageFactory;
import ntp.NTP;
import settings.Settings;

public class Peer extends Thread{

	private InetAddress address;
	private ConnectionCallback callback;
	private Socket socket;
	private OutputStream out;
	private Pinger pinger;
	private boolean white;
	private long pingCounter;
	private long connectionTime;
	
	private Map<Integer, BlockingQueue<Message>> messages;
	
	static Logger LOGGER = Logger.getLogger(Peer.class.getName());

	public Peer(InetAddress address)
	{
		this.address = address;
		this.messages = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Message>>());
	}
	
	public Peer(ConnectionCallback callback, Socket socket)
	{
		try
		{	
			this.callback = callback;
			this.socket = socket;
			this.address = socket.getInetAddress();
			this.messages = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Message>>());
			this.white = false;
			this.pingCounter = 0;
			this.connectionTime = NTP.getTime();
			
			//ENABLE KEEPALIVE
			//this.socket.setKeepAlive(true);
			
			//TIMEOUT
			this.socket.setSoTimeout(1000*60*60);
			
			//CREATE STRINGWRITER
			this.out = socket.getOutputStream();
			
			//START COMMUNICATON THREAD
			this.start();
			
			//START PINGER
			this.pinger = new Pinger(this);
			
			//ON SOCKET CONNECT
			this.callback.onConnect(this);			
		}
		catch(Exception e)
		{
			//FAILED TO CONNECT NO NEED TO BLACKLIST
			//LOGGER.info("Failed to connect to : " + address);
		}
	}
	
	public InetAddress getAddress()
	{
		return address;
	}
	
	public long getPingCounter()
	{
		return this.pingCounter;
	}
	
	public void addPingCounter()
	{
		this.pingCounter ++;
	}
	
	public long getPing()
	{
		return this.pinger.getPing();
	}
	
	public boolean isPinger()
	{
		return this.pinger != null;
	}
	
	public void connect(ConnectionCallback callback)
	{
		if(DBSet.getInstance().isStoped()){
			return;
		}
		
		this.callback = callback;
		this.white = true;
		this.pingCounter = 0;
		this.connectionTime = NTP.getTime();
		
		int steep = 0;
		try
		{
			//OPEN SOCKET
			steep++;
			this.socket = new Socket(address, Controller.getInstance().getNetworkPort());
			
			//ENABLE KEEPALIVE
			//steep++;
			//this.socket.setKeepAlive(true);
			
			//TIMEOUT
			steep++;
			this.socket.setSoTimeout(1000*60*60);
			
			//CREATE STRINGWRITER
			steep++;
			this.out = socket.getOutputStream();
			
			//START COMMUNICATON THREAD
			steep++;
			this.start();
			
			//START PINGER
			this.pinger = new Pinger(this);
			if (this.pinger.isInterrupted()) {
				LOGGER.info("peer.connect - Failed to connect to : " + address + " by interrupt!!!");
				this.close();
				return;
			}
			
			//ON SOCKET CONNECT
			steep++;
			this.callback.onConnect(this);			
		}
		catch(Exception e)
		{
			//FAILED TO CONNECT NO NEED TO BLACKLIST
			LOGGER.info(Lang.getInstance().translate("Failed to connect to : ") + address + " on steep: " + steep);
		}
	}
	
	public void run()
	{
		DataInputStream in = null;
		try 
		{
			in = new DataInputStream(socket.getInputStream());
		} 
		catch (Exception e) 
		{
			//LOGGER.error(e.getMessage(), e);
			
			//DISCONNECT
			callback.onDisconnect(this);
			return;
		}

		while(in != null)
		{
			//READ FIRST 4 BYTES
			byte[] messageMagic = new byte[Message.MAGIC_LENGTH];
			try 
			{
				in.readFully(messageMagic);
			} 
			catch (Exception e) 
			{
				//LOGGER.error(e.getMessage(), e);
				
				//DISCONNECT
				callback.onDisconnect(this);
				return;
			}
			
			if(Arrays.equals(messageMagic, Controller.getInstance().getMessageMagic()))
			{
				//PROCESS NEW MESSAGE
				Message message;
				try 
				{
					message = MessageFactory.getInstance().parse(this, in);
				} 
				catch (Exception e) 
				{
					//LOGGER.error(e.getMessage(), e);
					
					//DISCONNECT
					callback.onDisconnect(this);
					return;
				}
				
				//LOGGER.info("received message " + message.getType() + " from " + this.address.toString());
				
				//CHECK IF WE ARE WAITING FOR A MESSAGE WITH THAT ID
				if(message.hasId() && this.messages.containsKey(message.getId()))
				{
					//ADD TO OUR OWN LIST
					this.messages.get(message.getId()).add(message);
				}
				else
				{
					//CALLBACK
					// see in network.Network.onMessage(Message)
					// and then see controller.Controller.onMessage(Message)
					try // ICREATOR
					{
						this.callback.onMessage(message);
					} 
					catch (Exception e) 
					{
						LOGGER.error(e.getMessage(), e);
						//DISCONNECT
						this.onPingFail();
						//callback.onDisconnect(this); // ICREATOR
						return;
					}
				}
			}
			else
			{
				//ERROR
				callback.onError(this, Lang.getInstance().translate("received message with wrong magic"));
				return;
			}
		}
	}
	
	public boolean sendMessage(Message message)
	{
		try 
		{
			//CHECK IF SOCKET IS STILL ALIVE
			if(!this.socket.isConnected())
			{
				//ERROR
				callback.onError(this, Lang.getInstance().translate("socket not still alive"));
				
				return false;
			}
			
			//SEND MESSAGE
			synchronized(this.out)
			{
				this.out.write(message.toBytes());
				this.out.flush();
			}
			
			//RETURN
			return true;
		}
		catch (Exception e) 
		{
			//ERROR
			callback.onError(this, e.getMessage());
			
			//RETURN
			return false;
		}
	}
	
	public Message getResponse(Message message)
	{
		//GENERATE ID
		int id = (int) ((Math.random() * Integer.MAX_VALUE) + 1);
		
		//SET ID
		message.setId(id);
		
		//PUT QUEUE INTO MAP SO WE KNOW WE ARE WAITING FOR A RESPONSE
		BlockingQueue<Message> blockingQueue = new ArrayBlockingQueue<Message>(1);
		this.messages.put(id, blockingQueue);
		
		//WHEN FAILED TO SEND MESSAGE
		if(!this.sendMessage(message))
		{
			return null;
		}
		
		try 
		{
			Message response = blockingQueue.poll(Settings.getInstance().getConnectionTimeout(), TimeUnit.MILLISECONDS);
			this.messages.remove(id);
			
			return response;
		} 
		catch (InterruptedException e)
		{
			this.callback.onDisconnect(this); // icreator
			//NO MESSAGE RECEIVED WITHIN TIME;
			return null;
		}
	}
	
	public void onPingFail()
	{
		//DISCONNECTED
		LOGGER.info("Try callback.onDisconnect : " + this.callback.toString());
		this.callback.onDisconnect(this);
	}

	public boolean isWhite()
	{
		return this.white; 
	}
	
	public long getConnectionTime()
	{
		return this.connectionTime; 
	}	
	
	public boolean isBad()
	{
		return DBSet.getInstance().getPeerMap().isBad(this.getAddress()); 
	}
	
	public void close() 
	{
		
		LOGGER.info("Try close peer : " + address);
		
		try
		{
			//STOP PINGER
			if(this.pinger != null)
			{
				this.pinger.stopPing();
			}
			
			//CHECK IS SOCKET EXISTS
			if(socket != null)
			{
				//CHECK IF SOCKET IS CONNECTED
				if(socket.isConnected())
				{
					//CLOSE SOCKET
					socket.close();
				}
			}
		}
		catch(Exception e)
		{
			
		}		
	}
}
