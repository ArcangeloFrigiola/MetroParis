package it.polito.tdp.metroparis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import it.polito.tdp.metroparis.db.MetroDAO;

public class Model {

	/**
	 * Rappresentiamo il grafo
	 */
	
	/*
	 * SCEGLIERE UN METODO E COMMENTARE GLI ALTRI PER ESEGUIRE IL PROGRAMMA!
	 */
	private Graph<Fermata, DefaultEdge> graph;
	private List<Fermata> fermate;
	private Map<Integer, Fermata> fermateIdMap;
	
	public Model(){
		this.graph = new SimpleDirectedGraph<>(DefaultEdge.class);
		MetroDAO dao = new MetroDAO();
		
		//CREAZIONE DEI VERTICI (corrispondenti alle fermate)
		this.fermate = dao.getAllFermate();
		this.fermateIdMap = new HashMap<>();
		
		for(Fermata f: this.fermate) {
			this.fermateIdMap.put(f.getIdFermata(), f);
		}
		
		Graphs.addAllVertices(this.graph, this.fermate); //senza bisogno di ciclare
		
		
		//CREAZIONE DEGLI ARCHI - Metodo 1
		/**
		 * Chiedo al DAO se per ogni coppia di vertici esista o meno una connessione,
		 * operazione semplice, ma forse non la migliore (MOLTO LENTO, circa 5min)
		 */
		/*for(Fermata fp: this.fermate) {
			for(Fermata fa: this.fermate) {
				if(dao.fermateConnesse(fp, fa)) { //se esiste una connesione, aggiungo l'arco (condizione di if attraverso una query SQL)
					this.graph.addEdge(fp, fa);
				}
			}
		}*/
		
		//CREAZIONE DEGLI ARCHI - Metodo 2
		/**
		 * Preferibile se il grado medio dei nodi è basso rispetto al numero dei vertici
		 * aka se la densità è bassa.
		 * Altrimenti, non ci guadagnamo nulla
		 */
		/*for(Fermata fp: this.fermate) {//Itera N volte, con N numero di vertici
			List<Fermata> connesse = dao.fermateSuccessive(fp, fermateIdMap); //mi faccio resituire una lista di tutte le fermate adiacenti ad fp
			
			for(Fermata fa: connesse) { //itera tante volte quante è l'outdegree del nodo
				this.graph.addEdge(fp, fa);
			}
		}*/
		
		//CREAZIONE DEGLI ARCHI - Metodo 3
		/**
		 * Chiedo al DB l'elenco degli archi
		 * Creaiamo direttamente gli archi che ci servono
		 * Chiedo al DAO: "Dammi un lista di coppie di fermate", poi itero su tale lista
		 * e la aggiungo a grap tramite addEdge
		 * 
		 * Questo metodo è efficace nel caso in cui il DB sia ben orgranizzato in modo
		 * da fornire velocemente tutti gli archi. Si è dovuto lavorare un po' in Java 
		 * per creare la mappa, ect
		 */
		
		List<CoppiaFermate> coppie = dao.coppieFermate(fermateIdMap);
		for(CoppiaFermate c: coppie) {
			this.graph.addEdge(c.getFp(), c.getFa());
		}
		
		//System.out.println(this.graph);
		System.out.format("Grafo caricato con %d vertici e %d archi\n", this.graph.vertexSet().size(), this.graph.edgeSet().size());
		
	}
	
	/**
	 * Visita l'intero grafo con la strategia Breadth First
	 * e ritorna l'insieme dei vertici incontrati
	 * @param source vertice di partenza della visita
	 * @return insieme dei vertici incontrati
	 */
	public List<Fermata> visitaAmpiezza(Fermata source){
		
		GraphIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(graph, source); 
		//si posiziona sul primo elemento del grafo che gli passiamo
		
		List<Fermata> visit = new ArrayList<>();
		
		while(bfv.hasNext()) {
			visit.add(bfv.next());
		}
		
		return visit;
	}
	
	public List<Fermata> visitaProfondita(Fermata source) {

		GraphIterator<Fermata, DefaultEdge> dfv = new DepthFirstIterator<>(graph, source);
		// si posiziona sul primo elemento del grafo che gli passiamo

		List<Fermata> visit = new ArrayList<>();

		while (dfv.hasNext()) {
			visit.add(dfv.next());
		}

		return visit;
	}
	
	public Map<Fermata, Fermata>alberoVisita(Fermata source) {
		
		Map<Fermata, Fermata> albero = new HashMap<>();
		//Creato l'albero, la PRIMA OPERAZIONE DA FARE è aggiungere la sorgente a mano!
		albero.put(source, null);
		
		GraphIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(graph, source); 
		
		bfv.addTraversalListener(new TraversalListener<Fermata,DefaultEdge>(){

			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
				
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
				
			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultEdge> e) {
				
				/*
				 * la visita sta considerando un'arco
				 * questo arco ha scoperto un nuovo vertice?
				 * Se si, partendo da dove?
				 */
				DefaultEdge edge = e.getEdge(); //(a,b): ho scoperto 'a' partendo da 'b', oppure ho scoperto 'b' da 'a'?
				Fermata a = graph.getEdgeSource(edge);
				Fermata b = graph.getEdgeTarget(edge);
				if(albero.containsKey(a) && !albero.containsKey(b)) {
					//Ho scoperto 'b'
					albero.put(b, a);
				}else {
					//'a' non era già noto, probabilmente conoscevo b
					albero.put(a, b);
				}
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Fermata> e) {
				
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Fermata> e) {
				
			}
			
		});
		
		while(bfv.hasNext()) {
			bfv.next(); //estrai l'elemento e ignoralo
			//questa operazione popola l'albero
		}
		
		return albero;
		
		
	}
	
	/*
	 * Cerco i cammini minimi attraverso Dijkstra, partendo da una fermata "partenza"
	 */
	public List<Fermata> camminiMinimi(Fermata partenza, Fermata arrivo) {
		
		DijkstraShortestPath<Fermata, DefaultEdge> dij = new DijkstraShortestPath<>(graph);
		
		GraphPath<Fermata, DefaultEdge> cammino = dij.getPath(partenza, arrivo);
		
		return cammino.getVertexList();
	}
	
	//Costruisco un main a scopo di debug, per controllare che il grafo sia effettivamente creato
	public static void main(String args[]) {
		Model m = new Model();
		
		List<Fermata> visita1 = m.visitaAmpiezza(m.fermate.get(0)); //PORCATA, solo a scopo di debug
		//System.out.println(visita1);
		List<Fermata> visita2 = m.visitaProfondita(m.fermate.get(0)); //PORCATA, solo a scopo di debug
		//System.out.println(visita2);
		
		Map<Fermata, Fermata> albero = m.alberoVisita(m.fermate.get(0));
		for(Fermata f: albero.keySet()) {
			//System.out.format("%s <- %s\n", f, albero.get(f));
		}
		
		List<Fermata> cammino = m.camminiMinimi(m.fermate.get(0), m.fermate.get(1));
		System.out.println(cammino);
	}
}
