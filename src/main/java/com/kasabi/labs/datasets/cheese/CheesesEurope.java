package com.kasabi.labs.datasets.cheese;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.openjena.riot.Lang;
import org.openjena.riot.RiotException;
import org.openjena.riot.RiotLoader;
import org.openjena.riot.RiotWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.DatasetGraph;

public class CheesesEurope {

	private static final Logger log = LoggerFactory.getLogger(CheesesEurope.class) ;

	private static final File INPUT_PATH = new File("data/cheese/") ;
	private static final File OUTPUT_PATH = new File("data/cheese/europe/") ;
	private static final File OUTPUT_PATH_TARGET = new File("target") ;
	public static final String VERSION = "0.1";

	public static void main(String[] args) throws IOException {
		generate_european_cheeses();

		Dataset ds = DatasetFactory.createMem();
		DatasetGraph dsg = ds.asDatasetGraph();
		aggregate(dsg, INPUT_PATH);

		write (dsg, OUTPUT_PATH_TARGET);
	}

	public static void write (DatasetGraph dsg, File path) throws IOException {
		if ( path.isDirectory() ) {
			// N-Quad format
			FileOutputStream out = new FileOutputStream(new File(path, "cheese-" + VERSION + ".nq"));
			RiotWriter.writeNQuads(out, dsg);
			out.flush();
			out.close();

			// N-Triples format (default graph only!)
			out = new FileOutputStream(new File(path, "cheese-" + VERSION + ".nt"));
			RiotWriter.writeTriples(out, dsg.getDefaultGraph());
			out.flush();
			out.close();
			
			// RDF/JSON (default graph only!)
			out = new FileOutputStream(new File(path, "cheese-" + VERSION + ".json"));
			RiotWriter.writeRDFJSON(out, dsg.getDefaultGraph());
			out.flush();
			out.close();
			
			Model model = ModelFactory.createModelForGraph(dsg.getDefaultGraph());
			model.setNsPrefixes(dsg.getDefaultGraph().getPrefixMapping());

			// RDF/XML (default graph only!)
			out = new FileOutputStream(new File(path, "cheese-" + VERSION + ".rdf"));
			model.write(out, "RDF/XML"); // see also: "RDF/XML-ABBREV"
			out.flush();
			out.close();

			// Turtle (default graph only!)
			out = new FileOutputStream(new File(path, "cheese-" + VERSION + ".ttl"));
			model.write(out, "TURTLE");
			out.flush();
			out.close();
		}
	}
	
	public static void aggregate(DatasetGraph dsg, File path) {
		if ( path.isDirectory() ) {
			File[] files = path.listFiles();
			for (File file : files) {
				if ( file.isDirectory() ) {
					aggregate ( dsg, file );
				} else {
					load (dsg, file);
				}
			}
		} else {
			load (dsg, path);
		}
	}

	public static void load(DatasetGraph dsg, File file) {
		try {
			String filename = file.getAbsolutePath();
			Lang lang = Lang.guess(filename);
			if ( lang != null ) {
				log.debug("Loading {} ...", filename);
				RiotLoader.read(filename, dsg, lang);
			} else {
				log.debug("File {} is an unknown format, ignoring...", filename);
			}			
		} catch (RiotException e) {
			log.debug(e.getMessage(), e);
		}
	}

