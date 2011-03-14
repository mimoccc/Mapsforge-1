package org.mapsforge.preprocessing.automization;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

public class TestRunner {

	private static Logger LOGGER =
			Logger.getLogger(TestRunner.class.getName());

	public static void main(String[] args) throws Exception {
		JAXBContext jc = JAXBContext.newInstance("org.mapsforge.preprocessing.automization");
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<Configuration> conf = (JAXBElement<Configuration>) unmarshaller
				.unmarshal(new File("src/org/mapsforge/preprocessing/automization/test2.xml"));

		List<Pipeline> pipelines = conf.getValue().getPipeline();

		File workspaceFile = new File(conf.getValue().getWorkspace());
		if (!workspaceFile.exists())
			throw new IllegalArgumentException("cannot find workspace: "
					+ workspaceFile.getAbsolutePath());
		if (!workspaceFile.isDirectory())
			throw new IllegalArgumentException("workspace not a directory: "
					+ workspaceFile.getAbsolutePath());

		File osmosis_home = new File(conf.getValue().getOsmosisHome());
		if (!osmosis_home.exists())
			throw new IllegalArgumentException("cannot find OSMOSIS_HOME: "
					+ osmosis_home.getAbsolutePath());
		if (!osmosis_home.isDirectory())
			throw new IllegalArgumentException("OSMOSIS_HOME not a directory: "
					+ osmosis_home.getAbsolutePath());

		File osmosisBin = new File(osmosis_home, "bin/osmosis");
		if (!osmosisBin.exists())
			throw new RuntimeException("unable to access osmosis binary: "
					+ osmosisBin.getAbsolutePath());

		String osmosis = osmosisBin.getAbsolutePath();
		for (Pipeline pipeline : pipelines) {
			Process p = Runtime.getRuntime().exec(osmosis + " " + pipeline.generate(), null,
					workspaceFile);
			// BufferedReader err = new BufferedReader(new
			// InputStreamReader(p.getErrorStream()));
			if (p.waitFor() != 0) {
				String line = null;
				// while ((line = err.readLine()) != null) {
				// LOGGER.info(line);
				// }
				// err.close();
				LOGGER.severe("error while executing pipeline: " + pipeline.getName()
						+ ", see logs for more information. Stopping complete processing");
				System.exit(1);
			}
		}
	}
}
