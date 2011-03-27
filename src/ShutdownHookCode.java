import de.sciss.jcollider.Server;


public class ShutdownHookCode {
    public void start() {
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}

class ShutdownHook extends Thread {
    public void run() {
    	System.out.println("Shutdown Hook running now");
        Server.quitAll();
    }
}