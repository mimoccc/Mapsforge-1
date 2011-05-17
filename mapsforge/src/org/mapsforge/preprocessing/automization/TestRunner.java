/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapsforge.preprocessing.automization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * The basic implementation of the automatic execution of the mapsforge preprocessing.
 */
public class TestRunner {

	// private static Logger LOGGER =
	// Logger.getLogger(TestRunner.class.getName());
	private static String PLANETFILE = "files/planet.osm.pbf";
	private static String NEWLINE = System.getProperty("line.separator");
	private static ArrayList<String> MD5LIST = new ArrayList<String>();

	private static File workingDirectory;
	private static File loggingDirectory;
	private static File outputDirectory;

	private static String osmosis;

	final private static String osmosisPath = "bin/osmosis";
	final private static String md5sum = "/usr/bin/md5sum";

	/**
	 * This is starting point of the automatic execution tool of the mapsforge preprocessing.
	 * 
	 * Here the xml file would be parsed, the osmosis calls of update and the pipeline execution
	 * would be generated and started. The MD5 file generation and the moving of the output
	 * folder to the destination would be handeled.
	 * 
	 * @param args
	 *            the arguments of the runner.
	 */
	public static void main(String[] args) {

		String updateFile = null;
		Boolean update = false;
		// Boolean quiet = false;
		File xmlFile = null;

		if (args.length < 1) {
			usage();
			System.exit(1);
		}

		// TODO: DEBUG
		// System.out.println("DEBUG: args length: " + args.length);
		for (int i = 0; i < args.length; i++) {

			String argument = args[i];

			// TODO: DEBUG
			// System.out.println("DEBUG: argument: " + argument);
			// argument is an option
			if (argument.startsWith("-")) {
				// TODO: DEBUG
				// System.out.println("DEBUG: is argument");
				if (argument.equals("-update")) {
					// TODO: DEBUG
					// System.out.println("DEBUG: is update");
					update = true;
					argument = args[i + 1];
					if (argument.startsWith("file="))
						updateFile = argument.split("=")[1];
					else
						updateFile = PLANETFILE;
					// TODO: DEBUG
					// System.out.println("DEBUG: updateFile= " + updateFile);
					continue;

				} else if (argument == "-help") {
					usage();
					System.exit(0);
				} else if (argument == "-quiet") {
					// quiet = true;
					// LOGGER.setLevel(Level.OFF);
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

		JAXBContext jc = null;
		Unmarshaller unmarshaller = null;
		File osmosis_home = null;
		File osmosisBin = null;
		try {
			jc = JAXBContext.newInstance("org.mapsforge.preprocessing.automization");

			unmarshaller = jc.createUnmarshaller();

			JAXBElement<Configuration> conf = null;

			conf = (JAXBElement<Configuration>) unmarshaller
					.unmarshal(xmlFile);

			// get and check the vales of the configuration parameter: workingDir, osmosis_home,
			// outputDir, loggingDir, destinationDir and move

			// workingDir
			workingDirectory = FileOperation.createDirectory(conf.getValue().getWorkingDir());
			// osmosis _home and osmoisis
			osmosis_home = FileOperation.createDirectory(conf.getValue().getOsmosisHome());
			osmosisBin = FileOperation.createExecutionFile(osmosis_home, osmosisPath);
			osmosis = osmosisBin.getAbsolutePath();

			// loggingDir
			String directory = conf.getValue().getLoggingDir();
			if (directory == null || directory == "") {
				System.err.println("Error! No input for logging directory.");
				System.exit(-1);
			}
			loggingDirectory = FileOperation.createDirectory(workingDirectory, directory);
			// outputDir
			directory = conf.getValue().getOutputDir();
			if (directory == null || directory == "") {
				System.err.println("Error! No input for output directory");
				System.exit(-1);
			}
			outputDirectory = FileOperation.createDirectory(workingDirectory, directory);
			// move
			Boolean move = conf.getValue().isMove();

			// run update task
			if (update) {

				doUpdate(updateFile);
			}

			// get all pipelines of the xml file
			List<Pipeline> pipelines = conf.getValue().getPipeline();

			doPipelineExecution(pipelines);
			createMD5Files();

			// all pipelines are done. now we move or copy the generated files to the
			// destination
			// directory

			directory = conf.getValue().getDestinationDir();
			if (directory != null && directory != "") {
				// destinationDir
				File destinationDirFile = null;
				destinationDirFile = FileOperation
							.createDirectory(workingDirectory, directory);

				clearDestinationDirectory(destinationDirFile);
				doMove(destinationDirFile, move);

			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * This method prints the usage of the hole runner to standard out.
	 */
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

	/*
	 * This method checks if the osmosis replication task is initialized. Also the planet file
	 * would be moved to temporary copy that would be deleted later. This is necessary, because
	 * we could not read and write at the same time to the same file.
	 */
	private static File checkOsmosisReplicationTask(String file) {

		// check necessary files of the replication task
		FileOperation.createReadFile(workingDirectory.getAbsolutePath(),
				"configuration.txt");
		FileOperation.createReadFile(workingDirectory.getAbsolutePath(), "download.lock");

		File updatefile = FileOperation
				.createReadFile(workingDirectory.getAbsolutePath(), file);

		File renamedfile = FileOperation.createWriteFile(updatefile.getParent(),
				updatefile.getName() + ".old");

		// rename update file
		boolean success = updatefile.renameTo(renamedfile);
		if (!success) {
			// File was not successfully moved
			System.err.print("Can not move file: " + updatefile.getAbsolutePath() + " to "
					+ renamedfile.getAbsolutePath());
		}
		return renamedfile;
	}

	private static String generateOsmosisReplicationTask(String updateFile) {

		// check if the osmosis replication task is initialized
		File tmpFile = checkOsmosisReplicationTask(updateFile);

		// check if planet file exists
		File updatefile = FileOperation.createWriteFile(workingDirectory.getAbsolutePath(),
				updateFile);
		if (tmpFile == null)
			throw new RuntimeException("An unexpected error occured. Update file is null.");

		// create the osmosis call for the replication task
		StringBuilder sb = new StringBuilder();
		sb.append(osmosis).append(" ");
		sb.append("--rb file=").append(tmpFile.getAbsolutePath()).append(" ")
				.append("outPipe.0=data").append(" ");
		sb.append("--rri workingDirectory=").append(workingDirectory)
				.append(" ");
		sb.append("--simc outPipe.0=change").append(" ");
		sb.append("--ac inPipe.0=data inPipe.1=change").append(" ");
		sb.append("--log-progress interval=60").append(" ");
		sb.append("--wb file=").append(updatefile.getAbsolutePath()).append(" ")
				.append("omitmetadata=false").append(" ")
				.append("compress=deflate").append(" ");

		return sb.toString();

	}

	/*
	 * This method start the osmosis replication task to update the planet file.
	 */
	private static void doUpdate(String updatefile) throws IOException, InterruptedException {
		// get osmosis update call
		String call = generateOsmosisReplicationTask(updatefile);

		// execute the update
		Process process = Runtime.getRuntime().exec(call, null, workingDirectory);

		// log update task
		File updateLog = FileOperation.createWriteFile(loggingDirectory.getAbsolutePath(),
				"update.log");
		writeLog(updateLog.getAbsolutePath(), process, true);

		// wait for process and check the termination
		if (process.waitFor() != 0) {
			throw new RuntimeException();
		}

		// delete old planet file, because a new updated file would be written
		File deleteFile = new File(updatefile + ".old");
		Boolean success = deleteFile.delete();
		if (!success)
			throw new IOException("Can not delete File " + deleteFile.getAbsolutePath());
	}

	/*
	 * This method creates the osmosis calls for each pipeline and execute them. The output of
	 * the execution would be written o a log file with the name of the pipeline.
	 */
	private static void doPipelineExecution(List<Pipeline> pipelines) throws IOException,
			InterruptedException {

		String call;
		File logfile;
		Process process;
		// run pipelines
		for (Pipeline pipeline : pipelines) {

			// traverse the XML tree to the leafs to update the output directory of the sinks
			// and generate their osmosis calls. also registered the file for what a md5 file
			// should generated.
			call = osmosis
					+ " "
					+ pipeline.generate(MD5LIST, workingDirectory.getAbsolutePath(),
							outputDirectory.getAbsolutePath());

			// run the osmosis task of the pipeline
			process = Runtime.getRuntime().exec(call, null, workingDirectory);

			// log the output of the osmosis task to a file called like the name of the pipeline
			logfile = FileOperation.createWriteFile(loggingDirectory.getAbsolutePath(),
					pipeline.getName() + ".log");

			writeLog(logfile.getAbsolutePath(), process, true);

			// wait for process and check the termination
			if (process.waitFor() != 0) {
				throw new RuntimeException();
			}

		}
	}

	/*
	 * This method delete recursive all directories and files in the destination output
	 * directory. So no files that are out of date could be in the destination directory.
	 */
	private static void clearDestinationDirectory(File destinationDirFile) {

		File[] files = destinationDirFile.listFiles();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				clearDestinationDirectory(files[i]);
			files[i].delete();
		}
	}

	/*
	 * This method is moving or is coping the files from the output directory to the destination
	 * directory.
	 */
	private static void doMove(File destinationDirFile, Boolean move) throws IOException {

		if (move) {
			// Files or directories that should be moved to the destination
			File[] files = outputDirectory.listFiles();

			for (int i = 0; i < files.length; i++) {

				// Move file to new directory
				boolean success = files[i].renameTo(new File(destinationDirFile, files[i]
						.getName()));
				if (!success) {
					// File was not successfully moved
					System.err.print("Can not move file: " + files[i].getAbsolutePath() +
							" to directory " + destinationDirFile.getAbsolutePath());
				}
			}
		} else {
			// copy mode, copy the hole content of the output directory to the destination
			// directory
			copyDirectory(outputDirectory, destinationDirFile);
		}
	}

	/*
	 * This method checks all the data of a directory. If an element is a real file, the copy
	 * method for files would be called on it. Otherwise the element is a directory and this
	 * function would be executed recursive on this directory.
	 */
	private static void copyDirectory(File sourceDir, File destDir) throws IOException {

		// create destination folder
		if (!destDir.exists()) {
			destDir.mkdir();
		}

		// get all files
		File[] children = sourceDir.listFiles();

		for (File sourceChild : children) {
			String name = sourceChild.getName();
			File destChild = new File(destDir, name);
			if (sourceChild.isDirectory()) {
				// file is directory, check recursive
				copyDirectory(sourceChild, destChild);
			} else {
				// is file to copy
				copyFile(sourceChild, destChild);
			}
		}
	}

	/*
	 * This method copies a file from a source to a destination.
	 */
	private static void copyFile(File source, File dest) throws IOException {

		if (!dest.exists())
			dest.createNewFile();

		InputStream in = null;
		OutputStream out = null;

		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int len;

			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}

	/*
	 * This method create MD5 checksums of files an save them to an MD5 file. This file is
	 * called like the input file, only ends with ".md5".
	 * 
	 * Attention: This method use the unix program md5sum, therefore it only works on unix
	 * system, that has installed this program.
	 */
	private static void createMD5Files() throws IOException, InterruptedException {

		String call;
		Process process;
		File md5file;
		File tmpFile;

		// for each file of the MD5List a file would be generated
		for (String file : MD5LIST) {
			tmpFile = new File(file);

			// generate call for file
			call = md5sum + " " + tmpFile.getName();

			// execute the call
			process = Runtime.getRuntime().exec(call, null,
					new File(tmpFile.getParent()));

			md5file = FileOperation.createWriteFile(tmpFile.getParent(),
					tmpFile.getName() + ".md5");

			// no append to MD5 files
			BufferedWriter bw = new BufferedWriter(new FileWriter(md5file, false));
			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			String line;

			while ((line = br.readLine()) != null) {
				bw.append(line).append(NEWLINE);
				bw.flush();
			}

			bw.close();
			br.close();

			// wait for process and check the termination
			if (process.waitFor() != 0) {
				throw new RuntimeException();
			}

		}
	}

	/*
	 * This method write error logs of a process to file.
	 */
	private static void writeLog(String logfile, Process process, Boolean append)
			throws IOException {

		BufferedWriter bw = new BufferedWriter(new FileWriter(logfile, append));
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		String line = null;

		while ((line = br.readLine()) != null) {
			bw.append(line).append(NEWLINE);
			bw.flush();
		}

		bw.close();
		br.close();
	}
}
