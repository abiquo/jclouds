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
package org.jclouds.chef.filters;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.hash.Hashing.sha1;
import static com.google.common.io.BaseEncoding.base64;
import static com.google.common.io.ByteStreams.asByteSource;
import static com.google.common.io.ByteStreams.toByteArray;

import java.security.PrivateKey;
import java.util.NoSuchElementException;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jclouds.Constants;
import org.jclouds.date.TimeStamp;
import org.jclouds.http.HttpException;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpRequestFilter;
import org.jclouds.http.HttpUtils;
import org.jclouds.http.internal.SignatureWire;
import org.jclouds.io.ByteSources;
import org.jclouds.io.Payload;
import org.jclouds.io.Payloads;
import org.jclouds.io.payloads.MultipartForm;
import org.jclouds.io.payloads.Part;
import org.jclouds.io.payloads.RSAEncryptingPayload;
import org.jclouds.logging.Logger;
import org.jclouds.rest.annotations.Identity;
import org.jclouds.util.Strings2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * Ported from mixlib-authentication in order to sign Chef requests.
 * 
 * @see <a href= "http://github.com/opscode/mixlib-authentication" />
 * @author Adrian Cole
 * 
 */
@Singleton
public class SignedHeaderAuth implements HttpRequestFilter {
   public static final String SIGNING_DESCRIPTION = "version=1.0";

   private final SignatureWire signatureWire;
   private final String userId;
   private final PrivateKey privateKey;
   private final Provider<String> timeStampProvider;
   private final String emptyStringHash;
   private final HttpUtils utils;

   @Resource
   @Named(Constants.LOGGER_SIGNATURE)
   Logger signatureLog = Logger.NULL;

   @Inject
   public SignedHeaderAuth(SignatureWire signatureWire, @Identity String userId, PrivateKey privateKey,
            @TimeStamp Provider<String> timeStampProvider, HttpUtils utils) {
      this.signatureWire = signatureWire;
      this.userId = userId;
      this.privateKey = privateKey;
      this.timeStampProvider = timeStampProvider;
      this.emptyStringHash = hashBody(Payloads.newStringPayload(""));
      this.utils = utils;
   }

   public HttpRequest filter( HttpRequest request ) throws HttpException {

      String contentHash = hashBody(request.getPayload());
      Multimap<String, String> headers = ArrayListMultimap.create();
      headers.put( "X-Ops-Content-Hash", contentHash );
      String timestamp = timeStampProvider.get();
      String toSign = createStringToSign(request.getMethod(), hashPath(request.getEndpoint().getPath()), contentHash,
               timestamp);
      headers.put("X-Ops-Userid", userId);
      headers.put( "X-Ops-Sign", SIGNING_DESCRIPTION );
      request = calculateAndReplaceAuthorizationHeaders( request, toSign );
      headers.put( "X-Ops-Timestamp", timestamp );
      utils.logRequest( signatureLog, request, "<<" );

      return request.toBuilder().replaceHeaders(headers).build();
   }

   @VisibleForTesting
   HttpRequest calculateAndReplaceAuthorizationHeaders( HttpRequest request, String toSign ) throws HttpException {
      String signature = sign(toSign);
      if (signatureWire.enabled())
         signatureWire.input(Strings2.toInputStream(signature));
      String[] signatureLines = Iterables.toArray(Splitter.fixedLength(60).split(signature), String.class);

      Multimap<String, String> headers = ArrayListMultimap.create();
      for (int i = 0; i < signatureLines.length; i++) {
         headers.put("X-Ops-Authorization-" + (i + 1), signatureLines[i]);
      }
      return request.toBuilder().replaceHeaders(headers).build();
   }

   public String createStringToSign(String request, String hashedPath, String contentHash, String timestamp) {

      return new StringBuilder().append("Method:").append(request).append("\n").append("Hashed Path:").append(
               hashedPath).append("\n").append("X-Ops-Content-Hash:").append(contentHash).append("\n").append(
               "X-Ops-Timestamp:").append(timestamp).append("\n").append("X-Ops-UserId:").append(userId).toString();

   }

   @VisibleForTesting
   String hashPath(String path) {
      try {
         return base64().encode(asByteSource(canonicalPath(path).getBytes(UTF_8)).hash(sha1()).asBytes());
      } catch (Exception e) {
         Throwables.propagateIfPossible(e);
         throw new HttpException("error creating sigature for path: " + path, e);
      }
   }

   /**
    * Build the canonicalized path, which collapses multiple slashes (/) and removes a trailing
    * slash unless the path is only "/"
    */
   @VisibleForTesting
   String canonicalPath(String path) {
      path = path.replaceAll("\\/+", "/");
      return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
   }

   @VisibleForTesting
   String hashBody(Payload payload) {
      if (payload == null)
         return emptyStringHash;
      payload = useTheFilePartIfForm(payload);
      checkArgument(payload != null, "payload was null");
      checkArgument(payload.isRepeatable(), "payload must be repeatable: " + payload);
      try {
         return base64().encode(ByteSources.asByteSource(payload.getInput()).hash(sha1()).asBytes());
      } catch (Exception e) {
         Throwables.propagateIfPossible(e);
         throw new HttpException("error creating sigature for payload: " + payload, e);
      }
   }

   private Payload useTheFilePartIfForm(Payload payload) {
      if (payload instanceof MultipartForm) {
         Iterable<? extends Part> parts = MultipartForm.class.cast(payload).getRawContent();
         try {
            payload = Iterables.find(parts, new Predicate<Part>() {

               @Override
               public boolean apply(Part input) {
                  return "file".equals(input.getName());
               }

            });
         } catch (NoSuchElementException e) {

         }
      }
      return payload;
   }

   public String sign(String toSign) {
      try {
         byte[] encrypted = toByteArray(new RSAEncryptingPayload(Payloads.newStringPayload(toSign), privateKey));
         return base64().encode(encrypted);
      } catch (Exception e) {
         throw new HttpException("error signing request", e);
      }
   }

}
