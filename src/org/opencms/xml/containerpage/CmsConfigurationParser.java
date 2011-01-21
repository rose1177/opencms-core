/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/containerpage/Attic/CmsConfigurationParser.java,v $
 * Date   : $Date: 2011/01/21 11:06:52 $
 * Version: $Revision: 1.18 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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
 * For further information about Alkacon Software, please see the
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

import org.opencms.cache.CmsVfsMemoryObjectCache;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.CmsRuntimeException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsFormatterUtil;
import org.opencms.util.CmsPair;
import org.opencms.util.CmsUUID;
import org.opencms.workplace.explorer.CmsExplorerTypeSettings;
import org.opencms.xml.CmsXmlUtils;
import org.opencms.xml.I_CmsXmlDocument;
import org.opencms.xml.content.CmsXmlContentFactory;
import org.opencms.xml.content.CmsXmlContentProperty;
import org.opencms.xml.content.CmsXmlContentRootLocation;
import org.opencms.xml.content.I_CmsXmlContentLocation;
import org.opencms.xml.content.I_CmsXmlContentValueLocation;
import org.opencms.xml.sitemap.CmsDetailPageInfo;
import org.opencms.xml.types.I_CmsXmlContentValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Transformer;
import org.apache.commons.logging.Log;

/**
 * Class for managing the creation of new content elements in ADE.<p>
 * 
 * XML files in the VFS can be used to configure which files are used as
 * prototypes for new elements, and which file names are used for the new
 * elements.<p> 
 * 
 * TODO: separate the parser from the data it parses 
 * 
 * @author Georg Westenberger
 * 
 * @version $Revision: 1.18 $ 
 * 
 * @since 7.6 
 */
public class CmsConfigurationParser {

    /** The default maximum sitemap depth. */
    public static final int DEFAULT_MAX_DEPTH = 15;

    /** The tag name for the export name configuration. */
    public static final String N_ADE_EXPORTNAME = "ADEExportName";

    /** The node name for the container page name generator class. */
    public static final String N_ADE_NAME_GENERATOR = "ContainerPageNameGenerator";

    /** The tag name of the configuration for a single type. */
    public static final String N_ADE_TYPE = "ADEType";

    /** The tag name of the destination in the type configuration. */
    public static final String N_DESTINATION = "Destination";

    /** The detail page node name. */
    public static final String N_DETAIL_PAGE = "DetailPage";

    /** The tag name of the source file in the type configuration. */
    public static final String N_FOLDER = "Folder";

    /** The tag name of a formatter configuration. */
    public static final String N_FORMATTER = "Formatter";

    /** The tag name of the formatter jsp. */
    public static final String N_JSP = "Jsp";

    /** Node name for the maximum depth configuration. */
    public static final String N_MAXDEPTH = "MaxDepth";

    /** The Page node name. */
    public static final String N_PAGE = "Page";

    /** The tag name of the source file in the type configuration. */
    public static final String N_PATTERN = "Pattern";

    /** The tag name of the source file in the type configuration. */
    public static final String N_SOURCE = "Source";

    /** The tag name of the formatter container type. */
    public static final String N_TYPE = "Type";

    /** The tag name of the formatter width. */
    public static final String N_WIDTH = "Width";

    /** The instance cache. */
    private static CmsVfsMemoryObjectCache m_cache = new CmsVfsMemoryObjectCache();

    /** The tag name for elements containing field configurations. */
    private static final String N_ADE_FIELD = "ADEField";

    /** Configuration data, read from xml content. */
    private Map<String, CmsConfigurationItem> m_configuration = new HashMap<String, CmsConfigurationItem>();

    /** The xml document. */
    private I_CmsXmlDocument m_content;

    /** The detail pages from the configuration file. */
    private List<CmsDetailPageInfo> m_detailPages;

    /** The configured export name. */
    private String m_exportName;

    /** The formatter configuration maps. */
    private Map<String, CmsPair<Map<String, String>, Map<Integer, String>>> m_formatterConfiguration = new HashMap<String, CmsPair<Map<String, String>, Map<Integer, String>>>();

