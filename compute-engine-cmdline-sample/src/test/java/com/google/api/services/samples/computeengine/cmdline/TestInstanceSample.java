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
import com.google.api.services.compute.Compute.Instances.Delete;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceAggregatedList;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestInstanceSample {

  private static final String PROJECT_ID = "grass-clump-479"; // "test-project"; // Replace with your project ID
  private static final String INSTANCE_NAME = "test-instance";
  private static final String ZONE = "us-east1-b"; // "YOUR_ZONE"
  private static Compute computeService;

  @BeforeClass
  public static void beforeClass() throws IOException, GeneralSecurityException {
    computeService = InstanceSample.createComputeService();

    Compute.Instances.List request = computeService.instances().list(PROJECT_ID, ZONE);
    InstanceList list = request.execute();
    boolean found = false;
    for (Instance instanceName : list.getItems()) {
      if (instanceName.getName().equals(INSTANCE_NAME)) {
        found = true;
        break;
      }
    }
    if (found) {
      System.out.println("Existing instance will be deleted");
      Compute.Instances.Delete deleteRequest =
          computeService.instances().delete(PROJECT_ID, ZONE, INSTANCE_NAME);
      deleteRequest.execute();
    }
    System.out.println("Create new instance");
    Instance requestBody = new Instance().setName(INSTANCE_NAME)
        .setZone("projects/" + PROJECT_ID + "zones/" + ZONE)
        .setMachineType("zones/" + ZONE + "/machineTypes/n1-standard-1").setDisks(Lists
            .newArrayList(
                new AttachedDisk().setType("PERSISTENT").setBoot(true).setMode("READ_WRITE")
                    .setAutoDelete(true).setDeviceName(INSTANCE_NAME).setInitializeParams(
                    new AttachedDiskInitializeParams().setSourceImage(
                        "projects/debian-cloud/global/images/debian-9-stretch-v20181210")
                        .setDiskType(
                            "projects/grass-clump-479/zones/us-east1-b/diskTypes/pd-standard")
                        .setDiskSizeGb(new Long(10))))).setCanIpForward(false).setNetworkInterfaces(
            Lists.newArrayList(new NetworkInterface()
                .setSubnetwork("projects/" + PROJECT_ID + "/regions/us-east1/subnetworks/default")
                .setAccessConfigs(Lists.newArrayList(
                    new AccessConfig().setName("External NAT").setType("ONE_TO_ONE_NAT")))));
    Compute.Instances.Insert insertRequest =
        computeService.instances().insert(PROJECT_ID, ZONE, requestBody);
    insertRequest.execute();
  }

  @AfterClass
  public static void afterClass() throws IOException {
    Compute.Instances.Delete deleteRequest =
        computeService.instances().delete(PROJECT_ID, ZONE, INSTANCE_NAME);
    deleteRequest.execute();
  }

  @Test
  public void testCreateListDeleteInstance() throws IOException {
    // Create instance
    String fakeInstance = "fake-instance";
    Instance instance = InstanceSample.createInstance(computeService, fakeInstance);
    Assert.assertEquals(fakeInstance, instance.getName());

    //Delete instance
    Delete deleted = InstanceSample.deleteInstance(computeService, fakeInstance);
    Assert.assertEquals(deleted.getInstance(), fakeInstance);
  }

  @Test
  public void testCreateComputeService() throws IOException, GeneralSecurityException {
    Compute computeService = InstanceSample.createComputeService();
    Assert.assertNotNull(computeService);
  }

  //DOES THIS REALLY TEST print???\
  //????????????????????????????

  @Test
  public void testPrintInstances() throws IOException {
    // List instance
    InstanceList list = InstanceSample.printInstances(computeService);
    Assert.assertNotNull(list);
    //    boolean found = false;
    //    for (Instance instance : list.getItems()) {
    //      if (instance.getName().equals(instanceCreated.getName())) {
    //        found = true;
    //      }
    //    }
    //    Assert.assertTrue(found);
  }

  //DOES THIS REALLY TEST AGGREGATE??\
  //????????????????????????????

  @Test
  public void testGetInstance() throws IOException {
    Instance getInstance = InstanceSample.getInstance(computeService, INSTANCE_NAME);
    Assert.assertEquals(getInstance.getName(), INSTANCE_NAME);
  }

  @Test
  public void testGetAggregatedInstancesList() throws IOException {
    Compute.Instances.AggregatedList request =
        computeService.instances().aggregatedList(PROJECT_ID);
    InstanceAggregatedList response = request.execute();

    InstanceAggregatedList list = InstanceSample.getAggregatedInstancesList(computeService);

    Assert.assertEquals(list.getItems().size(), response.getItems().size());
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
}
