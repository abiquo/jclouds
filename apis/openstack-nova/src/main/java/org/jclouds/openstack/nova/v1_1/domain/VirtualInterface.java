/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.openstack.nova.v1_1.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.gson.annotations.SerializedName;

/**
 * Represents a Virtual Interface (VIF)
 *
 * @author Adam Lowe
 * @see org.jclouds.openstack.nova.v1_1.extensions.VirtualInterfaceClient
 */
public class VirtualInterface {

   public static Builder<?> builder() {
      return new ConcreteBuilder();
   }

   public Builder<?> toBuilder() {
      return new ConcreteBuilder().fromVirtualInterface(this);
   }

   public static abstract class Builder<T extends Builder<T>>  {
      protected abstract T self();

      private String id;
      private String macAddress;

      /**
       * @see VirtualInterface#getId()
       */
      public T id(String id) {
         this.id = id;
         return self();
      }

      /**
       * @see VirtualInterface#getMacAddress()
       */
      public T macAddress(String macAddress) {
         this.macAddress = macAddress;
         return self();
      }

      public VirtualInterface build() {
         return new VirtualInterface(this);
      }

      public T fromVirtualInterface(VirtualInterface in) {
         return this
               .id(in.getId())
               .macAddress(in.getMacAddress())
               ;
      }

   }

   private static class ConcreteBuilder extends Builder<ConcreteBuilder> {
      @Override
      protected ConcreteBuilder self() {
         return this;
      }
   }

   protected VirtualInterface() {
      // we want serializers like Gson to work w/o using sun.misc.Unsafe,
      // prohibited in GAE. This also implies fields are not final.
      // see http://code.google.com/p/jclouds/issues/detail?id=925
   }
  
   private String id;
   @SerializedName(value="mac_address")
   private String macAddress;

   protected VirtualInterface(Builder<?> builder) {
      this.id = checkNotNull(builder.id, "id");
      this.macAddress = checkNotNull(builder.macAddress, "macAddress");
   }

   public String getId() {
      return this.id;
   }
   
   public String getMacAddress() {
      return this.macAddress;
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(id, macAddress);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      VirtualInterface that = VirtualInterface.class.cast(obj);
      return Objects.equal(this.id, that.id)
            && Objects.equal(this.macAddress, that.macAddress)
            ;
   }

   protected ToStringHelper string() {
      return Objects.toStringHelper("")
            .add("id", id)
            .add("macAddress", macAddress)
            ;
   }

   @Override
   public String toString() {
      return string().toString();
   }

}