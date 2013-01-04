import java.util.Enumeration;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarScanner
{
	public static enum JavaVersion
	{
		JAVA_1_1((char)0x2D, "1.1"),
		JAVA_1_2((char)0x2E, "1.2"),
		JAVA_1_3((char)0x2F, "1.3"),
		JAVA_1_4((char)0x30, "1.4"),
		JAVA_5((char)0x31, "1.5"),
		JAVA_6((char)0x32, "1.6"),
		JAVA_7((char)0x33, "1.7");

		private char majorVersionNumber;
		private String sourceName;

		private static final Map<Character, JavaVersion> charMapping = new HashMap<>();
		private static final Map<String, JavaVersion> sourceNameMapping = new HashMap<>();
		static
		{
			JavaVersion values[] = JavaVersion.values();
			for(JavaVersion current : values)
			{
				charMapping.put(current.getMajorVersionNumber(), current);
				sourceNameMapping.put(current.getSourceName(), current);
			}
		}

		JavaVersion(char major, String sourceName)
		{
			this.majorVersionNumber = major;
			this.sourceName = sourceName;
		}

		public char getMajorVersionNumber()
		{
			return majorVersionNumber;
		}

		public String getSourceName()
		{
			return sourceName;
		}

		public static JavaVersion getVersionByMajorVersionNumber(char majorVersionNumber)
		{
			return charMapping.get(majorVersionNumber);
		}

		public static JavaVersion getVersionBySourceName(String sourceName)
		{
			return sourceNameMapping.get(sourceName);
		}

		@Override
		public String toString()
		{
			return "JavaVersion{" +
					"majorVersionNumber=0x" + Integer.toHexString(majorVersionNumber) +
					", sourceName='" + sourceName + '\'' +
					'}';
		}
	}
	private static final int MAGIC_NUMBER_CLASS = 0xCAFEBABE;

	public static void main(String args[])
	{
		//System.out.println(JavaVersion.JAVA_1_1.compareTo(JavaVersion.JAVA_7));
		if(args.length == 0)
		{
			System.out.println("Missing files.");
			return;
		}
		for(String current : args)
		{
			File f = new File(current);
			if(!f.isFile())
			{
				System.out.println("'"+f.getAbsolutePath()+"' is not a file.");
				continue;
			}
			try
			{
				System.out.println(scanJar(f) + " for file '"+f.getAbsolutePath()+"'.");
			}
			catch(IOException ex)
			{
				System.out.println("Exception while processing '"+f.getAbsolutePath()+"'. "+ex);
			}
		}
	}
	
	public static JavaVersion scanJar(File jarFile)
		throws IOException
	{
		//System.out.println("Scanning '"+jarFile.getAbsolutePath()+"'.");
		char highest=0;
		ZipFile zipFile = null;
		try
		{

			zipFile = new ZipFile(jarFile);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements())
			{
				ZipEntry current = entries.nextElement();
				if(current.isDirectory())
				{
					//System.out.println("Skipping "+current.getName());
					continue;
				}
				//System.out.println("Checking "+current.getName());
				if(!current.getName().endsWith(".class"))
				{
					//System.out.println("Ignoring '"+current.getName()+"'.");
					continue;
				}

				InputStream classStream = zipFile.getInputStream(current);
				if(classStream == null)
				{
					//System.out.println("Failed to get stream for '"+current.getName()+"'.");
					continue;
				}
				try
				{
					Character currentMajor = scanClass(classStream);
					if(currentMajor == null)
					{
						continue;
					}
					//System.out.println(Integer.toHexString(currentMajor));
					if(currentMajor > highest)
					{
						highest = currentMajor;
					}
				}
				catch(IOException ex)
				{
					System.out.println("Exception "+ex);
				}
				finally
				{
					try
					{
						classStream.close();
					}
					catch(IOException ex)
					{
						// ignore
					}
				}
			}
		}
		finally
		{
			if(zipFile != null)
			{
				try
				{
					zipFile.close();
				}
				catch(IOException ex)
				{
					// ignore
				}
			}
		}
		if(highest == 0)
		{
			return null;
		}
		return JavaVersion.getVersionByMajorVersionNumber(highest);
	}

	/**
	 * Returns the major version of the given Java class file.
	 *
	 * @param is the stream from which the java class version number will be read.
	 * @return the major version number of the class or null if is does not contain a class.
	 * @throws IOException
	 */
	private static Character scanClass(InputStream is)
			throws IOException
	{
		DataInputStream dis = new DataInputStream(is);
		int magic = dis.readInt();
		if(MAGIC_NUMBER_CLASS != magic)
		{
			System.out.println("Wrong magic number for class. Ignoring.");
			return null;
		}
		/*char minor = */dis.readChar();
		//System.out.println(Integer.toHexString(minor));
		return dis.readChar();
	}
}
