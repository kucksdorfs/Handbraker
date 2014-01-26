import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;

public class MovieFile {
	String input, baseInputDir, baseOutputDir, myExtension;
	Boolean hasSubtitles = null;
	
	String fileName = null, destinationPath = null;

	public MovieFile(String inputFile, String baseInputDir, String baseOutputDir, String outputExtension) {
		this.input = inputFile;
		this.baseInputDir = baseInputDir;
		this.baseOutputDir = baseOutputDir;
		this.myExtension = outputExtension;
	}

	public String GetFileName() {
		if (this.fileName != null)
			return this.fileName;
		File file = new File(this.input);
		this.fileName = file.getName();
		file = null;
		return this.fileName;
	}

	public String GetDestinationFullPath() throws Exception {
		if (this.destinationPath != null)
			return this.destinationPath;
		if (baseInputDir.startsWith(input)) {
			throw new Exception("Base directory is not part of the full directory.");
		}
		
		String endPath = input.substring(baseInputDir.length());
		String fullPath = baseOutputDir + (!baseOutputDir.endsWith(File.separator) || !endPath.startsWith(File.separator) ? "" : File.separator) + endPath;
		if (fullPath.endsWith(this.myExtension)) {
			this.destinationPath = fullPath;
			return destinationPath;
		}
		
		int indexOfExtension = fullPath.lastIndexOf('.');
		fullPath = fullPath.substring(0, indexOfExtension);

		this.destinationPath = fullPath + (!fullPath.endsWith(".") || !myExtension.startsWith(".") ? "" : ".") + myExtension; 
		return this.destinationPath;
	}
	
	public String GetDestinationDirectoryPath() throws Exception {
		String destinationFilePath = GetDestinationFullPath();
		int indexOfLastDirSeperator = destinationFilePath.lastIndexOf(File.separator);

		return destinationFilePath.substring(0, indexOfLastDirSeperator);
	}

	public String GetSourcePath() {
		return this.input;
	}

	public Boolean HasSubtitles() {
		String HandbrakeCLI = Main.GetCliLocation();

		ProcessBuilder pb = new ProcessBuilder(HandbrakeCLI, "-i", this.input, "--scan");
		Process p;
		try {
			p = pb.start();
			p.waitFor();  // wait for process to finish then continue.
		}
		catch (Exception ex) {
			System.err.println(ex.getMessage());
			return false;
		}

		BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line = "", resultError = "";
		Boolean hasSubtitles = false;
		try {
			while ((line = error.readLine()) != null) {
				if (hasSubtitles && !line.contains("HandBrake has exited."))
					resultError += line + '\n';
				if (line.contains("+ subtitle tracks:"))
					hasSubtitles = true;
			}
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			return false;
		}

		return resultError.trim().startsWith("+ 1, English");
	}

}
