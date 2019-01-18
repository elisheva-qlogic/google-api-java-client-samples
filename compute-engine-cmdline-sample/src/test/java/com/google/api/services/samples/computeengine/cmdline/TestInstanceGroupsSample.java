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
import com.google.api.services.compute.Compute.InstanceGroups.Delete;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.compute.model.InstanceGroupAggregatedList;
import com.google.api.services.compute.model.InstanceGroupList;
import com.google.api.services.compute.model.InstanceGroupsSetNamedPortsRequest;
import com.google.api.services.compute.model.InstanceReference;
import com.google.api.services.compute.model.NamedPort;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestInstanceGroupsSample {

  private static final String PROJECT_ID = "grass-clump-479"; // "test-project"; // Replace with your project ID
  private static final String INSTANCE_GROUP = "new-instance-group";
  private static final String ZONE = "us-east1-b";
  private static Compute computeService;

  @BeforeClass
  public static void beforeClass() throws IOException, GeneralSecurityException {
    computeService = InstanceSample.createComputeService();

    // Create instance group

    // use method I didnt create??
    InstanceGroupsSample.createInstanceGroup(computeService, INSTANCE_GROUP);
    //    InstanceGroup requestBody = new InstanceGroup().setName(instanceGroupName);
    //    Compute.InstanceGroups.Insert request =
    //        computeService.instanceGroups().insert(PROJECT_ID, zoneName, requestBody);
    //    request.execute();
  }

  @AfterClass
  public static void afterClass() throws IOException {
    // Delete instance group

    // use method i didnt create??
    InstanceGroupsSample.deleteInstanceGroup(computeService, INSTANCE_GROUP);
    //    Compute.InstanceGroups.Delete request =
    //        computeService.instanceGroups().delete(PROJECT_ID, zoneName, instanceGroupName);
    //    request.execute();
  }

  @Test
  public void testCreateComputeService() throws IOException, GeneralSecurityException {
    Compute computeService = InstanceSample.createComputeService();
    Assert.assertNotNull(computeService);
  }

  @Test
  public void testCreateListDeleteInstanceGroup() throws IOException {
    // Create instance
    String fakeInstanceGroup = "fake-instance-group";
    InstanceGroup instanceGroup =
        InstanceGroupsSample.createInstanceGroup(computeService, fakeInstanceGroup);
    Assert.assertEquals(fakeInstanceGroup, instanceGroup.getName());

    //Delete instance
    Delete deleted = InstanceGroupsSample.deleteInstanceGroup(computeService, fakeInstanceGroup);
    Assert.assertEquals(deleted.getInstanceGroup(), fakeInstanceGroup);
  }

  @Test
  public void testListInstanceGroups() throws IOException {
    InstanceGroupList list = InstanceGroupsSample.listInstanceGroups(computeService);
    Assert.assertNotNull(list);
  }

  @Test
  public void testAddAndRemoveInstancesFromGroup() throws IOException {
    String instanceName = "instance";
    Instance requestBody =
        new Instance().setName(instanceName).setZone("projects/" + PROJECT_ID + "zones/" + ZONE)
            .setMachineType("zones/" + ZONE + "/machineTypes/n1-standard-1").setDisks(Lists
            .newArrayList(
                new AttachedDisk().setType("PERSISTENT").setBoot(true).setMode("READ_WRITE")
                    .setAutoDelete(true).setDeviceName(instanceName).setInitializeParams(
                    new AttachedDiskInitializeParams().setSourceImage(
                        "projects/debian-cloud/global/images/debian-9-stretch-v20181210")
                        .setDiskType("projects/" + PROJECT_ID + "/zones/" + ZONE
                            + "/diskTypes/pd-standard").setDiskSizeGb(new Long(10)))))
            .setCanIpForward(false).setNetworkInterfaces(Lists.newArrayList(new NetworkInterface()
            .setSubnetwork("projects/" + PROJECT_ID + "/regions/us-east1/subnetworks/default")
            .setAccessConfigs(Lists.newArrayList(
                new AccessConfig().setName("External NAT").setType("ONE_TO_ONE_NAT")))));
    Compute.Instances.Insert request =
        computeService.instances().insert(PROJECT_ID, ZONE, requestBody);
    request.execute();

    String instanceUrl =
        "https://www.googleapis.com/compute/v1/projects/" + PROJECT_ID + "/zones/" + ZONE
            + "/instances/" + instanceName;

    // Add instance to instance group
    List<InstanceReference> instancesToAdd = Lists.newArrayList();
    instancesToAdd.add(new InstanceReference().setInstance(instanceUrl));
    InstanceGroupsSample.addInstancesToGroup(computeService, INSTANCE_GROUP, instancesToAdd);
    Compute.Instances.Get getRequest =
        computeService.instances().get(PROJECT_ID, ZONE, instanceName);
    Instance getResponse = getRequest.execute();
    Assert.assertEquals(getResponse.getName(), instanceName);

    // Remove instance from instance group
    List<InstanceReference> instancesToDelete = Lists.newArrayList();
    instancesToDelete.add(new InstanceReference().setInstance(instanceUrl));
    InstanceGroupsSample
        .removeInstancesFromGroup(computeService, INSTANCE_GROUP, instancesToDelete);
    Compute.Instances.Get getDeleted =
        computeService.instances().get(PROJECT_ID, ZONE, instanceName);
    Instance deleted = getDeleted.execute();
    Assert.assertEquals(deleted.getName(), instanceName);

    // Delete the instance
    System.out.println("Delete the instance");
    Compute.Instances.Delete deleteInstance =
        computeService.instances().delete(PROJECT_ID, ZONE, instanceName);
    deleteInstance.execute();
    System.out.println("Deleted instance");
  }

  @Test
  public void testGetSortedInstanceGroupsList() throws IOException {
    Compute.InstanceGroups.AggregatedList request =
        computeService.instanceGroups().aggregatedList(PROJECT_ID);
    InstanceGroupAggregatedList response = request.execute();

    InstanceGroupAggregatedList list =
        InstanceGroupsSample.getSortedInstanceGroupsList(computeService);
    Assert.assertEquals(list.getItems().size(), response.getItems().size());
  }

  @Test
  public void testSetNamedPort() throws IOException {
    List<NamedPort> namedPorts = Lists.newArrayList();
    namedPorts.add(new NamedPort().setName("port-name").setPort(1));
    InstanceGroupsSetNamedPortsRequest ports =
        InstanceGroupsSample.setNamedPort(computeService, INSTANCE_GROUP, namedPorts);
    Assert.assertEquals(ports.getNamedPorts(), namedPorts);
  }

  @Test
  public void testGetInstanceGroup() throws IOException {
    InstanceGroup getInstanceGroup =
        InstanceGroupsSample.getInstanceGroup(computeService, INSTANCE_GROUP);
    Assert.assertEquals(getInstanceGroup.getName(), INSTANCE_GROUP);
  }
}