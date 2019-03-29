/*
 * Copyright 2011 Park Jun-Hong (parkjunhong77/gmail/com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *
 * This file is generated under this project, "open-commons-redis".
 *
 * Date  : 2017. 10. 17. 오후 5:42:30
 *
 * Author: Park_Jun_Hong_(fafanmama_at_naver_com)
 * 
 */

package open.commons.redis;

import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import open.commons.Result;
import open.commons.concurrent.Mutex;
import open.commons.redis.section.RedisSection;
import open.commons.redis.utils.RedisUtils;
import open.commons.utils.IOUtils;
import open.commons.utils.ThreadUtils;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisCommandTimeoutException;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * 
 * @since 2017. 10. 17.
 * @author Park_Jun_Hong_(fafanmama_at_naver_com)
 */
public class RedisDao<T> implements AutoCloseable {
    public static final String REDIS_ROLE_MASTER = "master";
    public static final String REDIS_ROLE_SLAVE = "slave";

    protected final Logger logger = LogManager.getLogger(getClass());

    protected final RedisURI redisUri;
    protected final String password;

    protected final int database;

    protected RedisURI connectedUri;
    private RedisClient redisClient;
    @SuppressWarnings("rawtypes")
    private StatefulRedisConnection redisConnection;
    private Mutex mutexConnection = new Mutex("mutex for 'Redis Connection'");

    /** 현재 연결된 Redis Server가 slave 인지 여부 */
    private boolean isSlave;

    /** 재접속 시도 기본값 */
    private int masterAofEnabledRetryCount = 3;
    /** 재접속 시도 간격 */
    private long masterAofEnabledRetryInterval = 5000L;
    /** 재접속 시도 기본값 */
    private int slaveUpdateRetryCount = 3;
    /** 재접속 시도 간격 */
    private long slaveUpdateRetryInterval = 5000L;

    /**
     * 
     * @param redisUri
     * @param database
     * @since 2016. 6. 21.
     */
    public RedisDao(RedisURI redisUri, int database) {
        this(redisUri, null, database);
    }

    /**
     * 
     * @param redisUri
     * @param password
     * @param database
     * @since 2017. 10. 17.
     */
    public RedisDao(RedisURI redisUri, String password, int database) {
        this.redisUri = redisUri;
        this.password = password;
        this.database = database;
    }

    /**
     * 
     * @param targetRedisRole
     *            연결하고자 하는 Redis Server Role
     * @param codec
     * @return
     *
     * @since 2016. 8. 1.
     */
    @SuppressWarnings({ "rawtypes" })
    private Result<Boolean> checkConnection(String targetRedisRole, RedisCodec<String, T> codec) {

        Result<Boolean> result = new Result<>();

        RedisClient client = null;
        StatefulRedisConnection con = null;
        RedisSection replication = null;
        String curRedisRole = null;

        Function<String, Integer> aofEnabled = str -> {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        };

        RedisURI.Builder builder = RedisURI.Builder.redis(redisUri.getHost(), redisUri.getPort());

        if (password != null && !password.trim().isEmpty()) {
            builder.withPassword(password);
        }

        RedisURI uri = builder.withDatabase(database)//
                .build();

        int retryCount = 0;

        try {
            client = RedisUtils.create(uri);
            con = client.connect(codec);

            // 'replication' 정보 조회
            replication = new RedisSection(con.sync().info("replication"));

            curRedisRole = replication.get("role", "unknown");

            switch (targetRedisRole.toLowerCase()) {
                case REDIS_ROLE_MASTER:
                    if (REDIS_ROLE_MASTER.equalsIgnoreCase(curRedisRole)) {

                        int aofEnable = 0;

                        retryCount = 0;

                        RedisSection persistence = new RedisSection(con.sync().info("Persistence"));

                        boolean disabled = false;
                        do {
                            // 상태 확인
                            aofEnable = aofEnabled.apply(persistence.get("aof_enabled"));
                            if (aofEnable > 0) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Redis Server (master) 연결 정보가 생성되었습니다. URI: " + uri + ", URI-Database: " + uri.getDatabase() + ", CONF-Database: " + database);
                                }

                                this.redisClient = client;
                                this.redisConnection = con;
                                this.connectedUri = uri;

                                result.andTrue().setData(true);
                                result.setMessage("Redis Server (master) 연결 정보가 생성되었습니다. URI: " + uri + ", URI-Database: " + uri.getDatabase() + ", CONF-Database: " + database);

                                return result;

                            } else {

                                if (!disabled) {
                                    result.setMessage(
                                            "Redis Server (master) 구동이 완료되지 않았습니다. URI: " + uri + ", URI-Database: " + uri.getDatabase() + ", CONF-Database: " + database);

                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Redis Server (master) 구동이 완료되지 않았습니다. URI: " + uri + ", URI-Database: " + uri.getDatabase() + ", CONF-Database: " + database);
                                    }

                                    disabled = true;
                                }

                                ThreadUtils.sleep(masterAofEnabledRetryInterval);
                            }

                            // 갱신된 'master' 정보 조회
                            persistence = new RedisSection(con.sync().info("Persistence"));

                        } while (retryCount++ < masterAofEnabledRetryCount);

                        closeAndShutdown(client, con, uri);

                        this.connectedUri = null;
                    } else {
                        closeAndShutdown(client, con, uri);
                    }

