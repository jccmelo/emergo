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

package br.ufpe.cin.emergo.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import soot.toolkits.scalar.AbstractFlowSet;
import soot.toolkits.scalar.FlowSet;
import br.ufpe.cin.emergo.instrument.IConfigRep;

public abstract class AbstractMapLiftedFlowSet extends AbstractFlowSet {
	protected HashMap<IConfigRep, FlowSet> map;

	@Override
	public abstract AbstractMapLiftedFlowSet clone();

	@Override
	public void copy(FlowSet dest) {
		AbstractMapLiftedFlowSet destLifted = (AbstractMapLiftedFlowSet) dest;
		dest.clear();
		Set<Entry<IConfigRep, FlowSet>> entrySet = map.entrySet();
		for (Entry<IConfigRep, FlowSet> entry : entrySet) {
			IConfigRep key = entry.getKey();
			FlowSet value = entry.getValue();
			destLifted.map.put(key, value.clone());
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o.getClass() == getClass()))
			return false;
		AbstractMapLiftedFlowSet that = (AbstractMapLiftedFlowSet) o;
		return this.map.equals(that.map);
	}

	@Override
	public int hashCode() {
		return 31 + (null == map ? 0 : map.hashCode());
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public abstract void union(FlowSet other, FlowSet dest);

	@Override
	public abstract void intersection(FlowSet aOther, FlowSet aDest);

	public FlowSet add(IConfigRep config, FlowSet flow) {
		return map.put(config, flow);
	}

	@Override
	public void add(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(Object arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return this.map.size();
	}

	@Override
	public List toList() {
		List list = new ArrayList(this.map.values());
		
		return list;
//		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return map.toString();
	}
}
