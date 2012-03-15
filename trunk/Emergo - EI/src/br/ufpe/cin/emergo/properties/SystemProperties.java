package br.ufpe.cin.emergo.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import br.ufpe.cin.emergo.activator.Activator;

public class SystemProperties {

	public static final QualifiedName INTERPROCEDURALDEPTH_PROPKEY = new QualifiedName(Activator.PLUGIN_ID, "interproceduraldepth");
	private static final int DEFAULT_INTERPROCEDURALDEPTH = 1;
	
	public static final QualifiedName INTERPROCEDURAL_PROPKEY = new QualifiedName(Activator.PLUGIN_ID, "interprocedural");
	private static final boolean DEFAULT_INTERPROCEDURAL = true;
	
	public static final QualifiedName CHOOSEN_FEATURES = new QualifiedName(Activator.PLUGIN_ID, "features");
	
	public static boolean getInterprocedural(IResource resource){
		try {
			String value = resource.getPersistentProperty(INTERPROCEDURAL_PROPKEY);
			if (value == null){
				return DEFAULT_INTERPROCEDURAL;
			}
			return Boolean.parseBoolean(value);
		} catch (CoreException e) {
			return DEFAULT_INTERPROCEDURAL;
		}
	}
	
	public static int getInterproceduralDepth(IResource resource) {
		try {
			String value = resource.getPersistentProperty(INTERPROCEDURALDEPTH_PROPKEY);
			if (value == null){
				return DEFAULT_INTERPROCEDURALDEPTH;
			}
			return Integer.parseInt(value);
		} catch (CoreException e) {
			return DEFAULT_INTERPROCEDURALDEPTH	;
		}
	}

	public static void setInterprocedural(IResource resource, boolean interprocedural) {
		try {
			resource.setPersistentProperty(SystemProperties.INTERPROCEDURAL_PROPKEY, "" + interprocedural);
		} catch (CoreException e) {
			//XXX
		}	
	}
	
	public static void setInterproceduralDepth(IResource resource, int depth) {
		try {
			resource.setPersistentProperty(SystemProperties.INTERPROCEDURALDEPTH_PROPKEY, "" + depth);
		} catch (CoreException e) {
			//XXX
		}	
	}
}