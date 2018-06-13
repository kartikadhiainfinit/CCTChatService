/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.db;

/**
 *
 * @author sebesjir
 */
public class DBConstants {

    public static final String GMS_CUSTOM_ID = "CCTChatService";
    public static final String GMS_USER_HEADER = "gms_user";

    public static final String COLUMN_SESSION_ID = "SESSIONID";
    public static final String COLUMN_USER_ID = "USERID";
    public static final String COLUMN_SECURE_KEY = "SECUREKEY";
    public static final String COLUMN_NEXT_POSITION = "NEXT_POSITION";
    public static final String COLUMN_LAST_ACCESS = "LASTACCESS";
    public static final String COLUMN_STATUS = "STATUS";
    public static final String COLUMN_DISCONNECT_REASON = "DISCONNECT_REASON";
    public static final String COLUMN_CHATSERVICE_INSTANCES = "CSINSTANCES";
    
    public static final String CHAT_SERVICE_SESSION_LIST = "CHAT_SERVICE_SESSION_LIST";
    public static final int CHAT_SERVICE_CACHE_TIMEOUT = 86400;
    public static final int EVENT_CACHE_TIMEOUT = 300;
    public static final String EVENT_CACHE_PROCESSED = "PROCESSED";
}
