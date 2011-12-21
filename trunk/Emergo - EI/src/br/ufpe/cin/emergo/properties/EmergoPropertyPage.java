package br.ufpe.cin.emergo.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class EmergoPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	private Button radioInterprocedural;
	private Button radioIntraprocedural;

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

		Label mylabel = new Label(myComposite, SWT.NONE);
		mylabel.setLayoutData(new GridData());
		mylabel.setText("Choose the type of procedure");
		radioInterprocedural = new Button(myComposite, SWT.RADIO);
		radioInterprocedural.setText("Interprocedural");
		radioInterprocedural.setSelection(getInterprocedural().equals("true"));

		System.out.println("procedural key"
				+ SystemProperties.INTERPROCEDURAL_PROPKEY);

		radioIntraprocedural = new Button(myComposite, SWT.RADIO);
		radioIntraprocedural.setText("Intraprocedural");
		radioIntraprocedural.setSelection(getInterprocedural().equals("false"));

		return myComposite;
	}

	protected String getInterprocedural() {
		IResource resource = (IResource) getElement().getAdapter(
				IResource.class);
		try {
			String value = resource
					.getPersistentProperty(SystemProperties.INTERPROCEDURAL_PROPKEY);
			if (value == null)
				return "" + SystemProperties.getDefaultInterprocedure();
			return value;
		} catch (CoreException e) {
			return e.getMessage();
		}
	}

	protected void setInterprocedural(String interprocedural) {
		IResource resource = (IResource) getElement().getAdapter(
				IResource.class);
		String value = interprocedural;
		if (value.equals(SystemProperties.getDefaultInterprocedure()))
			value = null;
		try {
			resource.setPersistentProperty(
					SystemProperties.INTERPROCEDURAL_PROPKEY, value);
		} catch (CoreException e) {
			// doesnt return anything
		}
	}

	public boolean performOk() {
		setInterprocedural("" + radioInterprocedural.getSelection());
		return super.performOk();
	}

}