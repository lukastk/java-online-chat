import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.lang.Runnable;
import java.lang.Thread;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.Math;
import java.util.List;
import java.util.Collections;

public class ServerApp implements Runnable
{
	public static List<String> waitList = Collections.synchronizedList(new ArrayList<String>());
	public static ArrayList<String> chatMessages = new ArrayList<String>();
	private ServerConnection serverConnection;
	private boolean blinker = true;
	

    public static void main(String[] args)
	{
		ServerApp serverApp = new ServerApp();
    }
	
	private ServerApp()
	{
		serverConnection = new ServerConnection();
		Thread t = new Thread(this);
        t.start();
		serverConnection.run();
	}
	
	public void run()
	{
		while (blinker)
		{
		}
		
		finalize();
	}
	
	public void close()
	{
		blinker = false;
	}
	
	public void finalize()
	{
		waitList = null;
		chatMessages = null;
		serverConnection.close();
	}
}

class ServerConnection
{
    private ServerSocket server;
    private int port = 7777;
	public static List<ClientConnectionHandler> clients = Collections.synchronizedList(new ArrayList<ClientConnectionHandler>());
	private boolean blinker = true;
	
    public ServerConnection()
	{
        try
		{
            server = new ServerSocket(port);
        }
		catch (IOException e)
		{
            e.printStackTrace();
        }
    }

    public void run()
	{
        while (blinker)
		{
			// Check for new client connections, and then add them.
            try
			{
                Socket socket = server.accept();
                ClientConnectionHandler ch = new ClientConnectionHandler(socket);
				clients.add(ch);
            }
			catch (IOException e)
			{
                e.printStackTrace();
            }
        }
		
		finalize();
    }
	
	public static void passMessage(String message)
	{
		//FORMAT = chat:handle:message
		String[] splitMessage = message.split(":");
		if (splitMessage.length == 2)
			message = splitMessage[1] + "-";
		else if (splitMessage.length == 3)
			message = splitMessage[1] + "-" + splitMessage[2];
		
		//FORMAT = Host-chat:handle-message
		for (ClientConnectionHandler cch : clients)
		{
			cch.sendMessage("chat:" + message);
		}
	}
	
	public static void addToWaitList(String handle)
	{
		//Check if user is already on the waitlist.
		String username = handle.split(">")[1];
		boolean exists = false;
		for (int i = 0; i < ServerApp.waitList.size(); i++)
		{
			String u = ServerApp.waitList.get(i).split(">")[1];
			if (username.equals(u))
				exists = true;
		}
		
		if (exists)
			return;
		
		ServerApp.waitList.add(handle);
	}
	
	public static void removeFromWaitList(String handle)
	{
		for (int i = 0; i < ServerApp.waitList.size(); i++)
		{
			if (ServerApp.waitList.get(i).equals(handle))
				ServerApp.waitList.remove(i);
		}
	}
	
	public static void passWaitList()
	{
		//Check if any users have disconnected.
		for (int i = 0; i < ServerApp.waitList.size(); i++)
		{
			boolean exists = false;
		
			for (ClientConnectionHandler cch : clients)
			{
				String username = ServerApp.waitList.get(i).split(">")[1];
				if (username.equals(cch.username))
					exists = true;
			}
			
			if (!exists)
				ServerApp.waitList.remove(i);
		}
	
		String message = "";
		if (ServerApp.waitList.size() != 0)
		{
			for (String str : ServerApp.waitList)
				message += "-" + str;
		}
		
		for (ClientConnectionHandler cch : clients)
		{
			if (cch.clientIsLoggedIn)
				cch.sendMessage("waitList:" + message);
		}
	}
	
	public static boolean checkDuplicateLogin(ClientConnectionHandler c, String username, String handle)
	{
		for (ClientConnectionHandler cch : clients)
		{
			if (handle.equals(cch.handle))
			{
				c.sendMessage("handle:occupied");
				return true;
			}
			else if (username.equals(cch.username))
			{
				c.sendMessage("login:occupied");
				return true;
			}
		}
		
		return false;
	}
	
	public void close()
	{
		blinker = false;
	}
	
