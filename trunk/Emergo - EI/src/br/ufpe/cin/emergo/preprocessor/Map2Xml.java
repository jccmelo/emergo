package br.ufpe.cin.emergo.preprocessor;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;

public class Map2Xml {

	public static Map<String, Integer> createRandomMap() {
		Map<String, Integer> m = new HashMap<String, Integer>(5);
		for (char i = 'a'; i < 'f'; i++) {
			m.put(String.valueOf(i), (int) (Math.random() * 99));
		}
		return m;
	}

	public static <K, V> void toXml(Map<K, V> map, Writer wr)
			throws IOException, XMLStreamException {
		XMLStreamWriter xsw = null;
		try {
			try {
				XMLOutputFactory xof = XMLOutputFactory.newInstance();
				// If you want pretty-printing, you can use:
//				xsw = new javanet.staxutils.IndentingXMLStreamWriter(xof.createXMLStreamWriter(out));
				xsw = xof.createXMLStreamWriter(wr);
//				xsw.writeStartDocument("utf-8", "1.0");
				xsw.writeStartElement("ck");

				// Do the Collection
				for (Map.Entry<K, V> e : map.entrySet()) {
					xsw.writeStartElement("config");
					xsw.writeAttribute("expression", e.getKey().toString());
					xsw.writeAttribute("line", e.getValue().toString());
					xsw.writeEndElement();
				}
				xsw.writeEndElement();
				xsw.writeEndDocument();
			} finally {
				if (wr != null) {
					try {
						wr.close();
					} catch (IOException e) { /* ignore */
					}
				}
			}// end inner finally
		} finally {
			if (xsw != null) {
				try {
					xsw.close();
				} catch (XMLStreamException e) { /* ignore */
				}
			}
		}
	}
	
	public static void main(String[] args) {
		Map<String, Integer> m = Map2Xml.createRandomMap();
		System.out.println("Starting map:");
		System.out.println(m);
		// Now write it as xml
		try {
			Map2Xml.toXml(m, new FileWriter(args[0]));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

}