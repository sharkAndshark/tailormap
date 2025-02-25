/*
 * Copyright (C) 2011-2016 B3Partners B.V.
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
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.StrictBinding;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.util.HtmlUtil;
import net.sourceforge.stripes.util.StringUtil;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import nl.tailormap.i18n.ResourceBundleProvider;
import nl.tailormap.viewer.components.ComponentRegistry;
import nl.tailormap.viewer.components.ComponentRegistryInitializer;
import nl.tailormap.viewer.components.ViewerComponent;
import nl.tailormap.viewer.config.ClobElement;
import nl.tailormap.viewer.config.app.Application;
import nl.tailormap.viewer.config.app.ConfiguredComponent;
import nl.tailormap.viewer.config.metadata.Metadata;
import nl.tailormap.viewer.config.security.Authorizations;
import nl.tailormap.viewer.config.security.User;
import nl.tailormap.viewer.helpers.AuthorizationsHelper;
import nl.tailormap.viewer.helpers.app.ApplicationHelper;
import nl.tailormap.viewer.helpers.app.ComponentHelper;
import nl.tailormap.viewer.util.SelectedContentCache;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.Stripersist;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 *
 * @author Matthijs Laan
 */
@UrlBinding("/app/{name}/v{version}")
@StrictBinding
public class ApplicationActionBean extends LocalizableApplicationActionBean implements ActionBean {

    private ActionBeanContext context;
            
    @Validate
    private String name;

    @Validate
    private boolean unknown;

    @Validate
    private String version;

    // <editor-fold desc="bookmark variables" defaultstate="collapsed">
    @Validate
    private String bookmark;
    
    @Validate
    private String extent;
    
    @Validate
    private String layers;
    
    @Validate
    private String levelOrder;
    
    // </editor-fold>
    
    @Validate
    private boolean debug;

    @Validate(on = "retrieveAppConfigJSON")
    private Application application;

    private String componentSourceHTML;
    private String appConfigJSON;

    private String viewerType;

    private String title;

    private String language;

    private JSONObject user;

    private String loginUrl;
    private HashMap<String,Object> globalLayout;

    //<editor-fold defaultstate="collapsed" desc="getters en setters">
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    public ActionBeanContext getContext() {
        return context;
    }

    public String getComponentSourceHTML() {
        return componentSourceHTML;
    }

    public void setComponentSourceHTML(String componentSourceHTML) {
        this.componentSourceHTML = componentSourceHTML;
    }

    public String getAppConfigJSON() {
        return appConfigJSON;
    }

    public void setAppConfigJSON(String appConfigJSON) {
        this.appConfigJSON = appConfigJSON;
    }

    public String getViewerType(){
        return viewerType;
    }

