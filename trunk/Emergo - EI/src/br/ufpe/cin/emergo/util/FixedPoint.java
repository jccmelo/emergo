package br.ufpe.cin.emergo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class FixedPoint {
	
	public static abstract class Function<T> {
		public abstract Collection<T> apply(Collection<T> data);
		public boolean should(Collection<T> data) { return true; }
	}
	
	public static <D> Collection<D> find(Function<D> f, Collection<D> d) {
		Collection<D> tmp1 = d;
		Collection<D> tmp2;
		while (true) {
			tmp2 = f.apply(tmp1);
			if (tmp1.equals(tmp2)) {
				return tmp1;
			}
			tmp1 = tmp2;
		}
	}
	
	public static <K, V> Map<K, V> mapMerge(Map<K, V> map, Function<K> mergeKeys, Function<V> mergeValues) {
		MapMergeFunction<K, V> function = new MapMergeFunction<K, V>(map, mergeKeys, mergeValues);
		return find(function, Collections.singleton(function)).iterator().next().map;
	}
	
	public static <K extends Iterable<K>, V> Map<K, V> mapSplit(Map<K, V> map, Function<V> mergeValues) {
		MapSplitFunction<K, V> function = new MapSplitFunction<K, V>(map, mergeValues);
		return find(function, Collections.singleton(function)).iterator().next().map;
	}
	
	private static class MapSplitFunction<K extends Iterable<K>, V> extends Function<MapSplitFunction<K, V>> {

		private final Map<K, V> map;
		private final Function<V> mergeValues;

		public MapSplitFunction(Map<K, V> map, Function<V> mergeValues) {
			this.map = map;
			this.mergeValues = mergeValues;
		}

		@Override
		public Collection<MapSplitFunction<K, V>> apply(Collection<MapSplitFunction<K, V>> data) {
			Map<K, V> map = data.iterator().next().map;
			Map<K, V> tmp = new HashMap<K, V>(data.iterator().next().map);
			
			for (final Entry<K, V> entry : map.entrySet()) {
				K key = entry.getKey();
				boolean scheduleRemoval = false;
				int elementCount = 0;
				for (K element : key) {
					elementCount++;
					final V v = tmp.get(element);
					final V value = entry.getValue();
					if (v == null) {
						tmp.put(element, entry.getValue());
						scheduleRemoval = true;
					} else {
						if (!value.equals(v)) {
							ArrayList<V> merge = new ArrayList<V>() { { add(v); add(value); } }; 
							tmp.put(element, this.mergeValues.apply(merge).iterator().next());
							scheduleRemoval = true;
						}
					}
				}
				if (scheduleRemoval && elementCount > 1) {
					tmp.remove(key);
					scheduleRemoval = false;
				}
			}
			return Collections.singleton(new MapSplitFunction<K, V>(tmp, mergeValues));
		}

		@Override
		public int hashCode() {
			return 31 + ((map == null) ? 0 : map.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MapSplitFunction other = (MapSplitFunction) obj;
			if (map == null) {
				if (other.map != null)
					return false;
			} else if (!map.equals(other.map))
				return false;
			return true;
		}
		
	}
	
	private static class MapMergeFunction<K, V> extends Function<MapMergeFunction<K, V>> {
		
		private Map<K, V> map;
		Function<K> mergeKeys;
		Function<V> mergeValues;

		public MapMergeFunction(Map<K, V> map, Function<K> mergeKeys, Function<V> mergeValues) {
			this.map = map;
			this.mergeKeys = mergeKeys;
			this.mergeValues = mergeValues;
		}
		
		@Override
		public boolean equals(Object obj) {
			return (MapMergeFunction.class == obj.getClass() && ((MapMergeFunction) obj).map.equals(this.map));
		}
		
		@Override
		public int hashCode() {
			return 31 + this.map.hashCode();
		}

		@Override
		public Collection<MapMergeFunction<K, V>> apply(
				Collection<MapMergeFunction<K, V>> data) {
			Map<K, V> map = data.iterator().next().map;
			Map<K, V> tmp = new HashMap<K, V>(map);
			
			for (Entry<K, V> entry : map.entrySet()) {
				for (Entry<K, V> entry2 : map.entrySet()) {
					final K key = entry.getKey();
					final K key2 = entry2.getKey();
					if (key.equals(key2)) {
						continue;
					}
					final V value = entry.getValue();
					final V value2 = entry2.getValue();
					ArrayList<V> valueShould = new ArrayList<V>() { { add(value); add(value2); } };
					if (mergeValues.should(valueShould)) {
						ArrayList<K> keyShould = new ArrayList<K>() { { add(key); add(key2); } };
						K mergedKey = mergeKeys.apply(keyShould).iterator().next();
						final V config = tmp.get(mergedKey);
						tmp.remove(key);
						tmp.remove(key2);
						if (config == null) {
							tmp.put(mergedKey, value);
						} else {
							ArrayList<V> valueShouldForExisting = new ArrayList<V>() { { add(value); add(config); } };
							V mergedValue = mergeValues.apply(valueShouldForExisting).iterator().next();
							tmp.put(mergedKey, mergedValue);
						}
						return Collections.singleton(new MapMergeFunction<K, V>(tmp, mergeKeys, mergeValues));
					} 
				}
			}
			return Collections.singleton(new MapMergeFunction<K, V>(tmp, mergeKeys, mergeValues));
		}
	}
}
