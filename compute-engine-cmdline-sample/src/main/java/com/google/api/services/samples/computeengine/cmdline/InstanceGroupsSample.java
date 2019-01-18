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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.InstanceGroups.Delete;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.compute.model.InstanceGroupAggregatedList;
import com.google.api.services.compute.model.InstanceGroupList;
import com.google.api.services.compute.model.InstanceGroupsAddInstancesRequest;
import com.google.api.services.compute.model.InstanceGroupsListInstances;
import com.google.api.services.compute.model.InstanceGroupsListInstancesRequest;
import com.google.api.services.compute.model.InstanceGroupsRemoveInstancesRequest;
import com.google.api.services.compute.model.InstanceGroupsScopedList;
import com.google.api.services.compute.model.InstanceGroupsSetNamedPortsRequest;
import com.google.api.services.compute.model.InstanceReference;
import com.google.api.services.compute.model.InstanceWithNamedPorts;
import com.google.api.services.compute.model.NamedPort;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Sample to demo using Google Compute Engine Instance Groups using Java and the Google Compute
 * Engine API
 */
public class InstanceGroupsSample {

  private static final String PROJECT_ID = "grass-clump-479"; // "YOUR_PROJECT_ID";
  private static final String INSTANCE_NAME = "new-instance";
  private static final String ZONE = "us-east1-b";

  public static void main(String[] args) {
    // Start Authorization process
    try {
      Compute computeService = createComputeService();

      String instanceGroupName = "new-instance-group";

      // Create an instance group
      createInstanceGroup(computeService, instanceGroupName);

      // List instance groups
      listInstanceGroups(computeService);

      // Create instance to add to and remove from instance group
      createInstance(computeService);
      String instanceUrl =
          "https://www.googleapis.com/compute/v1/projects/" + PROJECT_ID + "/zones/" + ZONE
              + "/instances/" + INSTANCE_NAME;

      // Add a list of instances to the specified instance group
      List<InstanceReference> instancesToAdd = Lists.newArrayList();
      instancesToAdd.add(new InstanceReference().setInstance(instanceUrl));
      addInstancesToGroup(computeService, instanceGroupName, instancesToAdd);

      // List instances from a specific instance group
      listInstancesFromGroup(computeService, instanceGroupName);

      // Remove instances from the group
      List<InstanceReference> instancesToDelete = Lists.newArrayList();
      instancesToDelete.add(new InstanceReference().setInstance(instanceUrl));
      removeInstancesFromGroup(computeService, instanceGroupName, instancesToDelete);

      // Retrieve the list of instance groups sorted by zone
      getSortedInstanceGroupsList(computeService);

      // Set the named ports for the specified instance group
      List<NamedPort> namedPorts = Lists.newArrayList();
      namedPorts.add(new NamedPort().setName("port-name").setPort(1));
      setNamedPort(computeService, instanceGroupName, namedPorts);

      // Get the specified instance group
      getInstanceGroup(computeService, instanceGroupName);

      // Delete the instance
      deleteInstance(computeService);

      // Delete the specified instance group
      deleteInstanceGroup(computeService, instanceGroupName);

    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
    }
    System.exit(1);
  }

