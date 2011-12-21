package com.kasabi.labs.datasets.cheese.visualizations;

import static com.kasabi.labs.datasets.Constants.DATA_PATH;
import static com.kasabi.labs.datasets.Constants.QUERIES_PATH;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.kasabi.labs.datasets.Utils;

public class CheeseExhibit {
	
	public static void main(String[] args) throws IOException {
		File[] data = new File[]{
				new File ( DATA_PATH, "cheeses-0.1.ttl" ),
				new File ( "cheese.ttl" ),
		};
		File query = new File(QUERIES_PATH + "cheese-exhibit.sparql") ;
		FileWriter writer = new FileWriter("visualizations/exhibit/cheese.js");
		Utils.render(data, query, writer) ;
		writer.flush() ;
		writer.close() ;
	}

}
