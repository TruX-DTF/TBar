package edu.lu.uni.serval.tbar.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListSorter<T extends Comparable<? super T>> {

	private List<T> list;

	public ListSorter(List<T> list) {
		this.list = new ArrayList<>();
		this.list.addAll(list);
	}

	public List<T> getList() {
		return this.list;
	}

	public List<T> sortAscending() {
		try {
			if (list != null && list.size() > 0) {
				Collections.sort(this.list, new Comparator<T>() {

					@Override
					public int compare(T t1, T t2) {
						return t1.compareTo(t2);
					}
					
				});
			}
		} catch (Exception e) {
			return null;
		}
		return this.list;
	}

	public List<T> sortDescending() {
		if (list != null && list.size() > 0) {
			Collections.sort(this.list, Collections.reverseOrder());
		}
		return this.list;
	}
}
