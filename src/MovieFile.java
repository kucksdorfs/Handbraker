import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;

public class MovieFile {
	String fullInputFileName, baseInputDir, baseOutputDir, myExtension;
	Boolean hasSubtitles = null;
	
	String fileName = null, destinationPath = null;

	public MovieFile(String fullInputFileName, String baseInputDir, String baseOutputDir, String outputExtension) {
		this.fullInputFileName = fullInputFileName;
		this.baseInputDir = baseInputDir;
		this.baseOutputDir = baseOutputDir;
		this.myExtension = outputExtension;
	}

	public String GetFileNameWithoutPath() {
		if (this.fileName != null)
			return this.fileName;
		File file = new File(this.fullInputFileName);
		this.fileName = file.getName();
		file = null;
		return this.fileName;
	}
	
	public String GetRelativeFileName() throws Exception {
		if (!this.fullInputFileName.startsWith(this.baseInputDir))
			throw new Exception("Base directory is not part of the full directory.");
		
		String retValue = "";
		retValue = this.fullInputFileName.substring(this.baseInputDir.length());
		
		if (retValue.startsWith(File.separator))
			retValue = retValue.substring(1);

		return retValue;
	}

	public String GetDestinationFullPath() throws Exception {
		if (this.destinationPath != null)
			return this.destinationPath;
		if (baseInputDir.startsWith(fullInputFileName)) {
			throw new Exception("Base directory is not part of the full directory.");
		}
		
		String endPath = fullInputFileName.substring(baseInputDir.length());
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
		return this.fullInputFileName;
	}

	public Boolean HasSubtitles() {
		String HandbrakeCLI = Driver.GetCliLocation();

		ProcessBuilder pb = new ProcessBuilder(HandbrakeCLI, "-i", this.fullInputFileName, "--scan");
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
