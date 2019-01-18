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
 * Engine API.
 */
public class InstanceGroupRestructured {

  private static String projectId;
  private static String instanceId;
  private static String zone;
  private final Compute computeService;
  private final String instanceGroupName = "new-instance-group";

  public static void main(String[] args) throws IOException, GeneralSecurityException {

    if (args.length != 2) {
      System.out.println("Missing required project id or zone");
    }
    projectId = args[0];
    zone = args[1];

    InstanceGroupRestructured instance =
        new InstanceGroupRestructured(projectId, "test-group", zone);
    instance.run();
  }

  public InstanceGroupRestructured(String projectId, String instanceId, String zone)
      throws IOException, GeneralSecurityException {
    this.projectId = projectId;
    this.instanceId = instanceId;
    this.zone = zone;
    computeService = createComputeService();
  }

  public void run() throws IOException, GeneralSecurityException {

    // Create an instance group
    createInstanceGroup();

    // List instance groups
    listInstanceGroups();

    // Create an instance to add and delete from instance group
    createInstance();

    // Add instance to instance group
    addInstancesToGroup();

    // List instances from a specific instance group
    listInstancesFromGroup();

    // Remove instance from instance group
    removeInstancesFromGroup();

    // Retrieve the list of instance groups sorted by zone
    getSortedInstanceGroupsList();

    // Set the named ports for the specified instance group
    setNamedPort();

    // Get the specified instance group
    getInstanceGroup();

    // Delete the instance
    deleteInstance();

    // Delete the specified instance group
    deleteInstanceGroup();
  }

  /**
   * Creates a {@link Compute} object for listing instances.
   * @return {@link Compute} object
   * @throws IOException
   * @throws GeneralSecurityException
   */
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
   * Creates an instance resource in the specified project using the data included in the request.
   */
  private void createInstance() throws IOException {
    Instance requestBody =
        new Instance().setName(instanceId).setZone("projects/" + projectId + "zones/" + zone)
            .setMachineType("zones/" + zone + "/machineTypes/n1-standard-1").setDisks(Lists
            .newArrayList(
                new AttachedDisk().setType("PERSISTENT").setBoot(true).setMode("READ_WRITE")
                    .setAutoDelete(true).setDeviceName(instanceId).setInitializeParams(
                    new AttachedDiskInitializeParams().setSourceImage(
                        "projects/debian-cloud/global/images/debian-9-stretch-v20181210")
                        .setDiskType(
                            "projects/" + projectId + "/zones/" + zone + "/diskTypes/pd-standard")
                        .setDiskSizeGb(new Long(10))))).setCanIpForward(false).setNetworkInterfaces(
            Lists.newArrayList(new NetworkInterface()
                .setSubnetwork("projects/" + projectId + "/regions/us-east1/subnetworks/default")
                .setAccessConfigs(Lists.newArrayList(
                    new AccessConfig().setName("External NAT").setType("ONE_TO_ONE_NAT")))));
    Compute.Instances.Insert request =
        computeService.instances().insert(projectId, zone, requestBody);
    request.execute();
  }

  /**
   * Deletes the specified Instance resource.
   */
  private void deleteInstance() throws IOException {
    Compute.Instances.Delete deleteRequest =
        computeService.instances().delete(projectId, zone, instanceId);
    deleteRequest.execute();
  }

  /**
   * Creates an instance group in the specified project using the parameters that are included in
   * the request.
   */
  public void createInstanceGroup() throws IOException {
    System.out.println("Creating instance group");
    InstanceGroup requestBody = new InstanceGroup().setName(instanceGroupName);
    Compute.InstanceGroups.Insert request =
        computeService.instanceGroups().insert(projectId, zone, requestBody);
    Operation response = request.execute();
    System.out.println("Instance group created");
  }

  /**
   * Retrieves the list of instance groups that are located in the specified project and zone.
   */
  public void listInstanceGroups() throws IOException {
    System.out.println("Listing instance groups");
    Compute.InstanceGroups.List request = computeService.instanceGroups().list(projectId, zone);
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
  }

