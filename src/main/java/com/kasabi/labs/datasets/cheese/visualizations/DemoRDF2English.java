package com.kasabi.labs.datasets.cheese.visualizations;

import java.io.File;

import org.openjena.riot.RiotLoader;

import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.kasabi.labs.datasets.Constants;
import com.kasabi.labs.datasets.Utils;

public class DemoRDF2English {

	public static final String CHEESE_NS = "http://kasabi.com/dataset/cheese/";
	public static final String UNKNOWN = "*UNKNOWN*" ;

	public static String describe (Model data, String uri) {
		Model model = Utils.describe(data, uri);
		Resource cheese = ResourceFactory.createResource(uri);
		
		return name(cheese, model) + " is an " + country(cheese, model) + " " + certification(cheese, model) + " cheese made of " + pastorized(cheese, model) + " milk." ;
	}
	
	public static String name(Resource subject, Model cheese) {
		NodeIterator objects = cheese.listObjectsOfProperty(subject, RDFS.label);
		
		return objects.next().asLiteral().getLexicalForm() ;
	}

	public static String country(Resource subject, Model cheese) {
		NodeIterator objects = cheese.listObjectsOfProperty(subject, ResourceFactory.createProperty(CHEESE_NS + "country"));
		if ( !objects.hasNext() ) return UNKNOWN ;
		if ( objects.next().asResource().equals(ResourceFactory.createResource("http://dbpedia.org/resource/Italy")) ) {
			return "Italian" ;
		} else {
			return UNKNOWN ;
		}
	}

	public static String certification(Resource subject, Model cheese) {
		NodeIterator objects = cheese.listObjectsOfProperty(subject, ResourceFactory.createProperty(CHEESE_NS + "certification"));
		if ( !objects.hasNext() ) return UNKNOWN ;
		if ( objects.next().asResource().equals(ResourceFactory.createResource(CHEESE_NS + "CertificationDOP")) ) {
			return "DOP" ;
		} else {
			return UNKNOWN ;
		}
	}

	
	public static String pastorized (Resource subject, Model cheese) {
		NodeIterator objects = cheese.listObjectsOfProperty(subject, ResourceFactory.createProperty(CHEESE_NS + "pasteurised"));
		
		Boolean pasteurised = null ;
		Boolean unpasteurised = null ;
		while ( objects.hasNext() ) {
			RDFNode type = objects.next();
			if ( type.equals(ResourceFactory.createResource(CHEESE_NS + "PasteurisedMilk")) ) pasteurised = true ;
			else if ( type.equals(ResourceFactory.createResource(CHEESE_NS + "NonPasteurisedMilk")) ) unpasteurised = true ;
		}
		
		if ( (pasteurised == null) && (unpasteurised == null) ) {
 			return UNKNOWN ;
		} else if ( pasteurised && (!unpasteurised || ( unpasteurised == null)) ) {
			return "pasteurised" ;
		} else if ( (!pasteurised || ( pasteurised == null)) && unpasteurised ) {
			return "unpasteurised" ;
 		} else {
			return "both pasteurised and unpasteurised" ;
  		}
	}
	
	public static void main(String[] args) {

		DatasetGraph dsg = DatasetFactory.createMem().asDatasetGraph();
		File path = new File (Constants.DATA_PATH, "/cheese/italy/");
		File[] files = path.listFiles();
		for (File file : files) {
			RiotLoader.read(file.getAbsolutePath(), dsg);
		}
		Model data = ModelFactory.createModelForGraph(dsg.getDefaultGraph());
		ResIterator iter = data.listResourcesWithProperty(RDF.type, ResourceFactory.createResource(CHEESE_NS + "Cheese"));
		while ( iter.hasNext() ) {
			Resource resource = iter.nextResource() ;
			System.out.println(describe(data, resource.getURI()));
		}
	}

}
