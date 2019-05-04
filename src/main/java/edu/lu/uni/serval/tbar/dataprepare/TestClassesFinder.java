package edu.lu.uni.serval.tbar.dataprepare;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestClassesFinder implements Callable<Collection<Class<?>>> {

	public Collection<Class<?>> call() throws Exception {
		URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		ClassloaderFinder finder = new ClassloaderFinder(classLoader);
		TestFilter testFilter = new TestFilter();
//		testFilter.acceptInnerClass();
		TestFinder processor = new TestFinder(finder, testFilter);
		Class<?>[] classes = processor.process();
		return Arrays.asList(classes);
	}

	protected String[] namesFrom(Collection<Class<?>> classes) {
		String[] names = new String[classes.size()];
		int index = 0;
		for (Class<?> aClass : classes) {
			names[index] = aClass.getName();
			index += 1;
		}
		return names;
	}

	public String[] findIn(ClassLoader dumpedToClassLoader, boolean acceptTestSuite) {
		ExecutorService executor = Executors
				.newSingleThreadExecutor(new CustomClassLoaderThreadFactory(dumpedToClassLoader));
		String[] testClasses;
		try {
			TestClassesFinder finder = new TestClassesFinder();
			Collection<Class<?>> classes = executor.submit(finder).get();
			testClasses = namesFrom(classes);
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		} catch (ExecutionException ee) {
			throw new RuntimeException(ee);
		} finally {
			executor.shutdown();
		}

		if (!acceptTestSuite) {
			testClasses = removeTestSuite(testClasses);
		}

//		if (this.logger.isDebugEnabled()) {
//			this.logger.debug("Test clases:");
//			for (String testClass : testClasses) {
//				this.logger.debug(testClass);
//			}
//		}
		
//		StringBuilder b = new StringBuilder();
//		for (String testClass : testClasses) {
//			b.append(testClass).append("\n");
//		}
//		try {
//			FileUtils.writeStringToFile(new File("logs/testCases_1.txt"), b.toString());
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return testClasses;
	}

	public String[] findIn(final URL[] classpath, boolean acceptTestSuite) {
		return findIn(new URLClassLoader(classpath), acceptTestSuite);
	}

	public String[] removeTestSuite(String[] totalTest) {
		List<String> tests = new ArrayList<String>();
		for (int i = 0; i < totalTest.length; i++) {
			if (!totalTest[i].endsWith("Suite")) {
				tests.add(totalTest[i]);
			}
		}
		return tests.toArray(new String[tests.size()]);
	}
}