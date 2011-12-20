package com.kasabi.labs.datasets.cheese;

public class CheeseSchemagen {

	public static void main(String[] args) {
		jena.schemagen.main(new String[]{
				"-i", "cheese.ttl", 
				"-n", "CHEESE", 
				"--package", "com.kasabi.labs.vocabularies", 
				"-o", "src/main/java/",
				"--nocomments",			
				"--rdfs", 
			}) ;
		jena.schemagen.main(new String[]{
				"-i", "data/vocabularies/skos.ttl", 
				"-n", "SKOS", 
				"--package", "com.kasabi.labs.vocabularies", 
				"-o", "src/main/java/",
				"--nocomments",				
			}) ;
	}

}
