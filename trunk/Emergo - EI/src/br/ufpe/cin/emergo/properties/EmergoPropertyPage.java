package br.ufpe.cin.emergo.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class EmergoPropertyPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	private Button radioInterprocedural;
	private Button radioIntraprocedural;
	private Spinner depthSpinner;

	public EmergoPropertyPage() {
		super();
	}

	/**
	 * Creates the contents showed in the property page.
	 * 
	 */
	protected Control createContents(Composite parent) {
		Composite myComposite = new Composite(parent, SWT.NONE);
		GridLayout mylayout = new GridLayout();
		mylayout.marginHeight = 1;
		mylayout.marginWidth = 1;
		myComposite.setLayout(mylayout);

		Label procedureTypeLabel = new Label(myComposite, SWT.NONE);
		procedureTypeLabel.setLayoutData(new GridData());
		procedureTypeLabel.setText("Choose the type of analysis");
		radioInterprocedural = new Button(myComposite, SWT.RADIO);
		radioInterprocedural.setText("Interprocedural");
		boolean interprocedural = getInterprocedural();
		radioInterprocedural.setSelection(interprocedural);

		radioIntraprocedural = new Button(myComposite, SWT.RADIO);
		radioIntraprocedural.setText("Intraprocedural");
		radioIntraprocedural.setSelection(!interprocedural);

		Label interproceduralDepthLabel = new Label(myComposite, SWT.NONE);
		interproceduralDepthLabel.setText("Choose the maximum depth for interprocedural analysis");
		depthSpinner = new Spinner(parent, SWT.NONE);
		depthSpinner.setIncrement(1);
		depthSpinner.setMinimum(1);
		depthSpinner.setMaximum(Integer.MAX_VALUE);
		depthSpinner.setSelection(getInterproceduralDepth());

		return myComposite;
	}

	protected boolean getInterprocedural() {
		IResource resource = (IResource) getElement().getAdapter(IResource.class);
		return SystemProperties.getInterprocedural(resource);
	}

	protected void setInterprocedural(boolean interprocedural) {
		IResource resource = (IResource) getElement().getAdapter(IResource.class);
		SystemProperties.setInterprocedural(resource, interprocedural);
	}


	protected void setInterproceduralDepth(Integer depth) {
		IResource resource = (IResource) getElement().getAdapter(IResource.class);
		SystemProperties.setInterproceduralDepth(resource, depth);
	}
	
	protected int getInterproceduralDepth() {
		IResource resource = (IResource) getElement().getAdapter(IResource.class);
		return SystemProperties.getInterproceduralDepth(resource);
	}
	
	public boolean performOk() {
		setInterprocedural(radioInterprocedural.getSelection());
		setInterproceduralDepth(Integer.parseInt(depthSpinner.getText()));
		return super.performOk();
	}
	
	@Override
	protected void performDefaults() {
		super.performDefaults();
		//XXX: Implement fall back to default values
	}

}