import java.lang.Thread;
import java.lang.Runnable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.BevelBorder;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

public class ClientApp implements KeyListener, ActionListener
{
	ClientConnection clientConnection;
	
	String username;
	String handle;
	boolean blinker = true;
	
	DefaultListModel<Object> helpListModel;
	
	//GUI
	JFrame frame;
	JList<Object> helpList;
	JLabel helpListLabel;
	JTextArea chatTextArea;
	JScrollPane chatTextScrollArea;
	JLabel chatTextAreaLabel;
	JTextArea typingTextArea;
	
	JButton getWaitListButton;
	JButton addToWaitListButton;
	JButton removeFromWaitListButton;
	JButton quitButton;
	
	JPanel leftSidePanel;
	JPanel rightSidePanel;
	JPanel buttonPanel;
	
	public static void main(String[] args)
	{
		ClientApp ClientApp = new ClientApp();
    }
	
	public ClientApp()
	{
		init();
	}
	
	public void init()
	{
		connect();
		login();
	
		loadJFrame();
		frame.setVisible(true);
		
		run();
	}
	
	public void login()
	{
		LoginFrame loginFrame = new LoginFrame(clientConnection);
		
		
		while (loginFrame.loginWasSuccess == false) {}
		
		username = loginFrame.usernameTextField.getText();
		handle = loginFrame.handleTextField.getText();
	}
	
	public void loadJFrame()
	{			
		//GUI
		frame = new JFrame("Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		leftSidePanel = new JPanel();
		leftSidePanel.setPreferredSize(new Dimension(200, 500));
		
		helpListLabel = new JLabel("Hjälplista:");
		leftSidePanel.add(helpListLabel, BorderLayout.NORTH);
		
		helpListModel = new DefaultListModel<Object>(); 
		helpList = new JList<Object>(helpListModel);
		helpList.setBackground(Color.WHITE);
		helpList.setPreferredSize(new Dimension(200, 495));
		helpList.setBorder( new BevelBorder ( BevelBorder.RAISED ) );
		helpList.setLayout(new BorderLayout());
		leftSidePanel.add(helpList, BorderLayout.WEST);
		
		rightSidePanel = new JPanel();
		rightSidePanel.setPreferredSize(new Dimension(500, 500));
		
		chatTextAreaLabel = new JLabel("Chat:");
		rightSidePanel.add(chatTextAreaLabel, BorderLayout.NORTH);
		
		chatTextArea = new JTextArea();
		chatTextScrollArea = new JScrollPane(chatTextArea);
		chatTextScrollArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		chatTextScrollArea.setPreferredSize(new Dimension(500, 420));
		chatTextArea.setBackground(Color.WHITE);
		chatTextArea.setLineWrap(true);
		chatTextArea.setEditable(false);
		//chatTextArea.setPreferredSize(new Dimension(500, 420));
		chatTextArea.setBorder( new BevelBorder ( BevelBorder.RAISED ) );
		chatTextArea.setLayout(new BorderLayout());
		rightSidePanel.add(chatTextScrollArea);
		
		typingTextArea = new JTextArea();
		typingTextArea.setBackground(Color.WHITE);
		typingTextArea.setLineWrap(true);
		typingTextArea.addKeyListener(this);
		typingTextArea.setPreferredSize(new Dimension(500, 70));
		typingTextArea.setBorder( new BevelBorder ( BevelBorder.RAISED ) );
		typingTextArea.setLayout(new BorderLayout());
		rightSidePanel.add(typingTextArea, BorderLayout.SOUTH);
		
		buttonPanel = new JPanel();
		buttonPanel.setPreferredSize(new Dimension(700, 50));
		buttonPanel.setLayout(new GridLayout(1, 3));
		
		//getWaitListButton = new JButton("Hämta väntlista");
		//buttonPanel.add(getWaitListButton);
		addToWaitListButton = new JButton("Lägg till dig själv");
		buttonPanel.add(addToWaitListButton);
		addToWaitListButton.setActionCommand("addToWaitList");
		addToWaitListButton.addActionListener(this);
		
		removeFromWaitListButton = new JButton("Ta bort dig själv");
		buttonPanel.add(removeFromWaitListButton);
		removeFromWaitListButton.setActionCommand("removeFromWaitList");
		removeFromWaitListButton.addActionListener(this);
		
		quitButton = new JButton("Avsluta");
		buttonPanel.add(quitButton);
		quitButton.setActionCommand("quit");
		quitButton.addActionListener(this);
		
		frame.add(leftSidePanel, BorderLayout.WEST);
		frame.add(rightSidePanel, BorderLayout.EAST);
		frame.add(buttonPanel, BorderLayout.SOUTH);
		
		frame.setSize(725, 620);
		frame.setResizable(false);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if ("addToWaitList".equals(e.getActionCommand()))
		{
			clientConnection.addToWaitList(handle);
		}
		else if ("removeFromWaitList".equals(e.getActionCommand()))
		{
			clientConnection.removeFromWaitList(handle);
		}
		else if ("quit".equals(e.getActionCommand()))
		{
			close();
		}
	}
	
	public void connect()
	{
		clientConnection = new ClientConnection();
		while (clientConnection.connected == false)
			clientConnection.tryConnect();
	}
	
	public void keyPressed(KeyEvent e)
	{
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			//if (chatTextArea.getText().length() != 0)
			//	chatTextArea.append("\n");
			//chatTextArea.append(">> " + username + ":\n" + typingTextArea.getText());
			clientConnection.sendChatMessage(handle, typingTextArea.getText());
			typingTextArea.setText("");
			e.consume();
		}
	}
	
