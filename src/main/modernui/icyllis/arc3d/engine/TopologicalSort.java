/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import java.util.Collection;
import java.util.List;

/**
 * Provides a depth-first-search topological sort algorithm for simple directed acyclic graphs.
 */
public final class TopologicalSort {

    /**
     * Topologically sort the nodes in 'graph'. For this sort, when node 'i' depends
     * on node 'j' it means node 'j' must appear in the result before node 'i'.
     * <p>
     * The graph to sort must be directed, must not allow self loops, and must not contain
     * cycles. Otherwise, {@link IllegalStateException} will be thrown and the contents of
     * 'graph' will be in some arbitrary state.
     *
     * @param graph  the directed acyclic graph to sort
     * @param access the node access
     * @param <T>    the node type of the graph
     * @throws IllegalStateException if the graph contains loops
     */
    public static <T> void topologicalSort(List<T> graph, Access<T> access) {
        assert checkAllUnmarked(graph, access);

        int index = 0;

        // Start a DFS from each node in the graph.
        for (T node : graph) {
            // Output this node after all the nodes it depends on have been output.
            index = dfsVisit(node, access, index);
        }

        assert index == graph.size();

        // Reorder the array given the output order.
        for (int i = 0, e = graph.size(); i < e; i++) {
            for (int j = access.getIndex(graph.get(i)); j != i; ) {
                T temp = graph.set(j, graph.get(i));
                graph.set(i, temp);
                j = access.getIndex(temp);
            }
        }

        assert cleanExit(graph, access);
    }

    /**
     * Recursively visit a node and all the other nodes it depends on.
     */
    private static <T> int dfsVisit(final T node, Access<T> access, int index) {
        if (access.getIndex(node) != -1) {
            // If the node under consideration has been already been output it means it
            // (and all the nodes it depends on) are already in 'result'.
            return index;
        }
        if (access.isTempMarked(node)) {
            // There was a loop
            throw new IllegalStateException("cyclic dependency: " + node);
        }
        final Collection<T> edges = access.getIncomingEdges(node);
        if (edges != null && !edges.isEmpty()) {
            // Temporarily mark the node
            access.setTempMarked(node, true);
            // Recursively dfs all the node's edges
            for (T edge : edges) {
                index = dfsVisit(edge, access, index);
            }
            // Unmark the node from the temporary list
            access.setTempMarked(node, false);
        }
        // Mark this node as output
        access.setIndex(node, index);
        return index + 1;
    }

    private static <T> boolean checkAllUnmarked(List<T> graph, Access<T> access) {
        for (final T node : graph) {
            assert access.getIndex(node) == -1;
            assert !access.isTempMarked(node);
        }
        return true;
    }

    private static <T> boolean cleanExit(List<T> graph, Access<T> access) {
        for (int i = 0, e = graph.size(); i < e; i++) {
            final T node = graph.get(i);
            assert access.getIndex(node) == i;
            assert !access.isTempMarked(node);
        }
        return true;
    }

    /**
     * The node access. This removes the overhead of creating new data structures and searching for elements.
     *
     * @param <T> the node type of the graph
     */
    public interface Access<T> {

        /**
         * The sort has already seen and added the node to the result. The index may be retrieved
         * by {@link #getIndex(Object)}.
         *
         * @param node  a node of the graph
         * @param index the index of the node in the result
         */
        void setIndex(T node, int index);

        /**
         * Retrieves the index previously stored into the node by {@link #setIndex(Object, int)}.
         * This method <em>must</em> return -1 initially, and return the actual index once
         * {@link #setIndex(Object, int)} is called during the sort.
         *
         * @param node a node of the graph
         * @return the index of the node in the result
         */
        int getIndex(T node);

        /**
         * Sets a transient state to indicate that it is visited during sorting, used to check
         * cyclic dependencies. The state may be retrieved by {@link #isTempMarked(Object)}.
         *
         * @param node   a node of the graph
         * @param marked whether the node is temporarily marked or not
         */
        void setTempMarked(T node, boolean marked);

        /**
         * Retrieves the state previously set the node by {@link #setTempMarked(Object, boolean)}.
         *
         * @param node a node of the graph
         * @return whether the node is temporarily marked or not
         */
        boolean isTempMarked(T node);

        /**
         * Returns any incoming edges (dependencies) from the given node.
         *
         * @param node a node of the graph
         * @return a list containing any incoming edges, or null if there are none
         */
        Collection<T> getIncomingEdges(T node);
    }
}
