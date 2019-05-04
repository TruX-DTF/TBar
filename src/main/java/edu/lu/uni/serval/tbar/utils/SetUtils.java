package edu.lu.uni.serval.tbar.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SetUtils {

	public static <T> void addToMap(Map<String, List<T>> dictionary, String key, T t) {
		List<T> objects = dictionary.get(key);
		if (objects == null) {
			objects = new ArrayList<T>();
			objects.add(t);
			dictionary.put(key, objects);
		} else if (!objects.contains(t)) {
			objects.add(t);
		}
	}
}
