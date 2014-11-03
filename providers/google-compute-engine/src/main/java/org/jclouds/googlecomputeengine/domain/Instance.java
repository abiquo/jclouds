/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.googlecomputeengine.domain;

import static org.jclouds.googlecomputeengine.internal.NullSafeCopies.copyOf;

import java.net.URI;
import java.util.List;

import org.jclouds.javax.annotation.Nullable;
import org.jclouds.json.SerializedNames;

import com.google.auto.value.AutoValue;

/** Represents a virtual machine. */
@AutoValue
public abstract class Instance {

   @AutoValue
   public abstract static class AttachedDisk {
      public enum Type {
         PERSISTENT,
         SCRATCH;
      }

      public enum Mode {
         READ_WRITE,
         READ_ONLY;
      }

      /** A zero-based index to assign to this disk, where 0 is reserved for the boot disk. */
      @Nullable public abstract int index();

      public abstract Type type();

      public abstract Mode mode();

      /** Corresponds to {@linkplain Disk#selfLink()} when {@linkplain #type()} is {@linkplain Type#PERSISTENT}. */
      @Nullable public abstract URI source();

      /**
       * Must be unique within the instance when specified. This represents a unique
       * device name that is reflected into the /dev/ tree of a Linux operating system running within the
       * instance. If not specified, a default will be chosen by the system.
       */
      @Nullable public abstract String deviceName();

      public abstract boolean autoDelete();

      public abstract boolean boot();

      @SerializedNames({ "index", "type", "mode", "source", "deviceName", "autoDelete", "boot" })
      public static AttachedDisk create(int index, Type type, Mode mode, URI source, String deviceName,
            boolean autoDelete, boolean boot) {
         return new AutoValue_Instance_AttachedDisk(index, type, mode, source, deviceName, autoDelete, boot);
      }

      AttachedDisk() {
      }
   }

   @AutoValue
   public abstract static class NetworkInterface {
      /**
       * This specifies how this interface is configured to interact with other network services,
       * such as connecting to the internet.
       */
      @AutoValue
      public abstract static class AccessConfig {

         public enum Type {
            ONE_TO_ONE_NAT
         }

         @Nullable public abstract String name();

         public abstract Type type();

         /** An external IP address associated with this instance, if there is one. */
         @Nullable public abstract String natIP();

         @SerializedNames({ "name", "type", "natIP" })
         public static AccessConfig create(String name, Type type, String natIP) {
            return new AutoValue_Instance_NetworkInterface_AccessConfig(name, type, natIP);
         }

         AccessConfig() {
         }
      }

      public abstract String name();

      public abstract URI network();

      /** An IPV4 internal network address to assign to this instance. */
      @Nullable public abstract String networkIP();

      public abstract List<AccessConfig> accessConfigs();

      @SerializedNames({ "name", "network", "networkIP", "accessConfigs" })
      public static NetworkInterface create(String name, URI network, String networkIP,
            List<AccessConfig> accessConfigs) {
         return new AutoValue_Instance_NetworkInterface(name, network, networkIP, copyOf(accessConfigs));
      }

      NetworkInterface() {
      }
   }

   @AutoValue
   public abstract static class SerialPortOutput {

      @Nullable public abstract URI selfLink(); // TODO: is this really nullable?!

      /** The contents of the console output. */
      public abstract String contents();

      @SerializedNames({ "selfLink", "contents" })
      public static SerialPortOutput create(URI selfLink, String contents) {
         return new AutoValue_Instance_SerialPortOutput(selfLink, contents);
      }

      SerialPortOutput() {
      }
   }

   /**
    * A service account for which access tokens are to be made available to the instance through metadata queries.
    */
   @AutoValue
   public abstract static class ServiceAccount {

      public abstract String email();

      public abstract List<String> scopes();

      @SerializedNames({ "email", "scopes" })
      public static ServiceAccount create(String email, List<String> scopes) {
         return new AutoValue_Instance_ServiceAccount(email, scopes);
      }

      ServiceAccount() {
      }
   }

   public enum Status {
      PROVISIONING,
      STAGING,
      RUNNING,
      STOPPING,
      STOPPED,
      TERMINATED
   }

   public abstract String id();

   public abstract URI selfLink();

   public abstract String name();

   @Nullable public abstract String description();

   public abstract Tags tags();

   public abstract URI machineType();

   public abstract Status status();

   /** Human-readable explanation of the status. */
   @Nullable public abstract String statusMessage();

   /**
    * URL of the zone resource describing where this instance should be hosted; provided by the client when
    * the instance is created.
    */
   public abstract URI zone();

   public abstract List<NetworkInterface> networkInterfaces();

   public abstract List<AttachedDisk> disks();

   public abstract Metadata metadata();

   public abstract List<ServiceAccount> serviceAccounts();

   @SerializedNames({ "id", "selfLink", "name", "description", "tags", "machineType", "status", "statusMessage", "zone",
         "networkInterfaces", "disks", "metadata", "serviceAccounts" })
   public static Instance create(String id, URI selfLink, String name, String description, Tags tags, URI machineType,
         Status status, String statusMessage, URI zone, List<NetworkInterface> networkInterfaces,
         List<AttachedDisk> disks, Metadata metadata, List<ServiceAccount> serviceAccounts) {
      return new AutoValue_Instance(id, selfLink, name, description, tags, machineType, status, statusMessage, zone,
            copyOf(networkInterfaces), copyOf(disks), metadata, copyOf(serviceAccounts));
   }

   Instance() {
   }
}