  public static Compute createComputeService() throws IOException, GeneralSecurityException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    GoogleCredential credential = GoogleCredential.getApplicationDefault();
    if (credential.createScopedRequired()) {
      credential =
          credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
    }
    return new Compute.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("Google-ComputeSample/0.1").build();
  }

  /**
   * Creates an instance
   *
   * @param computeService a {@link Compute} object
   */
  private static void createInstance(Compute computeService) throws IOException {
    Instance requestBody =
        new Instance().setName(INSTANCE_NAME).setZone("projects/" + PROJECT_ID + "zones/" + ZONE)
            .setMachineType("zones/" + ZONE + "/machineTypes/n1-standard-1").setDisks(Lists
            .newArrayList(
                new AttachedDisk().setType("PERSISTENT").setBoot(true).setMode("READ_WRITE")
                    .setAutoDelete(true).setDeviceName(INSTANCE_NAME).setInitializeParams(
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
  }

  /**
   * Deletes an instance
   *
   * @param computeService a {@link Compute} object
   */
  private static void deleteInstance(Compute computeService) throws IOException {
    Compute.Instances.Delete deleteRequest =
        computeService.instances().delete(PROJECT_ID, ZONE, INSTANCE_NAME);
    deleteRequest.execute();
  }

  /**
   * Creates an instance group in the specified project using the parameters that are included in
   * the request.
   *
   * @param computeService a {@link Compute} object
   */
  public static InstanceGroup createInstanceGroup(Compute computeService, String instanceGroupName)
      throws IOException {
    System.out.println("Creating instance group");
    InstanceGroup requestBody = new InstanceGroup().setName(instanceGroupName);
    Compute.InstanceGroups.Insert request =
        computeService.instanceGroups().insert(PROJECT_ID, ZONE, requestBody);
    Operation response = request.execute();
    System.out.println("Instance group created");
    return requestBody;
  }

  /**
   * Retrieves the list of instance groups that are located in the specified project and zone.
   *
   * @param computeService a {@link Compute} object
   */
  public static InstanceGroupList listInstanceGroups(Compute computeService) throws IOException {
    System.out.println("Listing instance groups");
    Compute.InstanceGroups.List request = computeService.instanceGroups().list(PROJECT_ID, ZONE);
    InstanceGroupList response;
    do {
      response = request.execute();
      if (response.getItems() == null) {
        continue;
      }
      for (InstanceGroup instanceGroup : response.getItems()) {
        System.out.println(instanceGroup.getName() + " " + instanceGroup.toPrettyString());
      }
      request.setPageToken(response.getNextPageToken());
    } while (response.getNextPageToken() != null);
    return response;
  }

  /**
   * Adds a list of instances to the specified instance group. All of the instances in the instance
   * group must be in the same network/subnetwork.
   *
   * @param computeService a {@link Compute} object
   */
  public static void addInstancesToGroup(Compute computeService, String instanceGroup,
      List<InstanceReference> instances) throws IOException {
    System.out.println("Add an instance to the instance group");
    InstanceGroupsAddInstancesRequest requestBody =
        new InstanceGroupsAddInstancesRequest().setInstances(instances);
    Compute.InstanceGroups.AddInstances request = computeService.instanceGroups()
        .addInstances(PROJECT_ID, ZONE, instanceGroup, requestBody);
    Operation response = request.execute();
  }

  /**
   * Lists the instances in the specified instance group.
   *
   * @param computeService a {@link Compute} object
   */
  public static void listInstancesFromGroup(Compute computeService, String instanceGroup)
      throws IOException {
    System.out.println("List instances from instance group");
    InstanceGroupsListInstancesRequest requestBody = new InstanceGroupsListInstancesRequest();
    Compute.InstanceGroups.ListInstances request = computeService.instanceGroups()
        .listInstances(PROJECT_ID, ZONE, instanceGroup, requestBody);
    InstanceGroupsListInstances response;
    do {
      response = request.execute();
      if (response.getItems() == null) {
        continue;
      }
      for (InstanceWithNamedPorts instanceWithNamedPorts : response.getItems()) {
        System.out.println(instanceWithNamedPorts);
      }
      request.setPageToken(response.getNextPageToken());
    } while (response.getNextPageToken() != null);
  }

  /**
   * Removes one or more instances from the specified instance group, but does not delete those
   * instances.
   *
   * @param computeService a {@link Compute} object
   */
  public static void removeInstancesFromGroup(Compute computeService, String instanceGroup,
      List<InstanceReference> instances) throws IOException {
    System.out.println("Remove instances from instance group");
    InstanceGroupsRemoveInstancesRequest requestBody =
        new InstanceGroupsRemoveInstancesRequest().setInstances(instances);
    Compute.InstanceGroups.RemoveInstances request = computeService.instanceGroups()
        .removeInstances(PROJECT_ID, ZONE, instanceGroup, requestBody);
    Operation response = request.execute();
  }

  /**
   * Retrieves the list of instance groups and sorts them by zone.
   *
   * @param computeService a {@link Compute} object
   */
  public static InstanceGroupAggregatedList getSortedInstanceGroupsList(Compute computeService)
      throws IOException {
    System.out.println("Get sorted instance group list");
    Compute.InstanceGroups.AggregatedList request =
        computeService.instanceGroups().aggregatedList(PROJECT_ID);
    InstanceGroupAggregatedList response;
    do {
      response = request.execute();
      if (response.getItems() == null) {
        continue;
      }
      for (Map.Entry<String, InstanceGroupsScopedList> item : response.getItems().entrySet()) {
        System.out.println(item.getKey() + " : " + item.getValue());
      }
      request.setPageToken(response.getNextPageToken());
    } while (response.getNextPageToken() != null);
    return response;
  }

  /**
   * Sets the named ports for the specified instance group.
   *
   * @param computeService a {@link Compute} object
   */
  public static InstanceGroupsSetNamedPortsRequest setNamedPort(Compute computeService,
      String instanceGroup, List<NamedPort> namedPorts) throws IOException {
    System.out.println("Setting name port");
    InstanceGroupsSetNamedPortsRequest requestBody =
        new InstanceGroupsSetNamedPortsRequest().setNamedPorts(namedPorts);
    Compute.InstanceGroups.SetNamedPorts request = computeService.instanceGroups()
        .setNamedPorts(PROJECT_ID, ZONE, instanceGroup, requestBody);
    Operation response = request.execute();
    System.out.println("Set named port");
    return requestBody;
  }

  /**
   * Returns the specified instance group. Gets a list of available instance groups by making a
   * list() request.
   *
   * @param computeService a {@link Compute} object
   */
  public static InstanceGroup getInstanceGroup(Compute computeService, String instanceGroup)
      throws IOException {
    System.out.println("Get instance group");
    Compute.InstanceGroups.Get request =
        computeService.instanceGroups().get(PROJECT_ID, ZONE, instanceGroup);
    InstanceGroup response = request.execute();
    System.out.println(response.toPrettyString());
    return response;
  }

  /**
   * Deletes the specified instance group. The instances in the group are not deleted. Note that
   * instance group must not belong to a backend service.
   *
   * @param computeService a {@link Compute} object
   */
  public static Delete deleteInstanceGroup(Compute computeService, String instanceGroup)
      throws IOException {
    System.out.println("Deleting instance group");
    Compute.InstanceGroups.Delete request =
        computeService.instanceGroups().delete(PROJECT_ID, ZONE, instanceGroup);
    Operation response = request.execute();
    System.out.println("Deleted instance group");
    return request;
  }
}
