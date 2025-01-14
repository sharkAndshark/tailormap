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
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.StrictBinding;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import nl.tailormap.geotools.data.arcgis.ArcGISFeatureReader;
import nl.tailormap.geotools.data.arcgis.ArcGISFeatureSource;
import nl.tailormap.geotools.filter.visitor.RemoveDistanceUnit;
import nl.tailormap.viewer.config.app.Application;
import nl.tailormap.viewer.config.app.ApplicationLayer;
import nl.tailormap.viewer.config.services.Layer;
import nl.tailormap.viewer.helpers.AuthorizationsHelper;
import nl.tailormap.viewer.helpers.featuresources.FeatureSourceFactoryHelper;
import nl.tailormap.viewer.util.TailormapCQL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengis.filter.Filter;
import org.stripesstuff.stripersist.Stripersist;

import javax.persistence.EntityManager;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.List;

     
    // </editor-fold>

/**
 *
 * @author Matthijs Laan
 */
@UrlBinding("/action/arcquery")
@StrictBinding
public class ArcQueryUtilActionBean extends LocalizableApplicationActionBean implements ActionBean {

    private static final Log log = LogFactory.getLog(ArcQueryUtilActionBean.class);
    @Validate
    private String cql;
    @Validate
    private boolean whereOnly = false;
    @Validate
    private ApplicationLayer appLayer;
    @Validate
    private Application application;
    
    private boolean unauthorized;
    private Layer layer = null;
    private ActionBeanContext context;

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public ActionBeanContext getContext() {
        return context;
    }

    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    public String getCql() {
        return cql;
    }

    public void setCql(String cql) {
        this.cql = cql;
    }

    public boolean isWhereOnly() {
        return whereOnly;
    }

    public void setWhereOnly(boolean whereOnly) {
        this.whereOnly = whereOnly;
    }

    public ApplicationLayer getAppLayer() {
        return appLayer;
    }

    public void setAppLayer(ApplicationLayer appLayer) {
        this.appLayer = appLayer;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }
    // </editor-fold>

    @After(stages = LifecycleStage.BindingAndValidation)
    public void loadLayer() {
        layer = appLayer.getService().getSingleLayer(appLayer.getLayerName(), Stripersist.getEntityManager());
    }

    @Before(stages = LifecycleStage.EventHandling)
    public void checkAuthorization() {
        EntityManager em =Stripersist.getEntityManager();
        if (application == null || appLayer == null || !AuthorizationsHelper.isApplicationReadAuthorized(application, AuthorizationsHelper.getRoles(context.getRequest(), em), em)
                || !AuthorizationsHelper.isAppLayerReadAuthorized(application, appLayer, context.getRequest(), em)) {
            unauthorized = true;
        }
    }

    @DefaultHandler
    public Resolution getObjectIds() throws JSONException, Exception {
        JSONObject json = new JSONObject();

        if (unauthorized) {
            json.put("success", false);
            json.put("message", getBundle().getString("viewer.general.noauth"));
            return new StreamingResolution("application/json", new StringReader(json.toString(4)));
        }

        try {

            if (layer != null && layer.getFeatureType() != null) {
                FeatureSource fs;

                if (layer.getFeatureType().getFeatureSource() instanceof nl.tailormap.viewer.config.services.ArcGISFeatureSource) {
                    fs = FeatureSourceFactoryHelper.openGeoToolsFeatureSource(layer.getFeatureType());
                    final Query q = new Query(fs.getName().toString());
                    setFilter(q, Stripersist.getEntityManager());
                    ArcGISFeatureReader agfr = new ArcGISFeatureReader((ArcGISFeatureSource) fs, q);
                    List objIds = agfr.getObjectIds();

                    json.put("objectIds",objIds);
                    json.put("objectIdFieldName",agfr.getObjectIdFieldName());
                    json.put("success",true);
                }else{
                    json.put("success",false);
                    json.put("message", MessageFormat.format(getBundle().getString("viewer.arcqueryutilactionbean.incortype"), layer.getFeatureType().getFeatureSource().getClass()));
                }
            }
        } catch (Exception e) {
            log.error("Error loading feature ids", e);
            json.put("success", false);
            String message = MessageFormat.format(getBundle().getString("viewer.arcqueryutilactionbean.ff"), e.toString());
            Throwable cause = e.getCause();
            while (cause != null) {
                message += "; " + cause.toString();
                cause = cause.getCause();
            }
            json.put("message", message);
        }

        return new StreamingResolution("application/json", new StringReader(json.toString(4)));
    }

    private void setFilter(Query q, EntityManager em) throws Exception {
        if (cql != null && cql.trim().length() > 0) {
            Filter f = TailormapCQL.toFilter(cql,em);
            f = (Filter) f.accept(new RemoveDistanceUnit(), null);
            q.setFilter(f);
        }
    }
}
