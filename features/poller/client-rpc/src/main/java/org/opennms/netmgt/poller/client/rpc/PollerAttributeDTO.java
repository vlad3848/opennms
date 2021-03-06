/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.poller.client.rpc;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.netmgt.config.poller.PollerClassObjectAdapter;

@XmlRootElement(name = "attribute")
@XmlAccessorType(XmlAccessType.NONE)
public class PollerAttributeDTO {

    @XmlAttribute(name = "key")
    private String key;

    @XmlAttribute(name="value")
    private String value;

    @XmlAnyElement(lax=false)
    @XmlJavaTypeAdapter(PollerClassObjectAdapter.class)
    private Object contents;

    public PollerAttributeDTO() {
        // no-arg constructor for JAXB
    }

    public PollerAttributeDTO(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public PollerAttributeDTO(String key, Object contents) {
        this.key = key;
        if (contents != null && contents instanceof String) {
            this.value = (String)contents;
        } else {
            this.contents = contents;
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Object getContents() {
        return contents;
    }

    public void setContents(Object contents) {
        this.contents = contents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, contents);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PollerAttributeDTO other = (PollerAttributeDTO) obj;
        return Objects.equals(this.key, other.key)
                && Objects.equals(this.value, other.value)
                && Objects.equals(this.contents, other.contents);
    }

    @Override
    public String toString() {
        return String.format("PollerAttributeDTO[key='%s', value='%s', contents='%s']", key, value, contents);
    }
}
