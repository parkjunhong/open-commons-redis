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
 * Date  : 2017. 10. 17. 오후 5:36:50
 *
 * Author: Park_Jun_Hong_(fafanmama_at_naver_com)
 * 
 */

package open.commons.redis.section;

import java.util.Map.Entry;

import open.commons.collection.FIFOMap;
import open.commons.utils.StringUtils;

/**
 * <pre>
 * 'Replication' section string
 * 
 * <b>Case of 'master'</b>
 * # Replication
 * role:master
 * connected_slaves:2
 * slave0:ip=10.100.0.33,port=6380,state=online,offset=10473919468,lag=0
 * slave1:ip=10.100.0.34,port=6382,state=online,offset=10473919332,lag=1
 * master_repl_offset:10473919468
 * repl_backlog_active:1
 * repl_backlog_size:1048576
 * repl_backlog_first_byte_offset:10472870893
 * repl_backlog_histlen:1048576
 * 
 * <b>Case of 'slave'</b>
 * # Replication
 * role:slave
 * master_host:10.100.0.33
 * master_port:6379
 * master_link_status:down
 * master_last_io_seconds_ago:-1
 * master_sync_in_progress:0
 * slave_repl_offset:10474754990
 * master_link_down_since_seconds:4
 * slave_priority:100
 * slave_read_only:1
 * connected_slaves:0
 * master_repl_offset:0
 * repl_backlog_active:0
 * repl_backlog_size:1048576
 * repl_backlog_first_byte_offset:0
 * repl_backlog_histlen:0
 * </pre>
 * 
 * @since 2017. 10. 17.
 * @author Park_Jun_Hong_(fafanmama_at_naver_com)
 */
public class RedisSection {

    private String name;

    private FIFOMap<String, String> properties = new FIFOMap<>();

    /**
     * 
     * @since 2017. 10. 17.
     */
    public RedisSection() {
    }

    public RedisSection(String sectionString) {
        load(sectionString);
    }

    public String get(String name) {
        return this.properties.get(name);
    }

    public String get(String name, String defaultValue) {
        return this.properties.getOrDefault(name, defaultValue);
    }

    /**
     * 
     * @param name
     * @return <b><code>nullable</code></b>
     *
     * @since 2017. 10. 17.
     */
    public Boolean getAsBoolean(String name) {
        return this.properties.containsKey(name) ? Boolean.parseBoolean(this.properties.get(name)) : null;
    }

    public Boolean getAsBoolean(String name, Boolean defaultValue) {
        return this.properties.containsKey(name) ? Boolean.parseBoolean(this.properties.get(name)) : defaultValue;
    }

    /**
     * 
     * @param name
     * @return <b><code>nullable</code></b>
     *
     * @since 2017. 10. 17.
     */
    public Byte getAsByte(String name) {
        return this.properties.containsKey(name) ? Byte.parseByte(this.properties.get(name)) : null;
    }

    public Byte getAsByte(String name, Byte defaultValue) {
        return this.properties.containsKey(name) ? Byte.parseByte(this.properties.get(name)) : defaultValue;
    }

    /**
     * 
     * @param name
     * @return <b><code>nullable</code></b>
     *
     * @since 2017. 10. 17.
     */
    public Double getAsDouble(String name) {
        return this.properties.containsKey(name) ? Double.parseDouble(this.properties.get(name)) : null;
    }

    public Double getAsDouble(String name, Double defaultValue) {
        return this.properties.containsKey(name) ? Double.parseDouble(this.properties.get(name)) : defaultValue;
    }

    /**
     * 
     * @param name
     * @return <b><code>nullable</code></b>
     *
     * @since 2017. 10. 17.
     */
    public Integer getAsInteger(String name) {
        return this.properties.containsKey(name) ? Integer.parseInt(this.properties.get(name)) : null;
    }

    public Integer getAsInteger(String name, Integer defaultValue) {
        return this.properties.containsKey(name) ? Integer.parseInt(this.properties.get(name)) : defaultValue;
    }

    /**
     * 
     * @param name
     * @return <b><code>nullable</code></b>
     *
     * @since 2017. 10. 17.
     */
    public Long getAsLong(String name) {
        return this.properties.containsKey(name) ? Long.parseLong(this.properties.get(name)) : null;
    }

    public Long getAsLong(String name, Long defaultValue) {
        return this.properties.containsKey(name) ? Long.parseLong(this.properties.get(name)) : defaultValue;
    }

    /**
     *
     * @return the name
     *
     * @since 2017. 10. 17.
     */
    public String getName() {
        return name;
    }

    public void load(String sectionString) {

        String[] sections = StringUtils.split(sectionString, "\n", true);

        int ci = 0;
        for (String section : sections) {
            if (section.charAt(0) == '#') {
                this.name = section.substring(1);
            } else {
                ci = section.indexOf(':');
                properties.put(section.substring(0, ci), section.substring(ci + 1));
            }
        }

        if (this.name == null) {
            throw new IllegalArgumentException("There is no SECTION Name. input: " + sectionString);
        }
    }

    /**
     * @param name
     *            the name to set
     *
     * @since 2017. 10. 17.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (this.name == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        buf.append('#');
        buf.append(' ');
        buf.append(name);

        for (Entry<String, String> entry : this.properties.entrySet()) {
            if (buf.length() > 0) {
                buf.append('\n');
            }

            buf.append(entry.getKey());
            buf.append(':');
            buf.append(entry.getValue());
        }

        return buf.toString();
    }
}