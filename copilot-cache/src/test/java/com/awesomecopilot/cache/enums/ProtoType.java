package com.awesomecopilot.cache.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Copyright: (C), 2021-06-24 11:14
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
public enum ProtoType {

    HTTP("http"),

    HTTP_DATA("http-data"),

    TLS("tls"),

    SSH("ssh"),

    DNS("dns"),

    ICMP("icmp"),

    DHCP("dhcp"),

    FTP("ftp"),

    FTP_DATA("ftp-data"),

    SMTP("smtp"),

    SMTP_DATA("smtp-data"),

    IMAP("imap"),

    IMAP_DATA("imap-data"),

    POP3("pop3"),

    POP3_DATA("pop3-data"),

    SMB("smb"),

    SMB_DATA("smb-data"),

    NFS("nfs"),

    NFS_DATA("nfs-data"),

    TELNET("telnet"),

    MODBUS("modbus"),

    RDP("rdp"),

    LDAP("ldap"),

    /**
     * 这个只有网络日志有, 事件是没有的
     */
    FLOW_TCP("tcp(flow)"),

    FLOW_ICMP("icmp(flow)"),

    FLOW_UDP("udp(flow)"),


    ORACLE("oracle"),

    SQL_SERVER("sqlserver"),

    SYBASE("sybase"),

    DB2("db2"),

    TERADATA("teradata"),

    MYSQL("mysql"),

    POSTGRE("postgre"),

    DAMENG("dameng"),

    KINGBASE("kingbase"),

    CACHEDB("cachedb"),

    OSCAR("oscar"),

    HBASE("hbase"),

    MONGODB("mongodb"),

    HIVE("hive"),

    REDIS("redis"),

    HANA("hana"),

    MEMORY_CACHE("memorycache");

    @JsonValue
    private String key;

    private ProtoType(String key) {
        this.key = key;
    }

    public boolean isEqualTo(String protoType) {
        return this.key.equalsIgnoreCase(protoType);
    }

    public String getKey() {
        return key;
    }


    /**
     * 获取所有protoType
     *
     * @return
     */
    public static List<String> getAllProtoTypes() {
        List<String> protoTypes = new ArrayList<>();
        for (ProtoType value : ProtoType.values()) {
            protoTypes.add(value.getKey());
        }
        return protoTypes;
    }

    public static Map<String, String> handlerEventEnumForNetLog() {
        Map<String, String> map = new HashMap<>();
        for (ProtoType value : ProtoType.values()) {
            map.put(value.getKey(), value.getKey().toLowerCase());
        }
        return map;
    }
}
