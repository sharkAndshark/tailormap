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
import nl.tailormap.i18n.LocalizableActionBean;
import nl.tailormap.viewer.config.services.GeoService;
import nl.tailormap.viewer.config.services.Layer;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.Stripersist;

import javax.persistence.EntityManager;
import java.io.StringReader;

/**
 *
 * @author Matthijs Laan
 */
@UrlBinding("/service/feature")
@StrictBinding
public class FeatureActionBean extends LocalizableActionBean implements ActionBean {

    private ActionBeanContext context;
    
    @Validate
    private GeoService service;
    
    @Validate
    private String layer;

    //<editor-fold defaultstate="collapsed" desc="getters and setters">
    public ActionBeanContext getContext() {
        return context;
    }
    
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }
    
    public String getLayer() {
        return layer;
    }
    
    public void setLayer(String layer) {
        this.layer = layer;
    }
    
    public GeoService getService() {
        return service;
    }
    
    public void setService(GeoService service) {
        this.service = service;
    }
    //</editor-fold>

    public Resolution getLayerFeatureType() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("success", Boolean.FALSE);
        String error = null;
        
        if(service == null || layer == null) {
            error = getBundle().getString("viewer.featureactionbean.1");
        } else {
            EntityManager em = Stripersist.getEntityManager();
            service.loadLayerTree(em);
            Layer l = service.getLayer(layer, em);
            
            if(l == null) {
                error = "Can't find layer " + layer;
            } else {
                json.put("featureType", l.getFeatureType() == null ? null : l.getFeatureType().toJSONObject());
                json.put("success", Boolean.TRUE);
            }
        }
        
        if(error != null) {
            json.put("error", error);
        }
        
        return new StreamingResolution("application/json", new StringReader(json.toString()));        
    }    
}
