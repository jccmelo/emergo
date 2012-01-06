package br.ufpe.cin.emergo.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import br.ufpe.cin.emergo.activator.Activator;

/**
 * Class responsible for getting resources of the emergo plugin.
 * 
 * @author M�rcio Ribeiro
 * 
 */
public class ResourceUtil {
	
	public static IFile getIFile(String filename){
		IPath path = new Path(filename);
	    return ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);	
	}

	public static Image getEmergoIcon() {
		URL url = null;
		try {
			url = new URL(Activator.getDefault().getDescriptor().getInstallURL(), "icons/Emergo-Logo-Blue.png");
		} catch (MalformedURLException e) {
			//In this case, this method will return null.
			//However, this is OK, since clients of this method will automatically take the default eclipse icon into consideration.
			e.printStackTrace();
		}
		return ImageDescriptor.createFromURL(url).createImage();
	}

}