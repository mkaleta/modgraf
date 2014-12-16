package modgraf.algorithm;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import modgraf.jgrapht.DoubleWeightedGraph;
import modgraf.jgrapht.Vertex;
import modgraf.jgrapht.edge.DirectedDoubleWeightedEdge;
import modgraf.jgrapht.edge.DirectedTripleWeightedEdge;
import modgraf.jgrapht.edge.ModgrafEdge;
import modgraf.view.Editor;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

/**
 * 
 *
 * @see ModgrafAbstractAlgorithm
 * 
 */
public class ModgrafBusackerGowenCheapestFlow extends ModgrafAbstractAlgorithm {

	private int flow;

	public ModgrafBusackerGowenCheapestFlow(Editor e) {
		super(e);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (editor.getGraphT() instanceof DirectedGraph
				&& editor.getGraphT() instanceof DoubleWeightedGraph)
			openParamsWindow();
		else
			JOptionPane.showMessageDialog(
					editor.getGraphComponent(),
					lang.getProperty("warning-wrong-graph-type")
							+ lang.getProperty("alg-mf-graph-type"),
					lang.getProperty("warning"), JOptionPane.WARNING_MESSAGE);
	}

	@Override
	public String getName() {
		return lang.getProperty("menu-algorithm-cheapest-flow");
	}

	private void createTextResult(
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> graph) {
		StringBuilder builder = new StringBuilder();
		builder.append("Najtańszy przesył w sieci otrzymano w następujący sposób: \n");
		for (DirectedTripleWeightedEdge e : graph.edgeSet()) {
			if (e.getFlow() > 0) {
				builder.append("Krawędź: " + graph.getEdgeSource(e).getName()
						+ " do " + graph.getEdgeTarget(e).getName() + ":\n");
				builder.append("Ilość przesłanych jednostek: "
						+ (int) e.getFlow() + "\n");
			}
		}

		builder.append("Koszt przesyłu:\n");
		builder.append(this.calculateCost(graph));
		editor.setText(builder.toString());
	}

	private void createGraphicalResult(
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> graph) {
		int width = 4;
		int halfWidth = 2;
		Graph<Vertex, ModgrafEdge> graphT = editor.getGraphT();

		for (DirectedTripleWeightedEdge edge : graph.edgeSet()) {
			String sourceId = edge.getSource().getId();
			String targetId = edge.getTarget().getId();

			Vertex source = null;
			Vertex target = null;
			for (Vertex v : graphT.vertexSet()) {
				if (v.getId().equals(sourceId)) {
					source = v;
				} else if (v.getId().equals(targetId)) {
					target = v;
				}
			}

			if (null == source || null == target) {
				throw new IllegalArgumentException(
						"Something wrong with finding vertices");
			}

			ModgrafEdge newEdge = graphT.getEdge(source, target);
			if (null == newEdge) {
				throw new IllegalArgumentException("Edge not found");
			}

			if (edge.getFlow() > 0) {
				changeEdgeStrokeWidth(newEdge, width);
			} else {
				changeEdgeStrokeWidth(newEdge, halfWidth);
			}

		}
		editor.getGraphComponent().refresh();
	}

	@Override
	protected void findAndShowResult() {
		try {
			DirectedGraph<Vertex, ModgrafEdge> graph = (DirectedGraph<Vertex, ModgrafEdge>) editor
					.getGraphT();

			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> newGraph = this
					.generateNewGraph(graph);

			int W = 0;

			while (W < flow) {
				SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> residualGraph = this
						.getResidualNetwork(newGraph);
				List<Vertex> bellmanFordList = this
						.getBellmanFordPath(residualGraph);

				double min = this.findSmallestNumberInPath(bellmanFordList,
						residualGraph);

				double delta = (W + min > flow) ? flow - W : min;

				this.updateGraph(bellmanFordList, residualGraph, newGraph,
						delta);

				W += delta;
			}

			calculateCost(newGraph);
			createTextResult(newGraph);
			createGraphicalResult(newGraph);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(editor.getGraphComponent(),
					lang.getProperty("message-no-solution"),
					lang.getProperty("information"),
					JOptionPane.INFORMATION_MESSAGE);
		}

	}

