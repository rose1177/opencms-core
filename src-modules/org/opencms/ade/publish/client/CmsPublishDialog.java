/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/publish/client/Attic/CmsPublishDialog.java,v $
 * Date   : $Date: 2010/04/13 09:17:28 $
 * Version: $Revision: 1.6 $
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

package org.opencms.ade.publish.client;

import org.opencms.ade.publish.shared.CmsProjectBean;
import org.opencms.ade.publish.shared.CmsPublishData;
import org.opencms.ade.publish.shared.CmsPublishGroup;
import org.opencms.ade.publish.shared.CmsPublishOptions;
import org.opencms.ade.publish.shared.CmsPublishResource;
import org.opencms.ade.publish.shared.rpc.I_CmsPublishService;
import org.opencms.ade.publish.shared.rpc.I_CmsPublishServiceAsync;
import org.opencms.gwt.client.rpc.CmsRpcAction;
import org.opencms.gwt.client.ui.CmsButton;
import org.opencms.gwt.client.ui.CmsPopupDialog;
import org.opencms.util.CmsUUID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * 
 * Main class for the publish dialog.<p>
 * 
 * This class is mostly responsible for the control flow and RPC calls of the publish dialog.
 * It delegates most of the actual GUI work to the {@link CmsPublishSelectPanel} and {@link CmsBrokenLinksPanel} classes.
 * 
 * @author Georg Westenberger
 * 
 * @version $Revision: 1.6 $
 * 
 * @since 8.0.0
 * 
 */
public class CmsPublishDialog extends CmsPopupDialog {

    /**
     * The action for publishing and/or removing resources from the publish list.<p>
     */
    private class CmsPublishAction extends CmsRpcAction<List<CmsPublishResource>> {

        /** If true, try to ignore broken links when publishing. */
        private boolean m_force;

        /** Creates a new instance of this action. 
         * 
         * @param force if true, try to ignore broken links when publishing
         */
        public CmsPublishAction(boolean force) {

            m_force = force;
        }

        /**
         * @see org.opencms.gwt.client.rpc.CmsRpcAction#execute()
         */
        @Override
        public void execute() {

            start(0);
            List<CmsUUID> resourcesToPublish = new ArrayList<CmsUUID>(m_publishSelectPanel.getResourcesToPublish());
            List<CmsUUID> resourcesToRemove = new ArrayList<CmsUUID>(m_publishSelectPanel.getResourcesToRemove());
            m_publishService.publishResources(resourcesToPublish, resourcesToRemove, m_force, this);
        }

        /**
         * @see org.opencms.gwt.client.rpc.CmsRpcAction#onResponse(java.lang.Object)
         */
        @Override
        protected void onResponse(List<CmsPublishResource> result) {

            onReceiveStatus(result);
            stop();
        }
    }

    /**
     * The action for loading the publish list.<p>
     */
    private class CmsPublishListAction extends CmsRpcAction<List<CmsPublishGroup>> {

        /** The publish list options which should be used. */
        private CmsPublishOptions m_options;

        /**
         * Creates a new publish list action.<p>
         * 
         * @param options the publish list options which should be used 
         */
        public CmsPublishListAction(CmsPublishOptions options) {

            m_options = options;
        }

        /**
         * @see org.opencms.gwt.client.rpc.CmsRpcAction#execute()
         */
        @Override
        public void execute() {

            start(500);
            m_publishService.getPublishGroups(m_options, this);
        }

        /**
         * @see org.opencms.gwt.client.rpc.CmsRpcAction#onResponse(java.lang.Object)
         */
        @Override
        protected void onResponse(List<CmsPublishGroup> result) {

            onReceivePublishList(result);
            stop();
        }
    }

    /** The project map used by showPublishDialog. */
    public static Map<String, String> m_staticProjects;

    /** The index of the "broken links" panel. */
    private static final int PANEL_BROKEN_LINKS = 1;

    /** The index of the publish selection panel. */
    private static final int PANEL_SELECT = 0;

    /** The panel for selecting the resources to publish or remove from the publish list. */
    protected CmsPublishSelectPanel m_publishSelectPanel;

    /** The remote publish service used by this dialog. */
    protected I_CmsPublishServiceAsync m_publishService;

    /** The panel for showing the links that would be broken by publishing. */
    private CmsBrokenLinksPanel m_brokenLinksPanel = new CmsBrokenLinksPanel(this);

    /** The root panel of this dialog which contains both the selection panel and the panel for displaying broken links. */
    private DeckPanel m_panel = new DeckPanel();

