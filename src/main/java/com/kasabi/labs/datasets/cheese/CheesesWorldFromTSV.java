package com.kasabi.labs.datasets.cheese;

import static com.kasabi.labs.datasets.cheese.CheesesEurope.get;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import com.kasabi.labs.datasets.Utils;
import com.kasabi.labs.vocabularies.CHEESE;

public class CheesesWorldFromTSV {

	private static final Logger log = LoggerFactory.getLogger(CheesesWorldFromTSV.class) ;
	
	public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException {
		
		Dataset ds = DatasetFactory.createMem();
		
		File file = new File ( CheesesItalyFromTSV.INPUT_PATH + File.separator + "cheese", "cheeses-world.tsv");
		
		FileInputStream in = new FileInputStream ( file ) ;
		ResultSet rs = ResultSetFactory.fromTSV(in) ;
		int count = 0;
		
		Model model = ds.getDefaultModel() ;
		CheesesItalyFromTSV.setPrefixes(model);
		
		while ( rs.hasNext() ) {
			QuerySolution sol = rs.nextSolution() ;
			
			String name = get(sol,"name");
			// log.debug("Processing {} ...", name) ;
			
			Resource subject = ResourceFactory.createResource(CheesesItalyFromTSV.NS + Utils.toSlug(name));
			model.add(subject, RDF.type, CHEESE.Cheese);

			Property latitude = ResourceFactory.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
			Property longitude = ResourceFactory.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#long");
			
			String dbpedia = get(sol, "dbpedia");
			if ( dbpedia != null ) {
				dbpedia = URLEncoder.encode(dbpedia, "UTF-8");;
				try {
					FileManager.get().setModelCaching(true);
					Model m = FileManager.get().loadModel(CheesesItalyFromTSV.DBPEDIA_NS + dbpedia);
					Property country = ResourceFactory.createProperty("http://dbpedia.org/property/country");
					NodeIterator iter = m.listObjectsOfProperty(country);
					while ( iter.hasNext() ) {
						RDFNode node = iter.nextNode();
						if ( node.isURIResource() ) {
							log.debug ("Found a country {}...", node);
							Model c = FileManager.get().loadModel(node.asResource().getURI(), "NTRIPLE");
							c.write(System.out, "TURTLE");
							StmtIterator i1 = c.listStatements((Resource)null, latitude, (RDFNode)null);
							if ( i1.hasNext() ) {
								Statement stmt = i1.next();
								log.debug("Country has latitude {}...", stmt.getObject());
								StmtIterator i2 = c.listStatements((Resource)null, longitude, (RDFNode)null);
								if ( i2.hasNext() ) {
									System.out.println( node + " " + stmt.getObject() + ", " + i2.next().getObject() ); 
									
								}
							}
						}
					}
					// log.debug("Found {} triples for {} cheese.", m.size(), name);					
				} catch (JenaException e) {
					log.debug(e.getMessage());
				}
			}		
			count++ ;
		}
		log.debug("Found {} cheeses", count++);		

	}

}
