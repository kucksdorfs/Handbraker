import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Driver {
	private static String cliLocation = "/usr/bin/HandBrakeCLI";
	private static String validExtensions = ".mkv|.mp4";
	private static String outputExtension = "mp4";
	private static Boolean verboseOutput = false;
	private static Integer verboseNumber = 30;
	private static Boolean deleteOriginal = false;
	private static Integer conversionTolerance = 1; // second
	
	public static int GetConverstionTolerance() {
		return conversionTolerance;
	}

	public static String GetCliLocation() {
		return cliLocation;
	}

	public static String GetValidExtensions() {
		return validExtensions;
	}

	public static void main(String[] args) {
		// Check arguments
		if (args.length > 0 && args[0].equals("--h")) {
			System.out.println("Arguments include:");
			System.out
					.println("\t--cli {location} \t\t - The location of the HandBrakeCLI executable. The default is /usr/bin/HandBrakeCLI.");
			System.out
					.println("\t--extensions {extension} \t\t - The bar delimited \"|\" list of extensions to find. The default is .mkv|.mp4");
			System.out
					.println("\t--outputExtension {extension} \t\t - The extension for the output file. Note, HandbrakeCLI only supports mp4 and mkv. The default is mp4");
			System.out
					.println("\t-v or --verbose \t\t - Write the output from HandBrakeCLI to the stdout.");
			System.out
					.println("\t-vn {integer} - The number of outputs to skip. The smaller the number, the more verbose the output from HandbrakeCLI. Implies the -v. Default value is "
							+ verboseNumber.toString());
			System.out.println("\t-d - Flag to delete the original files.");
			System.out.println("\t-t {integer} - The tolerance in seconds used for determining if a conversion was successful.");
			System.out.println("\t-h \t\t - Displays this help.");
			return;
		}
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case ("--cli"):
				if (args.length >= ++i)
					cliLocation = args[i];
				else {
					System.out
							.println("No location given for HandBrakeCLI program.");
					return;
				}
				break;
			case ("--extensions"):
				if (args.length >= ++i)
					validExtensions = args[i];
				else {
					System.out.println("There are no extensions provided.");
					return;
				}
				break;
			case ("--outputExtension"):
				if (args.length >= ++i) {
					if (args[i].equals("mp4") || args[i].equals("mkv")) {
						outputExtension = args[i];
					} else {
						System.out.println("The extension " + args[i]
								+ " is not a valid extension.");
						return;
					}
				} else {
					System.out.println("There was no extension provided.");
					return;
				}
				break;
			case ("-vn"):
				// verboseNumber
				if (args.length >= ++i)
					try {
						verboseNumber = Integer.parseInt(args[i]);
						if (verboseNumber <= 0)
							throw new Exception(
									"The number must be greater than 0.");
					} catch (Exception ex) {
						System.out
								.println("The number provided for the -vn parameter must be greater than 0.");
						return;
					}
				else {
					System.out.println("There was no integer provided.");
					return;
				}
			case ("-v"):
			case ("--verbose"):
				verboseOutput = true;
				break;
			case ("-d"):
				deleteOriginal = true;
				break;
			case ("-t"):
				if (args.length >= ++i)
					try {
						conversionTolerance = Integer.parseInt(args[i]);
						if (conversionTolerance <= 0)
							throw new Exception(
									"The number must be greater than 0.");
					} catch (Exception ex) {
						System.out
								.println("The number provided for the -t parameter must be greater than 0.");
						return;
					}
				else {
					System.out.println("There was no integer provided.");
					return;
				}
				break;
			default:
				System.out.println("There was an invalid argument: " + args[i]);
				return;
			}
		}

		java.io.File cliProgram = new File(cliLocation);
		if (!cliProgram.exists()) {
			System.out.println("The program doesn't exist in: " + cliLocation);
			return;
		} else if (!cliProgram.canExecute()) {
			System.out
					.println("The program cannot be executed: " + cliLocation);
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
			if (!inputDir.isDirectory()) {
				System.out.println("The input directory does not exist");
				return;
			}
			System.out.println("Enter the full output location:");
			strOutputDir = in.readLine();
			outputDir = new File(strOutputDir);
			if (!outputDir.isDirectory()) {
				if (!outputDir.mkdir()) {
					System.out
							.println("Unable to create the output directory: "
									+ outputDir.getAbsolutePath());
					return;
				}
			}
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			return;
		}

		Node myMovieList = RecursiveFileSearch(inputDir,
				inputDir.getAbsolutePath(), outputDir.getAbsolutePath());
		MovieFile.Tolerance = conversionTolerance;

		while (myMovieList != null) {
			MovieFile currentMovie = myMovieList.GetMyMovieFile();
			String previousMovie = "";
			try {
				File parentDir = new File(
						currentMovie.GetDestinationDirectoryPath());
				if (!parentDir.exists() && !parentDir.isDirectory())
					if (!parentDir.mkdirs())
						throw new Exception(
								"Was unable to create the directory structure: "
										+ parentDir.getAbsolutePath());
				File destFile = new File(currentMovie.GetDestinationFullPath());
				if (destFile.exists())
					if (!destFile.delete())
						throw new Exception(
								"Was unable to delete the already created destination file: "
										+ destFile.getAbsolutePath());

				ProcessBuilder pb = new ProcessBuilder();
				if (currentMovie.HasSubtitles()) {
					pb.command(cliLocation, "-i", currentMovie.GetSourcePath(),
							"-o", currentMovie.GetDestinationFullPath(), "-e",
							"x264", "-q", "20.0", "-a", "1", "-E", "faac",
							"-B", "160", "-6", "dpl2", "-R", "Auto", "-D",
							"0.0", "--audio-copy-mask",
							"aac,ac3,dtshd,dts,mp3", "--audio-fallback",
							"ffac3", "-f", outputExtension,
							"--loose-anamorphic", "--modulus", "2", "-m",
							"--x264-preset", "veryfast", "--h264-profile",
							"main", "--h264-level", "4.0", "-s", "1",
							"--subtitle-default", "1");
				} else {
					pb.command(cliLocation, "-i", currentMovie.GetSourcePath(),
							"-o", currentMovie.GetDestinationFullPath(), "-e",
							"x264", "-q", "20.0", "-a", "1", "-E", "faac",
							"-B", "160", "-6", "dpl2", "-R", "Auto", "-D",
							"0.0", "--audio-copy-mask",
							"aac,ac3,dtshd,dts,mp3", "--audio-fallback",
							"ffac3", "-f", outputExtension,
							"--loose-anamorphic", "--modulus", "2", "-m",
							"--x264-present", "veryfast", "--h264-profile",
							"main", "--h264-level", "4.0");
				}
				Process p = pb.start();
				if (verboseOutput) {
					StreamGobbler outputGobbler = new StreamGobbler(
							p.getInputStream(),
							currentMovie.GetRelativeFileName(), verboseNumber);
					StreamGobbler errorGobbler = new StreamGobbler (
							p.getErrorStream(),
							"Error", 1);
					outputGobbler.start();
					errorGobbler.start();
				}
				p.waitFor();
			} catch (Exception ex) {
				System.out.println("There was a problem running HandbrakeCLI: "
						+ ex.getMessage());
			}

			if (currentMovie.EncodedSuccessfully()) {
				if (deleteOriginal) {
					File inputFile = new File(currentMovie.GetSourcePath());
					File parentFolder = inputFile.getParentFile();
					inputFile.delete();
					if (parentFolder.listFiles().length == 0) {
						parentFolder.delete();
					}
				}

				try {
					System.out.println("Finished transcoding "
							+ currentMovie.GetDestinationFullPath());
					System.out.println("There are " + --Node.NumInList
							+ " left to be transcoded.");
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
				myMovieList = myMovieList.next;
			} else {
				File destFile = null;
				try {
					destFile = new File(currentMovie.GetDestinationFullPath());
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
				if (!previousMovie.equals(currentMovie.GetSourcePath())) {
					myMovieList = myMovieList.next;
					System.err.println("There was an error encoding the file " + previousMovie);
				}
				else {
					destFile.delete();
				}
			}
			previousMovie = currentMovie.GetSourcePath();
		}

		System.out.println("Exiting program.");
	}

	private static Node RecursiveFileSearch(File baseDirectory,
			String baseInputDir, String baseOutputDir) {
		Node link = null, last = null;
		File[] files = baseDirectory.listFiles();
		for (int i = 0; i < files.length; ++i) {
			File currentFile = files[i];
			if (currentFile.isDirectory()) {
				if (link == null) {
					link = RecursiveFileSearch(currentFile, baseInputDir,
							baseOutputDir);
					last = link;
				} else {
					while (last.next != null) {
						last = last.next;
					}
					last.next = RecursiveFileSearch(currentFile, baseInputDir,
							baseOutputDir);
				}
			} else if (Driver.HasValidExtension(currentFile.getAbsolutePath())) {
				if (link == null) {
					link = new Node(new MovieFile(
							currentFile.getAbsolutePath(), baseInputDir,
							baseOutputDir, outputExtension));
					last = link;
				} else {
					while (last.next != null) {
						last = last.next;
					}
					last.next = new Node(new MovieFile(
							currentFile.getAbsolutePath(), baseInputDir,
							baseOutputDir, outputExtension));
					last = last.next;
				}
			}
		}

		return link;
	}

	private static Boolean HasValidExtension(String pathToFile) {
		String[] validExtensionsAry = validExtensions.split("\\|");
		for (int i = 0; i < validExtensionsAry.length; ++i) {
			if (pathToFile.endsWith(validExtensionsAry[i])) {
				return true;
			}
		}
		return false;
	}
}

class Node {
	private MovieFile myMovieFile;
	public Node next;
	public static int NumInList = 0;

	public Node(MovieFile myMovieFile) {
		this.myMovieFile = myMovieFile;
		++NumInList;
	}

	public MovieFile GetMyMovieFile() {
		return this.myMovieFile;
	}
}