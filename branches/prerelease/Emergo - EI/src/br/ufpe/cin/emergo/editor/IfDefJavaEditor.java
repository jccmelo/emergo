package br.ufpe.cin.emergo.editor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;

import br.ufpe.cin.emergo.listeners.Selection;

public class IfDefJavaEditor extends CompilationUnitEditor {

	//public static final String EDITOR_CIDEEI_ID = "br.ufal.cideei.editor.ColoredFoldingCompilationUnitEditor";
	
	private Set<List<Position>> positionsIfDefs;
	private Annotation[] oldAnnotationsIfDefs;
	private ProjectionAnnotationModel annotationModelIfDefs;

	public IfDefJavaEditor() {
		this.annotationModelIfDefs = new ProjectionAnnotationModel();
		
		
		/**
		 * Steps needed for syntax highlighting
		 */
//		IPreferenceStore preferenceStore = getPreferenceStore();
//		JavaColorManager javaColorManager = (JavaColorManager) JavaPlugin.getDefault().getJavaTextTools().getColorManager();
//
//		JavaPlugin.getDefault().getJavaTextTools().get
		
		//setSourceViewerConfiguration(new IfDefSourceViewerConfiguration(javaColorManager, preferenceStore, this, null));
		
//		setSourceViewerConfiguration(new IfDefSourceViewerConfiguration());
	}

//	public void doSetInput(IEditorInput input) throws CoreException {
//		 super.doSetInput(input);
//
//		 IPreferenceStore store = getPreferenceStore();
//		 IfDefJavaTextTools textTools = new IfDefJavaTextTools(store);
//		 IfDefJavaSourceViewerConfiguration ifDefSourceViewerConfiguration = 
//			 new IfDefJavaSourceViewerConfiguration(textTools, this);
//		 setSourceViewerConfiguration(ifDefSourceViewerConfiguration);
//	}

	public void colapseAll(){
		/**
		 * Lines for collapsing elements.
		 */
		this.annotationModelIfDefs.collapseAll(0, getDocument().getLength());
	}
	
	public void instantiatePositions() {
		this.positionsIfDefs = new HashSet<List<Position>>();
	}
	
	public Set<List<Position>> getPositions() {
		return this.positionsIfDefs;
	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		//test
		IWorkbenchPartSite site = getSite();
		IWorkbenchWindow window = site.getWorkbenchWindow();
		window.getSelectionService().addPostSelectionListener(new Selection());
		//END of test
		
		ProjectionViewer viewer = (ProjectionViewer) getSourceViewer();

		viewer.disableProjection();
		viewer.enableProjection();
		
		this.annotationModelIfDefs = viewer.getProjectionAnnotationModel();
	}
	
	public void updateFoldingStructure(List<Position> positions) {
		Annotation[] annotations = new Annotation[positions.size()];

		Map<ProjectionAnnotation,Position> newAnnotations = new HashMap<ProjectionAnnotation, Position>();
		
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

	public IDocument getDocument() {
		return getSourceViewer().getDocument();
	}
	
}