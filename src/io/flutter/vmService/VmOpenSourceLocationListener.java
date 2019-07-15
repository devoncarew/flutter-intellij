/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.vmService;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.EventDispatcher;
import io.flutter.FlutterUtils;
import io.flutter.utils.VmServiceListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.dartlang.vm.service.VmService;
import org.dartlang.vm.service.consumer.ServiceExtensionConsumer;
import org.dartlang.vm.service.consumer.VersionConsumer;
import org.dartlang.vm.service.element.Event;
import org.dartlang.vm.service.element.RPCError;
import org.dartlang.vm.service.element.Version;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

/**
 * This class manages the openSourceLocation VM service registration and exposes the request as an event to listeners.
 */
public class VmOpenSourceLocationListener {
  private static final Logger LOG = Logger.getInstance(VmOpenSourceLocationListener.class);

  public interface Listener extends EventListener {
    void onRequest(@NotNull String isolateId, @NotNull String scriptId, int tokenPos);
  }

  /**
   * Connect to the VM observatory service via the specified URI
   *
   * @return an API object for interacting with the VM service (not {@code null}).
   */
  public static VmOpenSourceLocationListener connect(@NotNull VmService service) {
    final VmOpenSourceLocationListener sourceLocationListener = new VmOpenSourceLocationListener(service);
    service.getVersion(new VersionConsumer() {
      @Override
      public void received(Version version) {
        sourceLocationListener.registerExtensionProvider(version);
      }

      @Override
      public void onError(RPCError error) {
        FlutterUtils.warn(LOG, error.getMessage());
      }
    });
    return sourceLocationListener;
  }

  private @NotNull final VmService service;
  private final EventDispatcher<Listener> dispatcher = EventDispatcher.create(Listener.class);

  private VmOpenSourceLocationListener(@NotNull VmService service) {
    this.service = service;

    service.addVmServiceListener(new VmServiceListenerAdapter() {
      @Override
      public void received(String streamId, Event event) {
        // TODO:

      }
    });
  }

  public void addListener(@NotNull Listener listener) {
    dispatcher.addListener(listener);
  }

  public void removeListener(@NotNull Listener listener) {
    dispatcher.removeListener(listener);
  }

  private void registerExtensionProvider(Version version) {
    // Register the openSourceLocation service.

    final String registerName;
    if (version.getMajor() <= 3 && version.getMinor() < 22) {
      registerName = "_registerService";
    }
    else {
      registerName = "registerService";
    }

    final JsonObject params = new JsonObject();
    params.addProperty("service", "openSourceLocation");
    params.addProperty("alias", "IntelliJ");
    service.callServiceExtension(registerName, params, new ServiceExtensionConsumer() {
      @Override
      public void received(JsonObject result) {
      }

      @Override
      public void onError(RPCError error) {
        FlutterUtils.warn(LOG, error.getMessage());
      }
    });
  }

  private void onMessage(@NotNull final JsonObject message) {
    final JsonElement id = message.get("id");
    final String isolateId;
    final String scriptId;
    final int tokenPos;
    try {
      if (id != null && !id.isJsonPrimitive()) {
        return;
      }

      final String jsonrpc = message.get("jsonrpc").getAsString();
      if (!"2.0".equals(jsonrpc)) {
        return;
      }

      final String method = message.get("method").getAsString();
      if (!"openSourceLocation".equals(method)) {
        return;
      }

      final JsonObject params = message.get("params").getAsJsonObject();
      if (params == null) {
        return;
      }

      isolateId = params.get("isolateId").getAsString();
      if (StringUtils.isEmpty(isolateId)) {
        return;
      }

      scriptId = params.get("scriptId").getAsString();
      if (StringUtils.isEmpty(scriptId)) {
        return;
      }

      tokenPos = params.get("tokenPos").getAsInt();

      dispatcher.getMulticaster().onRequest(isolateId, scriptId, tokenPos);
    }
    catch (Exception e) {
      FlutterUtils.warn(LOG, e);
    }

    if (id != null) {
      final JsonObject response = new JsonObject();
      response.addProperty("jsonrpc", "2.0");
      response.add("id", id);
      final JsonObject result = new JsonObject();
      result.addProperty("type", "Success");
      response.add("result", result);
      sender.sendMessage(response);
    }
  }
}
