package br.ufpe.cin.emergo.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;

public class IfDefJavaEditor extends CompilationUnitEditor {

	//public static final String EDITOR_CIDEEI_ID = "br.ufal.cideei.editor.ColoredFoldingCompilationUnitEditor";
	
	private HashSet<ArrayList<Position>> positionsIfDefs;
	private Annotation[] oldAnnotationsIfDefs;
	private ProjectionAnnotationModel annotationModelIfDefs;
		
	public IfDefJavaEditor() {
		this.annotationModelIfDefs = new ProjectionAnnotationModel();
	}
	
	public void instantiatePositions() {
		this.positionsIfDefs = new HashSet<ArrayList<Position>>();
	}
	
	public HashSet<ArrayList<Position>> getPositions() {
		return this.positionsIfDefs;
	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
//		Caret
//		try {
//			IWorkbenchPartSite site = getSite();
//			IWorkbenchWindow window = site.getWorkbenchWindow();
//			window.getSelectionService().addPostSelectionListener(new ChangeSelectionListener());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();

		viewer.disableProjection();
		viewer.enableProjection();
		
		this.annotationModelIfDefs = viewer.getProjectionAnnotationModel();
	}
	
	public void updateFoldingStructure(ArrayList<Position> positions) {
		Annotation[] annotations = new Annotation[positions.size()];

		HashMap<ProjectionAnnotation,Position> newAnnotations = new HashMap<ProjectionAnnotation, Position>();
		
		for (int i = 0; i < positions.size(); i++) {
			ProjectionAnnotation annotation = new ProjectionAnnotation();
			annotation.markCollapsed();
			
			newAnnotations.put(annotation,positions.get(i));
			annotationModelIfDefs.addAnnotation(annotation, (Position) positions.get(i));
			
			annotations[i] = annotation;
		}
		
		oldAnnotationsIfDefs = annotations;
	}
	
	public ProjectionAnnotationModel getProjectionAnnotationModel() {
		return this.annotationModelIfDefs;
	}
	
	@SuppressWarnings("unchecked")
	public void expandAllAnnotations(int fileLength) {
		this.annotationModelIfDefs.expandAll(0, fileLength);
	}

	public void removeAllAnnotations() {
		this.annotationModelIfDefs.removeAllAnnotations();
	}

}