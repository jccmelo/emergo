/* Soot - a J*va Optimization Framework
 * Copyright (C) 1997-1999 Raja Vallee-Rai
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */


package br.ufpe.cin.emergo.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import soot.toolkits.scalar.FlowSet;

public class ArraySparseSet extends AbstractFlowSet
{
	protected static final int DEFAULT_SIZE = 8; 
    
    protected int numElements;
    protected int maxElements;
    protected Object[] elements;

    public ArraySparseSet()
    {
        maxElements = DEFAULT_SIZE;
        elements = new Object[DEFAULT_SIZE];
        numElements = 0;
    }
    
    private ArraySparseSet(ArraySparseSet other)
    {
        numElements = other.numElements;
        maxElements = other.maxElements;
        elements = other.elements.clone();
    }
    
    /** Returns true if flowSet is the same type of flow set as this. */
    private boolean sameType(Object flowSet)
    {
        return (flowSet instanceof ArraySparseSet);
    }

    public ArraySparseSet clone()
    {
        return new ArraySparseSet(this);
    }

    public Object emptySet()
    {
        return new ArraySparseSet();
    }

    public void clear()
    {
        numElements = 0;
    }
    
    public int size()
    {
        return numElements;
    }

    public boolean isEmpty()
    {
        return numElements == 0;
    }

    /** Returns a unbacked list of elements in this set. */
    public List toList()
    {
        Object[] copiedElements = new Object[numElements];
        System.arraycopy(elements, 0, copiedElements, 0, numElements);
        return Arrays.asList(copiedElements);
    }

  /* Expand array only when necessary, pointed out by Florian Loitsch
   * March 08, 2002
   */
    public void add(Object e)
    {
      /* Expand only if necessary! and removes one if too:) */
        // Add element
            if(!contains(e)) {
              // Expand array if necessary
              if(numElements == maxElements)
                doubleCapacity();
              elements[numElements++] = e;
            }
    }

    private void doubleCapacity()
    {        
        int newSize = maxElements * 2;
                    
        Object[] newElements = new Object[newSize];
                
        System.arraycopy(elements, 0, newElements, 0, numElements);
        elements = newElements;
        maxElements = newSize;
    }    

    public void remove(Object obj)
    {
        int i = 0;
        while (i < this.numElements) {
            if (elements[i].equals(obj))
            {
            	numElements--;
            	//copy last element to deleted position
                elements[i] = elements[numElements];
                //delete reference in last cell so that
                //we only retain a single reference to the
                //"old last" element, for memory safety
                elements[numElements] = null;
                return;
            } else
                i++;
        }
    }

  public void union(FlowSet otherFlow, FlowSet destFlow)
    {
      if (sameType(otherFlow) &&
          sameType(destFlow)) {
        ArraySparseSet other = (ArraySparseSet) otherFlow;
        ArraySparseSet dest = (ArraySparseSet) destFlow;

        // For the special case that dest == other
            if(dest == other)
            {
                for(int i = 0; i < this.numElements; i++)
                    dest.add(this.elements[i]);
            }
        
        // Else, force that dest starts with contents of this
        else {
            if(this != dest)
                copy(dest);

            for(int i = 0; i < other.numElements; i++)
                dest.add(other.elements[i]);
        }
      } else
        super.union(otherFlow, destFlow);
    }

    public void intersection(FlowSet otherFlow, FlowSet destFlow)
    {
      if (sameType(otherFlow) &&
          sameType(destFlow)) {
        ArraySparseSet other = (ArraySparseSet) otherFlow;
        ArraySparseSet dest = (ArraySparseSet) destFlow;
        ArraySparseSet workingSet;
        
        if(dest == other || dest == this)
            workingSet = new ArraySparseSet();
        else { 
            workingSet = dest;
            workingSet.clear();
        }
        
        for(int i = 0; i < this.numElements; i++)
        {
            if(other.contains(this.elements[i]))
                workingSet.add(this.elements[i]);
        }
        
        if(workingSet != dest)
            workingSet.copy(dest);
      } else
        super.intersection(otherFlow, destFlow);
    }

    public void difference(FlowSet otherFlow, FlowSet destFlow)
    {
      if (sameType(otherFlow) &&
          sameType(destFlow)) {
        ArraySparseSet other = (ArraySparseSet) otherFlow;
        ArraySparseSet dest = (ArraySparseSet) destFlow;
        ArraySparseSet workingSet;
        
        if(dest == other || dest == this)
            workingSet = new ArraySparseSet();
        else { 
            workingSet = dest;
            workingSet.clear();
        }
        
        for(int i = 0; i < this.numElements; i++)
        {
            if(!other.contains(this.elements[i]))
                workingSet.add(this.elements[i]);
        }
        
        if(workingSet != dest)
            workingSet.copy(dest);
      } else
        super.difference(otherFlow, destFlow);
    }
    
