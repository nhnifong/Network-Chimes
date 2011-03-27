import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import de.sciss.jcollider.Group;
import de.sciss.jcollider.NodeWatcher;
import de.sciss.jcollider.Server;
import de.sciss.jcollider.ServerEvent;
import de.sciss.jcollider.ServerListener;
import de.sciss.jcollider.Synth;
import de.sciss.jcollider.SynthDef;
import de.sciss.jcollider.UGenInfo;
import de.sciss.jcollider.gui.ServerPanel;
import de.sciss.net.OSCMessage;
import processing.core.PApplet;
import javax.swing.JFrame;

import org.omg.CORBA.portable.InputStream;

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
	float cpuvol = 1.1f;
	float netvol = 0.03f;
	float inpackLast = -1;
	float outpackLast = -1;
	Process topChild;
	BufferedReader topResults;
	float nextTopChildStartTime = 0;
	float topInterval = 200;
	Process netChild;
	BufferedReader netResults;
	float nextNetChildStartTime = 0;
	float netInterval = 200;

	public void setup(){
		size(200,200);

		try {
			server = new Server("localhost");

			UGenInfo.readBinaryDefinitions();

			File f = new File("../sc/scsynth");
			String synthPath = f.getAbsolutePath();
			//System.out.println(synthPath);
			Server.setProgram(synthPath);
			server.addListener( this );
			try {
				//server.boot();
				server.start();
				server.startAliveThread();
			}
			catch( IOException e1 ) {  }
			// feedback window
			spf = ServerPanel.makeWindow( server, ServerPanel.MIMIC | ServerPanel.CONSOLE | ServerPanel.DUMP );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//load the defs from files
		//to make new files, see the unused UGenCreator class.
		loadDefs();
		
		//setup font
	}

	public void draw(){
		readStats();
		background(40,40,40);
		
		//draw two knobs
		
		//draw text "Network Chimes volume"
		//draw text "CPU Buzz volume"
		
		//draw more info button. (opens html file)
		
		
	}

	public void mousePressed(){

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
		File[] defFiles = new File[]{ new File("../sc/JNetChimes"), new File("../sc/JCpuBuzz") };
		SynthDef[] sdef;
		for( int i = 0; i < defFiles.length; i++ ) {
			try {
				sdef = SynthDef.readDefFile( defFiles[ i ]);
				defs[i] = sdef[0];  //stupid
			} catch( IOException e1 ) {
				System.err.println( defFiles[ i ].getName() + " : " + e1.getClass().getName() +
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
		cpuSynth.set("mul", cpuvol);
	}
}