	public void finalize()
	{
		try
		{
			server.close();
			for (ClientConnectionHandler cch : clients)
				cch.close();
			clients = null;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}

class ClientConnectionHandler implements Runnable
{
	public String username;
	public String handle;
    private Socket socket;
	private boolean blinker = true;
	public boolean clientIsLoggedIn = false;
	public ObjectOutputStream oos;
	private ObjectInputStream ois;

    public ClientConnectionHandler(Socket socket)
	{
        this.socket = socket;
		
		try 
		{
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
        Thread t = new Thread(this);
        t.start();
		System.out.println("New Connection: " + socket.toString());
    }
	
    public void run()
	{
		while (blinker)
		{
			try
			{
				// Read a message sent by client application
				String message = (String) ois.readObject();
				if (message.equals("waitList"))
				{
					sendWaitList();
				}
				else if (message.startsWith("addToWaitList:"))
				{
					String[] splitMessage = message.split(":");
					String userHandle;
					if (splitMessage.length == 2)
					{
						userHandle = splitMessage[1];
					
						ServerConnection.addToWaitList(userHandle + ">" + username);
						ServerConnection.passWaitList();
					}
				}
				else if (message.startsWith("removeFromWaitList"))
				{
					String[] splitMessage = message.split(":");
					String userHandle;
					if (splitMessage.length == 2)
					{
						userHandle = splitMessage[1];
						ServerConnection.removeFromWaitList(userHandle + ">" + username);
					}
					
					ServerConnection.passWaitList();
				}
				else if (message.startsWith("chat:"))
				{
					//String[] splitMessage = message.split(":");
					//if (splitMessage.length != 2) //Basically means if the text in chat:handle:text is not an empty string.
					//	message = splitMessage[1] + ":" + splitMessage[2];
					//else
					//	message = splitMessage[1] + ":";
					
					ServerConnection.passMessage(message);
				}
				else if (message.startsWith("login:"))
				{
					String[] splitMessage = message.split(":");
					checkUserInfo(splitMessage[2], splitMessage[3], splitMessage[1]);
					
					handle = splitMessage[1];
					username = splitMessage[2];
					String password = splitMessage[3];
					
					if (clientIsLoggedIn)
						ServerConnection.passWaitList();
					else
					{
						handle = "";
						username = "";
					}
				}
			}
			catch (IOException e)
			{
				close();
			}
			catch (ClassNotFoundException e)
			{
				close();
				e.printStackTrace();
			}
		}
		
		finalize();
    }
	
	public void sendWaitList()
	{
		// Take ServerApp.waitList and put it into a string form.
		String waitList = "";
		if (ServerApp.waitList.size() != 0)
			waitList += ServerApp.waitList.get(0);
		for (int i = 1; i < ServerApp.waitList.size(); i++)
			waitList += ":" + ServerApp.waitList.get(i);
			
		sendMessage(waitList);
	}
	
	public void sendMessage(String message)
	{
		try
		{
			oos.writeObject("Host-" + message);
		}
		catch (IOException e)
		{
            //e.printStackTrace();
        }
	}
	
	public void close()
	{
		if (blinker == false)
			return;
		
		ServerConnection.passWaitList();
		System.out.println("Disconnected: " + socket.toString());
		blinker = false;
	}
	
	public void finalize()
	{
		try
		{
			oos.close();
			ois.close();
			socket.close();
		}
		catch (IOException e)
		{
            //e.printStackTrace();
        }
		
		ServerConnection.clients.remove(this);
	}
	
	public void checkUserInfo(String username, String password, String handle)
	{
		File userInfo = null;
		
		if (ServerConnection.checkDuplicateLogin(this, username, handle))
			return;
		
		if (username.length() != 0 | password.length() != 0)
		{
			try
			{
				//TODO: check if directory is safe
				userInfo = new File("userInfo.dat");
				BufferedReader reader = new BufferedReader(new FileReader(userInfo));
				
				String line;
				while ( (line = reader.readLine()) != null )
				{
					String[] infos = line.split(":");
					for (int i = 0; i < infos.length; i += 2)
					{
						if (infos[i].equals(Encryption.encryptString(username)) & infos[i + 1].equals(Encryption.encryptString(password)))
						{
							clientIsLoggedIn = true;
							System.out.println("User " + username + "(" + handle + ") logged in from: " + socket.toString());
						}
					}
				}
				
				reader.close();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				//e.printStackTrace();
			}
		}
		
		if (clientIsLoggedIn)
			sendMessage("login:true");
		else
			sendMessage("login:false");
	}
}

class Encryption
{
	public static String encryptString(String str)
	{
		if (str == "")
			return null;
	
		byte[] bytes = str.getBytes();
		byte[] randomBytes = new byte[bytes.length];
		
		int byteSum = 0;
		for (byte b : bytes)
			byteSum += b;
			
		Random rand = new Random(byteSum);
		rand.nextBytes(randomBytes);
		
		
		for (int i = 0; i < 1; i++)
		{
			bytes = stage1(bytes, randomBytes);
			bytes = stage2(bytes);
		}
		
		return new String(bytes);
	}
	
	static byte addToByte(byte b, int add)
	{
		int bInt = (int)b;
		
		if ( (bInt + add) > Byte.MAX_VALUE )
		{
			bInt = Byte.MIN_VALUE + bInt + add - Byte.MAX_VALUE;
		}
		else if ( (bInt + add) < Byte.MIN_VALUE)
		{
			bInt = Byte.MAX_VALUE + bInt + add + Byte.MAX_VALUE;
		}
		else if ( (bInt + add) == ":".getBytes()[0])
		{
			addToByte(b, add + (add / Math.abs(add)) );
		}
		else
		{
			bInt += add;
		}
		
		return (byte)bInt;
	}
	
	static byte[] stage1(byte[] bytes, byte[] randomBytes)
	{
		//If the byte to the right is even add 3, if not subtract 3.
		for (int i = 0; i < bytes.length; i++)
		{
			if (getNextIndice(randomBytes, i + 1) % 2 == 0)
			{
				bytes[i] = addToByte(bytes[i], randomBytes[i]);
			}
			else
			{
				bytes[i] = addToByte(bytes[i], -randomBytes[i]);
			}
		}
		
		return bytes;
	}
	
	static byte[] stage2(byte[] bytes)
	{
		//Switch around the bytes s times. Where s is the sum of the bytes.
		int s = 0;
		
		for (Byte b : bytes)
			s += b;
			
		for (int i = 0; i < s; i++)
		{
			bytes = switchByte(bytes);
		}
		
		return bytes;
	}
	
	static byte[] switchByte(byte[] bytes)
	{
		byte b1 = bytes[0];
		byte b2 = bytes[bytes.length - 1];
		bytes[0] = b2;
		bytes[bytes.length - 1] = b1;
		return bytes;
	}
	
	static byte getNextIndice(byte[] bytes, int index)
	{
		if (index >= bytes.length )
			return bytes[bytes.length - index];
			
		return bytes[index];
	}
}