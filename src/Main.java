import java.io.*;

public class Main {
	private static String cliLocation = "/usr/bin/HandBrakeCLI";
	private static String validExtensions = ".mkv|.mp4";
	private static String outputExtension = ".mp4";
	private static Boolean verboseOutput = false;
	
	public static String GetCliLocation() {
		return cliLocation;
	}
	
	public static String GetValidExtensions() {
		return validExtensions;
	}

	public static void main(String[] args) {
		// Check arguments
		if (args.length > 0 && args[0].equals("--h"))
		{
			System.out.println("Arguments include:");
			System.out.println("\t--cli {location} \t\t - The location of the HandBrakeCLI executable. The default is /usr/bin/HandBrakeCLI.");
			System.out.println("\t--extensions {extension} \t\t - The bar delimited \"|\" list of extensions to find. The default is .mkv|.mp4");
			System.out.println("\t--outputExtension {extension} \t\t - The extension for the output file. Note, HandbrakeCLI only supports mp4 and mkv. The default is .mp4");
			System.out.println("\t-v or --verbose \t\t - Write the output from HandBrakeCLI to the stdout.");
			System.out.println("\t-h \t\t - Displays this help.");
			return;
		}
		for (int i = 0; i < args.length; ++i) {
			switch(args[i])
			{
			case("--cli"):
				if (args.length >= ++i)
					cliLocation = args[i];
				else {
					System.out.println("No location given for HandBrakeCLI program.");
					return;
				}
				break;
			case("--extensions"):
				if(args.length >= ++i)
					validExtensions = args[i];
				else {
					System.out.println("There are no extensions provided.");
					return;
				}
				break;
			case("--outputExtension"):
				if(args.length >= ++i)
					outputExtension = args[i];
				else {
					System.out.println("There was no extension provided.");
					return;
				}
				break;
			case ("-v"):
			case ("--verbose"):
				verboseOutput = true;
				break;
			default:
				System.out.println("There was an invalid argument: " + args[i]);
				return;
			}
		}
		
		java.io.File cliProgram = new File(cliLocation);
		if (!cliProgram.exists())
		{
			System.out.println("The program doesn't exist in: " + cliLocation);
			return;
		}
		else if (!cliProgram.canExecute())
		{
			System.out.println("The program cannot be executed: " + cliLocation);
			return;
		}
		cliProgram = null;
		
		// End checking arguments
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String strInputDir = "", strOutputDir = "";
		File inputDir = null, outputDir = null;

		try {
			System.out.println("Enter the full input location:");
			strInputDir = in.readLine();
			inputDir = new File(strInputDir);
			if (!inputDir.isDirectory())
			{
				System.out.println("The input directory does not exist");
				return;
			}
			System.out.println("Enter the full output location:");
			strOutputDir = in.readLine();
			outputDir = new File(strOutputDir);
			if (!outputDir.isDirectory())
			{
				if (!outputDir.mkdir())
				{
					System.out.println("Unable to create the output directory: " + outputDir.getAbsolutePath());
					return;
				}
			}
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			return;
		}
		
		LinkedList myMovieList = RecursiveFileSearch(inputDir, inputDir.getAbsolutePath(), outputDir.getAbsolutePath());
		
		while (myMovieList != null) {
			MovieFile currentMovie = myMovieList.GetMyMovieFile();

			try {
				File parentDir = new File(currentMovie.GetDestinationDirectoryPath());
				if (!parentDir.exists() && !parentDir.isDirectory())
					if (!parentDir.mkdirs())
						throw new Exception("Was unable to create the directory structure: " + parentDir.getAbsolutePath());
				File destFile = new File(currentMovie.GetDestinationFullPath());
				if (destFile.exists())
					if (!destFile.delete())
						throw new Exception("Was unable to delete the already created destination file: " + destFile.getAbsolutePath());
				
				ProcessBuilder pb = new ProcessBuilder();
				if (currentMovie.HasSubtitles()) {
					pb.command(cliLocation, "-i", currentMovie.GetSourcePath(), "-o", currentMovie.GetDestinationFullPath(), "-m", "-s", "1", "--subtitle-default", "1");
				}
				else {
					pb.command(cliLocation, "-i", currentMovie.GetSourcePath(), "-o", currentMovie.GetDestinationFullPath(), "-m");
				}
				Process p = pb.start();
				if (verboseOutput) {
					StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");
					outputGobbler.start();
				}
				p.waitFor();
				System.out.println("Finished transcoding " + currentMovie.GetDestinationDirectoryPath());
				System.out.println("There are " + --LinkedList.NumInList + " left to be transcoded.");
			}
			catch(Exception ex) {
				System.out.println("There was a problem running HandbrakeCLI: " + ex.getMessage());
			}
			
			myMovieList = myMovieList.next;
		}
		
		System.out.println("Exiting program.");
	}
	
	private static LinkedList RecursiveFileSearch(File baseDirectory, String baseInputDir, String baseOutputDir) {
		LinkedList link = null, last = null;
		File[] files = baseDirectory.listFiles();
		for (int i = 0; i < files.length; ++i) {
			File currentFile = files[i];
			if (currentFile.isDirectory()) {
				if (link == null) {
					link = RecursiveFileSearch(currentFile, baseInputDir, baseOutputDir);
					last = link;
				}
				else {
					while (last.next != null) {
						last = last.next;
					}
					last.next = RecursiveFileSearch(currentFile, baseInputDir, baseOutputDir);
				}
			}
			else if (currentFile.getAbsolutePath().endsWith(".mp4") || currentFile.getAbsolutePath().endsWith(".mkv")) {
				if (link == null) {
					link = new LinkedList(new MovieFile(currentFile.getAbsolutePath(), baseInputDir, baseOutputDir, outputExtension));
					last = link;
				}
				else {
					while (last.next != null) {
						last = last.next;
					}
					last.next = new LinkedList(new MovieFile(currentFile.getAbsolutePath(), baseInputDir, baseOutputDir, outputExtension));
					last = last.next;
				}
			}
		}
		
		return link;
	}
}

class LinkedList {
	private MovieFile myMovieFile;
	public LinkedList next;
	public static int NumInList = 0;
	
	public LinkedList(MovieFile myMovieFile) {
		this.myMovieFile = myMovieFile;
		++NumInList;
	}
	
	public MovieFile GetMyMovieFile() {
		return this.myMovieFile;
	}
}

class StreamGobbler extends Thread {
    InputStream is;
    String type;

    public StreamGobbler(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }

    @Override
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
                System.out.println(type + "> " + line);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.out.println(type + "> Finishing StreamGobbler Thread");
    }
}