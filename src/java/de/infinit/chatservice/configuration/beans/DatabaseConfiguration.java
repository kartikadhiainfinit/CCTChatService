package de.infinit.chatservice.configuration.beans;

public class DatabaseConfiguration {
    private int ttl;
    private int cassandra_timeout_s;
    private boolean useLocalCache = true;

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
    
    public int getCassandraTimeout() {
        return cassandra_timeout_s;
    }

    public void setCassandraTimeout(int cassandra_timeout_s) {
        this.cassandra_timeout_s = cassandra_timeout_s;
    }

    public boolean getUseLocalCache() {
        return useLocalCache;
    }

    public void setUseLocalCache(boolean useLocalCache) {
        this.useLocalCache = useLocalCache;
    }

    @Override
    public String toString() {
        return "DatabaseConfiguration [ttl=" + ttl + ", use_local_cache=" + useLocalCache + "]";
    }

}
