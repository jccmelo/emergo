package br.ufpe.cin.emergo.editor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

//public class IfDefSourceViewerConfiguration extends JavaSourceViewerConfiguration {
	
public class IfDefSourceViewerConfiguration extends SourceViewerConfiguration {

	private IfDefScanner ifDefScanner;

	public IfDefSourceViewerConfiguration() {
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
//	    PresentationReconciler reconciler = new PresentationReconciler();
//	    DefaultDamagerRepairer repairer = new DefaultDamagerRepairer(getIfDefScanner());
//
//	    reconciler.setDamager(repairer, IDocument.DEFAULT_CONTENT_TYPE);
//	    reconciler.setRepairer(repairer, IDocument.DEFAULT_CONTENT_TYPE);

//		JavaColorProvider provider= JavaEditorEnvironment.getJavaColorProvider();
		
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(JavaPlugin.getDefault().getJavaTextTools().getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(JavaPlugin.getDefault().getJavaTextTools().getJavaDocScanner());
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_DOC);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_DOC);

		dr = new DefaultDamagerRepairer(JavaPlugin.getDefault().getJavaTextTools().getMultilineCommentScanner());
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_MULTI_LINE_COMMENT);
		
	    return reconciler;
	}

	protected IfDefScanner getIfDefScanner() {
	    if (ifDefScanner == null) {
	    	ifDefScanner = new IfDefScanner();
	    	//Token token = new Token(new TextAttribute(new Color(Display.getCurrent(), new RGB(0, 0, 0))));
	    	//ifDefScanner.setDefaultReturnToken(token);
	    }
	    return ifDefScanner;
	}

}