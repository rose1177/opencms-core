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

package org.opencms.ade.contenteditor.client.widgets;

import com.alkacon.acacia.client.css.I_LayoutBundle;
import com.alkacon.acacia.client.widgets.I_EditWidget;

import org.opencms.gwt.client.ui.input.CmsCheckBox;

import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Provides a standard HTML form checkbox widget, for use on a widget dialog.<p>
 * 
 * */
public class CmsCheckboxWidget extends Composite implements I_EditWidget {

    /** The token to control activation. */
    private boolean m_active = true;

    /** The checkbox of this widget. */
    protected CmsCheckBox m_checkbox = new CmsCheckBox();

    /**
     * Constructs an OptionalTextBox with the given caption on the check.<p>
     * 
     */
    public CmsCheckboxWidget() {

        // Place the check above the text box using a vertical panel.
        VerticalPanel panel = new VerticalPanel();
        // adds the checkbot to the panel. 
        panel.add(m_checkbox);

        // Set the check box's caption, and check it by default.
        m_checkbox.setChecked(true);
        m_checkbox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            public void onValueChange(ValueChangeEvent<Boolean> event) {

                if (Boolean.parseBoolean(m_checkbox.getFormValueAsString())) {
                    getParent().getElement().addClassName(I_LayoutBundle.INSTANCE.form().inActive());
                } else {
                    getParent().getElement().removeClassName(I_LayoutBundle.INSTANCE.form().inActive());
                }
                fireChangeEvent();

            }

        });
        // All composites must call initWidget() in their constructors.
        initWidget(panel);

    }

    /**
     * @see com.google.gwt.event.dom.client.HasFocusHandlers#addFocusHandler(com.google.gwt.event.dom.client.FocusHandler)
     */
    public HandlerRegistration addFocusHandler(FocusHandler handler) {

        return null;
    }

    /**
     * @see com.google.gwt.event.logical.shared.HasValueChangeHandlers#addValueChangeHandler(com.google.gwt.event.logical.shared.ValueChangeHandler)
     */
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {

        return addHandler(handler, ValueChangeEvent.getType());
    }

    /**
     *  Represents a value change event.<p>
     */
    public void fireChangeEvent() {

        ValueChangeEvent.fire(this, m_checkbox.getFormValueAsString());
    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#getValue()
     */
    public String getValue() {

        return m_checkbox.getFormValueAsString();
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#isActive()
     */
    public boolean isActive() {

        return m_active;
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#onAttachWidget()
     */
    public void onAttachWidget() {

        super.onAttach();
    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#setActive(boolean)
     */
    public void setActive(boolean active) {

        // control if the value has not change do nothing.        
        if (m_active == active) {
            return;
        }
        // set the new value.
        m_active = active;
        // fire change event.
        if (active) {
            fireChangeEvent();
        }
        // activate the checkbox.
        m_checkbox.setEnabled(active);

    }

    /**
     * @see com.alkacon.acacia.client.widgets.I_EditWidget#setName(java.lang.String)
     */
    public void setName(String name) {

        // no input field so nothing to do

    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object)
     */
    public void setValue(String value) {

        m_checkbox.setFormValueAsString(value);

    }

    /**
     * @see com.google.gwt.user.client.ui.HasValue#setValue(java.lang.Object, boolean)
     */
    public void setValue(String value, boolean fireEvents) {

        if (Boolean.parseBoolean(value)) {
            getParent().getElement().addClassName(I_LayoutBundle.INSTANCE.form().inActive());
        } else {
            getParent().getElement().removeClassName(I_LayoutBundle.INSTANCE.form().inActive());
        }
        m_checkbox.setFormValueAsString(value);
        if (fireEvents) {
            fireChangeEvent();
        }

    }

}
