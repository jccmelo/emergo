package br.ufpe.cin.emergo.util;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.Position;

public class ColapsedFeatures {
	
	public static List<Position> getPositionsFromColapsedFeatures(){
		List<Position> positions = new ArrayList<Position>();
		Position position = new Position(85, 125);
		positions.add(position);
		return  positions;
		
	}
}
