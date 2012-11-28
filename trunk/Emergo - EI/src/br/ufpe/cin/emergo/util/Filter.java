package br.ufpe.cin.emergo.util;

public interface Filter<T> {
	boolean accept(T element);
}