	private void updateGraph(
			List<Vertex> bellmanFordList,
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> residualGraph,
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> newGraph,
			double delta) {

		List<DirectedTripleWeightedEdge> edges = new LinkedList<>();

		for (int i = 0; i < bellmanFordList.size() - 1; ++i) {
			edges.add(residualGraph.getEdge(bellmanFordList.get(i),
					bellmanFordList.get(i + 1)));
		}

		for (DirectedTripleWeightedEdge e : edges) {
			Vertex v1 = e.isConsistentWithFlow() ? residualGraph
					.getEdgeSource(e) : residualGraph.getEdgeTarget(e);
			Vertex v2 = e.isConsistentWithFlow() ? residualGraph
					.getEdgeTarget(e) : residualGraph.getEdgeSource(e);

			DirectedTripleWeightedEdge edge = newGraph.getEdge(v1, v2);

			edge.setFlow(edge.getFlow()
					+ (e.isConsistentWithFlow() ? delta : -delta));
		}

	}

	private SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> getResidualNetwork(
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> graph) {
		SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> residualGraph = new SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge>(
				DirectedTripleWeightedEdge.class);

		for (Vertex vertex : graph.vertexSet()) {
			Vertex newVertex = new Vertex(vertex.getId(), vertex.getName());
			residualGraph.addVertex(newVertex);
		}
		for (DirectedTripleWeightedEdge edge : graph.edgeSet()) {
			if (edge.getFlow() < edge.getCapacity()) {
				Vertex newEdgeSource = this.findVertexInResidualGraph(
						residualGraph, edge.getSource().getId());
				Vertex newEdgeTarget = this.findVertexInResidualGraph(
						residualGraph, edge.getTarget().getId());
				DirectedTripleWeightedEdge newEdge = new DirectedTripleWeightedEdge(
						newEdgeSource, newEdgeTarget);
				newEdge.setCost(edge.getCost());
				newEdge.setCapacity(edge.getCapacity() - edge.getFlow());
				newEdge.setFlow(edge.getFlow());
				newEdge.setConsistentWithFlow(true);
				residualGraph.addEdge(newEdgeSource, newEdgeTarget, newEdge);
			}
		}

		for (DirectedTripleWeightedEdge edge : graph.edgeSet()) {
			if (edge.getFlow() > 0) {
				Vertex newEdgeSource = this.findVertexInResidualGraph(
						residualGraph, edge.getSource().getId());
				Vertex newEdgeTarget = this.findVertexInResidualGraph(
						residualGraph, edge.getTarget().getId());
				DirectedTripleWeightedEdge newEdge = new DirectedTripleWeightedEdge(
						newEdgeTarget, newEdgeSource);
				newEdge.setCost(edge.getCost());
				newEdge.setCapacity(edge.getFlow());
				newEdge.setFlow(edge.getFlow());
				newEdge.setConsistentWithFlow(false);
				residualGraph.addEdge(newEdgeTarget, newEdgeSource, newEdge);
			}
		}
		return residualGraph;
	}

	private Vertex findVertexInResidualGraph(
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> newGraph,
			String id) {
		for (Vertex vertex : newGraph.vertexSet()) {
			if (vertex.getId().equals(id))
				return vertex;
		}
		throw new IllegalArgumentException("Vertex not found");
	}

	@SuppressWarnings("static-access")
	private List<Vertex> getBellmanFordPath(
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> graph) {
		BellmanFordShortestPath<Vertex, DefaultWeightedEdge> bf;

		SimpleDirectedGraph<Vertex, DefaultWeightedEdge> g = new SimpleDirectedGraph<Vertex, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);

		for (Vertex vertex : graph.vertexSet())
			g.addVertex(vertex);

