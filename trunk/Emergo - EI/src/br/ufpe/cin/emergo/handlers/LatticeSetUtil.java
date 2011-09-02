package br.ufpe.cin.emergo.handlers;

import java.lang.reflect.Field;
import java.util.Set;

import dk.brics.lattice.LatticeSet;

/**
 * Hacks LatticeSet inner storage using reflection in order to retrieve analyses' restults.
 * 
 * @author Társis
 * 
 */
public class LatticeSetUtil {

	public static <T> Set<T> getSet(LatticeSet<T> value) {
		Field f = null, f2 = null;
		Set<T> set = null;
		try {
			f = value.getClass().getDeclaredField("state");
			f.setAccessible(true);
			Object o = f.get(value);

			f2 = o.getClass().getDeclaredField("set");
			f2.setAccessible(true);
			set = (Set) f2.get(o);
			System.out.println(set);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return set;
	}
}
