/*
 * Copyright 2019 Google LLC.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.services.samples.computeengine.cmdline;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestInstanceRestructured {

  private static final String INSTANCE_PROPERTY_NAME = "bigtable.instance";
  private static final String INSTANCE_PREFIX = "instance";
  private static String projectId;
  private static String instanceId;
  private static String zone = "us-east1-b";
  private static Compute computeService;
  private static InstanceRestructored instanceRestructored;


  @BeforeClass
  public static void beforeClass() throws IOException, GeneralSecurityException {
    //String targetInstance = System.getProperty(INSTANCE_PROPERTY_NAME);
    //projectId = ProjectName.of(InstanceName.parse(targetProject).getProject());

    projectId = "grass-clump-479";
    computeService = InstanceSample.createComputeService();
  }

  @AfterClass
  public static void afterClass() throws IOException {
    Compute.Instances.Delete deleteRequest =
        computeService.instances().delete(projectId, zone, instanceId);
    deleteRequest.execute();
    garbageCollect();
  }

  @Before
  public void setUp() throws IOException, GeneralSecurityException {
    instanceId = generateInstanceId();
    instanceRestructored = new InstanceRestructored(projectId, instanceId, zone);

    // Check if instance exists, if does not, create the instance
    Compute.Instances.List request = computeService.instances().list(projectId, zone);
    InstanceList list = request.execute();
    boolean found = false;
    for (Instance instanceName : list.getItems()) {
      if (instanceName.getName().equals(instanceId)) {
        found = true;
        break;
      }
    }
    if (found) {
      Compute.Instances.Delete deleteRequest =
          computeService.instances().delete(projectId, zone, instanceId);
      deleteRequest.execute();
    }
    Instance requestBody = new Instance().setName(instanceId)
        .setZone("projects/" + projectId + "zones/" + zone)
        .setMachineType("zones/" + zone + "/machineTypes/n1-standard-1").setDisks(Lists
            .newArrayList(
                new AttachedDisk().setType("PERSISTENT").setBoot(true).setMode("READ_WRITE")
                    .setAutoDelete(true).setDeviceName(instanceId).setInitializeParams(
                    new AttachedDiskInitializeParams().setSourceImage(
                        "projects/debian-cloud/global/images/debian-9-stretch-v20181210")
                        .setDiskType(
                            "projects/grass-clump-479/zones/us-east1-b/diskTypes/pd-standard")
                        .setDiskSizeGb(10L)))).setCanIpForward(false).setNetworkInterfaces(
            Lists.newArrayList(new NetworkInterface()
                .setSubnetwork("projects/" + projectId + "/regions/us-east1/subnetworks/default")
                .setAccessConfigs(Lists.newArrayList(
                    new AccessConfig().setName("External NAT").setType("ONE_TO_ONE_NAT")))));
    Compute.Instances.Insert insertRequest =
        computeService.instances().insert(projectId, zone, requestBody);
    insertRequest.execute();
  }

  @After
  public void after() throws IOException {
    Compute.Instances.Delete deleteRequest =
        computeService.instances().delete(projectId, zone, instanceId);
    deleteRequest.execute();
  }

  @Test
  public void testCreateAndDeleteInstance()
      throws IOException, GeneralSecurityException {
    // Create instance
    String fakeInstance = generateInstanceId();
    InstanceRestructored testInstance = new InstanceRestructored(projectId, fakeInstance, zone);
    testInstance.createInstance();
    Compute.Instances.Get request = computeService.instances().get(projectId, zone, fakeInstance);
    Instance response = request.execute();
    Assert.assertEquals(response.getName(), fakeInstance);


    while (response.getStatus(IN_PROCESS)) {
      poll
    }

    //Delete instance
    testInstance.deleteInstance();
    Compute.Instances.Get requestDeleted = computeService.instances().get(projectId, zone, fakeInstance);
    Instance responseDeleted = requestDeleted.execute();
    System.out.println(responseDeleted.toPrettyString());
    Assert.assertNull(responseDeleted);
  }

  @Test
  public void testCreateComputeService() throws IOException, GeneralSecurityException {
    Compute computeService = instanceRestructored.createComputeService();
    Assert.assertNotNull(computeService);
  }

  //????
  @Test
  public void testPrintInstances() throws IOException {
    // List instance
    OutputStream outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    instanceRestructored.printInstances();
    Assert.assertTrue(outputStream.toString().contains("Listing Compute Engine Instances"));

    // Restore normal output
    System.setOut(new PrintStream(System.out));
  }

  @Test
  public void testGetInstance() throws IOException {
    Instance getInstance = instanceRestructored.getInstance();
    Assert.assertEquals(getInstance.getName(), instanceId);
  }

  //???
  @Test
  public void testGetAggregatedInstancesList() throws IOException {
    OutputStream outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    instanceRestructored.getAggregatedInstancesList();
    Assert.assertTrue(outputStream.toString().contains("Getting an aggregated list of all instances"));
  }

  //  @Test
  //  public void testResetInstance() throws IOException {
  //    instanceCreated.setDescription("test-reset");
  //    System.out.println(instanceCreated.getDescription());
  //    Assert.assertEquals(instanceCreated.getDescription(), "test-reset");
  //    InstanceSample.resetInstance(computeService, INSTANCE_NAME);
  //    System.out.println(instanceCreated.getDescription());
  //    Assert.assertEquals(instanceCreated.getDescription(), null);
  //  }


  private static String generateInstanceId() {
    return String.format(
        "%s-%016x-%d", INSTANCE_PREFIX, System.currentTimeMillis(), new Random().nextLong());
  }

  public static void garbageCollect() throws IOException {
    Pattern timestampPattern = Pattern.compile(INSTANCE_PREFIX + "-([0-9]+)");
    Compute.Instances.List request = computeService.instances().list(projectId, zone);
    InstanceList response;
    do {
      response = request.execute();
      if (response.getItems() == null) {
        continue;
      }
      for (Instance instance : response.getItems()) {
        Matcher matcher = timestampPattern.matcher(instance.getName());
        if (!matcher.matches()) {
          continue;
        }
        System.out.println("Garbage collecting orphaned instance: " + instance.getName());
        Compute.Instances.Delete deleteRequest =
            computeService.instances().delete(projectId, zone, instance.getName());
        deleteRequest.execute();
      }
      request.setPageToken(response.getNextPageToken());
    } while (response.getNextPageToken() != null);
  }
}
