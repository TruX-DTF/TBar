package edu.lu.uni.serval.jdt.generator;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public abstract class Registry<K, C, A> {

    Set<Entry> entries = new TreeSet<>(
    		new Comparator<Registry<K, C, A>.Entry>() {// JDK version lower than 1.8
				@Override
				public int compare(Registry<K, C, A>.Entry o1, Registry<K, C, A>.Entry o2) {
					int cmp = o1.priority - o2.priority;
					if (cmp == 0)
			            cmp = o1.id.compareToIgnoreCase(o2.id); // FIXME or not ... is id a good unique stuff
			        return cmp;
				}
			}
    );

    public class Priority {
        public static final int MAXIMUM = 0;
        public static final int HIGH = 25;
        public static final int MEDIUM = 50;
        public static final int LOW = 75;
        public static final int MINIMUM = 100;
    }

    public C get(K key, Object... args) {
        Factory<? extends C> factory = getFactory(key);
        if (factory != null)
            return factory.instantiate(args);
        return null;
    }

    public Factory<? extends C> getFactory(K key) {
        Entry entry = find(key);
        if (entry != null)
            return entry.factory;
        return null;
    }

    protected Entry find(K key) {
        Entry entry = findEntry(key);
        if (entry == null)
            return null;
        return entry;
    }

    protected Entry findById(String id) {
        for (Entry e: entries)
            if (e.id.equals(id))
                return e;
        return null;
    }

    public void install(Class<? extends C> clazz, A annotation) {
        Entry entry = newEntry(clazz, annotation);
        entries.add(entry);
    }

    protected abstract Entry newEntry(Class<? extends C> clazz, A annotation);

    protected Entry findEntry(K key) {
        for (Entry e: entries)
            if (e.handle(key))
                return e;
        return null;
    }

    public Entry findByClass(Class<? extends C> aClass) {
        for (Entry e: entries)
            if (e.clazz.equals(aClass))
                return e;
        return null;
    }

    public Set<Entry> getEntries() {
        return Collections.unmodifiableSet(entries);
    }

    public abstract class Entry {
        public final String id;
        public final int priority;
        final Class<? extends C> clazz;
        final Factory<? extends C> factory;

        protected Entry(String id, Class<? extends C> clazz, Factory<? extends C> factory, int priority) {
            this.id = id;
            this.clazz = clazz;
            this.factory = factory;
            this.priority = priority;
        }
        
        protected Entry(String id, Class<? extends C> clazz, int priority) {
            this.id = id;
            this.clazz = clazz;
            this.priority = priority;
            this.factory = null;
        }

        public C instantiate(Object[] args) {
            try {
                return factory.newInstance(args);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                return null;
            }
        }

        protected abstract boolean handle(K key);

        @Override
        public String toString() {
            return id;
        }
    }

    @SuppressWarnings("rawtypes")
	protected Factory<? extends C> defaultFactory(Class<? extends C> clazz, Class... signature) {
        try {
			final Constructor<? extends C> ctor = clazz.getConstructor(signature);
			return new Factory<C>() {
				@Override
				public C newInstance(Object[] args) {
					try {
						return ctor.newInstance(args);
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						return null;
					}
				}

				@Override
				public C instantiate(Object[] args) {
					return newInstance(args);
				}
			};
        } catch (NoSuchMethodException e) {
            System.out.println(Arrays.toString(clazz.getConstructors()));
            throw new RuntimeException(String.format("This is a static bug. Constructor %s(%s) not found",
                    clazz.getName(), Arrays.toString(signature)), e);
        }
    }

    public interface Factory<C> {
        C newInstance(Object[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException;
		C instantiate(Object[] args);
    }
}
