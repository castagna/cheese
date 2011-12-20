package com.kasabi.labs.datasets.cheese;

import static com.kasabi.labs.datasets.cheese.CheesesEurope.get;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.kasabi.labs.vocabularies.CHEESE;
import com.kasabi.labs.vocabularies.SKOS;

public class CheesesItalyFromTSV {

	private static final Logger log = LoggerFactory.getLogger(CheesesItalyFromTSV.class) ;
	
	private static final File INPUT_PATH = new File("data/") ;
    public static final String NS = "http://data.kasabi.com/dataset/cheese/" ;
    public static final String DBPEDIA_NS = "http://dbpedia.org/resource/" ;
    public static final String ITALY_NS = "http://data.kasabi.com/dataset/italy/" ;
    public static final String COOKIPEDIA_NS = "http://www.cookipedia.co.uk/wiki/index.php/";
    public static final String BBC_NS = "http://www.bbc.co.uk/food/" ;
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		
		FileInputStream in = new FileInputStream ( new File (INPUT_PATH, "cheeses.tsv") ) ;
		ResultSet rs = ResultSetFactory.fromTSV(in) ;
		int count = 0;

		Model model = ModelFactory.createDefaultModel() ;
		model.setNsPrefix("rdf", RDF.getURI());
		model.setNsPrefix("rdfs", RDFS.getURI());
		model.setNsPrefix("owl", OWL.getURI());
		model.setNsPrefix("skos", SKOS.getURI());
		model.setNsPrefix("foaf", FOAF.getURI());
		model.setNsPrefix("cheese", CHEESE.getURI());
		model.setNsPrefix("dbpedia", DBPEDIA_NS);
		model.setNsPrefix("italy", ITALY_NS);
		model.setNsPrefix("", NS) ;
		
