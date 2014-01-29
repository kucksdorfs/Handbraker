import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {
    InputStream is;
    String type;
    Integer verboseNumber;

    public StreamGobbler(InputStream is, String type, Integer verboseNumber) {
        this.is = is;
        this.type = type;
        this.verboseNumber = verboseNumber;
    }

    @Override
    public void run() {
    	int index = 0;
    	try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
            	if (index % verboseNumber == 0) {
            		System.out.println(type + "> " + line);
            	}
            	index = (++index) % verboseNumber;
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        System.out.println(type + "> Finishing StreamGobbler Thread");
    }
}