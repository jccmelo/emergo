package br.ufpe.cin.emergo.listeners;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Range;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import br.ufpe.cin.emergo.core.DependencyFinder;
import br.ufpe.cin.emergo.core.EmergoException;
import br.ufpe.cin.emergo.core.JWCompilerDependencyFinder;
import br.ufpe.cin.emergo.core.SelectionPosition;
import br.ufpe.cin.emergo.editor.IfDefJavaEditor;
import br.ufpe.cin.emergo.properties.SystemProperties;
import br.ufpe.cin.emergo.util.EmergoConstants;
import dk.au.cs.java.compiler.Flags;
import dk.au.cs.java.compiler.Main;
import dk.au.cs.java.compiler.ifdef.IfDefUtil;
import dk.au.cs.java.compiler.ifdef.IfDefVarSet;
import dk.au.cs.java.compiler.ifdef.SharedSimultaneousAnalysis;
import dk.au.cs.java.compiler.node.AProgram;
import dk.au.cs.java.compiler.node.PIfdefExp;
import dk.au.cs.java.compiler.type.environment.ClassEnvironment;
import dk.brics.util.file.WildcardExpander;

public class Selection implements ISelectionListener{

	IfDefJavaEditor ifDefJavaEditor;
	ISelection selection;
	IPath location;
	IJavaProject javaProject;
	private static AProgram rootNode;
	ArrayList<String> choosenFeatures;
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		this.selection = selection;
		if(part instanceof IfDefJavaEditor && selection instanceof TextSelection 
				&& ((TextSelection)selection).getEndLine() ==0 && ((TextSelection)selection).getStartLine() ==0){
			IDocument d = ((IfDefJavaEditor) part).getDocument();
			IFile textSelectionFile = (IFile) part.getAdapter(IFile.class);
			ITextEditor  te= (ITextEditor) part;
			IFile textSelectionFile2 = (IFile) te.getEditorInput().getAdapter(IFile.class);
			textSelectionFile2.getFullPath();
			textSelectionFile2.getRawLocation().toFile();
			this.location = textSelectionFile2.getRawLocation();
			
			
	//		Map<ConfigSet, Collection<Range<Integer>>> ifDefLineMapping = DependencyFinder.getIfDefLineMapping(textSelectionFile2.getRawLocation().toFile());
	//		Set<Entry<ConfigSet, Collection<Range<Integer>>>> configSets = ifDefLineMapping.entrySet();
	//		for (Entry<ConfigSet, Collection<Range<Integer>>> entry : configSets) {
	//			System.out.println(entry);
	//		}
			
			IProject project = textSelectionFile2.getProject();
	//		IProject project = textSelectionFile.getProject();
			/*
			 * Finds out the (partial) project's classpath as a list of Files. Each File either points to a source
			 * folder, or an archive like a jar.
			 */
			IJavaProject javaProject = JavaCore.create(project);
			this.javaProject = javaProject;
			// For a test Purpose
			try {
				String allFeatures = javaProject.getResource().getPersistentProperty(SystemProperties.CHOOSEN_FEATURES);
				String[] auxChoosen =allFeatures.split(";"); 
				choosenFeatures = new ArrayList<String>(); 
				for (int i = 0; i < auxChoosen.length; i++) {
					choosenFeatures.add(auxChoosen[i].trim());
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				auxiliarHidding(textSelectionFile2, part, d, selection);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			if (ifDefJavaEditor != (IfDefJavaEditor)part){
//				ifDefJavaEditor = (IfDefJavaEditor) part;
//				ifDefJavaEditor.colapseAll();				
//			}
		}
	}

	private void auxiliarHidding(IFile file, IWorkbenchPart editor, IDocument d, ISelection selection) throws CoreException, BadLocationException{
//		Object marker = ((IStructuredSelection) selection).getFirstElement();
//		ConfigSet selectedConfigSet = (ConfigSet) ((MarkerItem) marker).getMarker().getAttribute(IMarker.TEXT);
		try {
			doNeccessaryJob();
		} catch (EmergoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<PIfdefExp, Collection<Range<Integer>>> test = DependencyFinder.getIfDefLineMapping(file.getRawLocation().toFile(), rootNode);
		Set<Entry<PIfdefExp, Collection<Range<Integer>>>> testSet = test.entrySet();
		int lastSourceLineNumber2 = 0;
		
		Map<PIfdefExp, Collection<Integer>> ifDefLineMapping = new HashMap();
		
		for (Entry<PIfdefExp, Collection<Range<Integer>>> entry : testSet) {
			Collection<Range<Integer>> tt = entry.getValue();
			Object[] arrayTest = tt.toArray();
			Iterator<Range<Integer>> iterator = tt.iterator();
			Range<Integer> range = iterator.next();
			List<Integer> aux = new ArrayList<Integer>();
			aux.add(range.getMinimum());
			aux.add(range.getMaximum());
			if(!choosenFeatures.contains(entry.getKey().toString().trim()))
				ifDefLineMapping.put(entry.getKey(), aux);
//			Deque<Integer> sourceLineNumbers = new ArrayDeque<Integer>(entry.getValue());
//			sourceLineNumbers.removeFirst();
//			lastSourceLineNumber = sourceLineNumbers.getLast();
//			sourceLineNumbers.addLast(++lastSourceLineNumber);
//
//			entry.setValue(sourceLineNumbers);
		}
		
		/*
		 * FIXME Temporary computation to hide the endif statement and keep the ifdef.
		 */
		Set<Entry<PIfdefExp, Collection<Integer>>> configSets = ifDefLineMapping.entrySet();
		
		int lastSourceLineNumber = 0;
		for (Entry<PIfdefExp, Collection<Integer>> entry : configSets) {
			Deque<Integer> sourceLineNumbers = new ArrayDeque<Integer>(entry.getValue());
			Integer first = sourceLineNumbers.removeFirst();
			lastSourceLineNumber = sourceLineNumbers.getLast();
			sourceLineNumbers.addLast(++lastSourceLineNumber);
			sourceLineNumbers.removeFirst();
			sourceLineNumbers.addFirst(first);
			entry.setValue(sourceLineNumbers);
		}
		/*
		 * ConcurrentHashMap to avoid ConcurrentModificationException.
		 */
		ConcurrentHashMap<PIfdefExp, Collection<Integer>> concurrent = new ConcurrentHashMap<PIfdefExp, Collection<Integer>>();
		concurrent.putAll(ifDefLineMapping);
		Iterator<PIfdefExp> iterator = concurrent.keySet().iterator();
		
		while (iterator.hasNext()) {
			iterator.next();//ConfigSet configSet = iterator.next();
			
			/*
			 * configSet => ifdef feature expression (editor);
			 * selectedConfigSet ==> ifdef feature expression (emergo table view).
			 * 
			 * If both configSets are equivalent, we remove the configSet (editor).
			 * This way, its positions are ignored by the hiding mechanism.
			 * Therefore, configSet will not be hidden.
			 */
//			if ((configSet.and(selectedConfigSet)).equals(configSet)
//					|| (selectedConfigSet.and(configSet)).equals(selectedConfigSet)) {
//				concurrent.remove(configSet);
//			}
		}
		
		if (concurrent.size() == 0) {
			System.out.println("no computation");
		}	
		
		/*
		 * If concurrent.size() == 0, we still need to update the editor
		 * in case where we already have projections there. 
		 */
		List<Position> positions = createPositions(d, concurrent);
		List<Position> positionsEmpty = new ArrayList<Position>();
		
		/*
		 * The action which updates the editor to show the folding areas.
		 */
		if (editor instanceof IfDefJavaEditor) {
			((IfDefJavaEditor) editor).expandAllAnnotations(d.getLength());
			((IfDefJavaEditor) editor).removeAllAnnotations();
			
			((IfDefJavaEditor) editor).updateFoldingStructure(positionsEmpty);
			((IfDefJavaEditor) editor).updateFoldingStructure(positions);
		}

	}

	private List<Position> createPositions(
			IDocument d, Map<PIfdefExp, Collection<Integer>> featuresLineNumbers)
					throws BadLocationException {
		
		Iterator<PIfdefExp> featureNames;
		PIfdefExp featureName;
		Collection<Integer> lines;
		List<Position> positions = new ArrayList<Position>();
		
		Iterator<Integer> iteratorInteger = null;
		int line = 0;
		int previousLine = 0;
		int length = 0;
		int offset = 0;
		boolean newAnnotation = false;
		boolean first = true;
		featureNames = featuresLineNumbers.keySet().iterator();
		while (featureNames.hasNext()) {
			
			line = 0;
			previousLine = 0;
			length = 0;
			offset = 0;
			first = true;
			
			featureName = featureNames.next();
			lines = featuresLineNumbers.get(featureName);
			
			if (lines.size() > 1) {
			
				iteratorInteger = lines.iterator();
				
				while (iteratorInteger.hasNext()) {
					if (newAnnotation) {
						try {
							offset = d.getLineOffset(line);
							length = d.getLineLength(line );
						} catch(BadLocationException e) {
							length = 0;
							offset = d.getLineOffset(line);
						}
						newAnnotation = false;
					} else {
						line = iteratorInteger.next().intValue() - 1;
						if (first == true) {
							try {
								offset = d.getLineOffset(line);
								length = d.getLineLength(line);
							} catch (BadLocationException e) {
								length = 0;
								offset = d.getLineOffset(line);
							}
							first = false;
						}
					}
					length = length + d.getLineLength(line);
					if (previousLine > 0 && line > previousLine + 1) {
						length = 0;
						for(int i = previousLine; i<=line-1; i++){
							length = length + d.getLineLength(i);							
						}
						previousLine = line;
						newAnnotation = true;
						positions.add(new Position(offset,length));
						break;
					}
					previousLine = line;							
				}
				Position p = new Position(offset, length);
				if(!positions.contains(p))
					positions.add(new Position(offset,length));
			}
		}
		return positions;
	}
	
	private void doNeccessaryJob() throws JavaModelException, EmergoException{
		
		SelectionPosition selectionPosition = new SelectionPosition(200,4,1,2,3,4,location.toString());
		Main.resetCompiler();
		
		// The file in which the selection resides.
		File selectionFile;
		selectionFile = new File(selectionPosition.getFilePath());
		if (!selectionFile.exists()) {
			throw new EmergoException("File " + selectionPosition.getFilePath()
					+ " not found.");
		}
		String rawLocation = this.location.toOSString();
		String rawProjLocation = this.javaProject.getPath().toOSString();
		for(int i=0;i+rawProjLocation.length()<rawLocation.length();i++){
			if(rawLocation.subSequence(i, i+rawProjLocation.length()).equals(rawProjLocation))
				rawLocation = rawLocation.substring(0,i+rawProjLocation.length());
		}
		
		String rootpath = (String) rawLocation;//options.get("rootpath");
		File ifdefSpecFile = new File(rootpath + File.separator + EmergoConstants.FEATURE_MODEL_FILE_NAME);
		if (!ifdefSpecFile.exists()) {
			throw new RuntimeException(
					"The " + EmergoConstants.FEATURE_MODEL_FILE_NAME + " of the project was not found at " + rootpath);
		}

		// Holds a the list of Files to be parsed by the compiler.
		List<File> files = new ArrayList<File>();

		/*
		 * Builds the classpath in the format needed on Johnni Winther's
		 * compiler. Paths should be separated by a whitespace and may contain
		 * wildcards like ** and *.
		 * 
		 * For example:
		 * 
		 * ./src/**|/*.java ./tst/my/folder/*.java
		 * 
		 * (Ignore the '|' above)
		 */
		@SuppressWarnings("unchecked")
		
		IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
			List<File> classpath = new ArrayList<File>();
			for (IClasspathEntry cpEntry : resolvedClasspath) {
				switch (cpEntry.getEntryKind()) {
				case IClasspathEntry.CPE_CONTAINER:
					classpath.add(cpEntry.getPath().makeAbsolute().toFile());
					break;
				case IClasspathEntry.CPE_SOURCE:
					classpath.add(ResourcesPlugin.getWorkspace().getRoot().getFolder(cpEntry.getPath()).getLocation().toFile());
				}
			}
		
		//List<File> classpath = (List<File>) options.get("classpath");

		for (File file : classpath) {
			if (file.isDirectory()) {
				String filepath = file.getPath() + File.separator + "**"
						+ File.separator + "*.java";
				List<File> expandWildcards = WildcardExpander
						.expandWildcards(filepath);
				files.addAll(expandWildcards);

			} else if (file.isFile() && file.exists()) {
				// XXX also include .jar files.
			}
		}

		/*
		 * XXX find out classpath
		 * 
		 * XXX WARNING! This static method causes the Feature Model, among other
		 * things, to be RESETED. It SHOULD NOT be called AFTER the parsing of
		 * the ifdef specification file in any circustance.
		 */
		ClassEnvironment.init(System.getenv("CLASSPATH"), true);

		EnumSet<Flags> flags = (EnumSet<Flags>) Main.FLAGS;
		flags.add(Flags.IFDEF);
		SharedSimultaneousAnalysis.useSharedSetStrategy(true);
		IfDefUtil.parseIfDefSpecification(ifdefSpecFile);

		IfDefVarSet.getIfDefBDDFactory();

		// The root point of the parsed program.
		this.rootNode = JWCompilerDependencyFinder.parseProgram(files);
	}
}
