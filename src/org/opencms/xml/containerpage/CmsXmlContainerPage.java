/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.xml.containerpage;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsLink;
import org.opencms.relations.CmsRelationType;
import org.opencms.util.CmsMacroResolver;
import org.opencms.util.CmsUUID;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlException;
import org.opencms.xml.CmsXmlGenericWrapper;
import org.opencms.xml.CmsXmlUtils;
import org.opencms.xml.content.CmsXmlContent;
import org.opencms.xml.content.CmsXmlContentMacroVisitor;
import org.opencms.xml.content.CmsXmlContentProperty;
import org.opencms.xml.content.CmsXmlContentPropertyHelper;
import org.opencms.xml.page.CmsXmlPage;
import org.opencms.xml.types.CmsXmlNestedContentDefinition;
import org.opencms.xml.types.CmsXmlVfsFileValue;
import org.opencms.xml.types.I_CmsXmlContentValue;
import org.opencms.xml.types.I_CmsXmlSchemaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.xml.sax.EntityResolver;

/**
 * Implementation of a object used to access and manage the xml data of a container page.<p>
 * 
 * In addition to the XML content interface. It also provides access to more comfortable beans. 
 * 
 * @since 7.5.2
 * 
 * @see #getContainerPage(CmsObject, Locale)
 */
public class CmsXmlContainerPage extends CmsXmlContent {

    /** XML node name constants. */
    public enum XmlNode {

