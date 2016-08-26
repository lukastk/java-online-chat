import java.util.ArrayList;
import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.Math;

public class StringEncryptor
{
	public static void main(String[] args)
	{
		File currentDir = new File (".");
		File userInfo = null;
		ArrayList<String> userInfos = new ArrayList<String>();
		
		try
		{
			userInfo = new File(args[0]);
			BufferedReader reader = new BufferedReader(new FileReader(userInfo));
			
			String line;
			while ( (line = reader.readLine()) != null )
			{
				String[] infos = line.split(":");
				for (int i = 0; i < infos.length; i ++)
				{
					userInfos.add(infos[i]);
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
			e.printStackTrace();
		}
		
		try
		{
			FileWriter fstream = new FileWriter("userInfo.dat");
			BufferedWriter out = new BufferedWriter(fstream);
			
			String lastInfo = userInfos.get(userInfos.size() - 1);
			userInfos.remove(userInfos.size() - 1);
			for (String info : userInfos)
				out.write(Encryption.encryptString(info) + ":");
			
			out.write(Encryption.encryptString(lastInfo));
			out.close();
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
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