    /**
     * Constructs a new publish dialog.<p>
     * 
     * @param publishService the publish service to use 
     * @param projects the projects which should be selectable in the publish dialog
     * @param options the initial publish list options to use 
     */
    public CmsPublishDialog(
        I_CmsPublishServiceAsync publishService,
        List<CmsProjectBean> projects,
        CmsPublishOptions options) {

        super(Messages.get().key(Messages.GUI_PUBLISH_DIALOG_TITLE_0), new DeckPanel());
        initCss();
        setGlassEnabled(true);
        setAutoHideEnabled(false);
        setModal(true);
        setWidth("800px");
        m_panel = (DeckPanel)getContent();
        m_publishSelectPanel = new CmsPublishSelectPanel(this, projects, options);

        m_panel.add(m_publishSelectPanel);
        m_panel.add(m_brokenLinksPanel);
        setPanel(PANEL_SELECT);

        m_publishService = publishService;
    }

    /**
     * Convenience method which opens a publish dialog.<p>
     */
    public static void showPublishDialog() {

        showPublishDialog(null);
    }

    /**
     * Convenience method which opens a publish dialog.<p>
     * 
     * @param handler the close handler
     */
    public static void showPublishDialog(final CloseHandler<PopupPanel> handler) {

        final I_CmsPublishServiceAsync publishService = GWT.create(I_CmsPublishService.class);
        (new CmsRpcAction<CmsPublishData>() {

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#execute()
             */
            @Override
            public void execute() {

                start(200);
                publishService.getInitData(this);
            }

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#onResponse(java.lang.Object)
             */
            @Override
            protected void onResponse(final CmsPublishData result) {

                CmsPublishDialog publishDialog = new CmsPublishDialog(
                    publishService,
                    result.getProjects(),
                    result.getOptions());
                publishDialog.onReceivePublishList(result.getGroups());
                if (handler != null) {
                    publishDialog.addCloseHandler(handler);
                }
                stop();
                publishDialog.center();
            }
        }).execute();
    }

    /**
     * Ensures all style sheets are loaded.<p>
     */
    public void initCss() {

        I_CmsPublishLayoutBundle.INSTANCE.publishCss().ensureInjected();
    }

    /**
     * Method which is called when the cancel button is pressed.<p>
     */
    public void onCancel() {

        hide();
    }

    /**
     * Method which is called when the publish options are changed.<p>
     */
    public void onChangeOptions() {

        CmsPublishOptions options = m_publishSelectPanel.getPublishOptions();
        (new CmsPublishListAction(options)).execute();
    }

    /**
     * Method which is  called when the back button is pressed.<p>
     */
    public void onGoBack() {

        setPanel(PANEL_SELECT);
    }

    /**
     * Method which is called after the publish list has been received from the server.<p>
     * 
     * @param groups the groups of the publish list
     */
    public void onReceivePublishList(List<CmsPublishGroup> groups) {

        m_publishSelectPanel.setGroups(groups);
        setPanel(PANEL_SELECT);
        if (!isVisible()) {
            center();
        }
    }

    /**
     * Method which is called after the status from a publish action has arrived.<p>
     * 
     * @param brokenResources the list of broken resources
     */
    public void onReceiveStatus(List<CmsPublishResource> brokenResources) {

        if (m_panel.getVisibleWidget() == PANEL_SELECT) {
            if (brokenResources.isEmpty()) {
                hide();
            } else {
                m_brokenLinksPanel.setEntries(brokenResources);
                setPanel(PANEL_BROKEN_LINKS);
            }
        }
    }

    /**
     * Method which is called when the "force publish" button is pressed.<p>    
     */
    public void onRequestForcePublish() {

        (new CmsPublishAction(true)).execute();
    }

    /**
     * Method which is called when the publish button is pressed.<p>     
     */
    public void onRequestPublish() {

        (new CmsPublishAction(false)).execute();
    }

    /**
     * Changes the currently active panel.<p>
     * 
     * @param panelId the number of the panel to show
     */
    public void setPanel(int panelId) {

        m_panel.showWidget(panelId);
        removeAllButtons();
        if (panelId == PANEL_SELECT) {
            for (CmsButton button : m_publishSelectPanel.getButtons()) {
                addButton(button);
            }
        } else if (panelId == PANEL_BROKEN_LINKS) {
            for (CmsButton button : m_brokenLinksPanel.getButtons()) {
                addButton(button);
            }
        }

    }

}