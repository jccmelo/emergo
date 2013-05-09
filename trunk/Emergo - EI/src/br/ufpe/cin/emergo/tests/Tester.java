package br.ufpe.cin.emergo.tests;

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import br.ufpe.cin.emergo.util.DiffChecker;
import br.ufpe.cin.emergo.util.PositionXML;
import br.ufpe.cin.emergo.util.ProjectXML;
import br.ufpe.cin.emergo.util.XMLUtil;

public class Tester extends TestCase {

	public void testEmergo() throws Exception {
		
		TestSuiteEmergo suite = new TestSuiteEmergo();
		DiffChecker dc = new DiffChecker();
		
		XMLUtil xml = XMLUtil.getXMLUtil();
		List<ProjectXML> list = xml.getProjects();
		for (ProjectXML projectXML : list) {
			String projectName = projectXML.getName();
			String classpath = projectXML.getClasspath();
			String fileExtension = projectXML.getFileExtension();
			
			List<PositionXML> pos = projectXML.getPos();
			for (PositionXML positionXML : pos) {
				//executes emergo after the changes
				suite.testTool(projectName, classpath, fileExtension, positionXML);
				//regression test
				String className = positionXML.getClassName();
				assertTrue(dc.doTest(className, className + "-" + new Date().getDate()));
			}
		}
	}
	
}
