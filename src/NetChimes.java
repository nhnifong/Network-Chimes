import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import de.sciss.jcollider.Group;
import de.sciss.jcollider.NodeWatcher;
import de.sciss.jcollider.Server;
import de.sciss.jcollider.ServerEvent;
import de.sciss.jcollider.ServerListener;
import de.sciss.jcollider.Synth;
import de.sciss.jcollider.SynthDef;
import de.sciss.jcollider.UGenInfo;
import processing.core.PApplet;
import processing.core.PFont;
import javax.swing.JFrame;

public class NetChimes extends PApplet implements ServerListener {

	private static final long serialVersionUID = 1L;
	Server server;
	JFrame spf = null;
	Group grp;
	SynthDef[] defs = new SynthDef[2];
	Synth netSynth, cpuSynth;
	NodeWatcher nw;
	boolean readyForMessages = false;
	float inpack,outpack,userpercent,systpercent;
	float cpuvol = 0.5f;
	float netvol = 0.1f;
	float inpackLast = -1;
	float outpackLast = -1;
	Process topChild;
	BufferedReader topResults;
	float nextTopChildStartTime = 0;
	float topInterval = 100;
	Process netChild;
	BufferedReader netResults;
	float nextNetChildStartTime = 0;
	float netInterval = 100;
	float readInterval = 100;
	float nextReadTime = 0;
	String prefix = "Network Chimes.app/Contents/Resources/Java/";
	String[] defLocations = new String[]{ "sc/JNetChimes", "sc/JCpuBuzz" };
	String serverLocation = "sc/scsynth";
	// GUI
	PFont fontA;
	//PImage gear;
	int boxClicked = 0; // 0 is neither, 1 is top(net), 2 is bottom(cpu)

