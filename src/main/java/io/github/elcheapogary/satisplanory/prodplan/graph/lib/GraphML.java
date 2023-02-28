/*
 * Copyright (c) 2023 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory.prodplan.graph.lib;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class GraphML
{
    private GraphML()
    {
    }

    private static <N, E> void export(
            XMLStreamWriter w,
            Graph<N, E> graph,
            Function<? super N, String> nodeDescriptor,
            Function<? super E, String> edgeDescriptor,
            Function<? super N, ? extends Map<String, String>> nodeDataExtractor,
            Function<? super E, ? extends Map<String, String>> edgeDataExtractor
    )
            throws XMLStreamException
    {
        w.writeStartDocument();
        w.writeStartElement("graphml");
        w.writeDefaultNamespace("http://graphml.graphdrawing.org/xmlns");
        w.writeStartElement("graph");
        w.writeAttribute("id", "G");
        w.writeAttribute("edgedefault", "directed");

        List<Node<N, E>> nodes = new ArrayList<>(graph.getNodes());

        for (int i = 0; i < nodes.size(); i++){
            Node<N, E> node = nodes.get(i);
            w.writeStartElement("node");
            w.writeAttribute("id", "n" + i);
            if (nodeDescriptor != null){
                String description = nodeDescriptor.apply(node.getData());

                if (description != null){
                    w.writeStartElement("desc");
                    w.writeCharacters(description);
                    w.writeEndElement();
                }
            }
            if (nodeDataExtractor != null){
                for (var dataEntry : nodeDataExtractor.apply(node.getData()).entrySet()){
                    w.writeStartElement("data");
                    w.writeAttribute("key", dataEntry.getKey());
                    w.writeCharacters(dataEntry.getValue());
                    w.writeEndElement();
                }
            }
            w.writeEndElement();
        }

        for (int i = 0; i < nodes.size(); i++){
            Node<N, E> sourceNode = nodes.get(i);
            for (var entry : sourceNode.getOutgoingEdges().entrySet()){
                Node<N, E> targetNode = entry.getKey();
                Edge<N, E> edge = entry.getValue();

                w.writeStartElement("edge");
                int targetIndex = nodes.indexOf(targetNode);
                w.writeAttribute("id", "edge-n" + i + "-n" + targetIndex);
                w.writeAttribute("source", "n" + i);
                w.writeAttribute("target", "n" + targetIndex);


                if (edgeDescriptor != null){
                    String description = edgeDescriptor.apply(edge.getData());

                    if (description != null){
                        w.writeStartElement("desc");
                        w.writeCharacters(description);
                        w.writeEndElement();
                    }
                }
                if (edgeDataExtractor != null){
                    for (var dataEntry : edgeDataExtractor.apply(edge.getData()).entrySet()){
                        w.writeStartElement("data");
                        w.writeAttribute("key", dataEntry.getKey());
                        w.writeCharacters(dataEntry.getValue());
                        w.writeEndElement();
                    }
                }

                w.writeEndElement();
            }
        }

        w.writeEndElement(); // graph
        w.writeEndElement(); // graphml
        w.writeEndDocument();
    }

    public static <N, E> void export(
            Writer writer,
            Graph<N, E> graph,
            Function<? super N, String> nodeDescriptor,
            Function<? super E, String> edgeDescriptor,
            Function<? super N, ? extends Map<String, String>> nodeDataExtractor,
            Function<? super E, ? extends Map<String, String>> edgeDataExtractor
    )
            throws IOException
    {
        try {
            XMLStreamWriter w = new FormattedXmlStreamWriter(XMLOutputFactory.newDefaultFactory().createXMLStreamWriter(writer));
            try {
                export(w, graph, nodeDescriptor, edgeDescriptor, nodeDataExtractor, edgeDataExtractor);
            }finally{
                w.close();
            }
        }catch (XMLStreamException e){
            throw new IOException(e);
        }
    }

    public static <N, E> void export(
            OutputStream out,
            Graph<N, E> graph,
            Function<? super N, String> nodeDescriptor,
            Function<? super E, String> edgeDescriptor,
            Function<? super N, ? extends Map<String, String>> nodeDataExtractor,
            Function<? super E, ? extends Map<String, String>> edgeDataExtractor
    )
            throws IOException
    {
        try {
            XMLStreamWriter w = new FormattedXmlStreamWriter(XMLOutputFactory.newDefaultFactory().createXMLStreamWriter(out));
            try {
                export(w, graph, nodeDescriptor, edgeDescriptor, nodeDataExtractor, edgeDataExtractor);
            }finally{
                w.close();
            }
        }catch (XMLStreamException e){
            throw new IOException(e);
        }
    }

    private static class FormattedXmlStreamWriter
            implements XMLStreamWriter
    {
        private static final String EOL = System.getProperty("line.separator");
        private static final String INDENT = "    ";
        private final XMLStreamWriter w;
        private boolean needsFormatting = false;
        private int indentLevel = 0;

        public FormattedXmlStreamWriter(XMLStreamWriter w)
        {
            this.w = w;
        }

        @Override
        public void close()
                throws XMLStreamException
        {
            w.close();
        }

        private void doIndent()
                throws XMLStreamException
        {
            if (needsFormatting){
                w.writeCharacters(EOL);
                for (int i = 0; i < indentLevel; i++){
                    w.writeCharacters(INDENT);
                }
                needsFormatting = false;
            }
        }

        @Override
        public void flush()
                throws XMLStreamException
        {
            w.flush();
        }

        @Override
        public NamespaceContext getNamespaceContext()
        {
            return w.getNamespaceContext();
        }

        @Override
        public void setNamespaceContext(NamespaceContext context)
                throws XMLStreamException
        {
            w.setNamespaceContext(context);
        }

        @Override
        public String getPrefix(String uri)
                throws XMLStreamException
        {
            return w.getPrefix(uri);
        }

        @Override
        public Object getProperty(String name)
                throws IllegalArgumentException
        {
            return w.getProperty(name);
        }

        @Override
        public void setDefaultNamespace(String uri)
                throws XMLStreamException
        {
            w.setDefaultNamespace(uri);
        }

        @Override
        public void setPrefix(String prefix, String uri)
                throws XMLStreamException
        {
            w.setPrefix(prefix, uri);
        }

        @Override
        public void writeAttribute(String localName, String value)
                throws XMLStreamException
        {
            w.writeAttribute(localName, value);
        }

        @Override
        public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
                throws XMLStreamException
        {
            w.writeAttribute(prefix, namespaceURI, localName, value);
        }

        @Override
        public void writeAttribute(String namespaceURI, String localName, String value)
                throws XMLStreamException
        {
            w.writeAttribute(namespaceURI, localName, value);
        }

        @Override
        public void writeCData(String data)
                throws XMLStreamException
        {
            w.writeCData(data);
        }

        @Override
        public void writeCharacters(String text)
                throws XMLStreamException
        {
            w.writeCharacters(text);
            needsFormatting = false;
        }

        @Override
        public void writeCharacters(char[] text, int start, int len)
                throws XMLStreamException
        {
            w.writeCharacters(text, start, len);
            needsFormatting = false;
        }

        @Override
        public void writeComment(String data)
                throws XMLStreamException
        {
            w.writeComment(data);
        }

        @Override
        public void writeDTD(String dtd)
                throws XMLStreamException
        {
            w.writeDTD(dtd);
        }

        @Override
        public void writeDefaultNamespace(String namespaceURI)
                throws XMLStreamException
        {
            w.writeDefaultNamespace(namespaceURI);
        }

        @Override
        public void writeEmptyElement(String namespaceURI, String localName)
                throws XMLStreamException
        {
            doIndent();
            w.writeEmptyElement(namespaceURI, localName);
            needsFormatting = true;
        }

        @Override
        public void writeEmptyElement(String prefix, String localName, String namespaceURI)
                throws XMLStreamException
        {
            doIndent();
            w.writeEmptyElement(prefix, localName, namespaceURI);
            needsFormatting = true;
        }

        @Override
        public void writeEmptyElement(String localName)
                throws XMLStreamException
        {
            doIndent();
            w.writeEmptyElement(localName);
            needsFormatting = true;
        }

        @Override
        public void writeEndDocument()
                throws XMLStreamException
        {
            doIndent();
            w.writeEndDocument();
        }

        @Override
        public void writeEndElement()
                throws XMLStreamException
        {
            indentLevel--;
            doIndent();
            w.writeEndElement();
            needsFormatting = true;
        }

        @Override
        public void writeEntityRef(String name)
                throws XMLStreamException
        {
            w.writeEntityRef(name);
            needsFormatting = false;
        }

        @Override
        public void writeNamespace(String prefix, String namespaceURI)
                throws XMLStreamException
        {
            w.writeNamespace(prefix, namespaceURI);
        }

        @Override
        public void writeProcessingInstruction(String target)
                throws XMLStreamException
        {
            w.writeProcessingInstruction(target);
        }

        @Override
        public void writeProcessingInstruction(String target, String data)
                throws XMLStreamException
        {
            w.writeProcessingInstruction(target, data);
        }

        @Override
        public void writeStartDocument()
                throws XMLStreamException
        {
            w.writeStartDocument();
            needsFormatting = true;
        }

        @Override
        public void writeStartDocument(String version)
                throws XMLStreamException
        {
            w.writeStartDocument(version);
            needsFormatting = true;
        }

        @Override
        public void writeStartDocument(String encoding, String version)
                throws XMLStreamException
        {
            w.writeStartDocument(encoding, version);
            needsFormatting = true;
        }

        @Override
        public void writeStartElement(String localName)
                throws XMLStreamException
        {
            doIndent();
            w.writeStartElement(localName);
            needsFormatting = true;
            indentLevel++;
        }

        @Override
        public void writeStartElement(String namespaceURI, String localName)
                throws XMLStreamException
        {
            doIndent();
            w.writeStartElement(namespaceURI, localName);
            needsFormatting = true;
            indentLevel++;
        }

        @Override
        public void writeStartElement(String prefix, String localName, String namespaceURI)
                throws XMLStreamException
        {
            doIndent();
            w.writeStartElement(prefix, localName, namespaceURI);
            needsFormatting = true;
            indentLevel++;
        }
    }
}
