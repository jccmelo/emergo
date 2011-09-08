package br.ufpe.cin.emergo.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Handler for the br.ufal.cideei.commands.DoCompute extension command.
 * 
 * @author Társis
 * 
 */
public class EmergoHandler extends AbstractHandler {

	private ICompilationUnit compilationUnit;
	private CompilationUnit jdtCompilationUnit;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// XXX Try block for debugging only.
		try {
			/*
			 * Mechanism for passing through information that could make the dependency finder easier/faster to
			 * implement.
			 */
			final Map<Object, Object> options = new HashMap<Object, Object>();

			ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
			Shell shell = HandlerUtil.getActiveShellChecked(event);

			if (!(selection instanceof ITextSelection))
				throw new ExecutionException("Not a text selection");

			IFile textSelectionFile = (IFile) HandlerUtil.getActiveEditorChecked(event).getEditorInput().getAdapter(IFile.class);
			// ITextSelection textSelection = (ITextSelection) selection;

			ITextEditor editor = (ITextEditor) HandlerUtil.getActiveEditorChecked(event);
			IDocumentProvider provider = editor.getDocumentProvider();
			IDocument document = provider.getDocument(editor.getEditorInput());
			ITextSelection textSelection = (ITextSelection) editor.getSite().getSelectionProvider().getSelection();

			// The project from which the file belongs.
			IProject project = textSelectionFile.getProject();

			CompilationUnit compilationUnit = getCompilationUnit(textSelectionFile);
			SelectionNodesVisitor selectionVisitor = new SelectionNodesVisitor(textSelection);
			compilationUnit.accept(selectionVisitor);
			Set<ASTNode> nodesInSelection = selectionVisitor.getNodes();
			MethodDeclaration parentMethod = getParentMethod(nodesInSelection);
			IMethod method = (IMethod) this.compilationUnit.getElementAt(parentMethod.getStartPosition());

			options.put("type", method.getDeclaringType().getFullyQualifiedName());
			options.put("methodDescriptor", getMethodDescriptor(method));
			options.put("method", method.getElementName());
			
			/*
			 * Finds out the (partial) project's classpath as a list of Files. Each File either points to a source
			 * folder, or an archive like a jar.
			 */
			IJavaProject javaProject = JavaCore.create(project);
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

			options.put("classpath", classpath);

			/*
			 * Holds the textual selection information that needs to passed along to the underlying compiler
			 * infrastructure
			 */
			final SelectionPosition selectionPosition = SelectionPosition.builder().length(textSelection.getLength()).offSet(textSelection.getOffset()).startLine(textSelection.getStartLine()).startColumn(calculateColumnFromOffset(document, textSelection.getOffset())).endLine(textSelection.getEndLine()).endColumn(calculateColumnFromOffset(document, textSelection.getOffset() + textSelection.getLength())).filePath(textSelectionFile.getLocation().toOSString()).build();

			try {
				DependencyFinder.findFromSelection(DependencyFinderID.JWCOMPILER, selectionPosition, options);
			} catch (FileNotFoundException e) {
				ErrorDialog.openError(shell, "Error", "An error has occured", null);
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private MethodDeclaration getParentMethod(Set<ASTNode> nodesInSelection) {
		ASTNode parent;
		for (ASTNode node : nodesInSelection) {
			parent = node;
			do {
				if (parent.getNodeType() == ASTNode.METHOD_DECLARATION) {
					return (MethodDeclaration) parent;
				}
			} while ((parent = parent.getParent()) != null);
		}
		return null;
	}

	private CompilationUnit getCompilationUnit(IFile textSelectionFile) {
		this.compilationUnit = JavaCore.createCompilationUnitFrom(textSelectionFile);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(compilationUnit);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		this.jdtCompilationUnit = (CompilationUnit) parser.createAST(null);
		return jdtCompilationUnit;
	}

	private String getMethodDescriptor(IMethod method) throws JavaModelException {
		return Signature.createMethodSignature(method.getParameterTypes(), method.getReturnType().toString());
	}

	public int calculateColumnFromOffset(IDocument doc, int offset) {
		int sumpos = 0;
		int i = 0;

		try {
			while (sumpos + doc.getLineLength(i) - 1 < offset) {
				sumpos += doc.getLineLength(i);
				++i;
			}
		} catch (BadLocationException e) {
			// XXX ???
			e.printStackTrace();
		}

		return offset - sumpos + 1;
	}
}
