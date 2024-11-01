/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.markdown;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitHandler;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.text.*;
import icyllis.modernui.util.DataSet;

import java.util.Map;

/**
 * Configurable node visitor handler which does not know anything about node subclasses
 * while allowing easy configuration of custom visitor for nodes of interest to visit.
 */
public final class MarkdownVisitor implements NodeVisitHandler {

    final MarkdownConfig mConfig;

    final Map<Class<? extends Node>, NodeVisitor<Node>> mVisitors;

    private final SpannableStringBuilder mBuilder = new SpannableStringBuilder();

    private final DataSet mRenderArguments = new DataSet();

    @Nullable
    private final BlockHandler mBlockHandler;

    MarkdownVisitor(MarkdownConfig config) {
        mConfig = config;
        mVisitors = config.mVisitors;
        mBlockHandler = config.mBlockHandler;
    }

    @Override
    public void visit(@NonNull Node node) {
        NodeVisitor<Node> visitor = mVisitors.get(node.getClass());
        if (visitor != null) {
            visitor.visit(this, node);
        } else {
            visitChildren(node);
        }
    }

    @Override
    public void visitNodeOnly(@NonNull Node node) {
        NodeVisitor<Node> visitor = mVisitors.get(node.getClass());
        if (visitor != null) {
            visitor.visit(this, node);
        }
    }

    @Override
    public void visitChildren(@NonNull Node parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            // A subclass of this visitor might modify the node, resulting in getNext returning a different node or no
            // node after visiting it. So get the next node before visiting.
            Node next = child.getNext();
            visit(child);
            child = next;
        }
    }

    public SpannableStringBuilder builder() {
        return mBuilder;
    }

    public DataSet getRenderArguments() {
        return mRenderArguments;
    }

    public Editable append(char c) {
        return mBuilder.append(c);
    }

    public Editable append(CharSequence text) {
        return mBuilder.append(text);
    }

    public int length() {
        return mBuilder.length();
    }

    /**
     * Executes a check if there is further content available.
     *
     * @param node to check
     * @return boolean indicating if there are more nodes after supplied one
     */
    public boolean hasNext(@NonNull Node node) {
        return node.getNext() != null;
    }

    public void ensureNewLine() {
        int len = mBuilder.length();
        if (len > 0 && mBuilder.charAt(len - 1) != '\n') {
            mBuilder.append('\n');
        }
    }

    public void forceNewLine() {
        mBuilder.append('\n');
    }

    public void blockStart(@NonNull Node node) {
        if (mBlockHandler != null) {
            mBlockHandler.blockStart(this, node);
        } else {
            ensureNewLine();
        }
    }

    public void blockEnd(@NonNull Node node) {
        if (mBlockHandler != null) {
            mBlockHandler.blockEnd(this, node);
        } else {
            if (hasNext(node)) {
                ensureNewLine();
                forceNewLine();
            }
        }
    }

    @Nullable
    public <N extends Node> Object preSetSpans(@NonNull N node, int offset) {
        SpanFactory<Node> factory = mConfig.getSpanFactory(node.getClass());
        if (factory != null) {
            Object spans = factory.create(mConfig, node, mRenderArguments);
            setSpans0(spans, offset, offset, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            return spans;
        }
        return null;
    }

    public void postSetSpans(@Nullable Object spans, int offset) {
        setSpans0(spans, offset, mBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void setSpans0(Object spans, int start, int end, int flags) {
        if (spans != null) {
            if (spans.getClass().isArray()) {
                for (Object span : (Object[]) spans) {
                    setSpans0(span, start, end, flags);
                }
            } else {
                mBuilder.setSpan(spans, start, end, flags);
            }
        }
    }
}
