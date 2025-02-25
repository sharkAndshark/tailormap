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
import nl.tailormap.viewer.config.services.ArcGISService;
import nl.tailormap.viewer.config.services.GeoService;
import nl.tailormap.viewer.config.services.TileService;
import nl.tailormap.viewer.config.services.WMSService;
import nl.tailormap.viewer.helpers.services.ArcGISServiceHelper;
import nl.tailormap.viewer.helpers.services.GeoServiceHelper;
import nl.tailormap.viewer.helpers.services.TilingServiceHelper;
import nl.tailormap.viewer.helpers.services.WMSServiceHelper;
import nl.tailormap.web.WaitPageStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.Stripersist;

import javax.persistence.EntityManager;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Matthijs Laan
 */
@UrlBinding("/service/info")
@StrictBinding
public class ServiceActionBean extends LocalizableActionBean implements ActionBean {
    
    private ActionBeanContext context;

    @Validate
    private String protocol;
    @Validate
    private String url;
    @Validate
    private String serviceName;
    
    //<editor-fold defaultstate="collapsed" desc="getters en setters">
    public ActionBeanContext getContext() {
        return context;
    }
    
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    //</editor-fold>

    public Resolution info() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("success", Boolean.FALSE);
        String error = null;
        GeoService service = null;
        EntityManager em = Stripersist.getEntityManager();
        
        if(protocol == null || url == null) {
            error = getBundle().getString("viewer.serviceactionbean.1");
        } else {
            
            Map params = new HashMap();
            
            try {
                if(protocol.equals(WMSService.PROTOCOL)) {
                    //params.put(WMSService.PARAM_OVERRIDE_URL, overrideUrl);
                    service = WMSServiceHelper.loadFromUrl(url, params, new WaitPageStatus(), em);
                } else if(protocol.equals(ArcGISService.PROTOCOL)) {
                    service = ArcGISServiceHelper.loadFromUrl(url, params, new WaitPageStatus(), em);
                } else {
                    error = getBundle().getString("viewer.serviceactionbean.2");
                }            
            } catch(Exception e) {
                
                error = "Error loading service " + e.toString();
                if(e.getCause() != null) {
                    error += "; cause: " + e.getCause().toString();
                }
            }
        }

        if (service != null) {
            json.put("success", Boolean.TRUE);
            if (service instanceof TileService) {
                json.put("service", TilingServiceHelper.toJSONObject((TileService) service, true, null, false, false, em));
            } else if (service instanceof ArcGISService) {
                json.put("service", ArcGISServiceHelper.toJSONObject((ArcGISService) service, true, null, false, false, em));
            } else {
                json.put("service", GeoServiceHelper.toJSONObject(service, true, null, false, false, em));
            }
        } else {
            json.put("success", Boolean.FALSE);
            json.put("error", error);
        }

        return new StreamingResolution("application/json", new StringReader(json.toString()));        
    }
}
