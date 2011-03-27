import processing.core.PApplet;

public class Main extends NetChimes {

	private static final long serialVersionUID = 1L;

	public Main() {
    }

    public static void main(String[] args) {
      ShutdownHookCode demo = new ShutdownHookCode();
      demo.start();
      PApplet.main(new String[]{"Main"});
    }
}
