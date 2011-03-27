import java.io.File;
import java.io.IOException;

import de.sciss.jcollider.Control;
import de.sciss.jcollider.GraphElem;
import de.sciss.jcollider.GraphElemArray;
import de.sciss.jcollider.SynthDef;
import de.sciss.jcollider.UGen;


public class UGenCreator {
	GraphElem f, g, h, k; //temps for constructing ugens
	SynthDef cpudef, netdef;
	
	public UGenCreator(){
		
		// control parameters for net chime
		Control netCtrl = Control.kr( new String[] { "inpack", "outpack", "mul" }, new float[] { 30, 30, 0.12f });
		// construct ugen for inbound packet chime
		f = UGen.ar("Ringz",
				UGen.ar("*",
						UGen.ar("Dust",
								UGen.kr("*",
										netCtrl.getChannel( "inpack" ),
										UGen.ir( 0.6f )
								)
					    ),
					    netCtrl.getChannel( "mul" )
				),
				UGen.ir( 1600.0f ),
				UGen.ir( 2.0f )
		);
		// construct ugen for outbound packet chime (slightly higher pitch)
		g = UGen.ar("Ringz",
				UGen.ar("*",
						UGen.ar("Dust",
								UGen.kr("*",
										netCtrl.getChannel( "outpack" ),
										UGen.ir( 0.6f )
								)
					    ),
					    netCtrl.getChannel( "mul" )
				),
				UGen.ir( 2100.0f ),
				UGen.ir( 2.0f )
		);
		// create the synthdef that combines them.
		// this will put the left and right chanels I think
		netdef = new SynthDef( "JNetChimes", UGen.ar( "Out", UGen.ir( 0 ), new GraphElemArray( new GraphElem[] { f, g }) ));


		// control parameters for cpu buzz
		Control cpuCtrl = Control.kr( new String[] { "userpercent", "systpercent", "mul" }, new float[] { 30, 30, 0.12f });
		// construct ugen for system cpu buzz
		h = UGen.ar("*",
				UGen.ar("LPF",
					UGen.ar( "Gendy3", //( 1, 2, 0.3, -0.7, 75, 0.03, 0.1)
							UGen.ir(  1.0f  ),
							UGen.ir(  2.0f  ),
							UGen.ir(  0.3f  ),
							UGen.ir( -0.7f  ),
							UGen.ir( 75.0f  ),
							UGen.ir(  0.03f ),
							UGen.ir(  0.1f  ),
							UGen.ir(  12.0f  ),
							UGen.ir(  12.0f  )
					),
					UGen.kr("+",
							UGen.kr("*",
									cpuCtrl.getChannel( "systpercent" ),
									UGen.ir( 30.0f )
							),
							UGen.ir( 40.0f )
					)
				),
				UGen.kr("*",
						cpuCtrl.getChannel( "userpercent" ),
						UGen.kr("*",
								cpuCtrl.getChannel( "mul" ),
								UGen.ir( 0.005f )
						)
				)
		);
		// construct ugen for user cpu buzz
		k = UGen.ar("*",
				UGen.ar("LPF",
						UGen.ar( "Gendy3", //( 1, 2, 0.3, -0.7, 75, 0.03, 0.1)
								UGen.ir(  1.0f  ),
								UGen.ir(  2.0f  ),
								UGen.ir(  0.3f  ),
								UGen.ir( -0.7f  ),
								UGen.ir( 150.0f  ),
								UGen.ir(  0.03f ),
								UGen.ir(  0.1f  ),
								UGen.ir(  12.0f  ),
								UGen.ir(  12.0f  )
						),
						UGen.kr("+",
								UGen.kr("*",
										cpuCtrl.getChannel( "userpercent" ),
										UGen.ir( 30.0f )
								),
								UGen.ir( 40.0f )
						)
					),//     /Users/nhnifong/Dropbox/workspace/Network Chimes/sc/scsynth
					UGen.kr("*",
							cpuCtrl.getChannel( "userpercent" ),
							UGen.kr("*",
									cpuCtrl.getChannel( "mul" ),
									UGen.ir( 0.005f )
							)
					)
		);
		// create synthdef for cpu sound
		cpudef = new SynthDef( "JCpuBuzz", UGen.ar( "Out", UGen.ir( 0 ), new GraphElemArray( new GraphElem[] { h, k }) ));
		 
	}
	
	public void writeDefs(){
		try {
			netdef.writeDefFile(new File("../sc/JNetChimes"));
			cpudef.writeDefFile(new File("../sc/JCpuBuzz"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
