
import java.io.IOException;

import com.supasulley.speazy.JarController;

public class Test {
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		// Setup server
		JarController controller = new JarController("/Users/eboschert/Downloads/hi/app/build/libs/app.jar");
		controller.start();
	}
}
