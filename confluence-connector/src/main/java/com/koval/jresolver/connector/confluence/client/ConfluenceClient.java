package com.koval.jresolver.connector.confluence.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.confluence.api.model.Expansion;
import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.api.model.content.ContentType;
import com.atlassian.confluence.api.model.content.Space;
import com.atlassian.confluence.api.model.pagination.PageRequest;
import com.atlassian.confluence.api.model.pagination.PageResponse;
import com.atlassian.confluence.api.model.pagination.SimplePageRequest;
import com.atlassian.confluence.rest.client.*;
import com.atlassian.confluence.rest.client.authentication.AuthenticatedWebResourceProvider;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.koval.jresolver.connector.confluence.configuration.ConfluenceConnectorProperties;
import com.sun.jersey.api.client.Client;


public class ConfluenceClient implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceClient.class);

  private final ListeningExecutorService executor;
  private final AuthenticatedWebResourceProvider provider;

  public ConfluenceClient(ConfluenceConnectorProperties connectorProperties) {
    Client client = RestClientFactory.newClient();
    executor = MoreExecutors.newDirectExecutorService();
    provider = new AuthenticatedWebResourceProvider(client, connectorProperties.getConfluenceBaseUrl(), "");
    if (!connectorProperties.isAnonymous()) {
      provider.setAuthContext(connectorProperties.getUsername(), connectorProperties.getPassword().toCharArray());
    }
    LOGGER.info("Confluence client created for {}", connectorProperties.getConfluenceBaseUrl());
  }

  public List<Space> getSpacesByKeys(List<String> spaceKeys) {
    RemoteSpaceService spaceService = new RemoteSpaceServiceImpl(provider, executor);
    PageRequest pageRequest = new SimplePageRequest(0, 1000);
    LOGGER.info("Get spaces by keys: {} with page request: {}", spaceKeys, pageRequest);
    return spaceService
        .find()
        .withKeys(spaceKeys.toArray(new String[spaceKeys.size()]))
        .fetchMany(pageRequest)
        .claim()
        .getResults();
  }

  public PageResponse<Content> getPagesBySpaceKeys(List<Space> spaces, int startIndex, int limit) {
    RemoteContentService contentService = new RemoteContentServiceImpl(provider, executor);
    PageRequest pageRequest = new SimplePageRequest(startIndex, limit);
    LOGGER.info("Get pages by spaces: {} with page request: {}",
        spaces.stream().map(Space::getKey).collect(Collectors.toList()), pageRequest);
    return contentService
        .find(new Expansion("body.storage"))
        .withSpace(spaces.toArray(new Space[spaces.size()]))
        .fetchMany(ContentType.PAGE, pageRequest)
        .claim();
  }

  @Override
  public void close() throws IOException {
    provider.close();
    LOGGER.info("Confluence client has been closed");
  }
}
