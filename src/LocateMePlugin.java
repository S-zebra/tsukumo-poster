import java.io.IOException;

public class LocateMePlugin {
  
  public LocateMePlugin() {
    try {
      new ProcessBuilder("LocateMe").start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
}
