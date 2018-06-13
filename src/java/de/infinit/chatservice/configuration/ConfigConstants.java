/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.configuration;

/**
 *
 * @author sebesjir
 */
public class ConfigConstants {
    // Configuration - conf protocol name
    public static final String CONF_PROTOCOL_NAME = "ChatServiceProtocol";

    // Configuration - alerts
    public static final String CFG_SEC_ALERTS = "alerts";
    public static final String CFG_KEY_ALERTS_CCT_CHAT_SERVICE_NOT_AVAILABLE = "cct_chat_service_not_available";    
    public static final String CFG_KEY_ALERTS_PRIMARY_GMS_NOT_AVAILABLE = "primary_gms_node_not_available";    
    public static final String CFG_KEY_ALERTS_PRIMARY_GMS_RECONNECT = "primary_gms_node_reconnect";    
    public static final String CFG_KEY_ALERTS_GMS_NODES_NOT_AVAILABLE = "gms_nodes_not_available";    
    public static final String CFG_KEY_ALERTS_GMS_NODES_RECONNECT = "gms_nodes_reconnect";
        
    // Configuration - alerts - defaults
    public static final Integer ALERTS_CCT_CHAT_SERVICE_NOT_AVAILABLE_DEFAULT = 9700;
    public static final Integer ALERTS_PRIMARY_GMS_NOT_AVAILABLE_DEFAULT = 9701;
    public static final Integer ALERTS_PRIMARY_GMS_RECONNECT_DEFAULT = 9702;
    public static final Integer ALERTS_GMS_NODES_NOT_AVAILABLE_DEFAULT = 9703;
    public static final Integer ALERTS_GMS_NODES_RECONNECT_DEFAULT = 9704;
    public static final Integer ALERTS_CME_NOT_AVAILABLE_DEFAULT = 9705;
    public static final Integer ALERTS_CME_RECONNECT_DEFAULT = 9706;

    // Configuration - gms
    public static final String CFG_SEC_GMS = "gms";
    public static final String CFG_KEY_PRIMARY_GMS = "primary_gms_node";
    public static final String CFG_KEY_GMS_USER = "user";
    public static final String CFG_KEY_GMS_PASSWORD = "password";
    public static final String CFG_KEY_GMS_CHAT_SERVICE = "gms_chat_service";
    
    // Configuration - common
    public static final String CFG_SEC_COMMON = "common";
    public static final String CFG_KEY_COMMON_ENABLED = "enabled";
    public static final String CFG_KEY_COMMON_CHAT_CHANNEL = "chat_channel";
    public static final String CFG_KEY_COMMON_SEND_BACK_CLIENT_MESSAGES = "send_back_client_messages";
    public static final String CFG_KEY_COMMON_IDLE_CHAT_CLEANUP_TIME = "idle_chat_cleanup_time";
    public static final String CFG_KEY_COMMON_IDLE_TIMEOUT_MIN = "idle_timeout_min";
    public static final String CFG_KEY_COMMON_LO_CHAT_CATEGORIES = "lo_chat_categories";
    public static final String CFG_KEY_COMMON_LO_SERVICE_AVAILABILITY = "lo_service_availability";
    public static final String CFG_KEY_COMMON_POSPONE_EVENT_MS = "postpone_event_ms";
    public static final String CFG_KEY_COMMON_MAX_CLIENT_MESSAGE_LENGTH = "max_client_message_length";
    public static final String CFG_KEY_COMMON_OPEN_CHAT_SYNC_TIMEOUT = "open_chat_sync_timeout";
    public static final String CFG_KEY_COMMON_PING_CLIENT_INTERVAL_MS = "ping_client_ms";
    public static final String CFG_KEY_COMMON_CHANNEL = "channel";
    public static final String CFG_KEY_COMMON_BRAND_ID = "brand_id";
    public static final String CFG_KEY_COMMON_EXTERNAL_NICKNAME = "external_nickname";
            
    
    // Configuration - common - defaults    
    public static final boolean COMMON_ENABLED_DEFAULT = true;
    public static final String COMMON_IDLE_CHAT_CLEANUP_TIME_DEFAULT = "00:00";
    public static final boolean COMMON_SEND_BACK_CLIENT_MESSAGES_DEFAULT = false;
    public static final Integer COMMON_IDLE_TIMEOUT_MIN_DEFAULT = 120;
    public static final String COMMON_LO_CHAT_CATEGORIES_DEFAULT = "LO_CHAT_CATEGORIES";
    public static final String COMMON_LO_SERVICE_AVAILABILITY_DEFAULT = "LO_CHAT_SERVICE_AVAILABILITY";
    public static final Integer COMMON_POSTPONE_MESSAGES_DEFAULT = 0;
    public static final int COMMON_MAX_CLIENT_MESSAGE_LENGTH_DEFAULT = 300;
    public static final int COMMON_OPEN_CHAT_SYNC_TIMEOUT_DEFAULT = 30;    
    public static final int COMMON_PING_CLIENT_INTERVAL_MS_DEFAULT = 0; // disabled
    public static final String COMMON_CHANNEL_DEFAULT = "app";
    public static final int COMMON_BRAND_ID_DEFAULT = 20;
    public static final String COMMON_EXTERNAL_NICKNAME_DEFAULT = "Portal Chatserver";
    
