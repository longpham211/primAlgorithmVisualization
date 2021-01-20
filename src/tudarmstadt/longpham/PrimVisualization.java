/*
 * @Author: Thanh Long Pham
 * @Date: 10/06/2017
 * @University: TU Darmstadt
 * 
 */

package tudarmstadt.longpham;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JScrollPane;


//Most of the code for drawing vertices and edges at a specific location is modified 
//based on GraphPanel example of John B. Matthews
//https://sites.google.com/site/drjohnbmatthews/graphpanel
public class PrimVisualization {
	public static void main(String[] args){
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				JFrame f = new JFrame("Prim Visualization");
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				GraphPanel gp = new GraphPanel();
				f.add(gp.control, BorderLayout.NORTH);
				f.add(new JScrollPane(gp), BorderLayout.CENTER);
				f.getRootPane().setDefaultButton(gp.control.defaultButton);
				f.pack();
				f.setLocationByPlatform(true);
				f.setVisible(true);
			}			
		});
	}
}
