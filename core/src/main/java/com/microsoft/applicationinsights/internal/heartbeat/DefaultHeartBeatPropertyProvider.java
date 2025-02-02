package com.microsoft.applicationinsights.internal.heartbeat;

import com.microsoft.applicationinsights.internal.util.PropertyHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1>Base Heartbeat Property Provider</h1>
 *
 * <p>
 *   This class is a concrete implementation of {@link HeartBeatPayloadProviderInterface}
 *   It enables setting SDK Metadata to heartbeat payload.
 * </p>
 *
 * @author Dhaval Doshi
 */
public class DefaultHeartBeatPropertyProvider implements HeartBeatPayloadProviderInterface {

  private static final Logger logger = LoggerFactory.getLogger(DefaultHeartBeatPropertyProvider.class);

  /**
   * Collection holding default properties for this default provider.
   */
  private final Set<String> defaultFields;

  /**
   * Random GUID that would help in analysis when app has stopped and restarted. Each restart will
   * have a new GUID. If the application is unstable and goes through frequent restarts this will help
   * us identify instability in the analytics backend.
   */
  private static UUID uniqueProcessId;

  /**
   * Name of this provider.
   */
  private static final String name = "Default";

  private static final String JRE_VERSION = "jreVersion";

  private static final String SDK_VERSION = "sdkVersion";

  private static final String OS_VERSION = "osVersion";

  private static final String PROCESS_SESSION_ID = "processSessionId";

  public DefaultHeartBeatPropertyProvider() {
    defaultFields = new HashSet<>();
    initializeDefaultFields(defaultFields);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Callable<Boolean> setDefaultPayload(final List<String> disableFields,
      final HeartBeatProviderInterface provider) {
    return new Callable<Boolean>() {

      final Set<String> enabledProperties = MiscUtils.except(defaultFields, disableFields);
      @Override
      public Boolean call() {
        boolean hasSetValues = false;
        for (String fieldName : enabledProperties) {
          try {
            switch (fieldName) {
              case JRE_VERSION:
                provider.addHeartBeatProperty(fieldName, getJreVersion(), true);
                hasSetValues = true;
                break;
              case SDK_VERSION:
                provider.addHeartBeatProperty(fieldName, getSdkVersion(), true);
                hasSetValues = true;
                break;
              case OS_VERSION:
                provider.addHeartBeatProperty(fieldName, getOsVersion(), true);
                hasSetValues = true;
                break;
              case PROCESS_SESSION_ID:
                provider.addHeartBeatProperty(fieldName, getProcessSessionId(), true);
                hasSetValues = true;
                break;
              default:
                //We won't accept unknown properties in default providers.
                logger.trace("Encountered unknown default property");
                break;
            }
          }
          catch (Exception e) {
            if (logger.isWarnEnabled()) {
              logger.warn("Failed to obtain heartbeat property", e);
            }
          }
        }
        return hasSetValues;
      }
    };
  }

  /**
   * This method initializes the collection with Default Properties of this provider.
   * @param defaultFields collection to hold default properties.
   */
  private void initializeDefaultFields(Set<String> defaultFields) {
    defaultFields.add(JRE_VERSION);
    defaultFields.add(SDK_VERSION);
    defaultFields.add(OS_VERSION);
    defaultFields.add(PROCESS_SESSION_ID);
  }

  /**
   * Gets the JDK version being used by the application
   * @return String representing JDK Version
   */
  private String getJreVersion() {
    return System.getProperty("java.version");
  }

  /**
   * Returns the Application Insights SDK version user is using to instrument his application
   * @return returns string prefixed with "java" representing the Application Insights Java
   * SDK version.
   */
  private String getSdkVersion() {
    return PropertyHelper.getQualifiedSdkVersionString();
  }

  /**
   * Gets the OS version on which application is running.
   * @return String representing OS version
   */
  private String getOsVersion() {
    return System.getProperty("os.name");
  }

  /**
   * Returns the Unique GUID for the application's current session.
   * @return String representing GUID for each running session
   */
  private String getProcessSessionId() {
    if (uniqueProcessId == null) {
      uniqueProcessId = UUID.randomUUID();
    }
    return uniqueProcessId.toString();
  }
}
