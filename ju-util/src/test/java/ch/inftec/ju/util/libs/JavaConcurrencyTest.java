package ch.inftec.ju.util.libs;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import ch.inftec.ju.util.JuException;

public class JavaConcurrencyTest {
	private static ExecutorService executor = Executors.newSingleThreadExecutor();
	
	@AfterClass
	public static void shutdownExecutor() {
		executor.shutdown();
	}
	
	@Test
	public void futureTask_returningResult() throws Exception {
		FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "FutureTask result";
			}
		});
		
		executor.execute(futureTask);
		
		Assert.assertEquals("FutureTask result", futureTask.get());
		Assert.assertTrue(futureTask.isDone());
		Assert.assertFalse(futureTask.isCancelled());
	}
	
	@Test
	public void futureTask_throwingException() throws Exception {
		FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				throw new JuException("Failing...");
			}
		});
		
		executor.execute(futureTask);
		
		try {
			futureTask.get();
		} catch (ExecutionException ex) {
			Assert.assertEquals("Failing...", ex.getCause().getMessage());
			
			Assert.assertTrue(futureTask.isDone());
			Assert.assertFalse(futureTask.isCancelled());
		}
	}
	
	@Test
	public void futureTask_canBeCancelled() throws Exception {
		FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				while (true) {} // Run endlessly
			}
		});
		
		executor.execute(futureTask);
		futureTask.cancel(true);
		
		Assert.assertTrue(futureTask.isDone());
		Assert.assertTrue(futureTask.isCancelled());
	}
}
