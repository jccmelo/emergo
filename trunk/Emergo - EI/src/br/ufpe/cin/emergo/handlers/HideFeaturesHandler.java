package br.ufpe.cin.emergo.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.Range;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.views.markers.MarkerItem;

import br.ufpe.cin.emergo.core.ConfigSet;
import br.ufpe.cin.emergo.core.DependencyFinder;
import br.ufpe.cin.emergo.editor.IfDefJavaEditor;
import br.ufpe.cin.emergo.util.ResourceUtil;

public class HideFeaturesHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = (ISelection) HandlerUtil
				.getActiveMenuSelection(event);
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		Object marker = ((IStructuredSelection) selection).getFirstElement();

		try {

			if (marker instanceof MarkerItem) {

				/*
				 * Code which gets the document on the annotations will be
				 * placed.
				 */
				IfDefJavaEditor editor = (IfDefJavaEditor) HandlerUtil
						.getActiveEditor(event).getAdapter(
								IfDefJavaEditor.class);
				IDocument d = editor.getDocument();

				/*
				 * Get the file which will be analysed by the visitor.
				 */
				IFile file = (IFile) HandlerUtil.getActiveEditorChecked(event)
						.getEditorInput().getAdapter(IFile.class);

				/*
				 * selectedConfigSet ==> ifdef feature expression (emergo table
				 * view)
				 */
				ConfigSet selectedConfigSet = (ConfigSet) ((MarkerItem) marker)
						.getMarker().getAttribute(IMarker.TEXT);

				// Example of featureRangeLineNumbers
				// {(COPY)=[[12..17]], (SMS)=[[28..30], [19..24]]}
				Map<ConfigSet, Collection<Range<Integer>>> featureRangeLineNumbers = DependencyFinder
						.getIfDefLineMapping(file.getRawLocation().toFile());

				Map<ConfigSet, Collection<Integer>> ifDefLineMapping = new HashMap<ConfigSet, Collection<Integer>>();

				Set<Entry<ConfigSet, Collection<Range<Integer>>>> entrySet = featureRangeLineNumbers
						.entrySet();

				for (Entry<ConfigSet, Collection<Range<Integer>>> entry : entrySet) {
					Collection<Range<Integer>> rangeCollection = entry
							.getValue();

					Collection<Integer> lineNumbers = new ArrayList<Integer>();

					for (Range<Integer> range : rangeCollection) {
						Integer minimum = range.getMinimum();
						Integer maximum = range.getMaximum();

						// Object[] array = generateListFromXtoY(minimum,
						// maximum).toArray();
						// Arrays.sort(array);
						// Arrays.asList(array);

						lineNumbers.addAll(generateListFromXplus1toY(minimum,
								maximum));
					}

					System.out.println(lineNumbers + "    " + entry.getKey());
					ifDefLineMapping.put(entry.getKey(), lineNumbers);
				}

				/*
				 * ConcurrentHashMap to avoid ConcurrentModificationException.
				 */
				ConcurrentHashMap<ConfigSet, Collection<Integer>> concurrent = new ConcurrentHashMap<ConfigSet, Collection<Integer>>();
				concurrent.putAll(ifDefLineMapping);
				Iterator<ConfigSet> iterator = concurrent.keySet().iterator();

				while (iterator.hasNext()) {
					ConfigSet configSet = iterator.next();

					/*
					 * configSet => ifdef feature expression (editor);
					 * selectedConfigSet ==> ifdef feature expression (emergo
					 * table view).
					 * 
					 * If both configSets are equivalent, we remove the
					 * configSet (editor). This way, its positions are ignored
					 * by the hiding mechanism. Therefore, configSet will not be
					 * hidden.
					 */
					if ((configSet.and(selectedConfigSet)).equals(configSet)
							|| (selectedConfigSet.and(configSet))
									.equals(selectedConfigSet)) {
						concurrent.remove(configSet);
					}
				}

				if (concurrent.size() == 0) {
					new MessageDialog(shell, "Emergo Message",
							ResourceUtil.getEmergoIcon(),
							"There is nothing to hide!",
							MessageDialog.INFORMATION, new String[] { "Ok" }, 0)
							.open();
				}

				/*
				 * If concurrent.size() == 0, we still need to update the editor
				 * in case where we already have projections there.
				 */
				List<Position> positions = createPositions(d, concurrent);
				List<Position> positionsEmpty = new ArrayList<Position>();

				/*
				 * The action which updates the editor to show the folding
				 * areas.
				 */
				if (editor instanceof IfDefJavaEditor) {
					((IfDefJavaEditor) editor).expandAllAnnotations(d
							.getLength());
					((IfDefJavaEditor) editor).removeAllAnnotations();

					((IfDefJavaEditor) editor)
							.updateFoldingStructure(positionsEmpty);
					((IfDefJavaEditor) editor)
							.updateFoldingStructure(positions);
				}
			}

		} catch (BadLocationException e1) {
			e1.printStackTrace();
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		return null;
	}

	private List<? extends Integer> generateListFromXplus1toY(Integer x,
			Integer y) {

		List<Integer> result = new ArrayList<Integer>();

		for (int i = ++x; i <= y; i++) {
			result.add(i);
		}
		return result;
	}

	private List<Position> createPositions(IDocument d,
			Map<ConfigSet, Collection<Integer>> featuresLineNumbers)
			throws BadLocationException {

		Iterator<ConfigSet> featureNames;
		ConfigSet featureName;
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
							offset = d.getLineOffset(line - 1);
							length = d.getLineLength(line - 1);
						} catch (BadLocationException e) {
							length = 0;
							offset = d.getLineOffset(line);
						}
						newAnnotation = false;
					} else {
						line = iteratorInteger.next().intValue() - 1;
						if (first == true) {
							try {
								offset = d.getLineOffset(line - 1);
								length = d.getLineLength(line - 1);
							} catch (BadLocationException e) {
								length = 0;
								offset = d.getLineOffset(line);
							}
							first = false;
						}
					}
					length = length + d.getLineLength(line);
					if (previousLine > 0 && line > previousLine + 1) {
						previousLine = line;
						newAnnotation = true;
						positions.add(new Position(offset, length));
						break;
					}
					previousLine = line;
				}
				positions.add(new Position(offset, length));
			}
		}
		return positions;
	}

}