	public void keyReleased(KeyEvent e)
	{
	}
	
	public void keyTyped(KeyEvent e)
	{
	}
	
	public void run()
	{
		while (blinker)
		{
			if (clientConnection.connected == false)
			{
				System.out.println("Lost Connection: Will try again until a connection is made.");
				
				connect();
			}
			
			String message = clientConnection.getServerMessage();
			
			if (message.startsWith("Host-chat:"))
				handleNewChatMessage(message);
			else if (message.startsWith("Host-waitList:"))
				handleWaitList(message);
		}
		
		finalize();
	}
	
	public void addNewChatMessage(String chatMessage)
	{
		String[] splitMessage = chatMessage.split("-");
		String handle = "";
		String message = "";
		if (splitMessage.length >= 1)
			handle = splitMessage[0];
		if (splitMessage.length == 2)		
			message = splitMessage[1];
		String[] splitStr = chatMessage.split(":");
		
		if (chatTextArea.getText().length() == 0)
			chatTextArea.append(">> " + handle + ":\n" + message);
		else
			chatTextArea.append("\n>> " + handle + ":\n" + message);
	}
	
	public void handleWaitList(String message)
	{
		String[] handles;
		String[] splitMessage = message.split(":");
		if (splitMessage.length == 1)
			handles = new String[0];
		else
			handles = splitMessage[1].split("-");
		
		helpListModel.clear();
		
		for (String str : handles)
		{
			helpListModel.addElement(str);
		}
    }
	
	public void handleNewChatMessage(String message)
	{
		//FORMAT = Host-chat:handle-message
		String[] splitMessage = message.split(":");
		message = splitMessage[1];
		//FORMAT = handle-message
		
		addNewChatMessage(message);
    }
	
	public void close()
	{
		blinker = false;
	}
	
	public void finalize()
	{
		System.exit(0);
	}
}

class ClientConnection
{
	boolean connected = false;
	InetAddress host;
	Socket socket;
	ObjectOutputStream oos;
	ObjectInputStream ois;
	
	public ClientConnection()
	{
		tryConnect();
		
		if (connected == false)
			System.out.println("Connection to " + host.toString() + " failed. Will try again until a connection is made.");
	}

	public void tryConnect()
	{
		try
		{
			//Get the ip-adress stored in the file ip.txt.
			File currentDir = new File (".");
			File ipFile = new File("ip.dat");
			BufferedReader reader = new BufferedReader(new FileReader(ipFile));
			
			host = InetAddress.getByName(reader.readLine());
			//host = InetAddress.getLocalHost();
			reader.close();
			socket = new Socket(host.getHostName(), 7777);
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			connected = true;
			System.out.println("Connection succeeded. Host: " + host.toString() + "");
		}
		catch (java.net.ConnectException e)
		{
            //e.printStackTrace();
			connected = false;
        }
		catch (java.io.FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (UnknownHostException e)
		{
            e.printStackTrace();
        }
		catch (IOException e)
		{
			connected = false;
            //e.printStackTrace();
        }
	}
	
	public String getServerMessage()
	{
		String message = "";
		
	    try
		{
            // Get the message.
            message = (String)ois.readObject();
			System.out.println("Server Message: " + message);
        }
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
            //e.printStackTrace();
			connected = false;
        }
		
		return message;
	}
	
