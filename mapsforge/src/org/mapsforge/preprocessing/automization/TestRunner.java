package org.mapsforge.preprocessing.automization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AccessException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

public class TestRunner {

	private static Logger LOGGER =
			Logger.getLogger(TestRunner.class.getName());
	private static String PLANETFILE = "files/planet.osm.pbf";
	private static String NEWLINE = System.getProperty("line.separator");
	private static ArrayList<String> MD5LIST = new ArrayList<String>();

	private static void usage() {
		System.out.println("Usage: java -jar runner.jar [-options...] <xml-file>");
		System.out.println("");
		System.out.println("Options:");
		System.out
				.println(" -update	: run the osmosis replication task on the default file workingDir/files/planet.osm.pbf bevor running the pipeline tasks of the xml file");
		System.out
				.println(" -update file=<file>	: run the osmosis update task on the given file bevor running the pipeline tasks of the xml file");
		System.out.println(" -quiet		: disable logging output");
		System.out.println(" -help		: display this help message");
	}

	private static File checkOsmosisReplicationTask(String workingDirectory, String updateFile) {

		// TODO: DEBUG
		System.out.println("DEBUG: checkOsmosisReplicationTask ");
		// check if replication task is initialized
		try {
			FileOperation.createReadFile(workingDirectory, "configuration.txt");
			FileOperation.createReadFile(workingDirectory, "download.lock");
		} catch (FileNotFoundException e) {
			System.out.println("Error! The osmosis replication task is not initialized.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err
					.println("Error! Can not read or open the files for the osmosis replication task.");
			e.printStackTrace();
		}

		// check update file
		File result = null;
		try {
			result = FileOperation.createReadWriteFile(workingDirectory, updateFile);
		} catch (FileNotFoundException e) {
			System.err.println("Error! The file for update does not exists.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error! Can not operate on file.");
			e.printStackTrace();
		}

		return result;
	}

	private static String generateOsmosisReplicationTask(String osmosis,
			String workingDirectory, String updateFile) {

		// TODO: DEBUG
		System.out.println("DEBUG: generate osmosis replicationtask");
		File tmpFile = checkOsmosisReplicationTask(workingDirectory, updateFile);

		if (tmpFile == null)
			throw new RuntimeException("An unexpected error occured. Update file is null.");

		// TODO: DEBUG
		System.out.println("DEBUG: generate osmosis replicationtask: file: "
				+ tmpFile.getAbsolutePath());
		StringBuilder sb = new StringBuilder();
		sb.append(osmosis).append(" ");
		sb.append("--rb file=").append(tmpFile.getAbsolutePath()).append(" ")
				.append("outPipe.0=data").append(" ");
		sb.append("--rri workingDirectory=").append(workingDirectory)
				.append(" ");
		sb.append("--simc outPipe.0=change").append(" ");
		sb.append("--ac inPipe.0=data inPipe.1=change").append(" ");
		sb.append("--log-progress interval=60").append(" ");
		sb.append("--wb file=").append(workingDirectory)
				.append("/files/planet.osm.pbf").append(" ")
				.append("omitmetadata=false").append(" ")
				.append("compress=deflate").append(" ");

		return sb.toString();

	}

	public static void main(String[] args) throws Exception {

		String line = null;
		String updateFile = null;
		Boolean update = false;
		Boolean quiet = false;
		File xmlFile = null;
		BufferedWriter bw;
		BufferedReader br;
		Process process;

		if (args.length < 1) {
			usage();
			System.exit(1);
		}

		// TODO: DEBUG
		System.out.println("DEBUG: args length: " + args.length);
		for (int i = 0; i < args.length; i++) {

			String argument = args[i];

			// TODO: DEBUG
			System.out.println("DEBUG: argument: " + argument);
			// argument is an option
			if (argument.startsWith("-")) {
				// TODO: DEBUG
				System.out.println("DEBUG: is argument");
				if (argument.equals("-update")) {
					// TODO: DEBUG
					System.out.println("DEBUG: is update");
					update = true;
					argument = args[i + 1];
					if (argument.startsWith("file="))
						updateFile = argument.split("=")[1];
					else
						updateFile = PLANETFILE;
					// TODO: DEBUG
					System.out.println("DEBUG: updateFile= " + updateFile);
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
		if (!workingDirFile.canRead())
			throw new AccessException(
					"Error! You have no permission to read in working directory: "
							+ workingDirFile.getAbsolutePath());
		if (!workingDirFile.canWrite())
			throw new AccessException(
					"Error! You have no permission to write to working directory: "
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
		if (!osmosisBin.canExecute())
			throw new AccessException("Error! You have not the permission to execute osmosis: "
					+ osmosisBin.getAbsolutePath());

		String osmosis = osmosisBin.getAbsolutePath();

		// loggingDir
		String directory = conf.getValue().getLoggingDir();
		// TODO: DEBUG
		System.out.println("DEBUG: testrunner: logging dir: " + directory);
		if (directory == null || directory == "")
			throw new IllegalArgumentException(
					"Error! No input for logging directory.");

		File loggingDirFile = FileOperation.createDirectory(workingDirFile, directory);

		// outputDir
		directory = conf.getValue().getOutputDir();
		// TODO: DEBUG
		System.out.println("DEBUG: testrunner: output dir: " + directory);
		if (directory == null || directory == "")
			throw new IllegalArgumentException(
					"Error! No input for logging directory");

		File outputDirFile = FileOperation.createDirectory(workingDirFile, directory);

		// move
		Boolean move = conf.getValue().isMove();

		// TODO: DEBUG
		System.out.println("DEBUG: workingDir: " + workingDirFile.getAbsolutePath());
		System.out.println("DEBUG: osmosis_home: " + osmosis_home.getAbsolutePath());
		System.out.println("DEBUG: loggingDir: " + loggingDirFile.getAbsolutePath());
		System.out.println("DEBUG: outputDir: " + outputDirFile.getAbsolutePath());
		System.out.println("DEBUG: move: " + move);

		String call;

		// run update task
		if (update) {

			// TODO: DEBUG
			System.out.println("DEBUG: update is true ");

			call = generateOsmosisReplicationTask(osmosis, workingDirFile.getAbsolutePath(),
					updateFile);

			// TODO: DEBUG
			System.out.println("DEBUG: update call: " + call);
			process = Runtime.getRuntime().exec(call, null, workingDirFile);

			// log update task
			File updateLog = FileOperation.createWriteFile(loggingDirFile.getAbsolutePath(),
					"update.log");
			// TODO: DEBUG
			System.out.println("DEBUG: update log file: " + updateLog);

			bw = new BufferedWriter(new FileWriter(updateLog, true));
			br = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));

			while ((line = br.readLine()) != null) {
				bw.append(line).append(NEWLINE);
				bw.flush();
			}
			bw.close();
			br.close();

			process.waitFor();

		}

		// run pipelines
		for (Pipeline pipeline : pipelines) {

			// traverse the XML tree to the leafs to update the output directory of the sinks
			// and generate their osmosis calls. also registered the file for what a md5 file
			// should generated.
			call = osmosis
					+ " "
					+ pipeline.generate(MD5LIST, workingDirFile.getAbsolutePath(),
							outputDirFile.getAbsolutePath());
			// TODO: DEBUG
			System.out.println("DEBUG: md5 size: " + MD5LIST.size());
			System.out.println("DEBUG: pipeline call: " + call);

			// run osmosis task of pipeline
			process = Runtime.getRuntime().exec(call, null, workingDirFile);

			File updateLog = FileOperation.createWriteFile(loggingDirFile.getAbsolutePath(),
					pipeline.getName() + ".log");

			bw = new BufferedWriter(new FileWriter(updateLog, true));
			br = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			while ((line = br.readLine()) != null) {
				bw.append(line).append(NEWLINE);
				bw.flush();
			}

			bw.close();
			br.close();

			process.waitFor();

			File tmpFile;

			for (String file : MD5LIST) {
				tmpFile = new File(file);

				call = "/usr/bin/md5sum " + tmpFile.getName();
				// TODO: DEBUG
				System.out.println("DEBUG: md5 call: " + call);
				process = Runtime.getRuntime().exec(call, null,
						new File(tmpFile.getParent()));

				bw = new BufferedWriter(new FileWriter(new File(tmpFile.getParent(),
						tmpFile.getName() + ".md5"), false));
				br = new BufferedReader(new InputStreamReader(process.getInputStream()));

				while ((line = br.readLine()) != null) {
					System.out.println(line);
					bw.append(line).append(NEWLINE);
					bw.flush();
				}

				bw.close();
				br.close();

				process.waitFor();

			}
		}

		// all pipelines are done. now we move or copy the generated files to the destination
		// directory

		directory = conf.getValue().getDestinationDir();
		if (directory != null && directory != "") {

			// destinationDir
			File destinationDirFile = FileOperation.createDirectory(workingDirFile, directory);

			if (move) {
				call = "mv " + outputDirFile.getAbsolutePath() + " "
						+ destinationDirFile.getAbsolutePath();
			} else {

				call = "cp -r " + outputDirFile.getAbsolutePath() + " "
						+ destinationDirFile.getAbsolutePath();
			}

			process = Runtime.getRuntime().exec(call, null, workingDirFile);

			File logFile = FileOperation.createWriteFile(loggingDirFile.getAbsolutePath(),
					"coptToDestination.log");
			// TODO: DEBUG
			System.out.println("DEBUG: destination log file: " + logFile);
			bw = new BufferedWriter(new FileWriter(logFile));
			br = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			while ((line = br.readLine()) != null) {
				System.out.println(line);
				bw.append(line).append(NEWLINE);
				bw.flush();
			}
			bw.close();
			br.close();

			process.waitFor();

		}
	}
}