  /**
   * Adds a list of instances to the specified instance group. All of the instances in the instance
   * group must be in the same network/subnetwork.
   */
  public void addInstancesToGroup() throws IOException {
    System.out.println("Add an instance to the instance group");

    String instanceUrl =
        "https://www.googleapis.com/compute/v1/projects/" + projectId + "/zones/" + zone
            + "/instances/" + instanceId;

    List<InstanceReference> instances = Lists.newArrayList();
    instances.add(new InstanceReference().setInstance(instanceUrl));
    InstanceGroupsAddInstancesRequest addRequest =
        new InstanceGroupsAddInstancesRequest().setInstances(instances);
    Compute.InstanceGroups.AddInstances addInstancesRequst = computeService.instanceGroups()
        .addInstances(projectId, zone, instanceGroupName, addRequest);
    Operation response = addInstancesRequst.execute();
  }

  /**
   * Lists the instances in the specified instance group.
   */
  public void listInstancesFromGroup() throws IOException {
    System.out.println("List instances from instance group");
    InstanceGroupsListInstancesRequest requestBody = new InstanceGroupsListInstancesRequest();
    Compute.InstanceGroups.ListInstances request = computeService.instanceGroups()
        .listInstances(projectId, zone, instanceGroupName, requestBody);
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
   */
  public void removeInstancesFromGroup() throws IOException {
    System.out.println("Remove instances from instance group");
    // String instanceToDelete = "new-instance";
    String instanceUrl =
        "https://www.googleapis.com/compute/v1/projects/" + projectId + "/zones/" + zone
            + "/instances/" + instanceId;

    List<InstanceReference> instances = Lists.newArrayList();
    instances.add(new InstanceReference().setInstance(instanceUrl));
    InstanceGroupsRemoveInstancesRequest requestBody =
        new InstanceGroupsRemoveInstancesRequest().setInstances(instances);
    Compute.InstanceGroups.RemoveInstances request = computeService.instanceGroups()
        .removeInstances(projectId, zone, instanceGroupName, requestBody);
    Operation response = request.execute();
  }

  /**
   * Retrieves the list of instance groups and sorts them by zone.
   */
  public void getSortedInstanceGroupsList() throws IOException {
    System.out.println("Get sorted instance group list");
    Compute.InstanceGroups.AggregatedList request =
        computeService.instanceGroups().aggregatedList(projectId);
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
  }

  /**
   * Sets the named ports for the specified instance group.
   */
  public void setNamedPort() throws IOException {
    System.out.println("Setting name port");
    List<NamedPort> namedPorts = Lists.newArrayList();
    namedPorts.add(new NamedPort().setName("port-name").setPort(1));
    InstanceGroupsSetNamedPortsRequest requestBody =
        new InstanceGroupsSetNamedPortsRequest().setNamedPorts(namedPorts);
    Compute.InstanceGroups.SetNamedPorts request = computeService.instanceGroups()
        .setNamedPorts(projectId, zone, instanceGroupName, requestBody);
    Operation response = request.execute();
    System.out.println("Set named port");
  }

  /**
   * Returns the specified instance group. Gets a list of available instance groups by making a {@link List}
   * request.
   */
  public InstanceGroup getInstanceGroup() throws IOException {
    System.out.println("Get instance group");
    Compute.InstanceGroups.Get request =
        computeService.instanceGroups().get(projectId, zone, instanceGroupName);
    InstanceGroup response = request.execute();
    System.out.println(response.toPrettyString());
    return response;
  }

  /**
   * Deletes the specified instance group. The instances in the group are not deleted. Note that
   * instance group must not belong to a backend service.
   */
  public void deleteInstanceGroup() throws IOException {
    System.out.println("Deleting instance group");
    Compute.InstanceGroups.Delete request =
        computeService.instanceGroups().delete(projectId, zone, instanceGroupName);
    Operation response = request.execute();
    System.out.println("Deleted instance group");
  }
}
