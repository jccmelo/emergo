package br.ufpe.cin.emergo.popup;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class SampleDialog extends TitleAreaDialog {

	public static final int OPEN = 9999;
 	public static final int DELETE = 9998;
 	
	private Button[] features;
	private String[] featuresNames;
	private String[] selectedFeatures;
	private String[] alreadyChoosenFeatures;
	
	Composite parent;
	Composite top;
	Composite composite;
	
	public SampleDialog(Shell parentShell, String[] featuresNames, String[] choosenFeaturesArray) {
		super(parentShell);
		this.featuresNames = featuresNames;
		this.alreadyChoosenFeatures = choosenFeaturesArray;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		top = (Composite) super.createDialogArea(parent);
		composite = new Composite(top, SWT.NONE);
		
		this.parent = parent;
		setMessage("Please choose the features you want to visualize");
		setTitle("Feature Preferences");
		final Composite top = (Composite) super.createDialogArea(parent);

		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		features = new Button[featuresNames.length];
		for (int i = 0; i < featuresNames.length; i++) {
			features[i] = new Button(composite, SWT.CHECK);
			boolean check = false;
			for (int j = 0; j < alreadyChoosenFeatures.length; j++) {
				if(featuresNames[i].equals(alreadyChoosenFeatures[j])){
					check = true;
					break;
				}		
			}
			features[i].setSelection(check);
			features[i].setText(featuresNames[i]);
			features[i].pack();
		}
		
		return top;
	}

	private void validateFiels() {
		int size = 0;
		for (int i = 0; i < this.features.length; i++) {
			if (this.features[i].getSelection())
				size++;
		}
		this.selectedFeatures = new String[size];
		int count =0;
		for (int i = 0; i < this.features.length; i++) {
			if (this.features[i].getSelection()) {
				this.selectedFeatures[count] = this.features[i].getText();
				count++;
			}
		}
	}

	public String[] getSelectedFeatures() {
		return this.selectedFeatures;
	}

	@Override
	 protected void createButtonsForButtonBar(Composite parent) {
	    // Create Open button
	    Button openButton = createButton(parent, OPEN, "Ok", true);
	    // Initially deactivate it
	    //openButton.setEnabled(false);
	    // Add a SelectionListener
	    openButton.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent e) {
	        // Retrieve selected entries from list
	        //itemsToOpen = list.getSelection();
	        // Set return code
	    	validateFiels();
	        setReturnCode(OPEN);
	        // Close dialog
	        close();
	      }
	    });

	    // Create Cancel button
	    Button cancelButton = createButton(parent, CANCEL, "Cancel", false);
	    // Add a SelectionListener
	    cancelButton.addSelectionListener(new SelectionAdapter() {
	      public void widgetSelected(SelectionEvent e) {
	        setReturnCode(CANCEL);
	        close();
	      }
	    });
	}

}