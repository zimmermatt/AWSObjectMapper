/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.awsobjectmapper;

import com.amazonaws.services.ecs.model.VersionInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.ClassPath;
import com.google.common.io.Resources;

import io.github.benas.jpopulator.api.Populator;
import io.github.benas.jpopulator.impl.PopulatorBuilder;

import com.amazonaws.services.route53.model.ResourceRecordSet;

import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AmazonObjectMapperTest {

  private boolean hasEmptyConstructor(Class<?> c) {
    try {
      c.getConstructor(); // Throws if no match
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isModelClass(Class<?> c) {
    boolean skip = false;

    // Skip package and exception classes
    final String simpleName = c.getSimpleName();
    skip = simpleName == "package-info" || simpleName.endsWith("Exception");

    // Ignore transform classes
    skip = skip || c.getName().contains(".transform.");

    // Ignore interfaces
    skip = skip || c.isInterface();

    // Must have an empty constructor
    skip = skip || !hasEmptyConstructor(c);

    return !skip;
  }

  @Test
  public void mapRandomAwsObjects() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    AmazonObjectMapperConfigurer.configure(mapper);
    final Populator p = (new PopulatorBuilder()).build();
    final Set<ClassPath.ClassInfo> classes = ClassPath
        .from(getClass().getClassLoader())
        .getTopLevelClassesRecursive("com.amazonaws");
    for (ClassPath.ClassInfo cinfo : classes) {
      if (cinfo.getName().contains(".model.")
          && !cinfo.getSimpleName().startsWith("GetConsole")
          && !cinfo.getName().contains(".s3.model.")) { // TODO: problem with CORSRule
        final Class<?> c = cinfo.load();
        if (isModelClass(c)) {
          Object obj = p.populateBean(c);
          String j1 = mapper.writeValueAsString(obj);
          Object d1 = mapper.readValue(j1, c);
          String j2 = mapper.writeValueAsString(d1);
          Assert.assertEquals(j1, j2);
        }
      }
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedMapper() throws Exception {
    final AmazonObjectMapper mapper = new AmazonObjectMapper();
    final Populator p = new PopulatorBuilder().build();
    Object obj = p.populateBean(VersionInfo.class);
    String j1 = mapper.writeValueAsString(obj);
    Object d1 = mapper.readValue(j1, VersionInfo.class);
    String j2 = mapper.writeValueAsString(d1);
    Assert.assertEquals(j1, j2);
  }

  @Test
  public void namingStrategy() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    AmazonObjectMapperConfigurer.configure(mapper);
    byte[] json = Resources.toByteArray(Resources.getResource("recordSet.json"));
    ResourceRecordSet recordSet = mapper.readValue(json, ResourceRecordSet.class);
    Assert.assertEquals(60L, (long) recordSet.getTTL());
  }
}
