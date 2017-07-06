
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.com.bloomberg.bach;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.metrics2.MetricsSystem;
import org.apache.hadoop.metrics2.annotation.Metric;
import org.apache.hadoop.metrics2.annotation.Metric.Type;
import org.apache.hadoop.metrics2.annotation.Metrics;
import org.apache.hadoop.metrics2.impl.MetricsSystemImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.bloomberg.bach.metrics.ConsoleSink;

/**
 * Tests the ConsoleSink
 * Redirects output from std.out (System.out) to verify console output.
 */
public class TestConsoleSink {
	
	// configuration file
	private String prefix = "test";
	private File configFile = new File(String.format("hadoop-metrics2-%s.properties", prefix));
	
	// metrics records
	private MetricsSystem metricsSystem = new MetricsSystemImpl();
	
	// redirection io streams
	private BufferedReader reader;
	private PrintStream writer;
	private PrintStream console;
	
	@Rule // junit test name
	public TestName testName = new TestName();
	
	@Metrics(name = "jvmMetricsRecord", context = "TestConsoleSink")
	private static class JvmMetrics {
		
		private static Runtime runtime = Runtime.getRuntime();
		
		@Metric(value = { "freeMemory", "free memory in the jvm" }, type = Type.GAUGE)
		long freeMemory() {
			return runtime.freeMemory();
		}
		
		@Metric(value = { "maxMemory", "max memory in the jvm" }, type = Type.GAUGE)
		long maxMemory() {
			return runtime.maxMemory();
		}
		
		@Metric(value = { "totalMemory", "total memory in the jvm" }, type = Type.GAUGE)
		long getTotalMemory() {
			return runtime.totalMemory();
		}
		
	}
	
	@Metrics(name = "junkMetricsRecord", context = "TestConsoleSink")
	private static class JunkMetrics {
		
		@Metric(value = { "StringTag", "a simple string metric" }, type = Type.TAG)
		String stringTag() {
			return "testing the console sink ... ";
		}
		
	}
	
	@Before
	public void setUp() throws IOException {
		PipedInputStream inputStream = new PipedInputStream();
		PipedOutputStream outputStream = new PipedOutputStream(inputStream);
		reader = new BufferedReader(new InputStreamReader(inputStream));
		writer = new PrintStream(outputStream);
		console = System.out;
		System.setOut(writer); // redirects System.out to test pipedInputStream.
	}
	
	@Test
	public void testConsoleSink() throws Exception {
		console.format("running %s%n", testName.getMethodName());
		
		buildConfiguration("%n - %v");
		metricsSystem.init(prefix);
		metricsSystem.start();
		metricsSystem.publishMetricsNow();
		metricsSystem.stop();
		metricsSystem.shutdown();
		
		writer.close();
		String line = "";
		while ((line = reader.readLine()) != null) {
			console.println(line);
		}
	}
	
	@Test
	public void testConsoleOutput() throws Exception {
		console.format("running %s%n", testName.getMethodName());
		System.setOut(console);
		
		buildConfiguration("%n - %v");
		metricsSystem.init(prefix);
		metricsSystem.start();
		metricsSystem.publishMetricsNow();
		metricsSystem.stop();
		metricsSystem.shutdown();
		
	}
	
	@After
	public void tearDown() throws IOException {
		reader.close();
		writer.close();
		System.setOut(console);
		configFile.delete();
	}
	
	private void buildConfiguration(String formatString) throws ConfigurationException {
		PropertiesConfiguration config = new PropertiesConfiguration();
		config.addProperty("test.sink.console.publishSelfMetrics", false);
		config.addProperty("test.sink.console.class", ConsoleSink.class.getName());
		config.addProperty("test.sink.console.format", formatString);
		config.save(configFile);
	}
	
}