        /** Container attribute node name. */
        Attribute,
        /** Main node name. */
        Containers,
        /** The create new element node name. */
        CreateNew,
        /** Container elements node name. */
        Elements,
        /** Element formatter node name. */
        Formatter,
        /** Container attribute key node name. */
        Key,
        /** Container name node name. */
        Name,
        /** Container type node name. */
        Type,
        /** Element URI node name. */
        Uri,
        /** Container attribute value node name. */
        Value;
    }

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsXmlContainerPage.class);

    /** The container page objects. */
    private Map<Locale, CmsContainerPageBean> m_cntPages;

    /**
     * Hides the public constructor.<p>
     */
    protected CmsXmlContainerPage() {

        // noop
    }

    /**
     * Creates a new container page based on the provided XML document.<p>
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the cms context, if <code>null</code> no link validation is performed 
     * @param document the document to create the container page from
     * @param encoding the encoding of the container page
     * @param resolver the XML entity resolver to use
     */
    protected CmsXmlContainerPage(CmsObject cms, Document document, String encoding, EntityResolver resolver) {

        // must set document first to be able to get the content definition
        m_document = document;
        // for the next line to work the document must already be available
        m_contentDefinition = getContentDefinition(resolver);
        // initialize the XML content structure
        initDocument(cms, m_document, encoding, m_contentDefinition);
    }

    /**
     * Create a new container page based on the given default content,
     * that will have all language nodes of the default content and ensures the presence of the given locale.<p> 
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the current users OpenCms content
     * @param locale the locale to generate the default content for
     * @param modelUri the absolute path to the container page file acting as model
     * 
     * @throws CmsException in case the model file is not found or not valid
     */
    protected CmsXmlContainerPage(CmsObject cms, Locale locale, String modelUri)
    throws CmsException {

        // init model from given modelUri
        CmsFile modelFile = cms.readFile(modelUri, CmsResourceFilter.ONLY_VISIBLE_NO_DELETED);
        CmsXmlContainerPage model = CmsXmlContainerPageFactory.unmarshal(cms, modelFile);

        // initialize macro resolver to use on model file values
        CmsMacroResolver macroResolver = CmsMacroResolver.newInstance().setCmsObject(cms);

        // content definition must be set here since it's used during document creation
        m_contentDefinition = model.getContentDefinition();
        // get the document from the default content
        Document document = (Document)model.m_document.clone();
        // initialize the XML content structure
        initDocument(cms, document, model.getEncoding(), m_contentDefinition);
        // resolve eventual macros in the nodes
        visitAllValuesWith(new CmsXmlContentMacroVisitor(cms, macroResolver));
        if (!hasLocale(locale)) {
            // required locale not present, add it
            try {
                addLocale(cms, locale);
            } catch (CmsXmlException e) {
                // this can not happen since the locale does not exist
            }
        }
    }

    /**
     * Create a new container page based on the given content definition,
     * that will have one language node for the given locale all initialized with default values.<p> 
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the current users OpenCms content
     * @param locale the locale to generate the default content for
     * @param encoding the encoding to use when marshalling the container page later
     * @param contentDefinition the content definition to create the content for
     */
    protected CmsXmlContainerPage(
        CmsObject cms,
        Locale locale,
        String encoding,
        CmsXmlContentDefinition contentDefinition) {

        // content definition must be set here since it's used during document creation
        m_contentDefinition = contentDefinition;
        // create the XML document according to the content definition
        Document document = m_contentDefinition.createDocument(cms, this, locale);
        // initialize the XML content structure
        initDocument(cms, document, encoding, m_contentDefinition);
    }

    /**
     * Saves a container page bean to the in-memory XML structure and returns the changed content.<p>
     * 
     * @param cms the current CMS context 
     * @param locale the locale for which the content should be replaced 
     * @param cntPage the container page bean 
     * @return the new content for the container page 
     * @throws CmsException if something goes wrong 
     */
    public byte[] createContainerPageXml(CmsObject cms, Locale locale, CmsContainerPageBean cntPage)
    throws CmsException {

        writeContainerPage(cms, locale, cntPage);
        return marshal();

    }

    /**
     * Returns the container page bean for the given locale.<p>
     *
     * @param cms the cms context
     * @param locale the locale to use
     *
     * @return the container page bean
     */
    public CmsContainerPageBean getContainerPage(CmsObject cms, Locale locale) {

        Locale theLocale = locale;
        if (!m_cntPages.containsKey(theLocale)) {
            LOG.warn(Messages.get().container(
                Messages.LOG_CONTAINER_PAGE_LOCALE_NOT_FOUND_2,
                cms.getSitePath(getFile()),
                theLocale.toString()).key());
            return null;
        }
        return m_cntPages.get(theLocale);
    }

    /**
     * @see org.opencms.xml.content.CmsXmlContent#isAutoCorrectionEnabled()
     */
    @Override
    public boolean isAutoCorrectionEnabled() {

        return true;
    }

    /**
     * Saves given container page in the current locale, and not only in memory but also to VFS.<p>
     * 
     * @param cms the current cms context
     * @param locale the content locale
     * @param cntPage the container page to save
     * 
     * @throws CmsException if something goes wrong
     */
    public void save(CmsObject cms, Locale locale, CmsContainerPageBean cntPage) throws CmsException {

        CmsFile file = getFile();

        // lock the file
        cms.lockResourceTemporary(cms.getSitePath(file));
        byte[] data = createContainerPageXml(cms, locale, cntPage);
        file.setContents(data);
        cms.writeFile(file);
    }

    /**
     * Saves a container page in in-memory XML structure.<p>
     * 
     * @param cms the current CMS context 
     * @param locale the locale for which the content should be replaced 
     * @param cntPage the container page bean to save
     * 
     * @throws CmsException if something goes wrong 
     */
    public void writeContainerPage(CmsObject cms, Locale locale, CmsContainerPageBean cntPage) throws CmsException {

        // keep unused containers
        CmsContainerPageBean savePage = addUnusedContainers(cms, locale, cntPage);

        // wipe the locale
        if (hasLocale(locale)) {
            removeLocale(locale);
        }
        addLocale(cms, locale);

        // add the nodes to the raw XML structure
        Element parent = getLocaleNode(locale);
        saveContainerPage(cms, parent, savePage);
        initDocument(m_document, m_encoding, m_contentDefinition);
    }

    /**
     * Merges the containers of the current document that are not used in the given container page with it.<p>
     * 
     * @param cms the current CMS context
     * @param locale the content locale
     * @param cntPage the container page to merge
     * 
     * @return a new container page with the additional unused containers
     */
    protected CmsContainerPageBean addUnusedContainers(CmsObject cms, Locale locale, CmsContainerPageBean cntPage) {

        // get the used containers first
        Map<String, CmsContainerBean> currentContainers = cntPage.getContainers();
        List<CmsContainerBean> containers = new ArrayList<CmsContainerBean>();
        for (String cntName : cntPage.getNames()) {
            containers.add(currentContainers.get(cntName));
        }

        // now get the unused containers 
        CmsContainerPageBean currentContainerPage = getContainerPage(cms, locale);
        if (currentContainerPage != null) {
            for (String cntName : currentContainerPage.getNames()) {
                if (!currentContainers.containsKey(cntName)) {
                    containers.add(currentContainerPage.getContainers().get(cntName));
                }
            }
        }

        return new CmsContainerPageBean(locale, containers);
    }

    /**
     * Fills a {@link CmsXmlVfsFileValue} with the resource identified by the given id.<p>
     * 
     * @param cms the current CMS context
     * @param element the XML element to fill
     * @param resourceId the ID identifying the resource to use
     * 
     * @return the resource 
     * 
     * @throws CmsException if the resource can not be read
     */
    protected CmsResource fillResource(CmsObject cms, Element element, CmsUUID resourceId) throws CmsException {

        String xpath = element.getPath();
        int pos = xpath.lastIndexOf("/" + XmlNode.Containers.name() + "/");
        if (pos > 0) {
            xpath = xpath.substring(pos + 1);
        }
        CmsRelationType type = getHandler().getRelationType(xpath);
        CmsResource res = cms.readResource(resourceId);
        CmsXmlVfsFileValue.fillEntry(element, res.getStructureId(), res.getRootPath(), type);
        return res;
    }

    /**
     * @see org.opencms.xml.A_CmsXmlDocument#initDocument(org.dom4j.Document, java.lang.String, org.opencms.xml.CmsXmlContentDefinition)
     */
    @Override
    protected void initDocument(Document document, String encoding, CmsXmlContentDefinition definition) {

        m_document = document;
        m_contentDefinition = definition;
        m_encoding = CmsEncoder.lookupEncoding(encoding, encoding);
        m_elementLocales = new HashMap<String, Set<Locale>>();
        m_elementNames = new HashMap<Locale, Set<String>>();
        m_locales = new HashSet<Locale>();
        m_cntPages = new HashMap<Locale, CmsContainerPageBean>();
        clearBookmarks();

        // initialize the bookmarks
        for (Iterator<Element> itCntPages = CmsXmlGenericWrapper.elementIterator(m_document.getRootElement()); itCntPages.hasNext();) {
            Element cntPage = itCntPages.next();

            try {
                Locale locale = CmsLocaleManager.getLocale(cntPage.attribute(
                    CmsXmlContentDefinition.XSD_ATTRIBUTE_VALUE_LANGUAGE).getValue());

                addLocale(locale);

                List<CmsContainerBean> containers = new ArrayList<CmsContainerBean>();
                for (Iterator<Element> itCnts = CmsXmlGenericWrapper.elementIterator(cntPage, XmlNode.Containers.name()); itCnts.hasNext();) {
                    Element container = itCnts.next();
                    Map<String, String> attributes = new HashMap<String, String>();
                    for (Element attribute : CmsXmlGenericWrapper.elementIterable(container, XmlNode.Attribute.name())) {
                        Element keyElem = (Element)attribute.selectSingleNode("Key");
                        Element valElem = (Element)attribute.selectSingleNode("Value");
                        Node keyContent = keyElem.selectSingleNode("text()");
                        Node valContent = valElem.selectSingleNode("text()");
                        String keyValue = keyContent.getText();
                        String valValue = valContent.getText();
                        attributes.put(keyValue, valValue);
                    }

                    // container itself
                    int cntIndex = CmsXmlUtils.getXpathIndexInt(container.getUniquePath(cntPage));
                    String cntPath = CmsXmlUtils.createXpathElement(container.getName(), cntIndex);
                    I_CmsXmlSchemaType cntSchemaType = definition.getSchemaType(container.getName());
                    I_CmsXmlContentValue cntValue = cntSchemaType.createValue(this, container, locale);
                    addBookmark(cntPath, locale, true, cntValue);
                    CmsXmlContentDefinition cntDef = ((CmsXmlNestedContentDefinition)cntSchemaType).getNestedContentDefinition();

                    // name
                    Element name = container.element(XmlNode.Name.name());
                    addBookmarkForElement(name, locale, container, cntPath, cntDef);

                    // type
                    Element type = container.element(XmlNode.Type.name());
                    addBookmarkForElement(type, locale, container, cntPath, cntDef);

                    List<CmsContainerElementBean> elements = new ArrayList<CmsContainerElementBean>();
                    // Elements
                    for (Iterator<Element> itElems = CmsXmlGenericWrapper.elementIterator(
                        container,
                        XmlNode.Elements.name()); itElems.hasNext();) {
                        Element element = itElems.next();

                        // element itself
                        int elemIndex = CmsXmlUtils.getXpathIndexInt(element.getUniquePath(container));
                        String elemPath = CmsXmlUtils.concatXpath(
                            cntPath,
                            CmsXmlUtils.createXpathElement(element.getName(), elemIndex));
                        I_CmsXmlSchemaType elemSchemaType = cntDef.getSchemaType(element.getName());
                        I_CmsXmlContentValue elemValue = elemSchemaType.createValue(this, element, locale);
                        addBookmark(elemPath, locale, true, elemValue);
                        CmsXmlContentDefinition elemDef = ((CmsXmlNestedContentDefinition)elemSchemaType).getNestedContentDefinition();

                        // uri
                        Element uri = element.element(XmlNode.Uri.name());
                        addBookmarkForElement(uri, locale, element, elemPath, elemDef);
                        Element uriLink = uri.element(CmsXmlPage.NODE_LINK);
                        CmsUUID elementId = null;
                        if (uriLink == null) {
                            // this can happen when adding the elements node to the xml content
                            // it is not dangerous since the link has to be set before saving 
                        } else {
                            elementId = new CmsLink(uriLink).getStructureId();
                        }
                        Element createNewElement = element.element(XmlNode.CreateNew.name());
                        boolean createNew = (createNewElement != null)
                            && Boolean.parseBoolean(createNewElement.getStringValue());

                        // formatter
                        Element formatter = element.element(XmlNode.Formatter.name());
                        addBookmarkForElement(formatter, locale, element, elemPath, elemDef);
                        Element formatterLink = formatter.element(CmsXmlPage.NODE_LINK);
                        CmsUUID formatterId = null;
                        if (formatterLink == null) {
                            // this can happen when adding the elements node to the xml content
                            // it is not dangerous since the link has to be set before saving 
                        } else {
                            formatterId = new CmsLink(formatterLink).getStructureId();
                        }

                        // the properties
                        Map<String, String> propertiesMap = CmsXmlContentPropertyHelper.readProperties(
                            this,
                            locale,
                            element,
                            elemPath,
                            elemDef);

                        if (elementId != null) {
                            elements.add(new CmsContainerElementBean(elementId, formatterId, propertiesMap, createNew));
                        }
                    }
                    CmsContainerBean newContainerBean = new CmsContainerBean(name.getText(), type.getText(), elements);
                    newContainerBean.setAttributes(attributes);
                    containers.add(newContainerBean);
                }

                m_cntPages.put(locale, new CmsContainerPageBean(locale, containers));
            } catch (NullPointerException e) {
                LOG.error(
                    org.opencms.xml.content.Messages.get().getBundle().key(
                        org.opencms.xml.content.Messages.LOG_XMLCONTENT_INIT_BOOKMARKS_0),
                    e);
            }
        }
    }

    /**
     * Adds the given container page to the given element.<p>
     * 
     * @param cms the current CMS object
     * @param parent the element to add it
     * @param cntPage the container page to add
     * 
     * @throws CmsException if something goes wrong
     */
    protected void saveContainerPage(CmsObject cms, Element parent, CmsContainerPageBean cntPage) throws CmsException {

        parent.clearContent();

        for (String containerName : cntPage.getNames()) {
            CmsContainerBean container = cntPage.getContainers().get(containerName);

            // the container
            Element cntElement = parent.addElement(XmlNode.Containers.name());
            cntElement.addElement(XmlNode.Name.name()).addCDATA(container.getName());
            cntElement.addElement(XmlNode.Type.name()).addCDATA(container.getType());

            //            for (Map.Entry<String, String> entry : container.getAttributes().entrySet()) {
            //                Element attrElement = cntElement.addElement(XmlNode.Attribute.name());
            //                attrElement.addElement(XmlNode.Key.name()).addCDATA(entry.getKey());
            //                attrElement.addElement(XmlNode.Value.name()).addCDATA(entry.getValue());
            //            }

            // the elements
            for (CmsContainerElementBean element : container.getElements()) {
                Element elemElement = cntElement.addElement(XmlNode.Elements.name());

                // the element
                Element uriElem = elemElement.addElement(XmlNode.Uri.name());
                CmsResource uriRes = fillResource(cms, uriElem, element.getId());
                Element formatterElem = elemElement.addElement(XmlNode.Formatter.name());
                fillResource(cms, formatterElem, element.getFormatterId());

                // the properties
                Map<String, String> properties = element.getIndividualSettings();
                Map<String, CmsXmlContentProperty> propertiesConf = OpenCms.getADEManager().getElementSettings(
                    cms,
                    uriRes);

                CmsXmlContentPropertyHelper.saveProperties(cms, elemElement, properties, uriRes, propertiesConf);
            }
        }
    }

    /**
     * @see org.opencms.xml.content.CmsXmlContent#setFile(org.opencms.file.CmsFile)
     */
    @Override
    protected void setFile(CmsFile file) {

        // just for visibility from the factory
        super.setFile(file);
    }

}
