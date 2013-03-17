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
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.bidimap.UnmodifiableBidiMap;
import org.apache.commons.collections.map.UnmodifiableEntrySet;

import br.ufpe.cin.emergo.features.FeatureSetChecker;
import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;
import br.ufpe.cin.emergo.instrument.ILazyConfigRep;
import br.ufpe.cin.emergo.util.Pair;
import cern.colt.bitvector.BitVector;

public class BitVectorConfigRep implements ILazyConfigRep {

	private final BitVector bitVector;
	private final UnmodifiableBidiMap atoms;
	private final int hashCode;

	private BitVectorConfigRep(int size, UnmodifiableBidiMap atoms) {
		this.bitVector = new BitVector(size);
		this.atoms = atoms;
		this.hashCode = 31 + (null == this.bitVector ? 0 : this.bitVector.hashCode());
	}

	private BitVectorConfigRep(UnmodifiableBidiMap atoms, BitVector bitVector) {
		this.atoms = atoms;
		this.bitVector = bitVector;
		this.hashCode = 31 + (null == this.bitVector ? 0 : this.bitVector.hashCode());
	}

	public static BitVectorConfigRep localConfigurations(int highestId, UnmodifiableBidiMap atoms, FeatureSetChecker checker) {
		BitVector bitVector = new BitVector(highestId);
		for (int index = 0; index < highestId; index++) {
			if (BitConfigRep.isValid(index, atoms, checker))
				bitVector.putQuick(index, true);
		}
		return new BitVectorConfigRep(atoms, bitVector);
	}
	
	public static BitVectorConfigRep localConfigurations(int highestId, UnmodifiableBidiMap atoms) {
		BitVector bitVector = new BitVector(highestId);
		bitVector.not();
		return new BitVectorConfigRep(atoms, bitVector);
	}

	@Override
	public Pair<ILazyConfigRep, ILazyConfigRep> split(IFeatureRep featureRep) {
		if (featureRep instanceof BitVectorFeatureRep) {
			BitVectorFeatureRep bvRep = (BitVectorFeatureRep) featureRep;
			BitVector bitVector = bvRep.getBitVector();

			BitVector cloneForLeft = this.bitVector.copy();
			cloneForLeft.and(bitVector);

			BitVector cloneForRight = bitVector.copy();
			cloneForRight.not();
			cloneForRight.and(this.bitVector);

			return new Pair<ILazyConfigRep, ILazyConfigRep>(
					new BitVectorConfigRep(this.atoms, cloneForLeft), 
					new BitVectorConfigRep(this.atoms, cloneForRight));
		} else {
			throw new UnsupportedOperationException("Not implemented for type " + featureRep.getClass());
		}
	}

	@Override
	public Pair<ILazyConfigRep, ILazyConfigRep> split(Collection<IConfigRep> belongedConfigs) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public int size() {
		return bitVector.cardinality();
	}

	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		
		Collection<Integer> collection = atoms.values();
		int i = 0;		
		for (Integer v : collection) {
			i++;
			if (bitVector.toString().contains(v.toString())) {
				b.append(atoms.getKey(v));
				if (collection.size() != i) {
					b.append(" ^ ");
				}
			}
		}
		
		return b.toString();
	}

	@Override
	public IConfigRep intersection(IConfigRep aOther) {
		if (aOther instanceof BitVectorConfigRep) {
			BitVectorConfigRep other = (BitVectorConfigRep) aOther;
			BitVector copy = other.bitVector.copy();
			copy.and(this.bitVector);
			return new BitVectorConfigRep(this.atoms, copy);
		} else if (aOther instanceof BitConfigRep) {
			BitConfigRep other = (BitConfigRep) aOther;
			if (this.bitVector.get(other.getId())) {
				return other;
			} else {
				return empty();
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof BitVectorConfigRep))
			return false;
		BitVectorConfigRep that = (BitVectorConfigRep) o;
		return this.bitVector.equals(that.bitVector);
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public IConfigRep union(IConfigRep aOther) {
		if (aOther instanceof BitVectorConfigRep) {
			BitVectorConfigRep other = (BitVectorConfigRep) aOther;
			BitVector copy = other.bitVector.copy();
			copy.or(this.bitVector);
			return new BitVectorConfigRep(this.atoms, copy);
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean belongsToConfiguration(IFeatureRep rep) {
		throw new UnsupportedOperationException();
	}
	
	private BitVectorConfigRep empty() {
		return new BitVectorConfigRep(atoms, new BitVector(this.bitVector.size()));
	}
	
	public static BitVectorConfigRep union(BitConfigRep rep1, BitConfigRep rep2, int vectorSize, UnmodifiableBidiMap atoms) {
		if (vectorSize < 1)
			throw new IllegalArgumentException("The size of the vector must be > 1");
		
		int id = rep1.getId();
		int id2 = rep2.getId();
		
		if (vectorSize < id || vectorSize < id2)
			throw new IllegalArgumentException("The size of the vector is not big enough to hold " + Math.max(id, id2));
		
		BitVector vector = new BitVector(vectorSize+1); //Otherwise, it will throw IndexOutOfBoundsException..
		vector.put(id, true);
		vector.put(id2, true); //.. here!
		
		return new BitVectorConfigRep(atoms, vector);
	}
	
	public static BitVectorConfigRep convert(BitConfigRep rep, UnmodifiableBidiMap atoms) {
		int max = atoms.isEmpty() ? 1 : ((Integer) Collections.max(atoms.values())) << 1;
		BitVector bitVector = new BitVector(max);
		bitVector.put(rep.getId(), true);
		return new BitVectorConfigRep(atoms, bitVector);
	}

	@Override
	public Iterator<IConfigRep> iterator() {
		return new Iterator<IConfigRep>() {
			int index = 0;
			// number of bits initially set to true.
			int cardinality = bitVector.cardinality();

			@Override
			public boolean hasNext() {
				return cardinality > 0;
			}

			@Override
			public IConfigRep next() {
				// increment index until a bit that is set is found
				for (; !bitVector.getQuick(index); index++)
					;
				BitVector bv = new BitVector(bitVector.size());
				// set that one bit to true in this new BitVector
				bv.set(index);
				cardinality--;
				index++;
				return new BitVectorConfigRep(atoms, bv);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
} 