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

import java.util.Collections;
import java.util.Set;

import org.apache.commons.collections.BidiMap;

import br.ufpe.cin.emergo.instrument.IConfigRep;
import br.ufpe.cin.emergo.instrument.IFeatureRep;
import cern.colt.bitvector.BitVector;

public class BitVectorFeatureRep implements IFeatureRep, Cloneable {
	private Set<String> features;
	private BidiMap atoms;
	private int id;
	private BitVector bits;

	public BitVectorFeatureRep(Set<String> features, BidiMap originalFeatureIds) {
		this.features = features;
		int accumulator = 0;
		for (String element : this.features) {
			Integer featId = (Integer) originalFeatureIds.get(element);
			if (featId != null) {
				accumulator += featId;
			}
		}
		this.id = accumulator;
		this.atoms = originalFeatureIds;
	}

	private BitVectorFeatureRep(Set<String> features, BidiMap atoms, BitVector bits, int id) {
		this.features = features;
		this.atoms = atoms;
		this.id = id;
		this.bits = (BitVector) bits.clone();
	}

	@Override
	public IFeatureRep clone() {
		return new BitVectorFeatureRep(this.features, this.atoms, this.bits, this.id);
	}

	public void computeBitVector() {
		int highestId = atoms.isEmpty() ? 1 : ((Integer) Collections.max(this.atoms.values())) << 1;
		this.bits = new BitVector(highestId);
		for (int index = 0; index < highestId; index++) {
			if ((this.id & index) == index) {
				bits.set(index);
			}
		}
		return;
	}

	@Override
	public IFeatureRep addAll(IFeatureRep rep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean belongsToConfiguration(IConfigRep config) {
		return config.belongsToConfiguration(this);
	}

	@Override
	public Set<String> getFeatures() {
		return features;
	}

	@Override
	public int size() {
		return features.size();
	}

	@Override
	public String toString() {
		return this.bits.toString();
	}

	public BitVector getBitVector() {
		return this.bits;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (getClass() != o.getClass())
			return false;
		BitVectorFeatureRep that = (BitVectorFeatureRep) o;
		return this.id == that.id && this.bits.equals(that.bits);
	}
	

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + bits.hashCode();
		return 31 * hash + id;
	}

	public int getId() {
		return id;
	}
}