	public void setup(){
		size(320,150);
		smooth();

		try {
			server = new Server("localhost");

			UGenInfo.readBinaryDefinitions();

			File f = new File(prefix+serverLocation);
			String synthPath = f.getAbsolutePath();
			System.out.println(synthPath);
			Server.setProgram(synthPath);
			server.addListener( this );
			try {
				server.boot();
				//server.start();
				//server.startAliveThread();
			}
			catch( IOException e1 ) {  }
			// feedback window
			//spf = ServerPanel.makeWindow( server, ServerPanel.MIMIC | ServerPanel.CONSOLE | ServerPanel.DUMP );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//load the defs from files
		//to make new files, see the unused UGenCreator class.
		loadDefs();

		//gear = loadImage("/gui/gear.png");
		//fontA = loadFont("/gui/ArialNarrow-18.vlw");
		fontA = createFont("ArialNarrow", 18);
		textFont(fontA);
	}

	public void draw(){
		if (millis()>nextReadTime){
			readStats();
			nextReadTime = millis()+readInterval;
		}
		
		
		background(40,40,40);
		
		noStroke();
		fill(60,80,80);
		rect(10,10,width-20,60);
		fill(100,180,180);
		rect(10,10,map(netvol, 0, 1, 0, width-20),60);
		
		fill(80,80,60);
		rect(10,80,width-20,60);
		fill(180,180,100);
		rect(10,80,map(cpuvol, 0, 1, 0, width-20),60);

		/*
		float gearA = map(netvol, 0, 1, 0,TWO_PI);
		float gearB = map(cpuvol, 0, 1, 0,TWO_PI);
		
		//draw two knobs
		imageMode(CENTER);
		
		pushMatrix();
		translate(225,40);
		rotate(gearA);
		image(gear,0,0);
		popMatrix();
		
		pushMatrix();
		translate(225,110);
		rotate(gearB);
		image(gear,0,0);
		popMatrix();
		*/
		
		//draw text
		fill(255);
		text("Network Chime Volume",20,40+9);
		text("CPU Usage Buzz Volume",20,110+9);

		text(((int)(netvol*100))+"%",260,40+9);
		text(((int)(cpuvol*100))+"%",260,110+9);
		
		//draw more info button. (opens html file)
		
	}

	public void mousePressed(){
		// did we click one of the boxes?
		// which one?
		if ( mouseX>10 && mouseX<(width-10) ){
			if ( mouseY>10 && mouseY<70 ){
				boxClicked = 1;
			} else if ( mouseY>80 && mouseY<140 ){
				boxClicked = 2;
			}
		}
	}
	
	public void mouseDragged(){
		if (boxClicked==1){
			netvol = constrain(map(mouseX, 0, width-10, 0, 1),0,1);
			//send message
			if (readyForMessages){
				try { netSynth.set("mul", netvol); } catch (IOException e) { e.printStackTrace(); }
			}
		} else if (boxClicked==2){
			cpuvol = constrain(map(mouseX, 0, width-10, 0, 1),0,1);
			//send message
			if (readyForMessages){
				try { cpuSynth.set("mul", cpuvol*3); } catch (IOException e) { e.printStackTrace(); }
			}
		}
	}
	
	public void mouseReleased(){
		boxClicked = 0;
	}
	
	private void readStats(){
		/////////////// TOP
		if (topChild != null){
			try {
				if (topChild.exitValue()==0){
					try {
						String line = null;
						while (true){
							line = topResults.readLine();
							if (line==null)
								break;
							String[] sides = split(line,':');
							if (sides.length >= 2){
								if (sides[0].compareTo("CPU usage")==0){
									//System.out.println(sides[1]);
									String[] three = split(sides[1],',');
									userpercent = Float.parseFloat(trim(split(three[0],'%')[0]));
									systpercent = Float.parseFloat(trim(split(three[1],'%')[0]));
									if (readyForMessages){
										cpuSynth.set("userpercent", userpercent);
										cpuSynth.set("systpercent", systpercent);
									}
									break;
								}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					topChild = null;
				}
			} catch (IllegalThreadStateException e){
				//command is simply not finished yet.
				//this is OK
			}
		} else { //topChild was null
			if (millis() >= nextTopChildStartTime){
				try {
					topChild = Runtime.getRuntime().exec("top -l 1 -n 0 -s 0");
					topResults = new BufferedReader(new InputStreamReader(topChild.getInputStream()));
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextTopChildStartTime = millis() + topInterval;
			}
		}
		///////////////
		
		/////////////// NET
		if (netChild != null){
			try {
				if (netChild.exitValue()==0){
					try {
						String line = null;
						boolean sti = true;
						while (sti){
							line = netResults.readLine();
							if (line==null)
								break;
							String[] m = split(line,' ');
							int count = 0;
							for (int i=0; i<m.length; i++){
								if (m[i].length() > 0){
									if (count==4){
										try {
											float inpackNow = Float.parseFloat(m[i]);
											if (inpackLast == -1)
												inpackLast = inpackNow;
											inpack = inpackNow - inpackLast;
											inpackLast = inpackNow;
											if (readyForMessages)
												netSynth.set("inpack", inpack);
										} catch (NumberFormatException e){}
									} else if (count==6){
										try {
											float outpackNow = Float.parseFloat(m[i]);
											if (outpackLast == -1)
												outpackLast = outpackNow;
											outpack = outpackNow - outpackLast;
											outpackLast = outpackNow;
											if (readyForMessages)
												netSynth.set("outpack", outpack);
											sti = false;
											break;
										} catch (NumberFormatException e){}
									}
									count++;
								}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					netChild = null;
				}
			} catch (IllegalThreadStateException e){
				//command is simply not finished yet.
				//this is OK
			}
		} else { //topChild was null
			if (millis() >= nextNetChildStartTime){
				try {
					netChild = Runtime.getRuntime().exec("/usr/sbin/netstat -I en1");
					netResults = new BufferedReader(new InputStreamReader(netChild.getInputStream()));
				} catch (IOException e) {
					e.printStackTrace();
				}
				nextNetChildStartTime = millis() + netInterval;
			}
		}
		///////////////
	}

	public void stop(){
		try {
			System.out.println("Killing the server...");
			server.quitAndWait();
			System.out.println("Done.");
			if (topChild != null)
				topChild.destroy();
			if (netChild != null)
				netChild.destroy();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void loadDefs(){
		for( int i = 0; i < defLocations.length; i++ ) {
			File defFile;
			try {
				defFile = new File(prefix+defLocations[i]);
				SynthDef[] sdef = SynthDef.readDefFile( defFile);
				defs[i] = sdef[0];  //stupid
			} catch( IOException e1 ) {
				System.err.println( prefix+defLocations[i] + " : " + e1.getClass().getName() +
						" : " + e1.getLocalizedMessage() );
			}
		}
	}

	// ------------- ServerListener interface -------------

	public void serverAction( ServerEvent e ){
		switch( e.getID() ) {
		case ServerEvent.RUNNING:
			try {
				initServer();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			break;

		case ServerEvent.STOPPED:
			// re-run alive thread
			final javax.swing.Timer t = new javax.swing.Timer( 1000, new ActionListener() {
				public void actionPerformed( ActionEvent e )
				{
					try {
						if( server != null ) server.startAliveThread();
					} catch( IOException e1 ) { }
				}
			});
			t.setRepeats( false );
			t.start();
			break;

		default:
			break;
		}
	}

	private void initServer()
	throws IOException
	{
		if( !server.didWeBootTheServer() ) {
			server.initTree();
			server.notify( true );
		}
		//		if( nw != null ) nw.dispose();
		nw = NodeWatcher.newFrom( server );
		grp	= Group.basicNew( server );
		nw.register( server.getDefaultGroup() );
		nw.register( grp );
		server.sendMsg( grp.newMsg() );
		netSynth = defs[0].play(grp);
		cpuSynth = defs[1].play(grp);
		readyForMessages = true;
		netSynth.set("mul", netvol);
		cpuSynth.set("mul", cpuvol*3);
	}
}
