/*
 * Copyright (C) 2012-2013 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.tailormap.viewer.stripes;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.StrictBinding;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import nl.tailormap.viewer.config.app.Application;
import nl.tailormap.viewer.config.app.ApplicationLayer;
import nl.tailormap.viewer.helpers.AuthorizationsHelper;
import nl.tailormap.viewer.helpers.app.ApplicationLayerHelper;
import nl.tailormap.viewer.util.LayerListHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.stripesstuff.stripersist.Stripersist;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Roy Braam
 */
@UrlBinding("/action/layerList")
@StrictBinding
public class LayerListActionBean implements ActionBean {

    private static final Log log = LogFactory.getLog(LayerListActionBean.class);
    private ActionBeanContext context;

    public ActionBeanContext getContext() {
        return context;
    }

    public void setContext(ActionBeanContext context) {
        this.context = context;
    }
    @Validate
    private Long appId;
    @Validate
    private Boolean filterable = false;
    @Validate
    private Boolean bufferable = false;
    @Validate
    private Boolean editable = false;
    @Validate
    private Boolean influence = false;
    @Validate
    private Boolean arc = false;
    @Validate
    private Boolean wfs = false;
    @Validate
    private Boolean attribute = false;
    @Validate
    private Boolean hasConfiguredLayers = false;
    @Validate
    private List<Long> layers = new ArrayList<Long>();

    //<editor-fold defaultstate="collapsed" desc="Getters and setters">
    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public Boolean getBufferable() {
        return bufferable;
    }

    public void setBufferable(Boolean bufferable) {
        this.bufferable = bufferable;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Boolean getFilterable() {
        return filterable;
    }

    public void setFilterable(Boolean filterable) {
        this.filterable = filterable;
    }

    public Boolean getInfluence() {
        return influence;
    }

    public void setInfluence(Boolean influence) {
        this.influence = influence;
    }

    public Boolean getArc() {
        return arc;
    }

    public void setArc(Boolean arc) {
        this.arc = arc;
    }

    public Boolean getAttribute() {
        return attribute;
    }

    public void setAttribute(Boolean attribute) {
        this.attribute = attribute;
    }

    public Boolean getWfs() {
        return wfs;
    }

    public void setWfs(Boolean wfs) {
        this.wfs = wfs;
    }

    public List<Long> getLayers() {
        return layers;
    }

    public void setLayers(List<Long> layers) {
        this.layers = layers;
    }

    public Boolean getHasConfiguredLayers() {
        return hasConfiguredLayers;
    }

    public void setHasConfiguredLayers(Boolean hasConfiguredLayers) {
        this.hasConfiguredLayers = hasConfiguredLayers;
    }
    
    

    //</editor-fold>
    
    public Resolution source() {
        EntityManager em = Stripersist.getEntityManager();
        JSONArray jsonArray = new JSONArray();

        if (appId != null) {
            Application app = em.find(Application.class, appId);
            
            // TODO filter layers according to readers
            // set writeable according to writers
            HttpServletRequest request = context.getRequest();
            List<ApplicationLayer> filteredLayers = LayerListHelper.getLayers(app, filterable, bufferable, editable, influence, arc, wfs, attribute,hasConfiguredLayers,layers,em);
            for (ApplicationLayer layer : filteredLayers) {
                if (AuthorizationsHelper.isAppLayerReadAuthorized(app, layer, request, em)) {
                    try {
                        jsonArray.put(ApplicationLayerHelper.toJSONObject(layer, em));
                    } catch (JSONException je) {
                        log.error("Error while getting JSONObject of Layer with id: " + layer.getId(), je);
                    }
                }
            }
        }
        return new StreamingResolution("application/json", new StringReader(jsonArray.toString()));
    }

   
}
