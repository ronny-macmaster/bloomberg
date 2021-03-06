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

package com.bloomberg.bach.metrics.sink;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration2.SubsetConfiguration;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.metrics2.AbstractMetric;
import org.apache.hadoop.metrics2.MetricsRecord;
import org.apache.hadoop.metrics2.MetricsSink;
import org.apache.hadoop.metrics2.MetricsTag;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * A metrics sink that reports to a Kafka stream.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class KafkaSink implements MetricsSink, Closeable {

  // configuration keys
  public static final String CONFIG_FILE_KEY = "file";
  public static final String TOPIC_KEY = "topic";

  public String topic = "";
  private Properties properties = new Properties();
  private Producer<String, String> producer = null;

  @Override
  public void init(SubsetConfiguration conf) {
    String filename = conf.getString(CONFIG_FILE_KEY, null);
    topic = conf.getString(TOPIC_KEY, "test");

    if (filename == null) {
      // parse configuration file manually.
      Iterator<?> itr = conf.getKeys();
      while (itr.hasNext()) {
        String key = (String) itr.next();
        properties.put(key, conf.getString(key));
        System.out.format("%s : %s%n", key, conf.getString(key));
      }
    } else {
      try { // load configuration from file
        properties.load(new FileInputStream(new File(filename)));
      } catch (Exception error) {
        error.printStackTrace();
      }
    }

    // create the kafka producer. 
    producer = new KafkaProducer<String, String>(properties);
  }

  @Override
  public void close() throws IOException {
    producer.close();
  }

  @Override
  public void flush() {
    // producer.flush();
  }

  @Override
  public void putMetrics(MetricsRecord record) {
    System.out.format("%s: %s %n", record.name(), new Date(record.timestamp()));
    ProducerRecord<String, String> producerRecord = null;

    // send tags.
    for (MetricsTag tag : record.tags()) {
      String key = String.format("%s.%s", record.context(), tag.name());
      String value = tag.value();
      producerRecord = new ProducerRecord<String, String>(topic, key, value);
      producer.send(producerRecord);
    }

    // send metrics.
    for (AbstractMetric metric : record.metrics()) {
      String key = String.format("%s.%s", record.context(), metric.name());
      String value = metric.value().toString();
      producerRecord = new ProducerRecord<String, String>(topic, key, value);
      producer.send(producerRecord);
    }
  }

}