		while ( rs.hasNext() ) {
			QuerySolution sol = rs.nextSolution() ;

			Resource subject = ResourceFactory.createResource(NS + toSlug(get(sol, "name"))); 
			model.add(subject, RDF.type, CHEESE.Cheese);

			String name = get(sol,"name");
			String name_en = get(sol,"name_en");
			if ( name_en == null ) name_en = name;
			
			// log.debug("Processing {} ...", name);
			
			model.add(subject, RDFS.label, name, "it");
			model.add(subject, RDFS.label, name_en, "en");
			model.add(subject, SKOS.prefLabel, name, "it");
			model.add(subject, SKOS.prefLabel, name_en, "en");
			model.add(subject, SKOS.altLabel, name + " (formaggio)", "it");
			model.add(subject, SKOS.altLabel, name_en + " (cheese)", "en");
			
			String alternative_names = get(sol, "alternative_names");
			if ( alternative_names != null ) {
				for (String n : alternative_names.split(",")) {
					model.add(subject, SKOS.hiddenLabel, n.trim(), "it");
					model.add(subject, SKOS.hiddenLabel, n.trim(), "en");
				}				
			}
			
			String category_name = get(sol, "category_name");
			if ( category_name != null ) {
				model.add(subject, SKOS.hiddenLabel, category_name, "it");
				model.add(subject, SKOS.hiddenLabel, category_name, "en");
				// TODO: should we have cheese categories? 
				// model.add(subject, RDFS.subClassOf, toSlug(category_name)); 
			}
			
			String milk_source = get(sol, "milk_source");
			if ( milk_source != null ) {
				String[] sources = milk_source.split(",");
				for (String source : sources) {
					if ( source.toLowerCase().contains("cow") ) {
						model.add(subject, CHEESE.milkSource, CHEESE.CowMilk);
					} else if ( source.toLowerCase().contains("goat") ) {
						model.add(subject, CHEESE.milkSource, CHEESE.GoatMilk);
					} else if ( source.toLowerCase().contains("sheep") ) {
						model.add(subject, CHEESE.milkSource, CHEESE.SheepMilk);
					}
				}
				if ( sources.length > 1 ) {
					model.add(subject, CHEESE.milkSource, CHEESE.MixedMilk);
				}
			}

			String texture = get(sol, "texture");
			if ( texture != null ) {
				if ( texture.toLowerCase().equals("fresh") ) {
					model.add(subject, CHEESE.texture, CHEESE.FreshCheese);
				} else if ( texture.toLowerCase().equals("soft") ) {
					model.add(subject, CHEESE.texture, CHEESE.SoftCheese);
				} else if ( texture.toLowerCase().equals("semi-hard") ) {
					model.add(subject, CHEESE.texture, CHEESE.SemiHardCheese);
				} else if ( texture.toLowerCase().equals("hard") ) {
					model.add(subject, CHEESE.texture, CHEESE.HardCheese);
				} else if ( texture.toLowerCase().equals("very-hard") ) {
					model.add(subject, CHEESE.texture, CHEESE.VeryHardCheese);
				}
			}
			
			String pasteurized = get(sol, "pasteurized");
			if ( pasteurized != null ) {
				if ( pasteurized.toLowerCase().contains("yes") ) {
					model.add(subject, CHEESE.milkProcessing, CHEESE.PastourizedMilk);
				}				
				if ( pasteurized.toLowerCase().contains("no") ) {
					model.add(subject, CHEESE.milkProcessing, CHEESE.RawMilk);
				}
			}
			
			String milk_type = get(sol, "milk_type");
			if ( milk_type != null ) {
				if ( milk_type.toLowerCase().equals("whole") ) {
					model.add(subject, CHEESE.milkType, CHEESE.WholeMilk);
				} else if ( milk_type.toLowerCase().equals("semi-skimmed") ) {
					model.add(subject, CHEESE.milkType, CHEESE.SemiSkimmedMilk);
				} else if ( milk_type.toLowerCase().equals("skimmed") ) {
					model.add(subject, CHEESE.milkType, CHEESE.SkimmedMilk);
				}
			}

			String geographical_indication = get(sol, "geographical_indication");
			if ( geographical_indication != null ) {
				if ( geographical_indication.toUpperCase().equals("PDO") ) {
					model.add(subject, CHEESE.certification, CHEESE.PDO);
				} else if ( geographical_indication.toUpperCase().equals("PGI") ) {
					model.add(subject, CHEESE.certification, CHEESE.PGI);
				} else if ( geographical_indication.toUpperCase().equals("TSG") ) {
					model.add(subject, CHEESE.certification, CHEESE.TSG);
				}
			}

			String aging = get(sol, "aging");
			if ( aging != null ) {
				model.add(subject, CHEESE.aging, aging, "en");
			}

			String shape = get(sol, "shape");
			if ( shape != null ) {
				model.add(subject, CHEESE.shape, shape, "en");
			}

			String diameter = get(sol, "diameter");
			if ( diameter != null ) {
				model.add(subject, CHEESE.diameter, diameter);
			}
			
			String weight = get(sol, "weight");
			if ( weight != null ) {
				model.add(subject, CHEESE.weight, weight);
			}
			
			String height = get(sol, "height");
			if ( height != null ) {
				model.add(subject, CHEESE.height, height);
			}

			String bbc = get(sol, "bbc");
			if ( bbc != null ) {
				model.add(subject, RDFS.seeAlso, BBC_NS + bbc);
			}
			
			String cookipedia = get(sol, "cookipedia");
			if ( cookipedia != null ) {
				model.add(subject, RDFS.seeAlso, COOKIPEDIA_NS + cookipedia);
			} else {
				try {
		    		String prefix = "/wiki/index.php/" ;
				    URL url = new URL("http://www.cookipedia.co.uk/wiki/index.php?fulltext=Search&redirs=0&search=" + URLEncoder.encode(name + " cheese", "UTF-8"));
				    BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
				    String str;
				    boolean found = false ;
				    while ((str = input.readLine()) != null) {
				    	if ( str.contains("<span class=\"mw-headline\" id=\"Page_title_matches\">Page title matches</span>")) {
				    		input.readLine(); // skip a line
				    		String line = input.readLine();
				    		int index = line.indexOf(prefix);
				    		if ( index > 0 ) {
					    		String page = line.substring(index + prefix.length(), line.indexOf("\"", index));
					    		model.add(subject, RDFS.seeAlso, COOKIPEDIA_NS + page);
					    		found = true ;
					    		break ;
				    		}
				    	}
				    }
				    if ( !found ) {
				    	log.debug("Unable to find a link to Cookipedia for {} ...", name );
				    }
				    in.close();
				} catch (MalformedURLException e) {
					log.error(e.getMessage());
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}
			
			location ( get(sol, "country"), subject, CHEESE.country, CHEESE.Country, model );
			location ( get(sol, "region"), subject, CHEESE.region, CHEESE.Region, model );
			location ( get(sol, "province"), subject, CHEESE.province, CHEESE.Province, model );
			location ( get(sol, "comune"), subject, CHEESE.municipality, CHEESE.Municipality, model );
			location ( get(sol, "location"), subject, CHEESE.location, CHEESE.Location, model );
			
			String use = get(sol, "use");
			if ( use != null ) {
				String[] uses = use.split(",");
				for (String u : uses) {
					if ( u.toLowerCase().equals("table") ) {
						model.add(subject, CHEESE.use, CHEESE.TableUse);
					} else if ( u.toLowerCase().equals("grating") ) {
						model.add(subject, CHEESE.use, CHEESE.GratingUse);
					} else if ( u.toLowerCase().equals("melting") ) {
						model.add(subject, CHEESE.use, CHEESE.MeltingUse);
					} else if ( u.toLowerCase().equals("spreading") ) {
						model.add(subject, CHEESE.use, CHEESE.SpreadingUse);
					} else if ( u.toLowerCase().equals("grilling") ) {
						model.add(subject, CHEESE.use, CHEESE.GrillingUse);
					} else if ( u.toLowerCase().equals("cooking") ) {
						model.add(subject, CHEESE.use, CHEESE.CookingUse);
					}
				}
			}
			
			String dbpedia = get(sol, "dbpedia");
			if ( dbpedia != null ) {
				dbpedia = URLEncoder.encode(dbpedia, "UTF-8");;
				String uri = DBPEDIA_NS + dbpedia;
				Resource s = ResourceFactory.createResource(uri);
				model.add(subject, OWL.sameAs, s);

				// TODO: manage provenance better here!
				Model fromDbPedia = ModelFactory.createDefaultModel();
				copy ( s, new Property[]{ FOAF.page, OWL.sameAs, RDFS.seeAlso, RDFS.label }, subject, fromDbPedia );
				
				model.add(fromDbPedia);
			} else {
				boolean first = true ;
				String searchString = null;
				for ( String string : name.split("\\s") ) {
					if ( first ) {
						searchString = string;
						first = false;
					} else {
						searchString = searchString + " AND " + string;						
					}
				}
				searchString = searchString + " AND " + "cheese";
				String queryString = 
					"SELECT ?s {"+
					"  ?s ?p ?o ." +	
					"  ?o bif:contains \" ( " + searchString + " ) \" option ( score ?sc ) ." +
					"} ORDER BY DESC(?sc) LIMIT 1";
				QueryEngineHTTP qe = new QueryEngineHTTP("http://dbpedia.org/sparql", queryString);
				ResultSet result = null;
				try {
					result = qe.execSelect() ;
					while ( result.hasNext() ) {
						model.add(subject, RDFS.seeAlso, result.next().get("s").asResource());
					}
				} catch (QueryExceptionHTTP e) {
					log.debug("Exception while linking to DBPedia for {} ...", name );
				} finally {
					qe.close() ;
				}
			}
			
			count++ ;
		}
		
		model.write(System.out, "TURTLE");
		
		log.debug("Found {} cheeses", count++);
	}
	
	public static void copy ( Resource srcSubj, Property[] properties, Resource dstSubj, Model dstModel ) {
		FileManager.get().setModelCaching(true);
		Model srcModel = FileManager.get().loadModel(srcSubj.getURI());
		for ( Property prop : properties ) {
			NodeIterator iter = srcModel.listObjectsOfProperty(srcSubj, prop);
			while ( iter.hasNext() ) {
				RDFNode node = iter.nextNode();
				dstModel.add(dstSubj, prop, node);
			}
		}
	}
	
	public static void location ( String location, Resource subject, Property property, Resource type, Model model ) {
		if ( location != null ) {
			String[] locations = location.split(",");
			for (String l : locations) {
				Resource o = ResourceFactory.createResource(ITALY_NS + toSlug(l.trim()));
				model.add(subject, property, o);
				model.add(o, RDF.type, type);
				model.add(o, RDFS.label, l.trim());
			}
		}
	}
	
	
	  private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	  public static String toSlug(String input) {
	    String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
	    String normalized = Normalizer.normalize(nowhitespace, Form.NFD);
	    String slug = NONLATIN.matcher(normalized).replaceAll("");
	    return slug.toLowerCase(Locale.ENGLISH);
	  }


}
