/*
 * @Author: Thanh Long Pham
 * @Date: 10/06/2017
 * @University: TU Darmstadt
 * 
 */

package tudarmstadt.longpham;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

//Most of the code for drawing vertices and edges at a specific location is modified 
//based on GraphPanel example by John B. Matthews
//https://sites.google.com/site/drjohnbmatthews/graphpanel

public class GraphPanel extends JComponent{
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int SPEED = 5000;
	private static final Random rnd = new Random();
	
	protected ControlPanel control = new ControlPanel();
	
	private List<Vertex> vertices = new ArrayList<Vertex>();
	private List<Vertex> selectedVertices = new ArrayList<Vertex>();
	private List<Edge> edges = new ArrayList<Edge>();
	
	private int speed = SPEED;
	private Point mousePt = new Point(WIDTH /2, HEIGHT / 2);
	private Rectangle mouseRect = new Rectangle();
	private boolean selecting = false;
	private boolean runningAlgorithm = false;
	
	private Timer timer;
	
	protected GraphPanel(){
		this.setOpaque(true);
		this.addMouseListener(new MouseHandler());
		this.addMouseMotionListener(new MouseMotionHandler());		
	}
	
	@Override
	public Dimension getPreferredSize(){
		return new Dimension(WIDTH, HEIGHT);
	}
	
	@Override
	public void paintComponent(Graphics g){
		
		g.setColor(new Color(0x00f0f0f0));
		g.fillRect(0, 0, getWidth(), getHeight());		
		for(Edge e : edges) {
			e.draw(g);
		}
		for(Vertex v : vertices) {
			v.draw(g);
		}
		if(selecting) {
			g.setColor(Color.DARK_GRAY);
			g.drawRect(mouseRect.x, mouseRect.y, mouseRect.width, mouseRect.height);
		}
	}
	
	private class MouseHandler extends MouseAdapter {
		
		@Override
		public void mouseReleased(MouseEvent e) {
			selecting = false;
			mouseRect.setBounds(0, 0, 0, 0);
			if(e.isPopupTrigger()) {
				showPopup(e);
			}
			e.getComponent().repaint();
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			mousePt = e.getPoint();
			if(e.isControlDown()) {
				Vertex.selectToggle(vertices, mousePt);
			} else if (e.isPopupTrigger()) {
				Vertex.selectOne(vertices, mousePt);
				showPopup(e);
			} else if (Vertex.selectOne(vertices,  mousePt) != -1) {
				selecting = false;
			} else {
				Vertex.selectNone(vertices);
				selecting = true;
			}
			e.getComponent().repaint();
		}
		
		private void showPopup(MouseEvent e) {
			if(!runningAlgorithm)
				control.popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	private class MouseMotionHandler extends MouseMotionAdapter {
		
		Point delta = new Point();
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if(selecting) {
				mouseRect.setBounds(
						Math.min(mousePt.x, e.getX()),
						Math.min(mousePt.y, e.getY()),
						Math.abs(mousePt.x - e.getX()),
						Math.abs(mousePt.y - e.getY()));
				Vertex.selectRect(vertices, mouseRect);
			} else {
				delta.setLocation(
						e.getX() - mousePt.x,
						e.getY() - mousePt.y);
				Vertex.updatePosition(vertices, delta);
				mousePt = e.getPoint();
			}
			e.getComponent().repaint();
		}
	}

	protected class ControlPanel extends JToolBar {
		
		private Action newVertex = new NewVertexAction("New Vertex");
		private Action clearAll = new ClearAction("Clear");
		private Action random = new RandomAction("Random");
		private Action delete = new DeleteAction("Delete");
		private Action connect = new ConnectAction("Connect");
		private Action startVertex = new StartVertexAction("Start Vertex");
		private Action findMST = new FindMSTAction("Find MST");
		private Action stop = new StopAction("Stop");
		
