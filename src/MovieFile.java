import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;

public class MovieFile {
	public static int Tolerance = Driver.GetConverstionTolerance();
	String fullInputFileName, baseInputDir, baseOutputDir, myExtension;
	Boolean hasSubtitles = null;
	Boolean hasFailed = false;

	String fileName = null, destinationPath = null;

	public MovieFile(String fullInputFileName, String baseInputDir,
			String baseOutputDir, String outputExtension) {
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

	public Boolean EncodedSuccessfully() {
		TimeDuration inputDuration = GetDuration(GetSourcePath());
		TimeDuration outputDuration = null;
		try {
			outputDuration = GetDuration(GetDestinationFullPath());
		} catch (Exception ex) {
			return false;
		}

		System.out.println(GetSourcePath().trim());
		System.out.println(" Input file duration: " + inputDuration.toString());
		System.out.println("Output file duration: " + outputDuration.toString());

		return inputDuration.CompareWithTolerance(outputDuration, Tolerance);
	}

	private TimeDuration GetDuration(String inputFile) {
		String HandbrakeCLI = Driver.GetCliLocation();

		ProcessBuilder pb = new ProcessBuilder(HandbrakeCLI, "-i", inputFile,
				"--scan");
		Process p;
		try {
			p = pb.start();
			p.waitFor(); // wait for process to finish then continue.
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
			return null;
		}

		BufferedReader error = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		String line = "", duration = "";
		Boolean continueReading = true;
		try {
			while ((line = error.readLine()) != null && continueReading) {
				if (line.toLowerCase().contains("+ duration:")) {
					duration = line.trim();
					continueReading = false;
				}
			}
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			return null;
		}

		TimeDuration retDuration = null;
		if (!duration.equals("")) {
			retDuration = new TimeDuration(duration.substring(
					"+ duration:".length()).trim());
		}

		return retDuration;
	}

	public String GetRelativeFileName() throws Exception {
		if (!this.fullInputFileName.startsWith(this.baseInputDir))
			throw new Exception(
					"Base directory is not part of the full directory.");

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
			throw new Exception(
					"Base directory is not part of the full directory.");
		}

		String endPath = fullInputFileName.substring(baseInputDir.length());
		String fullPath = baseOutputDir
				+ (!baseOutputDir.endsWith(File.separator)
						|| !endPath.startsWith(File.separator) ? ""
						: File.separator) + endPath;
		if (fullPath.endsWith(this.myExtension)) {
			this.destinationPath = fullPath;
			return destinationPath;
		}

		int indexOfExtension = fullPath.lastIndexOf('.');
		fullPath = fullPath.substring(0, indexOfExtension);

		this.destinationPath = fullPath
				+ (!fullPath.endsWith(".") || !myExtension.startsWith(".") ? ""
						: ".") + myExtension;
		return this.destinationPath;
	}

	public String GetDestinationDirectoryPath() throws Exception {
		String destinationFilePath = GetDestinationFullPath();
		int indexOfLastDirSeperator = destinationFilePath
				.lastIndexOf(File.separator);

		return destinationFilePath.substring(0, indexOfLastDirSeperator);
	}

	public String GetSourcePath() {
		return this.fullInputFileName;
	}

	public Boolean HasSubtitles() {
		String HandbrakeCLI = Driver.GetCliLocation();

		ProcessBuilder pb = new ProcessBuilder(HandbrakeCLI, "-i",
				this.fullInputFileName, "--scan");
		Process p;
		try {
			p = pb.start();
			p.waitFor(); // wait for process to finish then continue.
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
			return false;
		}

		BufferedReader error = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
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

class TimeDuration {
	int hours = 0, minutes = 0, seconds = 0;

	public TimeDuration() {
		hours = 0;
		minutes = 0;
		seconds = 0;
	}

	public TimeDuration(String timeDuration) {
		this.hours = Integer.parseInt(timeDuration.substring(0, 2));
		this.minutes = Integer.parseInt(timeDuration.substring(3, 5));
		this.seconds = Integer.parseInt(timeDuration.substring(6));
	}

	public boolean CompareWithTolerance(TimeDuration compareTo, int tolerance) {
		int difference = 0;
		int thisTotalSeconds = ConvertToSeconds(this);
		int compareTotalSeconds = ConvertToSeconds(compareTo);
		difference = Math.abs(thisTotalSeconds - compareTotalSeconds);

		return (difference <= tolerance);
	}

	private int ConvertToSeconds(TimeDuration getSeconds) {
		int totalSeconds = 0;
		totalSeconds += getSeconds.hours * 3600;
		totalSeconds += getSeconds.minutes * 60;
		totalSeconds += getSeconds.seconds;

		return totalSeconds;
	}

	@Override
	public String toString() {
		return String.format("%02d", this.hours) + ":"
				+ String.format("%02d", this.minutes) + ":"
				+ String.format("%02d", this.seconds);
	}
}