    /**
     * @deprecated This method uses linear-time lookup.
     * For better performance, consider using a {@link HashSet} instead, if you require this operation.
     */
    public boolean contains(Object obj)
    {
        for(int i = 0; i < numElements; i++)
            if(elements[i].equals(obj))
                return true;
                
        return false;
    }

//    public boolean equals(Object otherFlow)
//    {
//      if (sameType(otherFlow)) {
//        MyArraySparseSet other = (MyArraySparseSet) otherFlow;
//         
//        if(other.numElements != this.numElements)
//            return false;
//     
//        int size = this.numElements;
//             
//        // Make sure that thisFlow is contained in otherFlow  
//            for(int i = 0; i < size; i++)
//                if(!other.contains(this.elements[i]))
//                    return false;
//
//            /* both arrays have the same size, no element appears twice in one
//             * array, all elements of ThisFlow are in otherFlow -> they are
//             * equal!  we don't need to test again!
//        // Make sure that otherFlow is contained in ThisFlow        
//            for(int i = 0; i < size; i++)
//                if(!this.contains(other.elements[i]))
//                    return false;
//             */
//        
//        return true;
//      } else
//        return super.equals(otherFlow);
//    }
    
    @Override
	public int hashCode() {
		int result = 0;
		for (int index = 0; index < numElements; index++)
			result += elements[index].hashCode();
		final int prime = 31;
		result = prime * result + numElements;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArraySparseSet other = (ArraySparseSet) obj;
		if (numElements != other.numElements)
			return false;
		for (int index = 0; index < numElements; index++)
			if (!other.contains(elements[index]))
				return false;
		return true;
	}

    public void copy(FlowSet destFlow)
    {
      if (sameType(destFlow)) {
        ArraySparseSet dest = (ArraySparseSet) destFlow;

        while(dest.maxElements < this.maxElements)
            dest.doubleCapacity();
    
        dest.numElements = this.numElements;
        
        System.arraycopy(this.elements, 0,
            dest.elements, 0, this.numElements);
      } else
        super.copy(destFlow);
    }

}

abstract class AbstractFlowSet implements FlowSet, Iterable {
  public abstract AbstractFlowSet clone();

  /**
   * implemented, but inefficient.
   */
  public Object emptySet() {
    FlowSet t = clone();
    t.clear();
    return t;
  }

  public void copy(FlowSet dest) {
    List elements = toList();
    Iterator it = elements.iterator();
    dest.clear();
    while (it.hasNext())
      dest.add(it.next());
  }

  /**
   * implemented, but *very* inefficient.
   */
  public void clear() {
    Iterator it = toList().iterator();
    while (it.hasNext())
      remove(it.next());
  }

  public void union(FlowSet other) {
    union(other, this);
  }

  public void union(FlowSet other, FlowSet dest) {
    if (dest != this && dest != other)
      dest.clear();

    if (dest != this) {
      Iterator thisIt = toList().iterator();
      while (thisIt.hasNext())
        dest.add(thisIt.next());
    }

    if (dest != other) {
      Iterator otherIt = other.toList().iterator();
      while (otherIt.hasNext())
        dest.add(otherIt.next());
    }
  }

  public void intersection(FlowSet other) {
    intersection(other, this);
  }

  public void intersection(FlowSet other, FlowSet dest) {
    if (dest == this && dest == other) return;
    List elements = null;
    FlowSet flowSet = null;
    if (dest == this) {
      /* makes automaticly a copy of <code>this</code>, as it will be cleared */
      elements = toList();
      flowSet = other;
    } else {
      /* makes a copy o <code>other</code>, as it might be cleared */
      elements = other.toList();
      flowSet = this;
    }
    dest.clear();
    Iterator it = elements.iterator();
    while (it.hasNext()) {
      Object o = it.next();
      if (flowSet.contains(o))
        dest.add(o);
    }
  }

  public void difference(FlowSet other) {
    difference(other, this);
  }

  public void difference(FlowSet other, FlowSet dest) {
    if (dest == this && dest == other) {
      dest.clear();
      return;
    }

    Iterator it = this.toList().iterator();
    FlowSet flowSet = (other == dest)? (FlowSet)other.clone(): other;
    dest.clear(); // now safe, since we have copies of this & other

    while (it.hasNext()) {
      Object o = it.next();
      if (!flowSet.contains(o))
        dest.add(o);
    }
  }

  public abstract boolean isEmpty();

  public abstract int size();

  public abstract void add(Object obj);

  public void add(Object obj, FlowSet dest) {
    if (dest != this)
      copy(dest);
    dest.add(obj);
  }
  
  public abstract void remove(Object obj);

  public void remove(Object obj, FlowSet dest) {
    if (dest != this)
      copy(dest);
    dest.remove(obj);
  }

  public abstract boolean contains(Object obj);

  public Iterator iterator() {
    return toList().iterator();
  }

  public abstract List toList();

  public boolean equals(Object o) {
    if (!(o instanceof FlowSet)) return false;
    FlowSet other = (FlowSet)o;
    if (size() != other.size()) return false;
    Iterator it = toList().iterator();
    while (it.hasNext())
      if (!other.contains(it.next())) return false;
    return true;
  }
  
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		Iterator iter = iterator();
		while(iter.hasNext()) {
			Object o = iter.next();
			result = PRIME * result + o.hashCode();
		}
		return result;
	}

  public String toString() {
    StringBuffer buffer = new StringBuffer("{");
    Iterator it = toList().iterator();
    if (it.hasNext()) {
      buffer.append(it.next());

      while(it.hasNext())
        buffer.append(", " + it.next());
    }
    buffer.append("}");
    return buffer.toString();
  }
}