		for (DirectedTripleWeightedEdge e : graph.edgeSet()) {
			DefaultWeightedEdge defaultWeightedEdge = new DefaultWeightedEdge();
			g.addEdge(e.getSource(), e.getTarget(), defaultWeightedEdge);
			g.setEdgeWeight(defaultWeightedEdge, e.getCost());
		}

		bf = new BellmanFordShortestPath<Vertex, DefaultWeightedEdge>(g,
				startVertex);
		List<DefaultWeightedEdge> list = bf.findPathBetween(g, startVertex,
				endVertex);

		if (list.size() == 0)
			throw new IllegalArgumentException(
					"Bellman-Ford algorithm went wrong");

		List<Vertex> vertices = new LinkedList<>();
		for (DefaultWeightedEdge e : list)
			vertices.add(g.getEdgeSource(e));

		vertices.add(endVertex);
		return vertices;
	}

	private double findSmallestNumberInPath(
			List<Vertex> bellmanFordList,
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> residualGraph) {
		double min = Double.MAX_VALUE;
		for (int i = 0; i < bellmanFordList.size() - 1; ++i) {
			DirectedTripleWeightedEdge edge = residualGraph.getEdge(
					bellmanFordList.get(i), bellmanFordList.get(i + 1));
			if (edge.getCapacity() < min)
				min = edge.getCapacity();
		}
		return min;
	}

	private int calculateCost(
			SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> graph) {
		int cost = 0;
		for (DirectedTripleWeightedEdge e : graph.edgeSet()) {
			cost += e.getCost() * e.getFlow();
		}
		System.out.println("KOSZT");
		System.out.println(cost);
		return cost;
	}

	private SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> generateNewGraph(
			DirectedGraph<Vertex, ModgrafEdge> graph) {

		SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge> newGraph = new SimpleDirectedWeightedGraph<Vertex, DirectedTripleWeightedEdge>(
				DirectedTripleWeightedEdge.class);

		for (Vertex vertex : graph.vertexSet()) {
			newGraph.addVertex(new Vertex(vertex.getId(), vertex.getName()));
		}

		for (ModgrafEdge e : graph.edgeSet()) {
			DirectedTripleWeightedEdge newEdge = new DirectedTripleWeightedEdge(
					e.getSource(), e.getTarget());
			newEdge.setFlow(0);
			double capacity = ((DirectedDoubleWeightedEdge) e).getCapacity();
			double cost = ((DirectedDoubleWeightedEdge) e).getCost();
			newEdge.setCapacity(capacity);
			newEdge.setCost(cost);
			newGraph.addEdge(e.getSource(), e.getTarget(), newEdge);
		}
		return newGraph;
	}

	protected void createParamsWindow() {
		JPanel paramsPanel = createParamsPanel();
		frame = new JFrame(lang.getProperty("frame-algorithm-params"));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setPreferredSize(new Dimension(200, 170));
		frame.add(paramsPanel);
		frame.pack();
		frame.setLocationRelativeTo(editor.getGraphComponent());
		frame.setVisible(true);
	}

	protected JPanel createParamsPanel() {
		JPanel paramsPanel = new JPanel();
		paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.Y_AXIS));
		Vector<Vertex> vertexVector = new Vector<>(editor.getGraphT()
				.vertexSet());
		startVertexComboBox = new JComboBox<>(vertexVector);
		endVertexComboBox = new JComboBox<>(vertexVector);

		paramsPanel.add(new JLabel(lang.getProperty("label-start-vertex")));
		paramsPanel.add(startVertexComboBox);
		paramsPanel.add(new JLabel(lang.getProperty("label-end-vertex")));
		paramsPanel.add(endVertexComboBox);
		paramsPanel.add(new JLabel(lang.getProperty("label-expected-flow")));
		final JTextField flowField = new JTextField();
		paramsPanel.add(flowField);
		JButton start = new JButton(lang.getProperty("button-run-algorithm"));
		start.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ModgrafBusackerGowenCheapestFlow.this.flow = Integer
						.parseInt(flowField.getText());
				startActionButton();
			}
		});
		paramsPanel.add(start);
		paramsPanel.invalidate();
		return paramsPanel;
	}
}