		protected JButton defaultButton = new JButton(newVertex);
		private JButton clearAllButton = new JButton(clearAll);
		private JButton randomButton = new JButton(random);
		private JButton connectButton = new JButton(connect);
		private JButton startVertexButton = new JButton(startVertex);
		private JSpinner js = new JSpinner();
		private JButton findMSTButton = new JButton(findMST);
		private JButton stopButton = new JButton(stop);		
		private JPopupMenu popup = new JPopupMenu();
		
		protected ControlPanel(){
			this.setLayout(new FlowLayout(FlowLayout.LEFT));
			this.setBackground(Color.LIGHT_GRAY);
			
			this.add(defaultButton);
			this.add(clearAllButton);
			this.add(randomButton);
			this.add(connectButton);
			this.add(startVertexButton);
            
            js.setModel(new SpinnerNumberModel(SPEED, 1000, 10000, 1000));
            js.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSpinner s = (JSpinner) e.getSource();
                    speed = (Integer) s.getValue();
                }
            });
            this.add(new JLabel("Speed:"));
            this.add(js);			
			this.add(findMSTButton);
			this.add(stopButton);
			
			stopButton.setEnabled(false);
			
			popup.add(new JMenuItem(newVertex));
			popup.add(new JMenuItem(connect));
			popup.add(new JMenuItem(delete));
			popup.add(new JMenuItem(startVertex));		
		}
	}
	
	//Action for create a new vertex at mouse location
	private class NewVertexAction extends AbstractAction {
		
		public NewVertexAction(String name) {
			super(name);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Vertex.selectNone(vertices);
			Point p = mousePt.getLocation();
			Vertex v = new Vertex(p, vertices);
			v.setSelected(true);
			vertices.add(v);
			repaint();				
		}
	}
	
	//clear vertices and edges list then repaint
	private class ClearAction extends AbstractAction {
		
		public ClearAction(String name) {
			super(name);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			vertices.clear();
			edges.clear();
			repaint();
		}	
	}
	
	//Create 8 vertices and 14 edges connected vertices with weight ranging from 1 - 20
	//Then choose a vertex as start vertex.
	private class RandomAction extends AbstractAction {

		public RandomAction(String name) {
			super(name);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			//Because of the heavy task while generating a non-match edges
			//it's necessary to create the worker for avoiding delay on Event Dispatcher Thread
			RandomWorker worker = new RandomWorker();
			worker.execute();
		}
		
		//For making sure that the edge which is just generated are the same with the previous ones
		private boolean isThisEdgeGenerated(int v1, int v2, int[][] edgesMatrix, int currentSize){
			for(int i = 0 ; i < currentSize; i++)
				if(v1 == edgesMatrix[i][0] && v2 == edgesMatrix[i][1]
				|| v1 == edgesMatrix[i][1] && v2 == edgesMatrix[i][0])
					return true;
			return false;
		}
		
		private class RandomWorker extends SwingWorker {
			@Override
			protected Object doInBackground() throws Exception {
				vertices.clear();
				edges.clear();
				
				Random rnd = new Random();
				//Generate 8 vertices
				for (int i = 0 ; i < 8; i++) {
					Point p = new Point(rnd.nextInt(getWidth()), rnd.nextInt(getHeight()));
					vertices.add(new Vertex(p, vertices));
				}
				
				//Choose a start Vertex
				int id = rnd.nextInt(vertices.size()); 
				vertices.get(id).becomeStartVertex(vertices);
				
				//Generate 14 edges
				int[][] edgesMatrix = new int[14][2];
				int temp;
				System.out.println(edgesMatrix.length);
				for(int i = 0 ; i < edgesMatrix.length; i ++) {
					edgesMatrix[i][0] = rnd.nextInt(vertices.size());
					
					//For making sure that edges connect 2 different vertices and the new generating edges aren't generated before
					temp = rnd.nextInt(vertices.size());				
					while(temp == edgesMatrix[i][0] || isThisEdgeGenerated(edgesMatrix[i][0], temp, edgesMatrix, i)) {
						temp = rnd.nextInt(vertices.size());
					}
					edgesMatrix[i][1] = temp;
					
					//Add a new edge generating by EdgesMatrix whose weight is a random number from 1 to 20
					edges.add(new Edge(vertices.get(edgesMatrix[i][0]), vertices.get(edgesMatrix[i][1]), rnd.nextInt(20) + 1));
				}
				return null;
			}
			
			//When heavy task is done, repaint the frame.
			@Override
			protected void done(){
				super.done();
				repaint();
			}
			
		}
	}
	
	//For delete use case.
	private class DeleteAction extends AbstractAction {
		
		public DeleteAction(String name) {
			super(name);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			ListIterator<Vertex> iter = vertices.listIterator();
			while (iter.hasNext()) {
				Vertex v = iter.next();
				
				//Just processing with the vertices which are selected.
				if(v.isSelected()) {
					
					//Delete association edges
					deleteEdges(v);
					
					//Delete vertices
					iter.remove();
				}
			}
			//After deleting all vertices and association edges, repaint the frame.
			repaint();
		}	
		
		private void deleteEdges(Vertex v) {
			ListIterator <Edge> iter = edges.listIterator();
			while(iter.hasNext()) {
				Edge e = iter.next();
				if(e.v1 == v || e.v2 == v)
					iter.remove();
			}
		}
	}
	
	
	//For connect 2 vertices use case.
	private class ConnectAction extends AbstractAction {

		public ConnectAction(String name) {
			super(name);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Vertex.getSelected(vertices, selectedVertices);
			if(selectedVertices.size() == 2) {
				try{
					int weight = Integer.parseInt(
							JOptionPane.showInputDialog(GraphPanel.this, "Input the weight of edge:"));
					edges.add(new Edge(selectedVertices.get(0), selectedVertices.get(1), weight));
				} catch(NumberFormatException ex){
					//In the case that user enter a non - numeric character, show dialog for warning user.
					JOptionPane.showMessageDialog(GraphPanel.this, "Please enter the number only!");
				}
				
			}
			//Repaint the new edge and its weight
			repaint();
		}
	}
	
	
	//For choosing a vertex as start vertex use case.
	private class StartVertexAction extends AbstractAction {
		
		public StartVertexAction(String name) {
			super(name);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			int selectedVertexPosition = Vertex.selectOne(vertices, mousePt);
			
			//Copy the selected vertices from vertices list to selectedVertices list.
			Vertex.getSelected(vertices, selectedVertices);
			
			if (selectedVertices.size() > 1) {
				//For the case that user chooses more than over vertex
				JOptionPane.showMessageDialog(
						GraphPanel.this, 
						"You are choosing more than one vertex. \nPlease choose just one vertex to become the start vertex!");			
			} else if (selectedVertexPosition == -1){
				//For the case that user hasn't chosen any vertex yet.
				JOptionPane.showMessageDialog(GraphPanel.this, "Please choose a vertex first!");
			} else {
				//If everything is okay, set the selected vertex to the start vertex by saving information and changing its color to green.
				vertices.get(selectedVertexPosition).becomeStartVertex(vertices);
			}
			//After all, repaint
			repaint();
		}	
	}
	
	
	//For finding MST use case
	
	private class FindMSTAction extends AbstractAction {
		private int[][] adjacencyMatrix;		
		
		public FindMSTAction(String name) {
			super(name);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Vertex startVertex = Vertex.findStartVertex(vertices);
			
			if(startVertex == null)
				//Check that, start vertex is selected already.
				JOptionPane.showMessageDialog(GraphPanel.this, "Plesae choose the start vertex first!");
			else {
				//If algorithm is fine to run, then process Initialization
				//by constructing an adjacency matrix, disabling other functions, showing stop button and disabling showing popup
				//Initialization part also set vertices' keys to INT_MAX(can be considered as infinite) and vertices' parents to null.
				primInitialization();
				
				//Set the key of the start vertex to be 0
				startVertex.key = 0;
				
				//Create a priority queue for faster executing Prim's algorithm.
				MinPriorityQueue queue = new MinPriorityQueue(vertices);		

				
				//Because of pausing the program for observing how algorithm is executed,
				//We must use the timer in order to avoid blocking Event Dispatcher Thread paints the swing components
				timer = new Timer(speed, new ActionListener() {
					ArrayList<Edge> needRepaintEdges = new ArrayList<Edge>();
					Vertex processingVertex;
					Vertex nextMinumumVertex;
					Edge nextMinimumEdge;
					
					@Override
					public void actionPerformed(ActionEvent e) {
						if(queue.isEmpty()) {	
							//When min-priority queue is empty, it means the algorithm is finished
							//set the last vertex and its light edge to Green
							//repaint the frame, shows the complete message and stop the timer.
							
							nextMinumumVertex.setVertexColor(Color.GREEN);
							nextMinimumEdge.setEdgeColor(Color.GREEN);							
							repaint();
							JOptionPane.showMessageDialog(GraphPanel.this, "Prim's algorithm is completed!");
							
							((Timer) e.getSource()).stop();
						} else {
							//Repaint the color of previous processing painted blue edges
							for(Edge edge: needRepaintEdges){
								if(edge!= nextMinimumEdge && needRepaintEdges.size() != 1){
									edge.setEdgeColor(Color.BLACK);
									edge.v1.setVertexColor(Color.BLACK);
								}
							}
							needRepaintEdges.clear();
							
							
							//After each iterator, a vertex with the minimum key is extracted.
							processingVertex = queue.heapExtractMin();
						
							//Set the color of this current processing vertex to green.
							processingVertex.setVertexColor(Color.GREEN);		
							
							if(processingVertex.parent != null){
								//If this vertex isn't the first selected vertex or isn't the vertex which has no connected edges
								//Then set the color of the edge connected it with its parent to green.
								Edge.findEdgeInList(processingVertex.vertexID, processingVertex.parent.vertexID, edges).setEdgeColor(Color.GREEN);
							}
							
							
							//this loop is for updating keys and parents information of remained vertices which aren't added to 
							//the tree yet. This is for making sure that, in the next iteration of while loop above, a vertex
							//with the correct minimum key is extracted.
							for(int i = 0; i< adjacencyMatrix[0].length; i++)
							if(adjacencyMatrix[processingVertex.getVertexID()][i] != 0) {
								int vertexPos = Vertex.findVertexInList(i, vertices);
								if(queue.contains(vertices.get(vertexPos).vertexID) && adjacencyMatrix[processingVertex.getVertexID()][i] < vertices.get(vertexPos).key){
									vertices.get(vertexPos).parent = processingVertex;
									vertices.get(vertexPos).key = adjacencyMatrix[processingVertex.vertexID][i];
									queue.buildMinHeap();
								}
							}
							
							//Change the color of edges and vertices connected the tree with remained vertices to BLUE
							int heapSize = queue.size();
							int j;
							Edge edge;					
							
							for(int i = 0; i< heapSize; i ++)
								for(j = heapSize, edge = null; j< vertices.size(); j++){
									edge = Edge.findEdgeInList(vertices.get(i).vertexID, vertices.get(j).vertexID, edges);
									if(edge != null) {
										needRepaintEdges.add(edge);
										edge.setEdgeColor(Color.BLUE);
										edge.v1.setVertexColor(Color.BLUE);
									}
								}
							
							
							//Change the color of the minimum key vertex and its edge to its parent to RED
							if(heapSize > 0){
								nextMinumumVertex = queue.heapMinimum();
								nextMinumumVertex.setVertexColor(Color.RED);
								if(nextMinumumVertex.parent != null){
									nextMinimumEdge = Edge.findEdgeInList(nextMinumumVertex.vertexID, nextMinumumVertex.parent.vertexID, edges);
									nextMinimumEdge.setEdgeColor(Color.RED);
								}
								repaint();
							}
						}
					}
				});
				timer.setRepeats(true);
				
				// 2 ticks of timer have the duration of speed value.
				timer.setDelay(speed);
				timer.start();				
			}			
		}
		
		private void primInitialization() {
			for (Vertex v: vertices) {
				//Initialize the key value of each vertex is infinite
				v.key = Integer.MAX_VALUE;
				
				//Initialize the parent of each vertex is null
				v.parent = null;
			}

			buildAdjacencyMatrix();
			disableFuntions();
		}
		
		private void buildAdjacencyMatrix() {
			adjacencyMatrix = new int[Vertex.maxVertexID(vertices)+1][Vertex.maxVertexID(vertices)+1];
			for(Edge e: edges) {
				int i = e.v1.getVertexID();
				int j = e.v2.getVertexID();
				adjacencyMatrix[i][j] = adjacencyMatrix[j][i] = e.weight;
			}
		}
		
		//Disabling other functions while running algorithm but still able to move vertices.
		private void disableFuntions(){
			runningAlgorithm = true;
			GraphPanel.this.control.defaultButton.setEnabled(false);
			GraphPanel.this.control.clearAllButton.setEnabled(false);
			GraphPanel.this.control.randomButton.setEnabled(false);
			GraphPanel.this.control.connectButton.setEnabled(false);
			GraphPanel.this.control.startVertexButton.setEnabled(false);
			GraphPanel.this.control.js.setEnabled(false);
			GraphPanel.this.control.findMSTButton.setEnabled(false);
			GraphPanel.this.control.stopButton.setEnabled(true);
		}

		//For faster handling Prim's algorithm, I use min-priority queue building on a heap data structure.
		private class MinPriorityQueue {
			private int heapSize;
			private List<Vertex> vertices;			
			
			public MinPriorityQueue(List<Vertex> vertices) {
				heapSize = vertices.size();
				this.vertices = vertices;
				
				buildMinHeap();
			}
			
			private int parent(int pos) {
				return pos/2;
			} 
			
			private int left(int pos) {
				return 2*pos;
			}
			
			private int right(int pos) {
				return 2*pos + 1;
			}
			
			//For ensuring the min-heap property.
			private void minHeapify(int pos) {
				int smallest;
				int leftChild = left(pos);
				int rightChild = right(pos);
				
				if(leftChild < heapSize && vertices.get(leftChild).key < vertices.get(pos).key)
					smallest = leftChild;
				else smallest = pos;
				if(rightChild < heapSize && vertices.get(rightChild).key < vertices.get(smallest).key)
					smallest = rightChild;
				
				if (smallest != pos) {
					Collections.swap(vertices, smallest, pos);
					minHeapify(smallest);
				}
			}
			
			//Build a min-heap using vertices list.
			private void buildMinHeap() {
				for(int i = heapSize/2; i >= 0; i--)
					minHeapify(i);
			}
			
			//The vertex at position 0 of vertices is the vertex has smallest key
			//This is property of heap data structure.
			private Vertex heapMinimum() {
				return vertices.get(0);
			}
			
			//Extract the vertex has minimum key then swap the position of vertices in vertices list
			//to ensure heap property. Note that, changing the position of vertices in vertices list doesn't affect
			//the appearance of vertices in Frame because vertices are drawn by location information, not the position of them on vertices list.
			protected Vertex heapExtractMin() {
				if(heapSize < 1)
					return null;
				
				Vertex min = heapMinimum();				
				Collections.swap(vertices, 0, heapSize -1);				
				heapSize --;
				minHeapify(0);				
				//return the position of Vertex in the Arraylist
				return min;
			}
			
			protected boolean isEmpty(){
				if(heapSize == 0) 
					return true;
				return false;
			}
			
			protected int size() {
				return heapSize;
			}
			
			protected boolean contains(int vertexID) {
				for(int i = 0; i < heapSize; i++)
					if(vertices.get(i).vertexID == vertexID)
						return true;
				
				return false;
			}
		}
	}
	
	//For stop use case.
	private class StopAction extends AbstractAction {
		
		public StopAction(String name) {
			super(name);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			//stop changing color of node, then reset all colors of vertices, edges and enable other functions.
			timer.stop();
			repaintAllEdgesAndVertices();
			enableFunctions();
		}	
		
		private void repaintAllEdgesAndVertices() {
			for(Vertex v: vertices) {
				v.setVertexColor(Color.BLACK);
			}
			for(Edge e: edges) {
				e.setEdgeColor(Color.BLACK);
			}
			Vertex.findStartVertex(vertices).setVertexColor(Color.GREEN);
			repaint();
		}
		private void enableFunctions() {
			runningAlgorithm = false;
			GraphPanel.this.control.defaultButton.setEnabled(true);
			GraphPanel.this.control.clearAllButton.setEnabled(true);
			GraphPanel.this.control.randomButton.setEnabled(true);
			GraphPanel.this.control.connectButton.setEnabled(true);
			GraphPanel.this.control.startVertexButton.setEnabled(true);
			GraphPanel.this.control.js.setEnabled(true);
			GraphPanel.this.control.findMSTButton.setEnabled(true);
			GraphPanel.this.control.stopButton.setEnabled(false);		
		}
	}
	
	private static class Edge {
		
		private Vertex v1;
		private Vertex v2;
		private int weight;
		private Font edgeFont = new Font("Verdana", Font.BOLD, 15);
		private Color color = Color.DARK_GRAY;
		
		protected Edge(Vertex v1, Vertex v2, int weight) {
			this.v1 = v1;
			this.v2 = v2;
			this.weight = weight;
		}
		
		protected void draw(Graphics g) {
			Point p1 = v1.getLocation();
			Point p2 = v2.getLocation();
			g.setColor(color);
			g.drawLine(p1.x, p1.y, p2.x, p2.y);			
			g.setFont(edgeFont);
			
			//Draw the weight of edge at the middle of edge's line.
			g.drawString(weight + "", (p1.x + p2.x)/2, (p1.y + p2.y)/2);
		}
		
		protected void setEdgeColor(Color color) {
			this.color = color;
		}
		
		protected static Edge findEdgeInList(int vertexID1, int vertexID2, List<Edge> edges) {
			for (Edge e : edges) {
				if(vertexID1 == e.v1.vertexID && vertexID2 == e.v2.vertexID)
					return e;
				if( vertexID1 == e.v2.vertexID && vertexID2 == e.v1.vertexID){
					Vertex temp = e.v1;
					e.v1 = e.v2;
					e.v2 = temp;
					return e;
				}
					
			}
			return null;
		}
	}
	
	private static class Vertex {
		private int vertexID;
		private Point p;
		private int r = 17; //radius of the vertex
		private Color color;
		private Font vertexFont = new Font("Verdana", Font.BOLD, 15);
		private boolean selected = false;
		private boolean isStartVertex = false;
		
		protected int key;
		protected Vertex parent;
		
		private Rectangle b = new Rectangle();
		

		protected Vertex(Point p, List<Vertex> vertices) {
			this.p = p;
			this.vertexID = generateVertexID(vertices);
			color = Color.BLACK;
			setBoundary(b);
		}
		
		private void setBoundary(Rectangle b) {
			b.setBounds(p.x - r, p.y - r, 2 * r, 2 * r);
		}
		
		//For generating vertex ID
		private int generateVertexID(List<Vertex> vertices){
			int id = 1;
			int i;
			while(true){
				for(i = 0 ; i < vertices.size(); i ++)
					if(vertices.get(i).getVertexID() == id)
						break;
				if( i != vertices.size())
					id++;
				else 
					return id;
			}
		}
		
		protected void draw(Graphics g) {
			g.setColor(this.color);
			g.fillOval(b.x, b.y, b.width, b.height);
			if(selected) {
				g.setColor(Color.DARK_GRAY);
				g.drawRect(b.x, b.y, b.width, b.height);
			}
			drawNameOfVertex(g);
		}
		
		private void drawNameOfVertex(Graphics g){
			
			//Get the FontMetrics of Font for Vertex
			FontMetrics metrics = g.getFontMetrics(vertexFont);
			//Determine the X coordinate for the text
			int x = b.x + (b.width - metrics.stringWidth(vertexID + ""))/2;
			//Determine the Y coordinate for the text
			int y = b.y + (b.height - metrics.getHeight() /2);
			
			g.setColor(Color.WHITE);
			g.setFont(vertexFont);
			//Draw the name of Vertex inside
			g.drawString(vertexID + "", x, y);		
		}
		
		private void setVertexColor(Color color){
			this.color = color;
		}
		
		protected int getVertexID() {
			return vertexID;
		}
		
		protected Point getLocation() {
			return p;
		}
		
		private boolean contains(Point p) {
			return b.contains(p);
		}
		
		protected boolean isSelected() {
			return selected;
		}
		
		protected void setSelected(boolean selected) {
			this.selected = selected;
		}
		
		//copy all selected vertices to selected list
		protected static void getSelected(List<Vertex> list, List<Vertex> selected) {
			selected.clear();
			for (Vertex v : list) {
				if (v.isSelected()) {
					selected.add(v);
				}
			}
		}
		
		//set a vertex as start point by changing startVertex field to true and set the color of vertex to GREEN.
		private void setStartVertex(boolean startVertex) {
			this.isStartVertex = startVertex;
			if(isStartVertex)
				this.color = Color.GREEN;
			else
				this.color = Color.BLACK;
		}
		
		protected void becomeStartVertex(List<Vertex> list){
			//clean the previous start vertex information
			for(Vertex v: list) {
				v.setStartVertex(false);
			}
			this.isStartVertex = true;
			this.color = Color.GREEN;			
		}
		
		protected static Vertex findStartVertex(List<Vertex> list){
			for (Vertex v : list) {
				if(v.isStartVertex)
					return v;
			}
			return null;
		}
		
		//set selected value of all vertices in list to false
		protected static void selectNone (List<Vertex> list) {
			for (Vertex v: list) {
				v.setSelected(false);
			}
		}
		
		//set selected value of current selected vertex to true while the selected values of remainders are false.
		protected static int selectOne(List<Vertex> list, Point p) {
			for (Vertex v : list) {
				if (v.contains(p)) {
					if (!v.isSelected()) {
						Vertex.selectNone(list);
						v.setSelected(true);
					}
					return list.indexOf(v);
				}
			}			
			return -1;
		}
		
		//return a vertex in list using vertexID.
		protected static int findVertexInList(int vertexID, List<Vertex> list) {
			for (Vertex v : list) {
				if(v.vertexID == vertexID)
					return list.indexOf(v);
			}
			
			return -1;
		}
		
		//this function is for constructing the adjacency matrix of graph whose number of columns and rows are maxVertexID
		protected static int maxVertexID(List<Vertex> list) {
			int max = -1;
			for(Vertex v: list) {
				if(v.vertexID > max)
					max = v.vertexID;
			}
			
			return max;
		}
		
		protected static void selectRect(List<Vertex> list, Rectangle r) {
			for (Vertex v : list) {
				v.setSelected(r.contains(v.p));
			}
		}
		
		protected static void selectToggle(List<Vertex> list, Point p) {
			for (Vertex v : list) {
				if (v.contains(p)) {
					v.setSelected(!v.isSelected());
				}
			}
		}
		
		protected static void updatePosition(List<Vertex> list, Point d) {
			for (Vertex v: list) {
				if(v.isSelected()) {
					v.p.x += d.x;
					v.p.y += d.y;
					v.setBoundary(v.b);
				}
			}
		}
	}
}