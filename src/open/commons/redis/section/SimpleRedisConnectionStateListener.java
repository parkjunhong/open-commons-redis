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
 * Date  : 2017. 10. 17. 오후 5:39:06
 *
 * Author: Park_Jun_Hong_(fafanmama_at_naver_com)
 * 
 */

package open.commons.redis.section;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import open.commons.utils.IOUtils;
import open.commons.utils.ThreadUtils;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnectionStateListener;
import com.lambdaworks.redis.RedisURI;

/**
 * 
 * @since 2017. 10. 17.
 * @author Park_Jun_Hong_(fafanmama_at_naver_com)
 */
public class SimpleRedisConnectionStateListener implements RedisConnectionStateListener {

    private Logger logger = LogManager.getLogger(getClass());

    private RedisClient client;

    private RedisURI uri;

    private String callerThread;

    /**
     * 
     * @since 2017. 10. 17.
     */
    public SimpleRedisConnectionStateListener() {
    }

    /**
     * 
     * @param client
     * @param uri
     * @param callerThread
     * @since 2017. 10. 17.
     */
    public SimpleRedisConnectionStateListener(RedisClient client, RedisURI uri, String callerThread) {
        this.client = client;
        this.uri = uri;

        this.callerThread = callerThread;
    }

    /**
     * @see com.lambdaworks.redis.RedisConnectionStateListener#onRedisConnected(com.lambdaworks.redis.RedisChannelHandler)
     */
    @Override
    public void onRedisConnected(RedisChannelHandler<?, ?> connection) {

        String otn = ThreadUtils.setThreadName(callerThread);

        if (logger.isDebugEnabled()) {
            logger.debug("[CONNECTED] " + uri);
        }

        ThreadUtils.setThreadName(otn);
    }

    /**
     * @see com.lambdaworks.redis.RedisConnectionStateListener#onRedisDisconnected(com.lambdaworks.redis.RedisChannelHandler)
     */
    @Override
    public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {

        String otn = ThreadUtils.setThreadName(callerThread);

        if (logger.isDebugEnabled()) {
            logger.debug("[DISCONNECTED] " + uri);

            logger.debug("RedisConnection will be closed...");
            logger.debug("RedisClient will be shutdowned...");
        }

        if (connection.isOpen()) {
            IOUtils.close(connection);
        }

        try {
            client.shutdown();
        } catch (Exception ignored) {
            logger.warn(ignored.getLocalizedMessage());
        }

        ThreadUtils.setThreadName(otn);

    }

    /**
     * @see com.lambdaworks.redis.RedisConnectionStateListener#onRedisExceptionCaught(com.lambdaworks.redis.RedisChannelHandler,
     *      java.lang.Throwable)
     */
    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {

        String otn = ThreadUtils.setThreadName(callerThread);

        logger.warn("[EXCEPTIONS] connection: " + connection, cause);
        logger.warn("RedisConnection will be closed...");
        logger.warn("RedisClient will be shutdowned...");

        if (connection.isOpen()) {
            IOUtils.close(connection);
        }

        try {
            client.shutdown();
        } catch (Exception ignored) {
            logger.warn(ignored.getLocalizedMessage());
        }

        ThreadUtils.setThreadName(otn);

    }

}
