package org.bridgedb.interaction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.rdb.construct.DBConnector;
import org.bridgedb.rdb.construct.DataDerby;
import org.bridgedb.rdb.construct.GdbConstruct;
import org.bridgedb.rdb.construct.GdbConstructImpl3;

/**
 * This program creates an Interaction annotation BridgeDB derby database
 * mappings from : Rhea
 * 
 * @author anwesha
 * 
 */
public class IntdbBuilder {

	private static String filesep = System.getProperty("file.separator");
	private static File mappingfile = new File("resources" + filesep
			+ "rhea2xrefs.txt");
	private static String dbname;
	private Xref idRhea;
	private static GdbConstruct newDb;
	private List<Xref> intxrefs = new ArrayList<Xref>();
	private String identifier;
	private String datasource;
	private String mainref;

	/**
	 * command line arguments 1 - absolute path of the interactions database to
	 * be created (for eg: /home/anwesha/interactions.bridge)
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		dbname = args[0];

		IntdbBuilder intdb = new IntdbBuilder();

		intdb.downloadMapping();

		try {
			newDb = GdbConstructImpl3.createInstance(dbname, new DataDerby(),
					DBConnector.PROP_RECREATE);
			InputStream mapping = new FileInputStream(mappingfile);
			intdb.init(newDb);
			intdb.run(mapping);
			intdb.done();
		} catch (Exception e) {
			System.out.println("Interaction Database creation failed!");
			e.printStackTrace();
		}

	}

	/**
	 * Downloads a fresh copy of interaction mappings from Rhea
	 */
	private void downloadMapping() {
		URL url;
		String inputline = "";
		try {
			url = new URL(
					"ftp://ftp.ebi.ac.uk/pub/databases/rhea/tsv/rhea2xrefs.tsv");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					url.openStream()));

			if (mappingfile.exists()) {
				mappingfile.delete();
			}
			BufferedWriter out = new BufferedWriter(new FileWriter(mappingfile,
					true));
			while ((inputline = in.readLine()) != null) {
				if (!inputline.startsWith("RHEA") & inputline.length() > 0) {
					out.write(inputline + "\n");
				}
			}
			out.close();
			in.close();
			System.out.println("Interaction Mapping Downloaded");

		} catch (Exception e) {
			System.out.println("Interaction Mapping Download failed!");
			e.printStackTrace();
		}
	}

	/**
	 * Creates an empty Derby database
	 * 
	 * @throws IDMapperException
	 *             when it cannot write to the database
	 */
	private void init(GdbConstruct newDb) throws IDMapperException {

		IntdbBuilder.newDb = newDb;

		newDb.createGdbTables();
		newDb.preInsert();

		String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
		newDb.setInfo("BUILDDATE", dateStr);
		newDb.setInfo("DATASOURCENAME", "EBI-RHEA");
		newDb.setInfo("DATASOURCEVERSION", "1.0.0");
		newDb.setInfo("SERIES", "standard-interaction");
		newDb.setInfo("DATATYPE", "Interaction");
	}

	/**
	 * Parses the mapping file and populates the database
	 * 
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws IDMapperException
	 */
	private void run(InputStream input) throws MalformedURLException,
			IOException, IDMapperException {
		mainref = "";
		String inputline;
		String[] array = new String[5];
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		while ((inputline = reader.readLine()) != null) {
			array = inputline.split("\t");
			identifier = array[3];
			datasource = array[4];
			if (!mainref.equalsIgnoreCase(array[0])) {
				mainref = array[0];
				System.out.println(mainref + "added");
				intxrefs.clear();
				idRhea = new Xref(mainref, BioDataSource.RHEA);
				intxrefs.add(new Xref(mainref, BioDataSource.RHEA));
			}

			if (mainref.equalsIgnoreCase(array[0])) {
				if (datasource.equalsIgnoreCase("EC")) {
					intxrefs.add(new Xref(identifier, BioDataSource.ENZYME_CODE));
				} else if (datasource.equals("KEGG_REACTION")) {
					intxrefs.add(new Xref(identifier,
							BioDataSource.KEGG_REACTION));

				} else if (datasource.equals("REACTOME")) {
					intxrefs.add(new Xref(identifier, BioDataSource.REACTOME));

				} else if (datasource.equals("METACYC")) {
					intxrefs.add(new Xref(identifier, BioDataSource.BIOCYC));

				} else if (datasource.equals("ECOCYC")) {
					intxrefs.add(new Xref(identifier, BioDataSource.BIOCYC));

				} else if (datasource.equals("MACIE")) {
					intxrefs.add(new Xref(identifier, BioDataSource.MACIE));

				} else if (datasource.equals("UNIPATHWAY")) {
					intxrefs.add(new Xref(identifier, BioDataSource.UNIPATHWAY));

				} else if (datasource.equals("UNIPROT")) {
					intxrefs.add(new Xref(identifier, BioDataSource.UNIPROT));

				}
			}

			Xref ref = idRhea;
			newDb.addGene(ref);
			newDb.addLink(ref, ref);
			for (Xref right : intxrefs) {
				System.out.println("id: "+right.getId()+ "added to the database");
				newDb.addGene(right);
				newDb.addLink(ref, right);
			}
		}
	}

	/**
	 * Finalizes the database
	 * 
	 * @throws IDMapperException
	 */
	private void done() throws IDMapperException {
		newDb.commit();

		System.out.println("END processing text file");

		System.out.println("Compacting database");

		System.out.println("Closing connections");

		newDb.finalize();
	}

}
