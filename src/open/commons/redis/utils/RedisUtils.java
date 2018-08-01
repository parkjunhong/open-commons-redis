/*
 * Copyright 2011 Park Jun-Hong_(fafanmama_at_naver_com)
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
 * Date  : 2017. 10. 17. 오후 5:41:32
 *
 * Author: Park_Jun_Hong_(fafanmama_at_naver_com)
 * 
 */

package open.commons.redis.utils;

import static com.google.common.base.Preconditions.checkArgument;

import open.commons.redis.section.SimpleRedisConnectionStateListener;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.resource.ClientResources;

/**
 * 
 * @since 2017. 10. 17.
 * @author Park_Jun_Hong_(fafanmama_at_naver_com)
 */
public class RedisUtils {

    private RedisUtils() {
    }

    private static void assertNotNull(RedisURI redisURI) {
        checkArgument(redisURI != null, "RedisURI must not be null");
    }

    /**
     * Create a new client that connects to the supplied {@link RedisURI uri} with default {@link ClientResources}. You
     * can connect to different Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param redisURI
     *            the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClient}
     */
    public static RedisClient create(RedisURI redisURI) {
        assertNotNull(redisURI);

        RedisClient client = RedisClient.create(redisURI);
        client.addListener(new SimpleRedisConnectionStateListener(client, redisURI, Thread.currentThread().getName()));
        return client;
    }

    /**
     * Create a new client that connects to the supplied uri with default {@link ClientResources}. You can connect to
     * different Redis servers but you must supply a {@link RedisURI} on connecting.
     *
     * @param uri
     *            the Redis URI, must not be {@literal null}
     * @return a new instance of {@link RedisClient}
     */
    public static RedisClient create(String uri) {
        checkArgument(uri != null, "uri must not be null");
        return create(RedisURI.create(uri));
    }
}