	public void sendChatMessage(String handle, String message)
	{
        try
		{
            // Send the message.
            oos.writeObject("chat:" + handle + ":" + message);
			System.out.println("Sent chat message: " + message);
        }
		catch (IOException e)
		{
            //e.printStackTrace();
			connected = false;
        }
    }
	
	public void addToWaitList(String handle)
	{
        try
		{
			System.out.println("Sending request to add user to waitlist.");
            oos.writeObject("addToWaitList:" + handle);
        }
		catch (IOException e)
		{
            //e.printStackTrace();
			connected = false;
        }
    }
	
	public void removeFromWaitList(String handle)
	{
        try
		{
            // Send the message.
			System.out.println("Sending request to remove from waitlist.");
            oos.writeObject("removeFromWaitList:" + handle);
        }
		catch (IOException e)
		{
			//e.printStackTrace();
			connected = false;
        }
    }
	
	public int isUserInfoValid(String handle, String username, String password)
	{
		int answer = -1;
		
		try
		{
			System.out.println("Asking server if user info is valid...");
			oos.writeObject("login:" + handle + ":" + username + ":" + password);
			
            // Read the answer.
            String stringAnswer = (String) ois.readObject();
			System.out.println(stringAnswer);
			if (stringAnswer.equals("Host-login:true"))
				answer = 0;
			else if (stringAnswer.equals("Host-handle:occupied"))
				answer = 1;
			else if (stringAnswer.equals("Host-login:occupied"))
				answer = 2;
			
			System.out.println("User info is: " + answer);
        }
		catch (ClassNotFoundException e)
		{
            e.printStackTrace();
        }
		catch (IOException e)
		{
            //e.printStackTrace();
			connected = false;
        }
		
		return answer;
	}
}

class LoginFrame implements java.awt.event.ActionListener
{
	ClientConnection clientConnection;
	
	JFrame frame;
	JLabel handleLabel;
	JLabel usernameLabel;
	JLabel passwordLabel;
	public JTextField handleTextField;
	public JTextField usernameTextField;
	public JTextField passwordTextField;
	JPanel buttonPanel;
	JButton loginButton;
	JButton cancelButton;
	
	public boolean loginWasSuccess = false;

	public LoginFrame(ClientConnection _clientConnection)
	{
		clientConnection = _clientConnection;
		
		frame = new JFrame("Login");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new GridLayout(7, 1));
		
		handleLabel = new JLabel("Chatnamn");
		frame.add(handleLabel);
		
		handleTextField = new JTextField();
		frame.add(handleTextField);
		
		usernameLabel = new JLabel("Användarnamn");
		frame.add(usernameLabel);
		
		usernameTextField = new JTextField();
		frame.add(usernameTextField);
		
		passwordLabel = new JLabel("Lösenord");
		frame.add(passwordLabel);
		
		passwordTextField = new JTextField();
		frame.add(passwordTextField);
		
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));
		
		loginButton = new JButton("Logga in");
		loginButton.setActionCommand("login");
		buttonPanel.add(loginButton);
		
		cancelButton = new JButton("Avbryt");
		cancelButton.setActionCommand("cancel");
		buttonPanel.add(cancelButton);
		
		loginButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		frame.add(buttonPanel);
		
		frame.setSize(370, 200);
		frame.setVisible(true);
		frame.setResizable(false);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if ("login".equals(e.getActionCommand()))
		{
			if (usernameTextField.getText().length() == 0 |
				passwordTextField.getText().length() == 0)
				return;
				
			if (handleTextField.getText().equals(""))
			{
				handleTextField.setText("Chatnamnet är inte ifyllt.");
			}
			int loginResult = clientConnection.isUserInfoValid(handleTextField.getText(),
																usernameTextField.getText(), passwordTextField.getText());
			if (loginResult == 0)
			{
				loginWasSuccess = true;
				frame.dispose();
			}
			else if (loginResult == 1)
			{
				handleTextField.setText("Chatnamnent är upptaget");
			}
			else if (loginResult == 2)
			{
				usernameTextField.setText("Användarnamnent är redan inloggat.");
			}
			else if (loginResult == -1)
			{
				usernameTextField.setText("Fel användarnamn eller lösenord.");
				passwordTextField.setText("");
			}
		}
		else if ("cancel".equals(e.getActionCommand()))
		{
			frame.dispose();
		}
	}
}