    /** The maximum sitemap depth. */
    private int m_maxDepth = DEFAULT_MAX_DEPTH;

    /** The container page name generator class. */
    private String m_nameGenerator;

    /** New elements. */
    private Collection<CmsResource> m_newElements = new LinkedHashSet<CmsResource>();

    /** The list of properties read from the configuration file. */
    private List<CmsXmlContentProperty> m_props = new ArrayList<CmsXmlContentProperty>();

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsConfigurationParser.class);

    /**
     * Default constructor.<p>
     */
    public CmsConfigurationParser() {

        // do nothing

    }

    /**
     * Constructs a new instance.<p>
     * 
     * @param cms the cms context used for reading the configuration
     * @param config the configuration file
     *  
     * @throws CmsException if something goes wrong
     */
    public CmsConfigurationParser(CmsObject cms, CmsResource config)
    throws CmsException {

        processFile(cms, config);
    }

    /**
     * Constructs a parser and caches it so that subsequent calls to this method with the same resource
     * will return the same object if the resource hasn'T changed.<p>
     * 
     * @param cms the CMS context 
     * @param config the configuration resource 
     * @return the configuration parser 
     * 
     * @throws CmsException if something goes wrong 
     */
    public static CmsConfigurationParser getParser(final CmsObject cms, final CmsResource config) throws CmsException {

        try {
            Object cachedObj = m_cache.loadVfsObject(cms, config.getRootPath(), new Transformer() {

                /**
                 * @see org.apache.commons.collections.Transformer#transform(java.lang.Object)
                 */
                public Object transform(Object o) {

                    try {
                        return new CmsConfigurationParser(cms, config);
                    } catch (CmsException e) {
                        // the Transformer interface does not allow checked exceptions, so we wrap
                        // them into runtime exceptions and then unwrap them again later.
                        throw new CmsRuntimeException(e.getMessageContainer(), e);
                    }
                }
            });
            return (CmsConfigurationParser)cachedObj;
        } catch (CmsRuntimeException e) {
            throw (CmsException)e.getCause();
        }
    }

    /**
     * Returns the container page name generator class name.<p>
     * 
     * @return the container page name generator class name 
     */
    public String getContainerPageNameGeneratorClass() {

        return m_nameGenerator;
    }

    /**
     * Returns an unmodifiable list of properties defined in the configuration file.<p>
     *  
     * @return the list of properties defined in the configuration file 
     */
    public List<CmsXmlContentProperty> getDefinedProperties() {

        return Collections.unmodifiableList(m_props);
    }

    /**
     * Returns the detail pages from the configuration.<p>
     * 
     * @return the detail pages from the configuration 
     */
    public List<CmsDetailPageInfo> getDetailPages() {

        return Collections.unmodifiableList(m_detailPages);
    }

    /**
     * Returns the configured export name.<p>
     * 
     * @return the configured export name 
     */
    public String getExportName() {

        return m_exportName;
    }

    /**
     * Returns the formatter configuration for a given element type.<p>
     * 
     * @param type a type name 
     * 
     * @return a pair of maps containing the formatter configuration for the type 
     */
    public CmsPair<Map<String, String>, Map<Integer, String>> getFormatterConfigurationForType(String type) {

        return m_formatterConfiguration.get(type);
    }

    /**
     * Returns the maximum sitemap depth.<p>
     * 
     * @return the maximum sitemap depth 
     */
    public int getMaxDepth() {

        return m_maxDepth;
    }

    /**
     * Gets the list of 'prototype resources' which are used for creating new content elements.
     * 
     * @param cms the CMS context
     * 
     * @return the resources which are used as prototypes for creating new elements
     * 
     * @throws CmsException if something goes wrong 
     */
    public Collection<CmsResource> getNewElements(CmsObject cms) throws CmsException {

        Set<CmsResource> result = new LinkedHashSet<CmsResource>();
        for (Map.Entry<String, CmsConfigurationItem> entry : m_configuration.entrySet()) {
            CmsConfigurationItem item = entry.getValue();
            String type = entry.getKey();
            CmsResource source = item.getSourceFile();
            //CmsResource folderRes = item.getLazyFolder().getValue(cms);
            CmsLazyFolder autoFolder = item.getLazyFolder();
            CmsResource permissionCheckFolder = autoFolder.getPermissionCheckFolder(cms);
            CmsExplorerTypeSettings settings = OpenCms.getWorkplaceManager().getExplorerTypeSetting(type);
            boolean editable = settings.isEditable(cms, permissionCheckFolder);
            boolean controlPermission = settings.getAccess().getPermissions(cms, permissionCheckFolder).requiresControlPermission();
            if (editable && controlPermission) {
                result.add(source);
            }
        }
        return result;
    }

    /**
     * Returns the configuration as an unmodifiable map.<p>
     * 
     * @return the configuration as an unmodifiable map
     */
    public Map<String, CmsConfigurationItem> getTypeConfiguration() {

        return Collections.unmodifiableMap(m_configuration);
    }

    /**
     * Reads additional configuration data from a file.<p>
     * 
     * @param cms the CMS context 
     * @param config the configuration file 
     * 
     * @throws CmsException if something goes wrong 
     */
    public void processFile(CmsObject cms, CmsResource config) throws CmsException {

        CmsFile configFile = cms.readFile(config);
        I_CmsXmlDocument content = CmsXmlContentFactory.unmarshal(cms, configFile);
        parseConfiguration(cms, content);
    }

    /**
     * Adds the configuration from another parser to this one.<p>
     * 
     * @param parser the configuration parser whose data should be added to this one 
     */
    public void update(CmsConfigurationParser parser) {

        for (Map.Entry<String, CmsConfigurationItem> entry : parser.m_configuration.entrySet()) {
            m_configuration.put(entry.getKey(), entry.getValue());
        }
        for (CmsResource res : parser.m_newElements) {
            m_newElements.add(res);
        }
        m_formatterConfiguration.putAll(parser.m_formatterConfiguration);
    }

    /**
     * Helper method for finding the locale for accessing the XML content.<p>
     * 
     * @param cms the CMS context 
     * @param content the XML content 
     * 
     * @return the locale
     * 
     * @throws CmsException if something goes wrong 
     */
    protected Locale getLocale(CmsObject cms, I_CmsXmlDocument content) throws CmsException {

        Locale currentLocale = cms.getRequestContext().getLocale();
        Locale defaultLocale = CmsLocaleManager.getDefaultLocale();
        Locale locale = null;
        if (content.hasLocale(currentLocale)) {
            locale = currentLocale;
        } else if (content.hasLocale(defaultLocale)) {
            locale = defaultLocale;
        } else {
            List<Locale> locales = content.getLocales();
            if (locales.size() == 0) {
                throw new CmsException(Messages.get().container(
                    Messages.ERR_NO_TYPE_CONFIG_1,
                    content.getFile().getRootPath()));
            }
            locale = locales.get(0);
        }
        return locale;
    }

    /**
     * Helper method for retrieving the OpenCms type name for a given type id.<p>
     * 
     * @param typeId the id of the type
     * 
     * @return the name of the type
     * 
     * @throws CmsException if something goes wrong
     */
    protected String getTypeName(int typeId) throws CmsException {

        return OpenCms.getResourceManager().getResourceType(typeId).getTypeName();
    }

    /**
     * Convenience method to retrieve the sub-value of an xml-element as id.<p>
     * 
     * @param cms the current cms object
     * @param field the xml-element
     * @param fieldName the element name
     * 
     * @return the value or <code>null</code> if the sub element by the given name is not present
     */
    private CmsUUID getSubValueID(CmsObject cms, I_CmsXmlContentLocation field, String fieldName) {

        I_CmsXmlContentValueLocation subValue = field.getSubValue(fieldName);
        if (subValue != null) {
            return subValue.asId(cms);
        }
        return null;
    }

    /**
     * Convenience method to retrieve the sub-value of an xml-element as string.<p>
     * 
     * @param cms the current cms object
     * @param field the xml-element
     * @param fieldName the element name
     * 
     * @return the value or <code>null</code> if the sub element by the given name is not present
     */
    private String getSubValueString(CmsObject cms, I_CmsXmlContentLocation field, String fieldName) {

        I_CmsXmlContentValueLocation subValue = field.getSubValue(fieldName);
        if (subValue != null) {
            return subValue.asString(cms);
        }
        return null;
    }

    /**
     * Parses a type configuration contained in an XML content.<p>
     * 
     * This method uses the first locale from the following list which has a corresponding
     * element in the XML content:
     * <ul>
     *  <li>the request context's locale</li>
     *  <li>the default locale</li>
     *  <li>the first locale available in the XML content</li>
     * </ul><p>
     *
     * @param cms the CmsObject to use for VFS operations
     * @param content the XML content with the type configuration
     * 
     * @throws CmsException if something goes wrong
     */
    private void parseConfiguration(CmsObject cms, I_CmsXmlDocument content) throws CmsException {

        Locale locale = getLocale(cms, content);
        m_content = content;
        I_CmsXmlContentLocation root = new CmsXmlContentRootLocation(content, locale);

        List<I_CmsXmlContentValueLocation> typeValues = root.getSubValues(N_ADE_TYPE);

        for (I_CmsXmlContentValueLocation xmlType : typeValues) {
            try {
                parseType(cms, xmlType, locale);
            } catch (Exception e) {
                LOG.error(Messages.get().container(Messages.ERR_CONFIG_MALFORMED_TYPE_0), e);
            }
        }

        List<I_CmsXmlContentValueLocation> fieldValues = root.getSubValues(N_ADE_FIELD);
        for (I_CmsXmlContentValueLocation xmlField : fieldValues) {
            parseField(cms, xmlField, locale);
        }

        List<I_CmsXmlContentValueLocation> detailPageValues = root.getSubValues(N_DETAIL_PAGE);
        for (I_CmsXmlContentValueLocation detailPageValue : detailPageValues) {
            parseDetailPage(cms, detailPageValue);
        }

        I_CmsXmlContentValue exportNameNode = content.getValue(N_ADE_EXPORTNAME, locale);
        if (exportNameNode != null) {
            m_exportName = exportNameNode.getStringValue(cms);
        }

        I_CmsXmlContentValue nameGeneratorNode = content.getValue(N_ADE_NAME_GENERATOR, locale);
        if (nameGeneratorNode != null) {
            m_nameGenerator = nameGeneratorNode.getStringValue(cms);
        }

        m_detailPages = parseDetailPages(cms, root);

        I_CmsXmlContentValue maxDepthNode = content.getValue(N_MAXDEPTH, locale);
        if (maxDepthNode != null) {
            try {
                m_maxDepth = Integer.parseInt(maxDepthNode.getStringValue(cms));
            } catch (NumberFormatException e) {
                // ignore, leave max depth at its default value 
            }
        }

    }

    /**
     * Parses a single detail page bean from the configuration.<p>
     * 
     * @param cms the current CMS context 
     * @param detailPageNode the location from which to read the detail page bean 
     * 
     * @return the parsed detail page bean  
     */
    private CmsDetailPageInfo parseDetailPage(CmsObject cms, I_CmsXmlContentValueLocation detailPageNode) {

        String type = getSubValueString(cms, detailPageNode, N_TYPE);
        I_CmsXmlContentValueLocation target = detailPageNode.getSubValue(N_PAGE);
        if ((target == null) || (type == null)) {
            return null;
        }
        CmsUUID targetId = target.asId(null);
        String targetPath = cms.getRequestContext().addSiteRoot(target.asString(cms));
        CmsDetailPageInfo result = new CmsDetailPageInfo(targetId, targetPath, type);
        return result;
    }

    /** 
     * Parses the detail pages from the configuration file.<p>
     * 
     * @param cms the current CMS context 
     * @param root the location from which to read the detail pages
     *  
     * @return the parsed detail page beans
     */
    private List<CmsDetailPageInfo> parseDetailPages(CmsObject cms, I_CmsXmlContentLocation root) {

        List<I_CmsXmlContentValueLocation> values = root.getSubValues(N_DETAIL_PAGE);
        List<CmsDetailPageInfo> result = new ArrayList<CmsDetailPageInfo>();
        for (I_CmsXmlContentValueLocation detailPageNode : values) {
            CmsDetailPageInfo info = parseDetailPage(cms, detailPageNode);
            result.add(info);
        }
        return result;
    }

    /**
     * Parses a single field definition from a content value.<p>
     * 
     * @param cms the CMS context 
     * @param field the content value to parse the field from 
     * @param locale the locale to use 
     */
    private void parseField(CmsObject cms, I_CmsXmlContentLocation field, Locale locale) {

        String name = getSubValueString(cms, field, "Name");
        String type = getSubValueString(cms, field, "Type");
        String widget = getSubValueString(cms, field, "Widget");
        String widgetConfig = getSubValueString(cms, field, "WidgetConfig");

        String ruleRegex = getSubValueString(cms, field, "RuleRegex");
        String ruleType = getSubValueString(cms, field, "RuleType");
        String default1 = getSubValueString(cms, field, "Default");
        String error = getSubValueString(cms, field, "Error");
        String niceName = getSubValueString(cms, field, "NiceName");
        String description = getSubValueString(cms, field, "Description");
        String advanced = getSubValueString(cms, field, "Advanced");
        String selectInherit = getSubValueString(cms, field, "SelectInherit");
        CmsXmlContentProperty prop = new CmsXmlContentProperty(
            name,
            type,
            widget,
            widgetConfig,
            ruleRegex,
            ruleType,
            default1,
            niceName,
            description,
            error,
            advanced,
            selectInherit);
        m_props.add(prop);
    }

    /**
     * Internal method for parsing the element types in the configuration file.<p>
     * 
     * @param cms the CMS context 
     * @param xmlType a content value representing an element type 
     * @param locale the locale to use 
     * 
     * @throws CmsException if something goes wrong 
     */
    private void parseType(CmsObject cms, I_CmsXmlContentLocation xmlType, Locale locale) throws CmsException {

        CmsUUID source = getSubValueID(cms, xmlType, N_SOURCE);
        CmsUUID folder = getSubValueID(cms, xmlType, CmsXmlUtils.concatXpath(N_DESTINATION, N_FOLDER));
        String pattern = getSubValueString(cms, xmlType, CmsXmlUtils.concatXpath(N_DESTINATION, N_PATTERN));
        CmsResource resource = cms.readResource(source);
        String type = getTypeName(resource.getTypeId());
        CmsResource folderRes = null;
        CmsLazyFolder lazyFolder = null;
        if (folder == null) {
            String path = "/" + type;
            lazyFolder = new CmsLazyFolder(path);
        } else {
            folderRes = cms.readResource(folder);
            lazyFolder = new CmsLazyFolder(folderRes);
        }

        CmsConfigurationItem configItem = new CmsConfigurationItem(resource, folderRes, lazyFolder, pattern);
        List<I_CmsXmlContentValueLocation> fmtValues = xmlType.getSubValues(N_FORMATTER);
        List<CmsFormatterConfigBean> formatterConfigBeans = new ArrayList<CmsFormatterConfigBean>();
        for (I_CmsXmlContentValueLocation fmtValue : fmtValues) {
            String jsp = getSubValueString(cms, fmtValue, N_JSP);
            String width = getSubValueString(cms, fmtValue, N_WIDTH);
            String fmtType = getSubValueString(cms, fmtValue, N_TYPE);
            formatterConfigBeans.add(new CmsFormatterConfigBean(jsp, fmtType, width));
        }
        if (!formatterConfigBeans.isEmpty()) {
            CmsPair<Map<String, String>, Map<Integer, String>> formatterMaps = CmsFormatterUtil.getFormatterMapsFromConfigBeans(
                formatterConfigBeans,
                m_content.getFile().getRootPath());
            m_formatterConfiguration.put(type, formatterMaps);
        }
        m_configuration.put(type, configItem);
    }

}
