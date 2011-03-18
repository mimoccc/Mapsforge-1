package org.mapsforge.preprocessing.automization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

public class TestRunner {

	private static Logger LOGGER =
			Logger.getLogger(TestRunner.class.getName());
	private static ArrayList<String> md5List = new ArrayList<String>();

	private static void usage() {
		System.out.println("Usage: java -jar runner.jar [-options...] <xml-file>");
		System.out.println("");
		System.out.println("Options:");
		System.out
				.println(" -update <file>	: start the osmosis update task on the given file bevor running the pipeline tasks of the xml file");
		System.out.println(" -quiet		: disable logging output");
		System.out.println(" -help		: display this help message");
	}

	public static String update(String osmosis, String workingDirectory) {
		StringBuilder sb = new StringBuilder();
		sb.append(osmosis);
		sb.append("--rri workingDirectory= ").append(workingDirectory)
				.append(" ");
		sb.append("--simc outPipe.0=change").append(" ");
		sb.append("ac inPipe.0=data inPipe.1=change").append(" ");
		sb.append("--log-progress interval=60");
		sb.append("--wb file=").append(workingDirectory)
				.append("/files/planet.osm.pbf").append(" ")
				.append("omitmetadata=false").append(" ")
				.append("compress=deflate").append(" ");

		return sb.toString();

	}

