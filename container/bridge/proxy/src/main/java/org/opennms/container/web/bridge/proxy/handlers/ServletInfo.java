/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019-2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

package org.opennms.container.web.bridge.proxy.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.container.web.bridge.proxy.pattern.PatternMatcher;
import org.opennms.container.web.bridge.proxy.pattern.PatternMatcherFactory;
import org.osgi.framework.ServiceReference;

// Info object for servlets
public class ServletInfo {
    private String alias;
    private String name;
    private final List<String> patterns;
    private final List<PatternMatcher> patternMatchers;

    public ServletInfo(ServiceReference reference) {
        this.name = getStringProperty(reference, "osgi.http.whiteboard.servlet.name");
        this.patterns = getListProperty(reference, "osgi.http.whiteboard.servlet.pattern");
        this.alias = getStringProperty(reference, "alias");
        this.patternMatchers = determinePatternMatcher(this.patterns);
    }

    private static List<PatternMatcher> determinePatternMatcher(List<String> patterns) {
        return patterns.stream().map(pattern -> createPatternMatcher(pattern)).collect(Collectors.toList());
    }

    private static PatternMatcher createPatternMatcher(String pattern) {
        return PatternMatcherFactory.createPatternMatcher(pattern);
    }

    public boolean canHandle(String path) {
        final Optional<PatternMatcher> any = patternMatchers.stream().filter(pm -> pm.matches(path)).findAny();
        return any.isPresent();
    }

    private List<String> getListProperty(ServiceReference reference, String key) {
        final List<String> returnList = new ArrayList<>();
        final Object property = reference.getProperty(key);
        if (property instanceof String) {
            final String value = ((String) property).trim();
            if (value != null && !"".equals(property)) {
                returnList.add(value);
            }
        }
        return returnList;
    }

    private static String getStringProperty(ServiceReference reference, String key) {
        final Object property = reference.getProperty(key);
        if (property instanceof String) {
            return ((String) property).trim();
        }
        return null;
    }

    public boolean isValid() {
        return !patterns.isEmpty() && !patternMatchers.isEmpty();
    }

    public boolean hasAlias() {
        return alias != null;
    }

    public String getAlias() {
        return alias;
    }

    public String getName() {
        return name;
    }

    public List<String> getPatterns() {
        return patterns;
    }
}
