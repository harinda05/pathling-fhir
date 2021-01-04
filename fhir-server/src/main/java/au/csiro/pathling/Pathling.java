/*
 * Copyright © 2018-2021, Commonwealth Scientific and Industrial Research
 * Organisation (CSIRO) ABN 41 687 119 230. Licensed under the CSIRO Open Source
 * Software Licence Agreement.
 */

package au.csiro.pathling;

import static au.csiro.pathling.utilities.Preconditions.checkNotNull;

import io.sentry.Sentry;
import java.io.PrintStream;
import javax.annotation.Nullable;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Main class that serves as the entry point for running the Pathling server.
 *
 * @author John Grimes
 */
@SpringBootApplication
@Import(Configuration.class)
@ServletComponentScan
public class Pathling {

  /**
   * @param args Arguments that will be passed to the main application.
   */
  public static void main(final String[] args) {

    // Get the Configuration bean so that we can initialise Sentry early.
    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(Configuration.class);
    context.refresh();
    final Configuration configuration = context.getBean(Configuration.class);

    // If the Sentry DSN is configured, initialise it.
    configuration.getSentryDsn().ifPresent(sentryDsn -> Sentry.init(options -> {
      options.setDsn(sentryDsn);
    }));

    new SpringApplicationBuilder(Pathling.class)
        .banner(new Banner())
        .run(args);
  }

  private static class Banner implements org.springframework.boot.Banner {

    @Override
    public void printBanner(@Nullable final Environment environment,
        @Nullable final Class<?> sourceClass, @Nullable final PrintStream out) {
      checkNotNull(out);
      out.println("    ____        __  __    ___              ");
      out.println("   / __ \\____ _/ /_/ /_  / (_)___  ____ _ ");
      out.println("  / /_/ / __ `/ __/ __ \\/ / / __ \\/ __ `/");
      out.println(" / ____/ /_/ / /_/ / / / / / / / / /_/ /   ");
      out.println("/_/    \\__,_/\\__/_/ /_/_/_/_/ /_/\\__, / ");
      out.println("                                /____/");
    }

  }
}