	public static void main(String[] args) throws Exception {

		if (args.length < 1) {
			usage();
			System.exit(1);
		}

		Boolean update = false;
		Boolean quiet = false;
		File updatePath = null;
		File xmlFile = null;

		System.out.println("args length: " + args.length);
		for (int i = 0; i < args.length; i++) {

			String argument = args[i];

			// argument is an option
			if (argument.startsWith("-")) {
				if (argument == "-update") {
					update = true;
					updatePath = new File(args[i++]);
					continue;
				} else if (argument == "-help") {
					usage();
					System.exit(0);
				} else if (argument == "-quiet") {
					quiet = true;
					LOGGER.setLevel(Level.OFF);
					continue;
				} else {
					System.err.println("no such option: " + argument);
					usage();
					System.exit(-1);
				}

			} else if (i != args.length - 1) {
				// argument is not last argument and no option, wrong input
				System.err.println("error! wrong input");
				usage();
				System.exit(-1);
			} else
				xmlFile = new File(argument);
		}

		if (xmlFile == null)
			throw new RuntimeException("Cannot start Runner. No XML file is given.");
		if (!xmlFile.exists())
			throw new IllegalArgumentException("cannot find file: "
					+ xmlFile.getAbsolutePath());
		if (!xmlFile.isFile())
			throw new IllegalArgumentException("file is not a file: "
					+ xmlFile.getAbsolutePath());

		JAXBContext jc = JAXBContext.newInstance("org.mapsforge.preprocessing.automization");
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<Configuration> conf = (JAXBElement<Configuration>) unmarshaller
				.unmarshal(xmlFile);

		// get all pipelines of the xml file
		List<Pipeline> pipelines = conf.getValue().getPipeline();

		// get and check the vales of the configuration parameter: workingDir, osmosis_home,
		// outputDir, loggingDir, destinationDir and move

		// workingDir
		File workingDirFile = new File(conf.getValue().getWorkingDir());
		if (!workingDirFile.exists())
			throw new IllegalArgumentException("cannot find working directory: "
					+ workingDirFile.getAbsolutePath());
		if (!workingDirFile.isDirectory())
			throw new IllegalArgumentException("working directory is not a directory: "
					+ workingDirFile.getAbsolutePath());

		// osmosis _home and osmoisis
		File osmosis_home = new File(conf.getValue().getOsmosisHome());
		if (!osmosis_home.exists())
			throw new IllegalArgumentException("cannot find OSMOSIS_HOME: "
					+ osmosis_home.getAbsolutePath());
		if (!osmosis_home.isDirectory())
			throw new IllegalArgumentException("OSMOSIS_HOME is not a directory: "
					+ osmosis_home.getAbsolutePath());

		File osmosisBin = new File(osmosis_home, "bin/osmosis");
		if (!osmosisBin.exists())
			throw new RuntimeException("unable to access osmosis binary: "
					+ osmosisBin.getAbsolutePath());

		String osmosis = osmosisBin.getAbsolutePath();

		// just testing
		File testO = new File(osmosis);
		System.out.println("osmosis: " + osmosis);
		System.out.println("parent: " + testO.getParent());
		System.out.println("getName(): " + testO.getName());
		System.out.println("getAbsolutePath: " + testO.getAbsolutePath());
		System.out.println("getCanonicalPath: " + testO.getCanonicalPath());
		System.out.println("getPath: " + testO.getPath());
		System.out.println("getAbsoluteFile: " + testO.getAbsoluteFile());
		System.out.println("getCanonicalFile: " + testO.getCanonicalFile());
		System.out.println("getParentFile: " + testO.getParentFile());

		// loggingDir
		File loggingDirFile;
		File loggingDirPath = new File(conf.getValue().getLoggingDir());
		if (!loggingDirPath.isAbsolute()) {
			loggingDirFile = new File(workingDirFile, loggingDirPath.getPath());
		} else
			loggingDirFile = new File(conf.getValue().getLoggingDir());

		if (!loggingDirFile.exists())
			throw new IllegalArgumentException("cannot find logging directory: "
					+ loggingDirFile.getAbsolutePath());
		if (!loggingDirFile.isDirectory())
			throw new IllegalArgumentException("logging directory is not a directory "
					+ loggingDirFile.getAbsolutePath());

		// outputDir

		File outputDirFile;
		File outputDirPath = new File(conf.getValue().getOutputDir());
		if (!outputDirPath.isAbsolute()) {
			outputDirFile = new File(workingDirFile, outputDirPath.getPath());
		} else
			outputDirFile = new File(conf.getValue().getOutputDir());

		if (!outputDirFile.exists())
			throw new IllegalArgumentException("cannot find output directory: "
					+ outputDirFile.getAbsolutePath());
		if (!outputDirFile.isDirectory())
			throw new IllegalArgumentException("output directory is not a directory "
					+ outputDirFile.getAbsolutePath());

		// destinationDir
		String destinationDirString = conf.getValue().getDestinationDir();
		File destinationDirFile = null;
		File destinationDirPath = null;
		if (destinationDirString != null) {
			destinationDirPath = new File(destinationDirString);
			if (!destinationDirPath.isAbsolute())
				destinationDirFile = new File(workingDirFile, destinationDirPath.getPath());
			else
				destinationDirFile = new File(conf.getValue().getDestinationDir());

			if (!destinationDirFile.exists())
				throw new IllegalArgumentException("cannot find destination directory: "
						+ destinationDirFile.getAbsolutePath());
			if (!destinationDirFile.isDirectory())
				throw new IllegalArgumentException("destination directory is not a directory "
						+ destinationDirFile.getAbsolutePath());
		}

		// move

		Boolean move = conf.getValue().isMove();

		System.out.println("workingDir: " + workingDirFile.getAbsolutePath());
		System.out.println("osmosis_home: " + osmosis_home.getAbsolutePath());
		System.out.println("loggingPath: " + loggingDirPath.getPath());
		System.out.println("loggingDir: " + loggingDirFile.getAbsolutePath());
		System.out.println("outputPath: " + outputDirPath.getPath());
		System.out.println("outputDir: " + outputDirFile.getAbsolutePath());
		if (destinationDirFile != null) {
			System.out.println("destinationPath: " + destinationDirPath.getPath());
			System.out.println("destinationDir: " + destinationDirFile.getAbsolutePath());
		}

		System.out.println("move: " + move);

		String logfile;
		File updateFile;
		FileHandler fh = null;

		// run update task
		if (update && updatePath != null) {
			if (updatePath.isAbsolute())
				updateFile = new File(updatePath.getAbsolutePath());
			else
				updateFile = new File(workingDirFile, updatePath.getCanonicalPath());

			Process p = Runtime.getRuntime().exec(
					update(osmosis, workingDirFile.getAbsolutePath()), null, workingDirFile);

			// log update task
			File updateLog = new File(loggingDirFile, "update.log");
			System.out.println("update log file: " + updateLog);

			BufferedWriter bw = new BufferedWriter(new FileWriter(updateLog));
			BufferedReader err = new BufferedReader(new InputStreamReader(
						p.getErrorStream()));
			String line = null;
			while ((line = err.readLine()) != null) {
				bw.append(line);
			}
			bw.flush();
			bw.close();
			err.close();

			p.waitFor();

		}

		// run pipelines
		for (Pipeline pipeline : pipelines) {

			// traverse the XML tree to the leafs to update the output directory of the sinks
			// and generate their osmosis calls. also registered the file for what a md5 file
			// should generated.
			String call = osmosis + " "
					+ pipeline.generate(md5List, outputDirFile.getAbsolutePath());
			System.out.println(md5List.size());
			System.out.println(call);

			// run osmosis task of pipeline
			Process p = Runtime.getRuntime().exec(call, null, workingDirFile);

			File updateLog = new File(loggingDirFile, pipeline.getName() + ".log");

			BufferedWriter bw = new BufferedWriter(new FileWriter(updateLog));
			BufferedReader err = new BufferedReader(new InputStreamReader(
						p.getErrorStream()));
			String line = null;
			while ((line = err.readLine()) != null) {
				bw.append(line);
			}
			bw.flush();
			bw.close();
			err.close();

			p.waitFor();

			File tmp;
			Process md5Process;
			String md5;
			BufferedReader br;
			for (String file : md5List) {
				tmp = new File(file);

				md5 = "/usr/bin/md5sum " + tmp.getName();
				System.out.println(md5);
				md5Process = Runtime.getRuntime().exec(md5, null,
						new File(tmp.getParent()));

				bw = new BufferedWriter(
						new FileWriter(new File(tmp.getParent(), tmp.getName() + ".md5")));
				br = new BufferedReader(new InputStreamReader(md5Process.getInputStream()));
				line = null;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
					bw.append(line);
					bw.flush();
				}
				// bw.flush();
				bw.close();
				err.close();

				md5Process.waitFor();

			}
		}
	}
}
