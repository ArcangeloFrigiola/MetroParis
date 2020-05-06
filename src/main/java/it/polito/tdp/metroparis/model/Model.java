package it.polito.tdp.metroparis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

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
		for(Fermata fp: this.fermate) {
			for(Fermata fa: this.fermate) {
				if(dao.fermateConnesse(fp, fa)) { //se esiste una connesione, aggiungo l'arco (condizione di if attraverso una query SQL)
					this.graph.addEdge(fp, fa);
				}
			}
		}
		
		//CREAZIONE DEGLI ARCHI - Metodo 2
		/**
		 * Preferibile se il grado medio dei nodi è basso rispetto al numero dei vertici
		 * aka se la densità è bassa.
		 * Altrimenti, non ci guadagnamo nulla
		 */
		for(Fermata fp: this.fermate) {//Itera N volte, con N numero di vertici
			List<Fermata> connesse = dao.fermateSuccessive(fp, fermateIdMap); //mi faccio resituire una lista di tutte le fermate adiacenti ad fp
			
			for(Fermata fa: connesse) { //itera tante volte quante è l'outdegree del nodo
				this.graph.addEdge(fp, fa);
			}
		}
		
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
		
		System.out.println(this.graph);
		System.out.format("Grafo caricato con %d vertici e %d archi", this.graph.vertexSet().size(), this.graph.edgeSet().size());
		
	}
	
	//Costruisco un main a scopo di debug, per controllare che il grafo sia effettivamente creato
	public static void main(String args[]) {
		Model m = new Model();
	}
}