                    result.setMessage("Redis 'slave' 서버에 연결하지 못하였습니다.");

                    break;
                case REDIS_ROLE_SLAVE:
                    if (REDIS_ROLE_SLAVE.equalsIgnoreCase(curRedisRole)) {

                        boolean disabled = false;
                        do {
                            // 상태 확인
                            if ("up".equalsIgnoreCase(replication.get("master_link_status"))) {

                                if (logger.isDebugEnabled()) {
                                    logger.debug("Redis Server (slave) 연결 정보가 생성되었습니다. URI: " + uri + ", URI-Database: " + uri.getDatabase() + ", CONF-Database: " + database);
                                }

                                this.redisConnection = con;
                                this.redisClient = client;
                                this.connectedUri = uri;

                                result.andTrue().setData(true);
                                result.setMessage("Redis Server (slave) 연결 정보가 생성되었습니다. URI: " + uri + ", URI-Database: " + uri.getDatabase() + ", CONF-Database: " + database);

                                return result;
                            } else {

                                if (!disabled) {
                                    result.setMessage("Redis Server (slave) 구동이 완료되지 않았습니다. URI: " + uri + ", URI-Database: " + uri.getDatabase() + ", CONF-Database: " + database);

                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Redis Server (slave) 구동이 완료되지 않았습니다. URI: " + uri + ", URI-Database: " + uri.getDatabase() + ", CONF-Database: " + database);
                                    }

                                    disabled = true;
                                }

                                ThreadUtils.sleep(slaveUpdateRetryInterval);
                            }

                            // 갱신된 'slave' 정보 조회
                            replication = new RedisSection(con.sync().info("replication"));

                        } while (retryCount++ < slaveUpdateRetryCount);

                        closeAndShutdown(client, con, uri);