    // Configuration - database
    public static final String CFG_SEC_DATABASE = "database";
    public static final String CFG_KEY_DATABASE_CASSANDRA_TIMEOUT = "cassandra_timeout_s";
    public static final String CFG_KEY_DATABASE_TTL = "ttl";
    
    // Configuration - adapter
    public static final String CFG_SEC_ADAPTER = "adapter_endpoint";
    public static final String CFG_KEY_PRIMARY_ADAPTER = "primary_adapter";
    public static final String CFG_KEY_ADAPTER_TIMEOUT = "timeout";
    public static final String CFG_KEY_ADAPTER_URL = "url";
    public static final String CFG_KEY_ADAPTER_USERNAME = "username";
    public static final String CFG_KEY_ADAPTER_PASSWORD = "password";
    
    // Configuration - adapter - defaults
    public static final Integer ADAPTER_TIMEOUT_DEFAULT = 5000;
    
    //Configuration - chat service endpoint
    public static final String CFG_SEC_CHAT_SERVICE = "chat_service_endpoint";
    public static final String CFG_KEY_CHAT_SERVICE_USERNAME = "username";
    public static final String CFG_KEY_CHAT_SERVICE_PASSWORD = "password";
    
    // Configuration - statistics    
    public static final String CFG_SEC_STATISTICS = "statistics";
    public static final String CFG_KEY_STATISTICS_CONCURRENT_CHAT_THRESHOLDS = "concurrent_chat_threshold";
    public static final String CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_AGENTS = "group_of_all_chat_agents";
    public static final String CFG_KEY_STATISTICS_GROUP_OF_ALL_CHAT_QUEUES = "group_of_all_chat_queues";
    public static final String CFG_KEY_STATISTICS_TIMEOUT = "statistics_timeout_ms";
    public static final String CFG_KEY_STATISTICS_CURRENT_NUMBER_ACCEPTED_CHATS = "current_number_accepted_chats";
    public static final String CFG_KEY_STATISTICS_WAITING_CHATS = "waiting_chats";
    
    // Configuration - statistics - defaults
    public static final Integer STATISTICS_TIMEOUT_DEFAULT = 10000;
    
    // LO_CHAT_SERVICE_AVAILABILITY
    public static final String SERVICES_LO_KEY_AVAILBALE = "available";
    public static final String SERVICES_LO_KEY_BRAND_ID = "brandid";
    public static final String SERVICES_LO_KEY_CHANNEL = "channel";
    public static final String SERVICES_LO_KEY_SYSTEM_DOWN_MESSAGE = "system_down_message";    
    
    // LO_CHAT_SERVICE_AVAILABILITY - Defaults
    public static final boolean SERVICES_LO_KEY_AVAILBALE_DEFAULTS = true;
    
    // LO_CHAT_CATEGORIES
    public static final String CATEGORIES_LO_KEY_DISPLAY_NAME = "display_name";
    public static final String CATEGORIES_LO_KEY_ID = "id";
    public static final String CATEGORIES_LO_KEY_OPENING_TIMETABLE = "opening_timetable";
    public static final String CATEGORIES_LO_KEY_QUEUING_MESSAGE = "queuing_message";
    public static final String CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE = "enable_queuing_message";
    public static final String CATEGORIES_LO_KEY_ROUTING_SERVICE = "routing_service";
    
    // LO_CHAT_CATEGORIES - Defaults
     public static final boolean CATEGORIES_LO_KEY_ENABLE_QUEUING_MESSAGE_DEFAULTS = true;
}
