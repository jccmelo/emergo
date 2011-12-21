package br.ufpe.cin.emergo.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import br.ufpe.cin.emergo.activator.Activator;

public class SystemProperties {

	private static boolean DEFAULT_INTERPROCEDURAL = true;
	public static QualifiedName INTERPROCEDURAL_PROPKEY = new QualifiedName(Activator.PLUGIN_ID, "interprocedural");
	public static QualifiedName CHOOSEN_FEATURES = new QualifiedName(Activator.PLUGIN_ID, "features");
    
	public static void setDefaultInterprocedure(String defaultInterprocedure) {
		Activator.getDefault().getPreferenceStore().setValue("" + DEFAULT_INTERPROCEDURAL,defaultInterprocedure);
	}

	public static String getDefaultInterprocedure() {
		return Activator.getDefault().getPreferenceStore().getString("" + DEFAULT_INTERPROCEDURAL);
	}
	
	public static String getInterprocedure(IResource resource){
		try {
			String value = resource.getPersistentProperty(INTERPROCEDURAL_PROPKEY);
			if (value == null){
				return getDefaultInterprocedure();
			}
			return value;
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			return e.getMessage();
		}
	}

}