                        this.connectedUri = null;
                    } else {
                        closeAndShutdown(client, con, uri);
                    }

                    break;
                default:
                    closeAndShutdown(client, con, uri);
                    break;
            }
        } catch (RedisConnectionException | RedisCommandTimeoutException e) {
            if (e.getLocalizedMessage().contains("Unable to connect to /" + uri.getHost() + ":" + uri.getPort())) {
                logger.warn("Redis Server가 구동 중이지 않습니다. URI: " + uri);

                result.setMessage("Redis Server가 구동 중이지 않습니다. URI: " + uri);
            } else {
                logger.warn("Redis Server 연결 생성시 오류가 발생하였습니다. URI: " + uri, e);

                result.setMessage("Redis Server 연결 생성시 오류가 발생하였습니다. URI: " + uri);
            }

            closeAndShutdown(client, con, uri);
        }

        return result;
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() {
        synchronized (mutexConnection) {
            closeAndShutdown(this.redisClient, this.redisConnection, null);
        }
    }

    @SuppressWarnings("rawtypes")
    protected final void closeAndShutdown(RedisClient client, StatefulRedisConnection con, RedisURI redisUri) {

        if (con != null && con.isOpen()) {

            if (logger.isInfoEnabled()) {
                logger.info("Redis Server 연결 정보를 해제합니다. client: " + client + ", connection: " + con
                        + (redisUri != null ? ", URI: " + redisUri + ", URI-Database: " + redisUri.getDatabase() + ", CONF-Database: " + database : ""));
            }

            IOUtils.close(con);
        }

        if (client != null) {
            try {
                client.shutdown();
            } catch (Throwable e) {
            }
        }

    }

    /**
     *
     * @return the connectedUri
     *
     * @since 2016. 8. 1.
     */
    public final RedisURI getConnectedUri() {
        return connectedUri;
    }

    /**
     * Redis 서버와의 Connection을 제공한다.
     * 
     * @param redisRole
     *            'master' | 'slave'
     * @param codec
     * 
     * @return
     *
     * @since 2016. 7. 1.
     */
    @SuppressWarnings({ "unchecked" })
    protected Result<StatefulRedisConnection<String, T>> getConnection(String redisRole, RedisCodec<String, T> codec) {

        Result<StatefulRedisConnection<String, T>> result = new Result<>();
        Result<Boolean> resultConnection = null;

        synchronized (mutexConnection) {
            if (isOpen()) {
                result.setData(this.redisConnection);
                return result;
            }

            switch (redisRole.toLowerCase()) {
                case REDIS_ROLE_SLAVE:
                    if ((resultConnection = checkConnection(REDIS_ROLE_SLAVE, codec)).getResult()) {
                        isSlave = true;

                        result.andTrue().setData(this.redisConnection);
                        return result;
                    }
                case REDIS_ROLE_MASTER:
                    if ((resultConnection = checkConnection(REDIS_ROLE_MASTER, codec)).getResult()) {
                        isSlave = false;

                        result.andTrue().setData(this.redisConnection);
                        return result;
                    }
                    break;
                default:
                    break;
            }

        }

        isSlave = false;
        result.setMessage(resultConnection.getMessage());

        return result;
    }

    /**
     *
     * @return the database
     *
     * @since 2016. 7. 15.
     */
    public int getDatabase() {
        return database;
    }

    /**
     *
     * @return the masterAofEnabledRetryCount
     *
     * @since 2017. 10. 17.
     */
    public int getMasterAofEnabledRetryCount() {
        return masterAofEnabledRetryCount;
    }

    /**
     *
     * @return the masterAofEnabledRetryInterval
     *
     * @since 2017. 10. 17.
     */
    public long getMasterAofEnabledRetryInterval() {
        return masterAofEnabledRetryInterval;
    }

    /**
     *
     * @return the slaveUpdateRetryCount
     *
     * @since 2017. 10. 17.
     */
    public int getSlaveUpdateRetryCount() {
        return slaveUpdateRetryCount;
    }

    /**
     *
     * @return the slaveUpdateRetryInterval
     *
     * @since 2017. 10. 17.
     */
    public long getSlaveUpdateRetryInterval() {
        return slaveUpdateRetryInterval;
    }

    /**
     * Redis 연결이 유효한지 여부를 반환한다.
     * 
     * @return
     *
     * @since 2016. 8. 11.
     */
    public boolean isOpen() {
        synchronized (mutexConnection) {
            if (redisConnection != null && redisConnection.isOpen()) {
                return true;
            } else {
                redisClient = null;
                redisConnection = null;
                return false;
            }
        }
    }

    /**
     *
     * @return the isSlave
     *
     * @since 2016. 8. 1.
     */
    public final boolean isSlave() {
        return isSlave;
    }

    /**
     * 현재 연결된 Redis Server가 master인 경우 연결을 해제한다.
     * 
     *
     * @since 2016. 8. 1.
     */
    public final void releaseIfNotSlaveConnection() {
        if (!isSlave) {
            closeAndShutdown(this.redisClient, this.redisConnection, this.connectedUri);
        }
    }

    /**
     * @param masterAofEnabledRetryCount
     *            the masterAofEnabledRetryCount to set
     *
     * @since 2017. 10. 17.
     */
    public void setMasterAofEnabledRetryCount(int masterAofEnabledRetryCount) {
        this.masterAofEnabledRetryCount = masterAofEnabledRetryCount;
    }

    /**
     * @param masterAofEnabledRetryInterval
     *            the masterAofEnabledRetryInterval to set, <b><code>millisecond</code></b>
     *
     * @since 2017. 10. 17.
     */
    public void setMasterAofEnabledRetryInterval(long masterAofEnabledRetryInterval) {
        this.masterAofEnabledRetryInterval = masterAofEnabledRetryInterval;
    }

    /**
     * @param slaveUpdateRetryCount
     *            the slaveUpdateRetryCount to set
     *
     * @since 2017. 10. 17.
     */
    public void setSlaveUpdateRetryCount(int slaveUpdateRetryCount) {
        this.slaveUpdateRetryCount = slaveUpdateRetryCount;
    }

    /**
     * @param slaveUpdateRetryInterval
     *            the slaveUpdateRetryInterval to set, <b><code>millisecond</code></b>
     *
     * @since 2017. 10. 17.
     */
    public void setSlaveUpdateRetryInterval(long slaveUpdateRetryInterval) {
        this.slaveUpdateRetryInterval = slaveUpdateRetryInterval;
    }
}
