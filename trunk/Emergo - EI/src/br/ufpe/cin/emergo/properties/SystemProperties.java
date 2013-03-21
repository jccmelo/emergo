package br.ufpe.cin.emergo.properties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import br.ufpe.cin.emergo.activator.Activator;

public class SystemProperties {

	public static final QualifiedName INTERPROCEDURAL_MAXDEPTH_PROPKEY = new QualifiedName(Activator.PLUGIN_ID, "interprocedural.maxdepth");
	private static final int DEFAULT_INTERPROCEDURAL_MAXDEPTH = 1;
	
	public static final QualifiedName INTERPROCEDURAL_MAXINLINE_PROPKEY = new QualifiedName(Activator.PLUGIN_ID, "interprocedural.maxinline");
	private static final int DEFAULT_INTERPROCEDURAL_MAXINLINE = -1;
	
	public static final QualifiedName INTERPROCEDURAL_PROPKEY = new QualifiedName(Activator.PLUGIN_ID, "interprocedural");
	private static final boolean DEFAULT_INTERPROCEDURAL = true;
	
	public static final QualifiedName CHOOSEN_FEATURES = new QualifiedName(Activator.PLUGIN_ID, "features");
	
	public static final QualifiedName FEATURE_DEPENDENCE_PROPKEY = new QualifiedName(Activator.PLUGIN_ID, "featureDependence");
	private static final boolean DEFAULT_FEATURE_DEPENDENCE = true;
	
	public static boolean getFeatureDependence(IResource resource) {
		try {
			String value = resource.getPersistentProperty(FEATURE_DEPENDENCE_PROPKEY);
			if (value == null){
				return DEFAULT_FEATURE_DEPENDENCE;
			}
			return Boolean.parseBoolean(value);
		} catch (CoreException e) {
			e.printStackTrace();
			return DEFAULT_FEATURE_DEPENDENCE;
		}
	}
	
	public static void setFeatureDependence(IResource resource, boolean feature) {
		try {
			resource.setPersistentProperty(SystemProperties.FEATURE_DEPENDENCE_PROPKEY, "" + feature);
		} catch (CoreException e) {
			e.printStackTrace();
			//XXX
		}	
	}
	
	public static boolean getInterprocedural(IResource resource){
		try {
			String value = resource.getPersistentProperty(INTERPROCEDURAL_PROPKEY);
			if (value == null){
				return DEFAULT_INTERPROCEDURAL;
			}
			return Boolean.parseBoolean(value);
		} catch (CoreException e) {
			e.printStackTrace();
			return DEFAULT_INTERPROCEDURAL;
		}
	}

	public static void setInterprocedural(IResource resource, boolean interprocedural) {
		try {
			resource.setPersistentProperty(SystemProperties.INTERPROCEDURAL_PROPKEY, "" + interprocedural);
		} catch (CoreException e) {
			e.printStackTrace();
			//XXX
		}	
	}
	
	public static int getInterproceduralDepth(IResource resource) {
		try {
			String value = resource.getPersistentProperty(INTERPROCEDURAL_MAXDEPTH_PROPKEY);
			if (value == null){
				return DEFAULT_INTERPROCEDURAL_MAXDEPTH;
			}
			return Integer.parseInt(value);
		} catch (CoreException e) {
			e.printStackTrace();
			return DEFAULT_INTERPROCEDURAL_MAXDEPTH	;
		}
	}
	
	public static void setInterproceduralDepth(IResource resource, int depth) {
		try {
			resource.setPersistentProperty(SystemProperties.INTERPROCEDURAL_MAXDEPTH_PROPKEY, "" + depth);
		} catch (CoreException e) {
			e.printStackTrace();
			//XXX
		}	
	}
	
	public static int getInterproceduralInline(IResource resource) {
		try {
			String value = resource.getPersistentProperty(INTERPROCEDURAL_MAXINLINE_PROPKEY);
			if (value == null){
				return DEFAULT_INTERPROCEDURAL_MAXINLINE;
			}
			return Integer.parseInt(value);
		} catch (CoreException e) {
			e.printStackTrace();
			return DEFAULT_INTERPROCEDURAL_MAXINLINE	;
		}
	}
	
	public static void setInterproceduralInline(IResource resource, int depth) {
		try {
			resource.setPersistentProperty(SystemProperties.INTERPROCEDURAL_MAXINLINE_PROPKEY, "" + depth);
		} catch (CoreException e) {
			e.printStackTrace();
			//XXX
		}	
	}
}