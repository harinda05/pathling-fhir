/*
 * Copyright © 2018-2022, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling.async;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Provides dependencies for asynchronous processing.
 *
 * @author John Grimes
 */
@Configuration
@EnableAsync
@Profile("server")
@ConditionalOnProperty(prefix = "pathling", name = "async.enabled", havingValue = "true")
public class AsyncConfigurer implements org.springframework.scheduling.annotation.AsyncConfigurer {

  @Override
  public ThreadPoolTaskExecutor getAsyncExecutor() {
    return new ThreadPoolTaskExecutor();
  }

}
