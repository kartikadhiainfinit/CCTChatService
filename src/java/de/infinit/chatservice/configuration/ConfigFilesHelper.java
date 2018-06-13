package de.infinit.chatservice.configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.genesyslab.platform.configuration.protocol.types.CfgAppType;
import de.infinit.configuration.beans.CMEConnectionParameters;

public class ConfigFilesHelper {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ConfigFilesHelper.class);

    /**
     * Read Genesys config. server configuration from properties file
     *
     * @param applicationPath
     * @param isDebug
     * @return - Genesys config. server configuration
     */
    public static CMEConnectionParameters loadConfigServerConfiguration(final String applicationPath, final boolean isDebug) {
        if (log.isInfoEnabled()) {
            log.info("loadConfigServerConfiguration() entered...");
        }

        String configPath = applicationPath;
        if (!isDebug) {
            configPath = getConfigPath(applicationPath);
        }

        if (configPath == null) {
            log.error("Not able to get path of config file.");
            return null;
        }

        if (log.isInfoEnabled()) {
            log.info("Read application configuration from properties file: " + configPath);
        }

        CMEConnectionParameters cmeSettings = null;
        // read CME properties
        Properties config = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configPath);
            config.load(inputStream);
            cmeSettings = new CMEConnectionParameters();
            cmeSettings.setHost(config.getProperty("host"));
            cmeSettings.setPort(config.getProperty("port"));
            cmeSettings.setHostBackup(config.getProperty("hostBackup"));
            cmeSettings.setPortBackup(config.getProperty("portBackup"));
//			cmeSettings.setApplicationType(CfgAppType.CFGThirdPartyServer);
            cmeSettings.setApplicationType(CfgAppType.CFGThirdPartyServer);
            cmeSettings.setProtocolName(ConfigConstants.CONF_PROTOCOL_NAME);
            cmeSettings.setApplicationName(config.getProperty("applicationName"));
        } catch (FileNotFoundException ex) {
            log.error("Not able to find config file.", ex);
        } catch (IOException ex) {
            log.error("Exception during reading of configuration file.", ex);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    log.info("Error during closing config file input stream.", ex);
                }
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Leaving loadConfiguration with configuration:" + cmeSettings);
        }

        return cmeSettings;
    }

    /**
     * Get GMSService properties configuration file path
     *
     * @return path
     */
    private static String getConfigPath(final String applicationPath) {
        if (log.isDebugEnabled()) {
            log.debug("getConfigPath() entered...");
        }
        String configPath;
        try {
            configPath = applicationPath;
            if (log.isDebugEnabled()) {
                log.debug("Project Path:" + configPath);
            }

            if (configPath.contains("SourceCode")) {
                configPath = configPath.substring(0, configPath.lastIndexOf("SourceCode"));
                configPath = configPath.replaceAll("%20", " ");
                configPath = configPath.concat("Configuration/cct-chat-service.properties");
            } else if (configPath.contains("wtpwebapps")) {
                configPath = configPath.substring(0, configPath.lastIndexOf("wtpwebapps"));
                configPath = configPath.replaceAll("%20", " ");
                configPath = configPath.concat("conf/cct-chat-service.properties");
            } else {
                configPath = configPath.substring(0, configPath.lastIndexOf("webapps"));
                configPath = configPath.replaceAll("%20", " ");
                configPath = configPath.concat("conf/cct-chat-service.properties");
            }

            if (log.isDebugEnabled()) {
                log.debug("Config Path:" + configPath);
            }
        } catch (Exception e) {
            log.error("Error while creating config path:" + e);
            return null;
        }
        if (log.isInfoEnabled()) {
            log.info("Use configuration file: " + configPath);
        }

        return configPath;
    }
}
