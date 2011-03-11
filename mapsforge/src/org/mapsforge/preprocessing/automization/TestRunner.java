package org.mapsforge.preprocessing.automization;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

public class TestRunner {

	public static void main(String[] args) throws Exception {
		JAXBContext jc = JAXBContext.newInstance("org.mapsforge.preprocessing.automization");
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<Configuration> conf = (JAXBElement<Configuration>) unmarshaller
				.unmarshal(new File("src/org/mapsforge/preprocessing/automization/test.xml"));
		System.out.println(conf.getValue().getPipeline().get(0).getSource().getValue()
				.generate());
	}

}
