package br.ufpe.cin.emergo.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public class XMLUtil {

	List<ProjectXML> projects = new ArrayList<ProjectXML>();
	
	// singleton
	private static final XMLUtil instance = new XMLUtil();

	private XMLUtil(){
		Element rootNode = loadXML();
		populateXML(rootNode);
	}
	
	public static XMLUtil getXMLUtil() {
		return instance;
	}

	private void populateXML(Element rootNode) {
		try {
			List projectList = rootNode.getChildren("project");
			for (int i = 0; i < projectList.size(); i++) {
				Element node = (Element) projectList.get(i);

				String name = node.getChildText("name");
				String classpath = node.getChildText("classpath");
				String fileExtension = node.getChildText("fileExtension");

				List<PositionXML> selPosXML = new ArrayList<PositionXML>();

				List positionList = node.getChildren("selectionPosition");
				for (int j = 0; j < positionList.size(); j++) {
					Element selPosNode = (Element) positionList.get(j);

					int sl = Integer.parseInt(selPosNode.getChildText("startLine"));
					int el = Integer.parseInt(selPosNode.getChildText("endLine"));
					int sc = Integer.parseInt(selPosNode.getChildText("startColumn"));
					int ec = Integer.parseInt(selPosNode.getChildText("endColumn"));
					String cn = selPosNode.getChildText("className");
					String mn = selPosNode.getChildText("methodName");

					PositionXML pos = new PositionXML(sl, el, sc, ec, cn, mn);
					selPosXML.add(pos);

				}
				ProjectXML projectXML = new ProjectXML(name, classpath, fileExtension, selPosXML);
				projects.add(projectXML);
			}
		
		} catch (Exception e) {
			System.err.println("Fill out the file properties.xml correctly!");
		}
	}
	
	private Element loadXML() {
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File("properties.xml");
		
		Document document;
		try {
			document = (Document) builder.build(xmlFile);
			Element rootNode = document.getRootElement();
			return rootNode;
		} catch (Exception e) {
			System.err.println("Failed to load file properties.xml");
		}
		return null;
	}
	
	public List<ProjectXML> getProjects() {
		return projects;
	}

	public static void main(String[] args) {
		XMLUtil xml = XMLUtil.getXMLUtil();
		
		List<ProjectXML> list = xml.getProjects();
		for (ProjectXML projectXML : list) {
			System.out.println(projectXML.getName());
			System.out.println(projectXML.getClasspath());
			
			List<PositionXML> pos = projectXML.getPos();
			for (PositionXML positionXML : pos) {
				System.out.println(positionXML.getStartLine());
				System.out.println(positionXML.getEndLine());
				System.out.println(positionXML.getStartColumn());
				System.out.println(positionXML.getEndColumn());
				System.out.println(positionXML.getLength());
				System.out.println(positionXML.getOffSet());
				
				System.out.println(positionXML.getClassName());
				System.out.println(positionXML.getMethodName());
				System.out.println("----------------");
			}
			System.out.println("================");
		}
	}
}
