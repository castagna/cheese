package com.kasabi.labs.cheese;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class EurostatTSV2MotionGraph {

	private static String filename = "market/tag00040_sorted.tsv";
	private static String template = "market/cheese-eurostat.template";
	private static String output = "market/cheese-eurostat.html";
	// private static String output = "/var/www/cheese-eurostat.html";

	public static void main(String[] args) {
		try {
			String templateContent = readFile(template);

			StringBuffer data = new StringBuffer();
			data.append("[\n");
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String str;
			String[] headers = in.readLine().split("\\t") ;
			boolean first = true;
			while ((str = in.readLine()) != null) {
				String[] fields = str.split("\\t");
				for ( int i = 1; i < fields.length; i++ ) {
					if ( !fields[i].trim().equals("-") ) {
						if ( first ) {
							data.append("[");
							first = false ;
						} else {
							data.append(",\n[");
						}
						data.append("'" + fields[0] + "', new Date(" + headers[i].trim() + ",11,31), " + fields[i].trim() + "]");						
					}
				}
			}
			data.append("\n]");
			in.close();
			
			templateContent = templateContent.replaceFirst("@@DATA@@", data.toString());
			
			FileOutputStream out = new FileOutputStream(output) ;
			out.write(templateContent.getBytes()) ;
			out.close() ;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String readFile(String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			return Charset.defaultCharset().decode(bb).toString();
		} finally {
			stream.close();
		}
	}

}
