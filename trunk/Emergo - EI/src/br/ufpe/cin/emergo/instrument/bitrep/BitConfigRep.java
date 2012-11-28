/*
 * This is a prototype implementation of the concept of Feature-Sen
 * sitive Dataflow Analysis. More details in the AOSD'12 paper:
 * Dataflow Analysis for Software Product Lines
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package br.ufpe.cin.emergo.instrument.bitrep;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.UnmodifiableBidiMap;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import br.ufpe.cin.emergo.features.FeatureSetChecker;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;
import cern.colt.bitvector.BitVector;

public final class BitConfigRep implements IConfigRep {

	private final int id;
	// String->int
	private final UnmodifiableBidiMap atoms;

	private final int hashCode;

	public BitConfigRep(int identifier, UnmodifiableBidiMap atoms) {
		this.atoms = atoms;
		this.id = identifier;
		this.hashCode = new HashCodeBuilder(17, 31).append(id).toHashCode();
	}
	
	public BitConfigRep(int identifier, BidiMap atoms) {
		this.atoms = (UnmodifiableBidiMap) UnmodifiableBidiMap.decorate(atoms);
		id = identifier;
		this.hashCode = new HashCodeBuilder(17, 31).append(id).toHashCode();
	}

	public static boolean isValid(int identifier, BidiMap atoms, FeatureSetChecker checker) {
		if (checker == null)
			return true;
		Set<String> enabledFeatures = new HashSet<String>();
		Set<String> disabledFeatures = new HashSet<String>();
		Collection<Integer> values = atoms.values();
		for (Integer featureId : values) {
			String featureStr = (String) atoms.getKey(featureId);
			if ((featureId & identifier) == featureId)
				enabledFeatures.add(featureStr);
			else
				disabledFeatures.add(featureStr);
		}
		return checker.check(enabledFeatures, disabledFeatures);
	}
	
	public static SetBitConfigRep localConfigurations(int highestId, UnmodifiableBidiMap atoms, FeatureSetChecker checker) {
		Set<IConfigRep> localConfigs = new HashSet<IConfigRep>();
		for (int index = 0; index < highestId; index++) {
			if (isValid(index, atoms, checker)) {
				localConfigs.add(new BitConfigRep(index, atoms));
			}
		}
		return new SetBitConfigRep(localConfigs, atoms, highestId);
	}
	
	public static SetBitConfigRep localConfigurations(int highestId, UnmodifiableBidiMap atoms) {
		Set<IConfigRep> localConfigs = new HashSet<IConfigRep>();
		for (int index = 0; index < highestId; index++) {
			localConfigs.add(new BitConfigRep(index, atoms));
		}
		return new SetBitConfigRep(localConfigs, atoms, highestId);
	}

	public int getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof BitConfigRep))
			return false;
		BitConfigRep that = (BitConfigRep) o;
		return this.id == that.id;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean belongsToConfiguration(IFeatureRep rep) {
		if (rep instanceof BitFeatureRep) {
			BitFeatureRep bitRep = (BitFeatureRep) rep;
			int repId = bitRep.getId();
			return ((repId & this.id) == repId);
		} else if (rep instanceof BitVectorFeatureRep) {
			BitVectorFeatureRep bitVectorFeatureRep = (BitVectorFeatureRep) rep;
			BitVector bitVector = bitVectorFeatureRep.getBitVector();
			return bitVector.get(this.id);
		} 
		throw new IllegalArgumentException();
	}

	@Override
	public int size() {
		return Integer.bitCount(id);
	}

	@Override
	public String toString() {
		return "" + id;
	}
	
	public UnmodifiableBidiMap getAtoms() {
		return atoms;
	}

	@Override
	public IConfigRep union(IConfigRep rep) {
		if (rep instanceof BitConfigRep) {
			BitConfigRep bitRep = (BitConfigRep) rep;
			return BitVectorConfigRep.union(this, bitRep, Collections.max(((Collection<Integer>) atoms.values())), atoms);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public IConfigRep intersection(IConfigRep rep) {
		if (rep instanceof BitVectorConfigRep) {
			return rep.intersection(this);
		} else if (rep instanceof BitConfigRep) {
			if (this.id == ((BitConfigRep) rep).id) {
				return this;
			} else {
				return empty();
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private BitConfigRep empty() {
		return new BitConfigRep(0, atoms);
	}

	@Override
	public Iterator<IConfigRep> iterator() {
		throw new UnsupportedOperationException();
	}

}