    public void setViewerType(String viewerType){
        this.viewerType = viewerType;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getLanguage(){
        return language;
    }

    public void setLanguage(String language){
        this.language = language;
    }

    public JSONObject getUser() {
        return user;
    }

    public void setUser(JSONObject user) {
        this.user = user;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public HashMap getGlobalLayout() {
        return globalLayout;
    }

    public void setGlobalLayout(HashMap globalLayout) {
        this.globalLayout = globalLayout;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public void setUnknown(boolean unknown) {
        this.unknown = unknown;
    }

    public String getBookmark() {
        return bookmark;
    }

    public void setBookmark(String bookmark) {
        this.bookmark = bookmark;
    }

    public String getExtent() {
        return extent;
    }

    public void setExtent(String extent) {
        this.extent = extent;
    }

    public String getLayers() {
        return layers;
    }

    public void setLayers(String layers) {
        this.layers = layers;
    }

    public String getLevelOrder() {
        return levelOrder;
    }

    public void setLevelOrder(String levelOrder) {
        this.levelOrder = levelOrder;
    }
    //</editor-fold>
    
    static Application findApplication(String name, String version) {
        EntityManager em = Stripersist.getEntityManager();
        if(name != null) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery q = cb.createQuery(Application.class);
            Root<Application> root = q.from(Application.class);
            Predicate namePredicate = cb.equal(root.get("name"), name);
            Predicate versionPredicate = version != null
                    ? cb.equal(root.get("version"), version)
                    : cb.isNull(root.get("version"));
            q.where(cb.and(namePredicate, versionPredicate));
            try {
                return (Application) em.createQuery(q).getSingleResult();
            } catch(NoResultException nre) {
                String decodedName = StringUtil.urlDecode(name);
                if(!decodedName.equals(name)){
                    return findApplication(decodedName, version);
            }
        }
        }
        return null;
    }

    public Resolution saveCache() throws JSONException, IOException{
        Resolution view = view();

        EntityManager em = Stripersist.getEntityManager();
        Resolution r = checkRestriction(context, application, em);
        if (r != null) {
            return r;
        }
        SelectedContentCache cache = new SelectedContentCache();
        JSONObject sc = cache.createSelectedContent(application, false,false, false,em);
        application.getDetails().put("selected_content_cache", new ClobElement(sc.toString()));
        em.getTransaction().commit();
        return view;
    }

     public Resolution retrieveCache() throws JSONException, IOException{
        Resolution view = view();

        EntityManager em = Stripersist.getEntityManager();
        Resolution r = checkRestriction(context, application, em);
        if (r != null) {
            return r;
        }
        ClobElement el = application.getDetails().get("selected_content_cache");
        appConfigJSON = el.getValue();
        return view;
    }

    public Resolution retrieveAppConfigJSON() throws IOException {
        EntityManager em = Stripersist.getEntityManager();
        JSONObject response = new JSONObject();
        response.put("success", false);
        JSONObject obj = ApplicationHelper.toJSON(
                application,
                AuthorizationsHelper.getRoles(context.getRequest(), em),
                URI.create(context.getRequest().getRequestURI()),
                context.getRequest().getServletContext().getInitParameter("proxy"),
                false,
                false,
                false,
                false,
                em,
                true,
                true
        );
        JSONObject details = obj.optJSONObject("details");
        if (details != null) {
            details.remove(SelectedContentCache.DETAIL_CACHED_EXPANDED_SELECTED_CONTENT);
            details.remove(SelectedContentCache.DETAIL_CACHED_SELECTED_CONTENT);
        }
        appConfigJSON = obj.toString();
        response.put("config", appConfigJSON);
        response.put("success", true);
        return new StreamingResolution("application/json", new StringReader(response.toString()));
    }

    @DefaultHandler
    public Resolution view() throws JSONException, IOException {
        if(unknown){
            getDefaultViewer();
            /* Redirected here from /index.jsp: further redirect to app with
             * name and version parameters of default app in URL and
             * unknown=false. This makes sure that links in URL always include
             * the app name. Required for compact bookmark links for default app
             * to work.
             */
            return new RedirectResolution(ApplicationActionBean.class)
                    .addParameter("name", name)
                    .addParameter("version", version)
                    .addParameter("debug", debug);

        }
        application = findApplication(name, version);

        if(application == null) {
            getContext().getValidationErrors().addGlobalError(new LocalizableError("app.notfound", HtmlUtil.encode(name + (version != null ? " v" + version : ""))));
            return new ForwardResolution("/WEB-INF/jsp/error.jsp");
        }

        RedirectResolution login = new RedirectResolution(ApplicationActionBean.class)
                .addParameter("name", name) // binded parameters not included ?
                .addParameter("version", version)
                .addParameter("debug", debug)
                .addParameter("uitloggen", true)
                .includeRequestParameters(true);

        addBookmarkParameters(login);
        loginUrl = login.getUrl(context.getLocale());

        String username = context.getRequest().getRemoteUser();
        if(application.isAuthenticatedRequired() && username == null) {
            return login;
        }

        EntityManager em = Stripersist.getEntityManager();
        Resolution r = checkRestriction(context, application, em);
        if(r != null){
            return r;
        }

        if(username != null) {
            user = new JSONObject();
            user.put("name", username);
            JSONObject roles = new JSONObject();
            user.put("roles", roles);
            for(String role: AuthorizationsHelper.getRoles(context.getRequest(),em)) {
                roles.put(role, Boolean.TRUE);
            }
        }

        buildComponentSourceHTML(em);

        appConfigJSON = ApplicationHelper.toJSON( application,  AuthorizationsHelper.getRoles(context.getRequest(), em),
                URI.create(context.getRequest().getRequestURI()),
                context.getRequest().getServletContext().getInitParameter("proxy"),  false,  false,  false,  false,
                em,  true,  false).toString();

        this.viewerType = retrieveViewerType();
        if(StringUtils.isBlank(title)) {
            this.title = application.getName();
        }
        this.language = application.getLang();
        if(StringUtils.isBlank(language)) {
            this.language = "nl_NL";
        }

        //make hashmap for jsonobject.
        this.globalLayout = new HashMap<String,Object>();
        JSONObject layout = application.getGlobalLayout();
        Iterator<String> keys = layout.keys();
        while (keys.hasNext()){
            String key = keys.next();
            this.globalLayout.put(key, layout.get(key));
        }
        context.getResponse().addHeader("X-UA-Compatible", "IE=edge");
        return new ForwardResolution("/WEB-INF/jsp/app.jsp");
    }

    public static Resolution checkRestriction(ActionBeanContext context, Application application, EntityManager em){

        String username = context.getRequest().getRemoteUser();
        User u = null;
        if(username != null){
            Principal p = context.getRequest().getUserPrincipal();
            if( p instanceof User){
                u = (User)p;
            }else{
                u = em.find(User.class, p.getName());
            }
        }

        if(Authorizations.isUserExpired(u)){
            ResourceBundle bundle = ResourceBundleProvider.getResourceBundle(determineLocaleForBundle(context, application));
            String msg = bundle.getString("viewer.applicationactionbean.expired");
            context.getValidationErrors().addGlobalError(new SimpleError(msg));
            context.getRequest().getSession().invalidate();
            return new ForwardResolution("/WEB-INF/jsp/error.jsp");
        }
        else if (!AuthorizationsHelper.isApplicationReadAuthorized(application, AuthorizationsHelper.getRoles(context.getRequest(), em), em) && (username == null || u != null && u.isAuthenticatedByIp())) {
            RedirectResolution login = new RedirectResolution(LoginActionBean.class)
                    .addParameter("name", application.getName()) // binded parameters not included ?
                    .addParameter("version", application.getVersion())
                    .includeRequestParameters(true);
            context.getRequest().getSession().invalidate();
            return login;
        } else if (!AuthorizationsHelper.isApplicationReadAuthorized(application, AuthorizationsHelper.getRoles(context.getRequest(), em), em) && username != null) {
            ResourceBundle bundle = ResourceBundleProvider.getResourceBundle(determineLocaleForBundle(context, application));
            String msg = bundle.getString("viewer.applicationactionbean.norights");
            context.getValidationErrors().addGlobalError(new SimpleError(msg));
            context.getRequest().getSession().invalidate();
            return new ForwardResolution("/WEB-INF/jsp/error_retry.jsp");
        }
        return null;
    }

    /**
     * Build a hash key to make the single component source for all components
     * cacheable but updateable when the roles of the user change. This is not
     * meant to be a secure hash, the roles of a user are not secret.
     *
     * @param request servlet request with user credential
     * @param em the entitymanahger to use for database access
     * @return a key to use as a cache identifyer
     */
    public static int getRolesCachekey(HttpServletRequest request, EntityManager em) {
        Set<String> roles = AuthorizationsHelper.getRoles(request, em);

        if(roles.isEmpty()) {
            return 0;
        }

        List<String> sorted = new ArrayList<String>(roles);
        Collections.sort(sorted);

        int hash = 0;
        for(String role: sorted) {
            hash = hash ^ role.hashCode();
        }
        return hash;
    }

    public Resolution uitloggen(){
        application = findApplication(name, version);

        context.getRequest().getSession().invalidate();

        if("true".equals(context.getRequest().getParameter("logout"))
        && "true".equals(context.getRequest().getParameter("returnAfterLogout"))) {
            RedirectResolution r = new RedirectResolution(ApplicationActionBean.class)
                    .addParameter("name", application.getName())
                    .addParameter("version", application.getVersion());
            addBookmarkParameters(r);
            return r;
        } else {
            RedirectResolution r = new RedirectResolution(LoginActionBean.class)
                    .addParameter("name", application.getName())
                    .addParameter("debug", debug)
                    .addParameter("version", application.getVersion());
            addBookmarkParameters(r);
            return r;
        }
    }

    private void addBookmarkParameters(RedirectResolution r) {
        if (bookmark != null) {
            r.addParameter("bookmark", bookmark);
        }
        if(extent != null){
            r.addParameter("extent", extent);
        }
        if(layers != null){
            r.addParameter("layers", layers);
        
        }
        if(levelOrder != null){
            r.addParameter("levelOrder", levelOrder);
        }
    }

    private void buildComponentSourceHTML(EntityManager em) throws IOException {

        StringBuilder sb = new StringBuilder();

        // Sort components by classNames, so order is always the same for debugging
        ComponentRegistry cr = ComponentRegistryInitializer.getInstance();

        List<ConfiguredComponent> comps = new ArrayList<ConfiguredComponent>(application.getComponents());
        Collections.sort(comps);
        if(isDebug()) {

            Set<String> classNamesDone = new HashSet<String>();
            for(ConfiguredComponent cc: comps) {

                if(!AuthorizationsHelper.isConfiguredComponentAuthorized(cc, context.getRequest(), em)) {
                    continue;
                }
                if(!classNamesDone.contains(cc.getClassName())) {
                    classNamesDone.add(cc.getClassName());
                    ViewerComponent vc = ComponentHelper.getViewerComponent(cc.getClassName());

                    if(vc!= null && vc.getSources() != null) {
                        for(File f: vc.getSources()) {
                            String url = new ForwardResolution(ComponentActionBean.class, "source")
                                    .addParameter("app", name)
                                    .addParameter("version", version)
                                    .addParameter("className", cc.getClassName())
                                    .addParameter("file", f.getName())
                                    .getUrl(context.getLocale());

                            sb.append("        <script type=\"text/javascript\" src=\"");
                            sb.append(HtmlUtil.encode(context.getServletContext().getContextPath() + url));
                            sb.append("\"></script>\n");
                        }
                    }
                }
            }
        } else {
            // If not debugging, create a single script tag with all source
            // for all components for the application for a minimal number of HTTP requests

            // The ComponentActionBean supports conditional HTTP requests using
            // Last-Modified.
            // Create a hash value that will change when the classNames used
            // in the application change, so that a browser will not use a
            // previous version from cache with other contents.

            int hash = 0;
            Set<String> classNamesDone = new HashSet<String>();
            for (ConfiguredComponent cc : comps) {
                if (!AuthorizationsHelper.isConfiguredComponentAuthorized(cc, context.getRequest(), em)) {
                    continue;
                }

                if(!classNamesDone.contains(cc.getClassName())) {
                    hash = hash ^ cc.getClassName().hashCode();
                } else {
                    classNamesDone.add(cc.getClassName());
                }
            }
            if(user != null) {
                // Update component sources when roles of user change
                hash = hash ^ getRolesCachekey(context.getRequest(), em);

                // Update component sources when roles of configured components
                // may have changed
                hash = hash ^ (int)application.getAuthorizationsModified().getTime();
            }

            String url = new ForwardResolution(ComponentActionBean.class, "source")
                    .addParameter("app", name)
                    .addParameter("version", version)
                    .addParameter("minified", true)
                    .addParameter("hash", hash)
                    .getUrl(context.getLocale());

            sb.append("        <script type=\"text/javascript\" src=\"");
            sb.append(HtmlUtil.encode(context.getServletContext().getContextPath() + url));
            sb.append("\"></script>\n");
        }

        componentSourceHTML = sb.toString();
    }

    private String retrieveViewerType (){
        String type = "openlayers";
        String typePrefix = "viewer.mapcomponents";
        Set<ConfiguredComponent> components = application.getComponents();
        for (ConfiguredComponent component : components) {
            String className = component.getClassName();
            if(className.startsWith(typePrefix)){
                type = className.substring(typePrefix.length() +1).toLowerCase().replace("map", "");
                break;
            }
        }
        return type;
    }

    private void getDefaultViewer(){
        EntityManager em = Stripersist.getEntityManager();
        try {
            Metadata md = em.createQuery("from Metadata where configKey = :key", Metadata.class).setParameter("key", Metadata.DEFAULT_APPLICATION).getSingleResult();
            String appId = md.getConfigValue();
            Long id = Long.parseLong(appId);
            Application app = em.find(Application.class, id);
            name = app.getName();
            version = app.getVersion();
        } catch (NoResultException | NullPointerException e) {
            name = "default";
            version = null;
        }
    }
}
