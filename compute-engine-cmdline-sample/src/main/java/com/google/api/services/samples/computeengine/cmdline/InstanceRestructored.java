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
import com.google.api.services.compute.Compute.GlobalOperations;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceAggregatedList;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.InstancesScopedList;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Map;

/**
 * Sample to demo using Google Compute Engine instances using Java and the Google Compute
 * Engine API.
 */
public class InstanceRestructored {

  private static String projectId;
  private static String instanceId;
  private static String zone;
  private final Compute computeService;

  public static void main(String[] args) throws IOException, GeneralSecurityException {

    if (args.length != 2) {
      System.out.println("Missing required project id or zone");
    }
    projectId = args[0];
    zone = args[1];

    InstanceRestructored instance = new InstanceRestructored(projectId, "test-instance", zone);
    instance.run();
  }

  public InstanceRestructored(String projectId, String instanceId, String zone) throws IOException, GeneralSecurityException {
    this.projectId = projectId;
    this.instanceId = instanceId;
    this.zone = zone;
    computeService = createComputeService();
  }

  public void run() {
    // Start Authorization process
    try {
      // Create an instance
      createInstance();

      // List out instances
      printInstances();

      // Get an instance
      getInstance();

      // Delete an instance
      deleteInstance();

      // Get sorted list of instances in the project
      getAggregatedInstancesList();

      //      // Add access config
      //      resetInstance(computeService, testInstance);

    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  /**
   * Creates a {@link Compute} object for listing instances.
   * @return {@link Compute} object
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public Compute createComputeService() throws IOException, GeneralSecurityException {
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
  public void createInstance()
      throws IOException {
    System.out.println("Create new instance");
    Instance requestBody =
        new Instance().setName(instanceId).setZone("projects/" + projectId + "zones/" + zone)
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
    Compute.Instances.Insert request =
        computeService.instances().insert(projectId, zone, requestBody);
    Operation response = request.execute();

    Operation actualResponse =

    // Need to poll the response here until it is done and created.
    while (response.getStatus(IN_PROCESS)) {
      poll
    }

    System.out.println(response.toPrettyString());
    System.out.printf("Instance created: %s%n", instanceId);
  }

  /**
   * Prints available machine instances.
   */
  public void printInstances()
      throws IOException {
    System.out.println("Listing Compute Engine Instances");
    Compute.Instances.List instances = computeService.instances().list(projectId, zone);
    InstanceList list = instances.execute();
    if (list.getItems() == null) {
      System.out.println("No instances found. Sign in to the Google APIs Console and create "
          + "an instance at: code.google.com/apis/console");
    } else {
      for (Instance instance : list.getItems()) {
        System.out.println(instance.toPrettyString());
      }
    }
  }

  /**
   * Returns the specified Instance resource. Gets a list of available instances by making a list()
   * request.
   */
  public Instance getInstance()
      throws IOException {
    Compute.Instances.Get request = computeService.instances().get(projectId, zone, instanceId);
    Instance response = request.execute();
    System.out.println(response.toString());
    System.out.println(response);
    return response;
  }

  /**
   * Deletes the specified Instance resource.
   */
  public void deleteInstance() throws IOException {
    System.out.println("Deleting an Instance");
    Compute.Instances.Delete request =
        computeService.instances().delete(projectId, zone, instanceId);
    Operation response = request.execute();
    System.out.printf("Deleted instance: %s%n", instanceId);
  }

  /**
   * Retrieves aggregated list of all of the instances in your project across all regions and
   * zones.
   */
  public void getAggregatedInstancesList()
      throws IOException {
    System.out.println("Getting an aggregated list of all instances");
    Compute.Instances.AggregatedList request = computeService.instances().aggregatedList(projectId);
    InstanceAggregatedList response;
    do {
      response = request.execute();
      if (response.getItems() == null) {
        continue;
      }
      for (Map.Entry<String, InstancesScopedList> item : response.getItems().entrySet()) {
        System.out.println(item.getKey() + " " + item.getValue());
      }
      request.setPageToken(response.getNextPageToken());
    } while (response.getNextPageToken() != null);
  }

  //  /**
  //   * Adds an access config to an instance's network interface.
  //   */
  //  public static void resetInstance() throws IOException {
  //    Compute.Instances.Reset request =
  //        computeService.instances().reset(projectId, zoneName, instanceId);
  //    Operation response = request.execute();
  //  }

  public void waitForOperationCompletion() {
    GlobalOperationClient globalOperationClient =

  }

}