	private static void generate_european_cheeses() throws IOException {
		FileInputStream in = new FileInputStream ( new File (INPUT_PATH, "cheeses-europe.tsv") ) ;
		ResultSet rs = ResultSetFactory.fromTSV(in) ;
		int count = 0;

		VelocityEngine ve = new VelocityEngine();
		ve.setProperty("file.resource.loader.path", INPUT_PATH.getAbsolutePath()) ;
		ve.init();
		Template t = ve.getTemplate( "cheese-ttl.template" );
		
		while ( rs.hasNext() ) {
			QuerySolution qs = rs.nextSolution();

			
//			if ( false ) {
//			
//
//
//			System.out.println(get(qs, "Country"));
//			System.out.println(get(qs, "Status"));
//			System.out.println(get(qs, "Designation"));
//			System.out.println(get(qs, "Type"));
//			System.out.println(get(qs, "Dossier Number"));
//			System.out.println(get(qs, "ISO"));
//			System.out.println(get(qs, "Latin Transcription"));
//			System.out.println(get(qs, "Last relevant date"));
//			System.out.println(get(qs, "Registration date"));
//			System.out.println(get(qs, "Submission date"));
//			System.out.println(get(qs, "Publication date"));
//			System.out.println(get(qs, "1st Amendment date"));
//			System.out.println(get(qs, "2st Amendment date"));
//			System.out.println(get(qs, "3st Amendment date"));
//
//			
//			}

			String country = get(qs, "Country").trim().toLowerCase() ;
			File path = new File (OUTPUT_PATH, filename(country));
			if ( !path.exists() ) path.mkdirs();

			String names[] = normalizeName(name(qs));
			String filename = filename(names[0]);

			FileWriter writer = new FileWriter(new File(path, filename + ".trig"));
			VelocityContext context = new VelocityContext();
			context.put("qs", qs);
			context.put("names", names);
			context.put("country", get(qs, "Country").trim());
			context.put("localname", names[0].replaceAll(" ", ""));
			context.put("dossier_number", get(qs, "Dossier Number"));
			context.put("type", get(qs, "Type"));
			t.merge(context, writer);
			writer.flush();
			writer.close();

			count++;
		}
		log.debug("Found {} cheeses", count++);
	}
	
	public static String name(QuerySolution qs) {
		String name = get(qs, "Latin Transcription");
		if ( name != null ) {
			return name ;
		} else {
			return get(qs, "Designation" );
		}
	}
	
	public static String filename(String name) {
		return name.replaceAll(" ", "_").toLowerCase();
	}
	
	public static String[] normalizeName (String name) {
		name = name.replaceAll(" cheese", "").replaceAll("  ", " ").trim();
		String[] names = null;
		if ( name.contains(" / ") ) {
			names = name.split(" / ");
		} else if ( name.contains(" ; ") ) {
			names = name.split(" ; ");
		} else {
			names = new String[]{ name };
		}
		return titleCase(names);
	}

	public static String[] titleCase (String[] names) {
		String[] result = new String[names.length];
		for ( int i=0; i < names.length; i++ ) {
			result[i] = WordUtils.capitalizeFully(names[i])
				.replaceAll(" Della Valle Del ", " della valle del ")
				.replaceAll(" Dell'alta ", " dell'Alta ")
				.replaceAll(" D'aosta ", " d'Aosta ")
				.replaceAll(" Di ", " di ")
				.replaceAll(" Y ", " y ")
				.replaceAll(" Del ", " del ")
				.replaceAll(" Des ", " des ")
				.replaceAll(" Met ", " met ")
				.replaceAll(" De ", " de ")
				.replaceAll(" Delle ", " delle ")
				.replaceAll(" La ", " la ")
				.replaceAll(" Al ", " al ")
				.replaceAll(" Du ", " du ")
				.replaceAll(" Do ", " do ")
				.replaceAll(" Ser ", " ser ")
				.replaceAll(" Da ", " da ")
				.replaceAll(" D'u", " d'U")
				.replaceAll(" D'a", " d'A")
				.replaceAll(" D'o", " d'O")
				.replaceAll(" D'", " d'")
				.replaceAll(" L'", " l'")
				.replaceAll("-d", "-D")
				.replaceAll("-i", "-I")
				.replaceAll("-l", "-L")
				.replaceAll("-j", "-J")
				.replaceAll("-g", "-G")
				.replaceAll("-s", "-S")
				.replaceAll("-p", "-P")
				.replaceAll("-t", "-T")
				.replaceAll("-u", "-U")
				.replaceAll("-m", "-M")
				.replaceAll("-n", "-N")
				.replaceAll("-h", "-H")
				.replaceAll(" Los ", " los ")
				.replaceAll("-Sur-c", "-sur-C")
				.replaceAll("L'é", "l'É")
				;
			if ( !names[i].equals(result[i]) )
				log.debug ("Original name '{}' has been changed into '{}'", names[i], result[i]);
		}
		return result;
	}
	
	public static String get(QuerySolution qs, String varName) {
		RDFNode node = qs.get("\"" + varName + "\"") ;
		if ( node != null ) return node.toString() ;
		else return null ;
	}

}
