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
import com.google.api.services.compute.Compute.Instances.Delete;
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
 * Engine API
 */
public class InstanceSample {

  private static final String PROJECT_ID = "grass-clump-479"; // "YOUR_PROJECT_ID";
  private static final String ZONE = "us-east1-b"; // "YOUR_ZONE"

  public static void main(String[] args) {
    // Start Authorization process
    try {
      String testInstance = "test-instance";

      // Create compute engine object for listing instances
      Compute computeService = createComputeService();

      // Create an instance
      //      String instance = createInstance(computeService);
      createInstance(computeService, testInstance);

      // List out instances
      printInstances(computeService);

      // Get an instance
      getInstance(computeService, testInstance);

      // Delete an instance
      deleteInstance(computeService, testInstance);

      // Get sorted list of instances in the project
      getAggregatedInstancesList(computeService);

//      // Add access config
//      resetInstance(computeService, testInstance);

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

  private static boolean instanceExists(Compute computeService, String instance)
      throws IOException {
    Compute.Instances.List request = computeService.instances().list(PROJECT_ID, ZONE);
    InstanceList list = request.execute();
    boolean found = false;
    for (Instance instanceName : list.getItems()) {
      if (instanceName.getName().equals(instance)) {
        found = true;
        break;
      }
    }
    return found;
  }

  /**
   * Creates an instance resource in the specified project using the data included in the request.
   *
   * @param computeService a {@link Compute} object
   */
  public static Instance createInstance(Compute computeService, String instanceName)
      throws IOException {
    System.out.println("Creating an Instance");
    if (instanceExists(computeService, instanceName)) {
      System.out.println("Existing instance will be deleted");
      deleteInstance(computeService, instanceName);
    }
    System.out.println("Create new instance");
    Instance requestBody =
        new Instance().setName(instanceName).setZone("projects/" + PROJECT_ID + "zones/" + ZONE)
            .setMachineType("zones/" + ZONE + "/machineTypes/n1-standard-1").setDisks(Lists
            .newArrayList(
                new AttachedDisk().setType("PERSISTENT").setBoot(true).setMode("READ_WRITE")
                    .setAutoDelete(true).setDeviceName(instanceName).setInitializeParams(
                    new AttachedDiskInitializeParams().setSourceImage(
                        "projects/debian-cloud/global/images/debian-9-stretch-v20181210")
                        .setDiskType(
                            "projects/grass-clump-479/zones/us-east1-b/diskTypes/pd-standard")
                        .setDiskSizeGb(10L)))).setCanIpForward(false).setNetworkInterfaces(
            Lists.newArrayList(new NetworkInterface()
                .setSubnetwork("projects/" + PROJECT_ID + "/regions/us-east1/subnetworks/default")
                .setAccessConfigs(Lists.newArrayList(
                    new AccessConfig().setName("External NAT").setType("ONE_TO_ONE_NAT")))));
    Compute.Instances.Insert request =
        computeService.instances().insert(PROJECT_ID, ZONE, requestBody);
    Operation response = request.execute();
    System.out.println(response.toPrettyString());
    System.out.println("Instance created");
    return requestBody;
  }

  /**
   * Print available machine instances.
   *
   * @param computeService The main API access point
   */
  public static InstanceList printInstances(Compute computeService)
      throws IOException {
    System.out.println("Listing Compute Engine Instances");
    Compute.Instances.List instances = computeService.instances().list(PROJECT_ID, ZONE);
    InstanceList list = instances.execute();
    if (list.getItems() == null) {
      System.out.println("No instances found. Sign in to the Google APIs Console and create "
          + "an instance at: code.google.com/apis/console");
    } else {
      for (Instance instance : list.getItems()) {
        System.out.println(instance.toPrettyString());
      }
    }
    return list;
  }

  /**
   * Returns the specified Instance resource. Gets a list of available instances by making a list()
   * request.
   *
   * @param computeService
   * @param testInstance
   */
  public static Instance getInstance(Compute computeService, String testInstance)
      throws IOException {
    Compute.Instances.Get request = computeService.instances().get(PROJECT_ID, ZONE, testInstance);
    Instance response = request.execute();
    return response;
  }

  /**
   * Deletes the specified Instance resource.
   *
   * @param computeService a {@link Compute} object
   */
  public static Delete deleteInstance(Compute computeService, String instance) throws IOException {
    System.out.println("Deleting an Instance");
    Compute.Instances.Delete request =
        computeService.instances().delete(PROJECT_ID, ZONE, instance);
    Operation response = request.execute();
    System.out.println("Deleted instance");
    return request;
  }

  /**
   * Retrieves aggregated list of all of the instances in your project across all regions and
   * zones.
   *
   * @param computeService a {@link Compute} object
   */
  public static InstanceAggregatedList getAggregatedInstancesList(Compute computeService)
      throws IOException {
    System.out.println("Getting an aggregated list of all instances");
    Compute.Instances.AggregatedList request = computeService.instances().aggregatedList(PROJECT_ID);
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
    return response;
  }

//  /**
//   * Adds an access config to an instance's network interface.
//   */
//  public static void resetInstance(Compute computeService, String instance) throws IOException {
//    Compute.Instances.Reset request =
//        computeService.instances().reset(projectId, zoneName, instance);
//    Operation response = request.execute();
//  }
}

