/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.features.topology.api.topo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.opennms.features.topology.api.browsers.ContentType;
import org.opennms.features.topology.api.browsers.SelectionChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTopologyProvider implements GraphProvider {

    /**
     * This class generates an unique id.
     * The generated id has the format '<prefix><counter>' (e.g. v100).
     * So the generator must be initialized with a prefix and the initial counter.
     *
     * @author Markus von Rüden
     *
     */
    protected abstract static class IdGenerator {
        /**
         * The topology provider. It is needed to initialize the counter.
         */
        private final AbstractTopologyProvider provider;
        /**
         * The prefix of the generated id. Must not be null.
         */
        private final String idPrefix;
        /**
         * The counter of the "next" (not current) id.
         */
        private int counter;
        /**
         * Defines if this generator is initialized or not.
         */
        private boolean initialized;

        protected IdGenerator(String idPrefix, AbstractTopologyProvider provider) {
            this.idPrefix = idPrefix;
            this.provider = provider;
        }

        /**
         * Returns the next id in format '<prefix><counter>' (e.g. v100).
         *
         * If an entry with the generated id (see {@link #createId()}
         * already exists in {@link #provider} a new one is created.
         * This process is done until a key is created which is not already in {@link #provider}
         * @return The next id in format '<prefix><counter>' (e.g. v100).
         */
        public String getNextId() {
            try {
                initializeIfNeeded();
                while (!isValid(createId())) counter++;
                return createId();
            } finally {
                counter++;
            }
        }

        /**
         * Creates the id in format '<prefix><counter>' (e.g. v100)
         * @return the id in format '<prefix><counter>' (e.g. v100)
         */
        private String createId() {
            return idPrefix + counter;
        }

        /**
         * Returns the initial value of counter.
         *
         * Therefore the maximum number of each id from the {@link #getContent()} values are used.
         * A id can start with any prefix (or none) so only ids which starts with the same id as {@link #idPrefix} are considered.
         * If there is no matching content, 0 is returned.
         *
         * @return The initial value of counter.
         */
        private int getInitValue() {
            int max = 0;
            for (Ref ref : getContent()) {
                if (!ref.getId().startsWith(idPrefix)) continue;
                max = Math.max(max, extractIntegerFromId(ref.getId()));
            }
            return max;
        }

        /**
         * Returns true if the {@link #provider} does not contain a vertex id '<generatedId>', false otherwise.
         * @param generatedId The generated id
         * @return true if the {@link #provider} does not contain a vertex id '<generatedId>', false otherwise.
         */
        @SuppressWarnings("deprecation")
        private boolean isValid(String generatedId) {
            return !provider.containsVertexId(new DefaultVertexRef(provider.getNamespace(), generatedId));
        }

        public void reset() {
            counter = 0;
            initialized = false;
        }

        /**
         * Gets the integer value from the id.
         * If the id does not match to this generator or the id cannot be parsed as an integer 0 is returned.
         *
         * @param id the generated id. Should start with {@link #idPrefix}.
         * @return the integer value from the id. If the id does not match to this generator or the id cannot be parsed as an integer 0 is returned.
         */
        private int extractIntegerFromId(String id) {
            try {
                return Integer.parseInt(id.replaceAll(idPrefix, "").trim());
            } catch (NumberFormatException nfe) {
                return 0;
            } catch (IllegalArgumentException ilargex) {
                return 0;
            }
        }

        private void initializeIfNeeded() {
            if (!initialized) {
                counter = getInitValue();
                initialized = true;
            }
        }

        public abstract List<Ref> getContent();
    }

    protected static final String SIMPLE_VERTEX_ID_PREFIX = "v";
	protected static final String SIMPLE_EDGE_ID_PREFIX = "e";
    protected TopologyProviderInfo topologyProviderInfo = new DefaultTopologyProviderInfo();

	private static final Logger LOG = LoggerFactory.getLogger(AbstractTopologyProvider.class);


	private IdGenerator edgeIdGenerator = new IdGenerator(SIMPLE_EDGE_ID_PREFIX, this) {
        @Override
        public List<Ref> getContent() {
            return new ArrayList<Ref>(getEdges());
        }
	};
	
	private IdGenerator vertexIdGenerator = new IdGenerator(SIMPLE_VERTEX_ID_PREFIX, this) {
	    @Override
	    public List<Ref> getContent() {
	        return new ArrayList<>(getVertices());
        }
	};

    protected SimpleVertexProvider m_vertexProvider;
    protected SimpleEdgeProvider m_edgeProvider;

    protected AbstractTopologyProvider(String namespace) {
        this(new SimpleVertexProvider(namespace), new SimpleEdgeProvider(namespace));
	}

    protected AbstractTopologyProvider(SimpleVertexProvider vertexProvider, SimpleEdgeProvider edgeProvider) {
        m_vertexProvider = vertexProvider;
        m_edgeProvider = edgeProvider;
        if (!m_edgeProvider.getNamespace().equals(edgeProvider.getNamespace())) {
            throw new IllegalStateException("Namespace of edge and vertex provider must match");
        }
    }

    public String getNextVertexId() {
        return vertexIdGenerator.getNextId();
    }

    protected String getNextEdgeId() {
        return edgeIdGenerator.getNextId();
    }

    // TODO MVR ..
    public List<Vertex> getVerticesWithoutCollapsibleVertices() {
        return getVertices().stream()
                .filter(input -> input != null && !(input instanceof CollapsibleVertex))
                .collect(Collectors.toList());
    }

    @Override
    public final void removeVertex(VertexRef... vertexId) {
        for (VertexRef vertex : vertexId) {
            if (vertex == null) continue;
            
            getSimpleVertexProvider().remove(vertexId);
            
            removeEdges(getEdgeIdsForVertex(vertex));
        }
    }

    @Override
    public final void addVertices(Vertex... vertices) {
        getSimpleVertexProvider().add(vertices);
    }

    @Override
    public final void addEdges(Edge... edges) {
        getSimpleEdgeProvider().add(edges);
    }

    @Override
    public final void removeEdges(EdgeRef... edge) {
        getSimpleEdgeProvider().remove(edge);
    }

    @Override
    public final EdgeRef[] getEdgeIdsForVertex(VertexRef vertex) {
        if (vertex == null) return new EdgeRef[0];
        List<EdgeRef> retval = new ArrayList<EdgeRef>();
        for (Edge edge : getEdges()) {
            // If the vertex is connected to the edge then add it
            if (new RefComparator().compare(edge.getSource().getVertex(), vertex) == 0 || new RefComparator().compare(edge.getTarget().getVertex(), vertex) == 0) {
                retval.add(edge);
            }
        }
        return retval.toArray(new EdgeRef[0]);
    }

    @Override
    public final Map<VertexRef, Set<EdgeRef>> getEdgeIdsForVertices(VertexRef... vertices) {
        List<Edge> edges = getEdges();
        Map<VertexRef,Set<EdgeRef>> retval = new HashMap<VertexRef,Set<EdgeRef>>();
        for (VertexRef vertex : vertices) {
            if (vertex == null) continue;
            Set<EdgeRef> edgeSet = new HashSet<EdgeRef>();
            for (Edge edge : edges) {
                // If the vertex is connected to the edge then add it
                if (new RefComparator().compare(edge.getSource().getVertex(), vertex) == 0 || new RefComparator().compare(edge.getTarget().getVertex(), vertex) == 0) {
                    edgeSet.add(edge);
                }
            }
            retval.put(vertex, edgeSet);
        }
        return retval;
    }

    @Override
	public Edge connectVertices(VertexRef sourceVertextId, VertexRef targetVertextId) {
        String nextEdgeId = getNextEdgeId();
        return connectVertices(nextEdgeId, sourceVertextId, targetVertextId, getNamespace());
    }

    protected final AbstractEdge connectVertices(String id, VertexRef sourceId, VertexRef targetId, String namespace) {
        if (sourceId == null) {
            if (targetId == null) {
                LOG.warn("Source and target vertices are null");
                return null;
            } else {
                LOG.warn("Source vertex is null");
                return null;
            }
        } else if (targetId == null) {
            LOG.warn("Target vertex is null");
            return null;
        }
        SimpleConnector source = new SimpleConnector(sourceId.getNamespace(), sourceId.getId()+"-"+id+"-connector", sourceId);
        SimpleConnector target = new SimpleConnector(targetId.getNamespace(), targetId.getId()+"-"+id+"-connector", targetId);

        AbstractEdge edge = new AbstractEdge(namespace, id, source, target);

        addEdges(edge);
        
        return edge;
    }

    protected final SimpleVertexProvider getSimpleVertexProvider() {
        return m_vertexProvider;
    }

    protected final SimpleEdgeProvider getSimpleEdgeProvider() {
        return m_edgeProvider;
    }

    @Override
    public final void addVertexListener(VertexListener vertexListener) {
        m_vertexProvider.addVertexListener(vertexListener);
    }

    @Override
    public final void clearVertices() {
        m_vertexProvider.clearVertices();
    }

    @Override
    public int getVertexTotalCount() {
        return m_vertexProvider.getVertexTotalCount();
    }

    @Override
    public int getEdgeTotalCount() {
        return m_edgeProvider.getEdgeTotalCount();
    }

    @Override
    public final boolean contributesTo(String namespace) {
        return m_vertexProvider.contributesTo(namespace);
    }

    @Override
    public boolean containsVertexId(String id) {
        return m_vertexProvider.containsVertexId(id);
    }

    @Override
    public boolean containsVertexId(VertexRef id, Criteria... criteria) {
        return m_vertexProvider.containsVertexId(id, criteria);
    }

    @Override
    public final String getNamespace() {
        return m_vertexProvider.getNamespace();
    }

    @Override
    public final int getSemanticZoomLevel(VertexRef vertex) {
        return m_vertexProvider.getSemanticZoomLevel(vertex);
    }

    @Override
    public final Vertex getVertex(String namespace, String id) {
        return m_vertexProvider.getVertex(namespace, id);
    }

    @Override
    public final Vertex getVertex(VertexRef reference, Criteria... criteria) {
        return m_vertexProvider.getVertex(reference, criteria);
    }

    @Override
    public List<Vertex> getVertices(CollapsibleRef collapsibleRef, Criteria... criteria) {
        return m_vertexProvider.getVertices(collapsibleRef, criteria);
    }

    @Override
    public final List<Vertex> getVertices(Criteria... criteria) {
        return m_vertexProvider.getVertices(criteria);
    }

    @Override
    public final List<Vertex> getVertices(Collection<? extends VertexRef> references, Criteria... criteria) {
        return m_vertexProvider.getVertices(references, criteria);
    }

    @Override
    public final void removeVertexListener(VertexListener vertexListener) {
        m_vertexProvider.removeVertexListener(vertexListener);
    }

    @Override
    public final void addEdgeListener(EdgeListener listener) {
        m_edgeProvider.addEdgeListener(listener);
    }

    @Override
    public final void clearEdges() {
        m_edgeProvider.clearEdges();
    }

    @Override
    public final Edge getEdge(String namespace, String id) {
        return m_edgeProvider.getEdge(namespace, id);
    }

    @Override
    public final Edge getEdge(EdgeRef reference) {
        return m_edgeProvider.getEdge(reference);
    }

    @Override
    public final List<Edge> getEdges(Criteria... criteria) {
        return m_edgeProvider.getEdges(criteria);
    }

    @Override
    public final List<Edge> getEdges(Collection<? extends EdgeRef> references) {
        return m_edgeProvider.getEdges(references);
    }

    @Override
    public final void removeEdgeListener(EdgeListener listener) {
        m_edgeProvider.removeEdgeListener(listener);
    }

    @Override
    public void resetContainer() {
        clearVertices();
        clearEdges();
        clearCounters();
    }

    protected void clearCounters() {
        vertexIdGenerator.reset();
        edgeIdGenerator.reset();
    }

    @Override
    public abstract void refresh();

    public TopologyProviderInfo getTopologyProviderInfo() {
        return topologyProviderInfo;
    }

    public void setTopologyProviderInfo(TopologyProviderInfo topologyProviderInfo) {
        this.topologyProviderInfo = topologyProviderInfo;
    }

    protected static SelectionChangedListener.Selection getSelection(String namespace, List<VertexRef> selectedVertices, ContentType type) {
        final Set<Integer> nodeIds = selectedVertices.stream()
                .filter(v -> namespace.equals(v.getNamespace()))
                .filter(v -> v instanceof AbstractVertex)
                .map(v -> (AbstractVertex) v)
                .map(v -> v.getNodeID())
                .filter(nodeId -> nodeId != null)
                .collect(Collectors.toSet());
        if (type == ContentType.Alarm) {
            return new SelectionChangedListener.AlarmNodeIdSelection(nodeIds);
        }
        if (type == ContentType.Node) {
            return new SelectionChangedListener.IdSelection<>(nodeIds);
        }
        return SelectionChangedListener.Selection.NONE;
    }
}
