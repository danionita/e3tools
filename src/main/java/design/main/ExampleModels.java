package design.main;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.mxgraph.model.mxGraphModel;

public class ExampleModels {
	public static class SmallTricky extends AbstractAction {
		private Main main;

		public SmallTricky(Main main) {
			super("Small tricky model");
			
			this.main = main;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			E3Graph graph = main.getCurrentGraph();
			mxGraphModel model = (mxGraphModel) graph.getModel();
			
			ToolComponent tc = Main.globalTools;
			
			Object tlBottom, blTop;
			
			model.beginUpdate();
			try {
				System.out.println("Adding small tricky graph");
				Object tl = graph.addActor(100, 100);
				Object bl = graph.addActor(100, 250);
				
				tlBottom = graph.addValueInterface(tl, 30, 50);
				blTop = graph.addValueInterface(bl, 30, 0);
				
				graph.connectVE(tlBottom, blTop);
				graph.connectVE(blTop, tlBottom);
				
				Object ss = graph.addStartSignal(bl, 20, 20);
				Object es = graph.addEndSignal(tl, 20, 20);
				
				graph.connectCE(ss, blTop);
				graph.connectCE(tlBottom, es);

//				System.out.println("Pre-commit: ");
//				Base info = (Base) ((mxCell) model.getChildAt(tlBottom, 0)).getValue();
//				System.out.println("SUID of ValuePort " + 0 + ": " + info.getSUID());
//				info = (Base) ((mxCell) model.getChildAt(tlBottom, 1)).getValue();
//				System.out.println("SUID of ValuePort " + 1 + ": " + info.getSUID());
//				info = (Base) ((mxCell) model.getChildAt(blTop, 0)).getValue();
//				System.out.println("SUID of ValuePort " + 0 + ": " + info.getSUID());
//				info = (Base) ((mxCell) model.getChildAt(blTop, 1)).getValue();
//				System.out.println("SUID of ValuePort " + 1 + ": " + info.getSUID());
			} finally {
				model.endUpdate();
			}
		}
	}
	
	public static class MediumTricky extends AbstractAction {
		private Main main;

		public MediumTricky(Main main) {
			super("Medium tricky model");
			this.main = main;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			E3Graph graph = main.getCurrentGraph();
			mxGraphModel model = (mxGraphModel) graph.getModel();
			
			ToolComponent tc = Main.globalTools;
			
			Object root = graph.getDefaultParent();
			
			model.beginUpdate();
			try {
				Object tl = graph.addActor(100, 100);
				Object bl = graph.addActor(100, 250);
				Object tr = graph.addActor(250, 100);
				
				Object tlBottom = graph.addValueInterface(tl, 30, 50);
				Object blTop = graph.addValueInterface(bl, 30, 0);
				Object tlRight = graph.addValueInterface(tl, 50, 30);
				Object trLeft = graph.addValueInterface(tr, 0, 30);
				
				graph.connectVE(tlBottom, blTop);
				graph.connectVE(blTop, tlBottom);
				graph.connectVE(tlRight, trLeft);
				graph.connectVE(trLeft, tlRight);
				
				Object ss = graph.addStartSignal(bl, 20, 20);
				Object es =  graph.addEndSignal(tr, 20, 20);
				
				graph.connectCE(ss, blTop);
				graph.connectCE(tlBottom, tlRight);
				graph.connectCE(trLeft, es);
			} finally {
				model.endUpdate();
			}	
		}
	}
}
