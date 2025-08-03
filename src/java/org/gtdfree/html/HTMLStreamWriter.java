/*
 *    Copyright (C) 2008-2010 Igor Kriznar
 *    
 *    This file is part of GTD-Free.
 *    
 *    GTD-Free is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *    
 *    GTD-Free is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 *    You should have received a copy of the GNU General Public License
 *    along with GTD-Free.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gtdfree.html;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.StringTokenizer;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.gtdfree.html.Structure.ElementType;

/**
 * @author ikesan
 *
 */
public class HTMLStreamWriter implements XMLStreamWriter {

	private final static String EOL="\n"; //$NON-NLS-1$
	public static HTMLStreamWriter newInstance(OutputStream out) throws IOException, XMLStreamException, FactoryConfigurationError {
		Writer wr= new OutputStreamWriter(out,"UTF-8"); //$NON-NLS-1$
		XMLStreamWriter wx= XMLOutputFactory.newInstance().createXMLStreamWriter(wr);
		return new HTMLStreamWriter(wx,wr);
	}
	
	private XMLStreamWriter wx;
	private Writer wr;
	
	/**
	 * @param wx
	 * @param wr
	 */
	private HTMLStreamWriter(XMLStreamWriter wx, Writer wr) {
		super();
		this.wx = wx;
		this.wr = wr;
	}
	
	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#close()
	 */
	@Override
	public void close() throws XMLStreamException {
		wx.close();
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#flush()
	 */
	@Override
	public void flush() throws XMLStreamException {
		wx.flush();
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#getNamespaceContext()
	 */
	@Override
	public NamespaceContext getNamespaceContext() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#getPrefix(java.lang.String)
	 */
	@Override
	public String getPrefix(String uri) throws XMLStreamException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#getProperty(java.lang.String)
	 */
	@Override
	public Object getProperty(String name) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#setDefaultNamespace(java.lang.String)
	 */
	@Override
	public void setDefaultNamespace(String uri) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#setNamespaceContext(javax.xml.namespace.NamespaceContext)
	 */
	@Override
	public void setNamespaceContext(NamespaceContext context)
			throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#setPrefix(java.lang.String, java.lang.String)
	 */
	@Override
	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeAttribute(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeAttribute(String localName, String value)
			throws XMLStreamException {
		wx.writeAttribute(localName, value);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeAttribute(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void writeAttribute(String namespaceURI, String localName,
			String value) throws XMLStreamException {
		wx.writeAttribute(namespaceURI, localName, value);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeAttribute(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void writeAttribute(String prefix, String namespaceURI,
			String localName, String value) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeCData(java.lang.String)
	 */
	@Override
	public void writeCData(String data) throws XMLStreamException {
		wx.writeCData(data);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeCharacters(java.lang.String)
	 */
	@Override
	public void writeCharacters(String text) throws XMLStreamException {
		wx.writeCharacters(text);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeCharacters(char[], int, int)
	 */
	@Override
	public void writeCharacters(char[] text, int start, int len)
			throws XMLStreamException {
		wx.writeCharacters(text, start, len);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeComment(java.lang.String)
	 */
	@Override
	public void writeComment(String data) throws XMLStreamException {
		wx.writeComment(data);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeDTD(java.lang.String)
	 */
	@Override
	public void writeDTD(String dtd) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeDefaultNamespace(java.lang.String)
	 */
	@Override
	public void writeDefaultNamespace(String namespaceURI)
			throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeEmptyElement(java.lang.String)
	 */
	@Override
	public void writeEmptyElement(String localName) throws XMLStreamException {
		wx.writeEmptyElement(localName);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeEmptyElement(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeEmptyElement(String namespaceURI, String localName)
			throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeEmptyElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void writeEmptyElement(String prefix, String localName,
			String namespaceURI) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeEndDocument()
	 */
	@Override
	public void writeEndDocument() throws XMLStreamException {
		wx.writeEndDocument();
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeEndElement()
	 */
	@Override
	public void writeEndElement() throws XMLStreamException {
		wx.writeEndElement();
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeEntityRef(java.lang.String)
	 */
	@Override
	public void writeEntityRef(String name) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeNamespace(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeNamespace(String prefix, String namespaceURI)
			throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeProcessingInstruction(java.lang.String)
	 */
	@Override
	public void writeProcessingInstruction(String target)
			throws XMLStreamException {
		wx.writeProcessingInstruction(target);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeProcessingInstruction(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeProcessingInstruction(String target, String data)
			throws XMLStreamException {
		wx.writeProcessingInstruction(target, data);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeStartDocument()
	 */
	@Override
	public void writeStartDocument() throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeStartDocument(java.lang.String)
	 */
	@Override
	public void writeStartDocument(String version) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeStartDocument(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeStartDocument(String encoding, String version)
			throws XMLStreamException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeStartElement(java.lang.String)
	 */
	@Override
	public void writeStartElement(String localName) throws XMLStreamException {
		wx.writeStartElement(localName);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeStartElement(java.lang.String, java.lang.String)
	 */
	@Override
	public void writeStartElement(String namespaceURI, String localName)
			throws XMLStreamException {
		wx.writeStartElement(namespaceURI,localName);
	}

	/* (non-Javadoc)
	 * @see javax.xml.stream.XMLStreamWriter#writeStartElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void writeStartElement(String prefix, String localName,
			String namespaceURI) throws XMLStreamException {
		wx.writeStartElement(prefix, localName, namespaceURI);
	}

	public void startHtmlDocument(String title) throws XMLStreamException, IOException {
		writeUnescaped("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">"); //$NON-NLS-1$
		writenl();
		wx.writeStartElement("HTML"); //$NON-NLS-1$
		wx.writeCharacters(EOL);
		wx.writeStartElement("HEAD"); //$NON-NLS-1$
		wx.writeCharacters(EOL);
		if (title!=null) {
			wx.writeStartElement("TITLE"); //$NON-NLS-1$
			wx.writeCharacters(title);
			wx.writeEndElement();
		}
		wx.writeCharacters(EOL);
		writeUnescaped("<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"); //$NON-NLS-1$
		writenl();
	}
	public void writeUnescaped(String meta) throws XMLStreamException, IOException {
		wx.flush();
		wr.write(meta);
		wr.flush();
	}
	public void startHtmlBody() throws XMLStreamException {
		wx.writeEndElement();
		wx.writeCharacters(EOL);
		wx.writeStartElement(TAG.BODY.getName());
		wx.writeCharacters(EOL);
	}

	public void writeTag(String tag, String value) throws XMLStreamException {
		wx.writeStartElement(tag);
		wx.writeCharacters(value);
		wx.writeEndElement();
		wx.writeCharacters(EOL);
	}

	public void writeHr() throws XMLStreamException, IOException {
		writeUnescaped("<HR>"); //$NON-NLS-1$
		wx.writeCharacters(EOL);
	}
	
	public void writeBr() throws XMLStreamException, IOException {
		writeUnescaped("<BR>"); //$NON-NLS-1$
		wx.writeCharacters(EOL);
	}

	public void startTag(String tag) throws XMLStreamException {
		wx.writeStartElement(tag);
	}
	
	public void startTag(TAG tag) throws XMLStreamException {
		wx.writeStartElement(tag.getName());
		for (Attribute attribute : tag) {
			wx.writeAttribute(attribute.toString(), tag.getAttribute(attribute));
		}
	}

	public void writeTag(TAG tag, String val) throws XMLStreamException {
		startTag(tag);
		wx.writeCharacters(val);
		endTag();
	}

	public void writeTag(TAG a) throws XMLStreamException {
		if (a.getAttributeCount()>0) {
			startTag(a);
			endTag();
		} else {
			wx.writeEmptyElement(a.getName());
		}
	}

	public void endTag() throws XMLStreamException {
		wx.writeEndElement();
		wx.writeCharacters(EOL);
	}

	public void writeTableRow(String... cells) throws XMLStreamException {
		wx.writeStartElement(TAG.TR.getName());
		for (String c : cells) {
			wx.writeStartElement(TAG.TD.getName());
			wx.writeCharacters(c);
			wx.writeEndElement();
		}
		wx.writeEndElement();
		wx.writeCharacters(EOL);
	}

	public void writeTableRow(String[] cells, TAG[] tags) throws XMLStreamException {
		wx.writeStartElement(TAG.TR.getName());
		for (int i=0; i<cells.length; i++) {
			startTag(tags[i]);
			wx.writeCharacters(cells[i]);
			wx.writeEndElement();
		}
		wx.writeEndElement();
		wx.writeCharacters(EOL);
	}

	public void endHtmlDocument() throws XMLStreamException {
		wx.writeEndElement();
		wx.writeCharacters(EOL);
		wx.writeEndElement();
		wx.writeCharacters(EOL);
	}
	
	public void writenl() throws XMLStreamException {
		wx.writeCharacters(EOL);
	}
	
	public void writeStyleRule(String selector, String declaration) throws XMLStreamException {
		wx.writeCharacters(selector);
		wx.writeCharacters(" { "); //$NON-NLS-1$
		wx.writeCharacters(declaration);
		wx.writeCharacters(" } "); //$NON-NLS-1$
		wx.writeCharacters(EOL);
	}

	public void writeMultiline(String s) throws XMLStreamException {
		if (s==null) {
			return;
		}
		StringTokenizer st= new StringTokenizer(s,"\n"); //$NON-NLS-1$
		
		while(st.hasMoreTokens()) {
			wx.writeCharacters(st.nextToken());
			if (st.hasMoreTokens()) {
				writeTag(TAG.BR);
			}
		}
	}

	public void writeStructure(Structure s) throws XMLStreamException {
		for (Structure st : s) {
			writeStructureElement(st);
		}
	}
	
	public void writeStructureElement(Structure s) throws XMLStreamException {
		if (s!=null) {
			if (s.getType()==ElementType.TAG_START) {
				startTag(s.getTag());
			} else if (s.getType()==ElementType.TAG) {
					writeTag(s.getTag());
			} else if (s.getType()==ElementType.TAG_END) {
				writeEndElement();
			} else if (s.getType()==ElementType.TEXT) {
				writeCharacters(s.getText());
			}

		}
	}
	
}
