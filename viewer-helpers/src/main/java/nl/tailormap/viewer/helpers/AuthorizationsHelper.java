package nl.tailormap.viewer.helpers;

import nl.tailormap.viewer.config.app.Application;
import nl.tailormap.viewer.config.app.ApplicationLayer;
import nl.tailormap.viewer.config.app.ConfiguredComponent;
import nl.tailormap.viewer.config.app.Level;
import nl.tailormap.viewer.config.security.Authorizations;
import nl.tailormap.viewer.config.services.Layer;
import nl.tailormap.viewer.util.DB;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AuthorizationsHelper  {

    /**
     * The set of role names which mean nobody has access; a set which only contains
     * null.
     */
    public static final Set<String> NOBODY = new HashSet<>(Arrays.asList(new String[]{null}));
    /**
     * The empty set of role names which mean everybody has access.
     */
    public static final Set<String> EVERYBODY = Collections.emptySet();
    /**
     * Map of protected Layers per GeoService. Only public for UserAction to
     * display all authorizations.
     */
    public static final Map<Long, GeoServiceCache> serviceCache = new HashMap<>();
    private static final Log log = LogFactory.getLog(AuthorizationsHelper.class);
    /** Humongous lock for everything, but should be locked for only short times */
    private static final Object LOCK = new Object();
    private static final String ROLES_ATTRIBUTE = Authorizations.class.getName() + ".roles";
    /**
     * Map of protected Levels and ApplicationLayers per Application
     */
    private static final Map<Long, ApplicationCache> applicationCache = new HashMap<>();
    /**
     * Map of reader role names per ConfiguredComponent per Application
     */
    private static final Map<Long, AppConfiguredComponentsReadersCache> appConfiguredComponentsReadersCache = new HashMap<>();
    private static final String REQUEST_APP_CACHE = Authorizations.class.getName() + ".REQUEST_APP_CACHE";

    public static void checkConfiguredComponentAuthorized(ConfiguredComponent component, HttpServletRequest request, EntityManager em) throws Exception {
        if(!isReadAuthorized(getRoles(request, em), new Read(component.getReaders()))) {
            throw new Exception(("User " + request.getRemoteUser() == null ? "(none)" : request.getRemoteUser() + " not authorized to " + "edit ") + " configured component #" + component.getName() + " of app #" + component.getApplication().getId());
        }
    }

    public static boolean isLevelReadAuthorized(Application app, Level l, HttpServletRequest request, EntityManager em) {
        return isLevelReadAuthorized(app, l, request, getApplicationCacheFromRequest(app, request,em), em);
    }

    public static boolean isLevelReadAuthorized(Application app, Level l, HttpServletRequest request, ApplicationCache appCache, EntityManager em) {
        if(app.isAuthenticatedRequired() && request.getRemoteUser() == null) {
            return false;
        }

        if(appCache == null) {
            appCache = getApplicationCache(app, em);
        }
        Read auths = appCache.protectedLevels.get(l.getId());
        return isReadAuthorized(getRoles(request, em), auths);
    }

    public static Set<String> getRoles(HttpServletRequest request, EntityManager em) {

        if(request.getRemoteUser() == null) {
            return Collections.emptySet();
        }

        Set<String> roles = (Set<String>)request.getAttribute(ROLES_ATTRIBUTE);

        if(roles == null) {
            roles = new HashSet<>();
            List<String> groups = em.createQuery("select name FROM Group").getResultList();
            for (String group : groups) {
                if(request.isUserInRole(group)){
                    roles.add(group);
                }
            }

            request.setAttribute(ROLES_ATTRIBUTE, roles);
        }
        return roles;
    }

    public static boolean isReadAuthorized(Set<String> roles, Read auths) {

        if(auths == null  || auths.readers.equals(EVERYBODY)) {
            return true;
        }

        if(auths.readers.equals(NOBODY)) {
            return false;
        }

        if(roles.isEmpty()) {
            return false;
        }

        return !Collections.disjoint(auths.readers, roles);
    }

    public static boolean isWriteAuthorized(Set<String> roles, ReadWrite auths, EntityManager em) {
        if(!isReadAuthorized(roles, auths)) {
            return false;
        }
        if(auths == null || auths.writers.equals(EVERYBODY)) {
            return true;
        }
        if(auths.writers.equals(NOBODY)) {
            return false;
        }
        if(roles.isEmpty()) {
            return false;
        }

        return !Collections.disjoint(auths.writers, roles);
    }

    public static ApplicationCache getApplicationCacheFromRequest(Application app, HttpServletRequest request, EntityManager em) {

        // Cache applicationCache instances per request so the
        // allServicesAuthLastChanged date is not requested multiple times
        // for a single request

        // It is requested once per request, so the applicationCache is still
        // immediately refreshed once authorizations change for new requests

        Map<Long, ApplicationCache> requestCache = (Map<Long, ApplicationCache>)request.getAttribute(REQUEST_APP_CACHE);
        if(requestCache == null) {
            requestCache = new HashMap<>();
            request.setAttribute(REQUEST_APP_CACHE, requestCache);
        }
        ApplicationCache appCache = requestCache.get(app.getId());
        if(appCache == null) {
            appCache = getApplicationCache(app,em);
            requestCache.put(app.getId(),appCache);
        }
        return appCache;
    }

    public static boolean isAppLayerReadAuthorized(Application app, ApplicationLayer al, HttpServletRequest request, EntityManager em) {
        return isAppLayerReadAuthorized(app, al, request, getApplicationCacheFromRequest(app, request,em), em);
    }

    @Deprecated(since = "5.9.9")
    public static boolean isAppLayerReadAuthorized(Application app, ApplicationLayer al, HttpServletRequest request, ApplicationCache appCache, EntityManager em) {
        return isAppLayerReadAuthorized( app,  al, getRoles(request, em),  appCache,  em);
    }

    public static boolean isAppLayerReadAuthorized(Application app, ApplicationLayer al, Set<String> roles, ApplicationCache appCache, EntityManager em) {
        if(app == null || app.isAuthenticatedRequired() && (roles == null || roles.isEmpty())) {
            return false;
        }
        if(appCache == null) {
            appCache = getApplicationCache(app,em);
        }
        ReadWrite auths = appCache.protectedAppLayers.get(al.getId());
        return isReadAuthorized(roles, auths);
    }

    public static boolean isAppLayerWriteAuthorized(Application app, ApplicationLayer al, HttpServletRequest request, EntityManager em) {
        return isAppLayerWriteAuthorized(app, al, request, getApplicationCacheFromRequest(app, request, em), em);
    }

    public static boolean isAppLayerWriteAuthorized(Application app, ApplicationLayer al, HttpServletRequest request, ApplicationCache appCache, EntityManager em) {
        if(app == null || app.isAuthenticatedRequired() && request.getRemoteUser() == null) {
            return false;
        }

        if(appCache == null) {
            appCache = getApplicationCache(app, em);
        }
        ReadWrite auths = appCache.protectedAppLayers.get(al.getId());
        return isWriteAuthorized(getRoles( request,em), auths,em);
    }

    @Deprecated(forRemoval = true, since = "5.9.9")
    public static boolean isConfiguredComponentAuthorized(ConfiguredComponent component, HttpServletRequest request, EntityManager em) {
        return isConfiguredComponentAuthorized(component, getRoles(request, em), em);
    }

    public static boolean isConfiguredComponentAuthorized(ConfiguredComponent component, Set<String> userRoles, EntityManager em) {
        Application app = component.getApplication();
        Long appId = app.getId();

        Set<String> componentReaders;

        synchronized(appConfiguredComponentsReadersCache) {
            AppConfiguredComponentsReadersCache appCache = appConfiguredComponentsReadersCache.get(appId);

            if(appCache == null || appCache.modified.before(app.getAuthorizationsModified())) {

                appCache = new AppConfiguredComponentsReadersCache();
                appConfiguredComponentsReadersCache.put(appId, appCache);
                appCache.modified = component.getApplication().getAuthorizationsModified();
                appCache.readersByConfiguredComponentId = new HashMap<>();

                List<Object[]> readers = em.createQuery(
                          "select cc.id, r "
                        + "from ConfiguredComponent cc "
                        + "join cc.readers r "
                        + "where cc.application = :app")
                        .setParameter("app", component.getApplication())
                        .getResultList();
                for(Object[] row: readers) {
                    Long ccId = (Long)row[0];
                    String role = (String)row[1];
                    Set<String> roles = appCache.readersByConfiguredComponentId.computeIfAbsent(ccId, k -> new HashSet<>());
                    roles.add(role);
                }
            }
            componentReaders = appCache.readersByConfiguredComponentId.get(component.getId());
        }

        if(componentReaders == null) {
            componentReaders = EVERYBODY;
        }

        return isReadAuthorized(userRoles, new Read(componentReaders));
    }

    /**
     * Returns set of authorized readers and writers for this layer. If null is
     * returned, everyone is authorized for reading and writing. Note: even if
     * not null, the "readers" and "writers" properties of the returned
     * ReadWriteAuthorizations may be equal to EVERYONE.
     *
     * @param l the layer to check
     * @param em the entity manager to use
     * @return the authorizations
     */
    public static ReadWrite getLayerAuthorizations(Layer l, EntityManager em) {
        synchronized(LOCK) {
            GeoServiceCache cache = serviceCache.get(l.getService().getId());

            if(cache != null) {

                if(cache.modified.equals(l.getService().getAuthorizationsModified())) {
                    return cache.protectedLayers.get(l.getId());
                }
            }

            cache = new GeoServiceCache();
            serviceCache.put(l.getService().getId(), cache);
            cache.modified = l.getService().getAuthorizationsModified();
            cache.protectedLayers = new HashMap<>();

            List<Layer> layers = l.getService().loadLayerTree(em);
            if(!layers.isEmpty()) {
                // Prevent n+1 queries
                int i = 0;
                do {
                    List<Layer> subList = layers.subList(i, Math.min(layers.size(), i+ DB.MAX_LIST_EXPRESSIONS));
                    em.createQuery("from Layer l "
                            + "left join fetch l.readers "
                            + "left join fetch l.writers "
                            + "where l in (:layers)")
                            .setParameter("layers", subList)
                            .getResultList();
                    i += subList.size();
                } while(i < layers.size());
            }

            walkLayer(l.getService().getTopLayer(), EVERYBODY, EVERYBODY, cache.protectedLayers, em);

            return cache.protectedLayers.get(l.getId());
        }
    }

    /**
     * Apply the security inheritence rules.
     */
    private static Set<String> inheritAuthorizations(Set<String> current, Set<String> _new) {

        if(_new.equals(EVERYBODY)) {
            // must be copied on write
            return current;
        } else {

            if(current.equals(EVERYBODY)) {
                return new HashSet<>(_new);
            } else {
                HashSet<String> copy = new HashSet<>(current);
                copy.retainAll(_new);
                if(copy.isEmpty()) {
                    return NOBODY;
                } else {
                    return copy;
                }
            }
        }
    }

    private static void walkLayer(Layer l, Set<String> currentReaders, Set<String> currentWriters, Map<Long, ReadWrite> serviceProtectedLayers, EntityManager em) {

        currentReaders = inheritAuthorizations(currentReaders, l.getReaders());
        currentWriters = inheritAuthorizations(currentWriters, l.getWriters());

        if(!currentReaders.equals(EVERYBODY) || !currentWriters.equals(EVERYBODY)) {
            serviceProtectedLayers.put(l.getId(), new ReadWrite(currentReaders, currentWriters ));
        }

        for(Layer child: l.getCachedChildren(em)) {
            walkLayer(child, currentReaders, currentWriters, serviceProtectedLayers, em);
        }
    }

    public static ApplicationCache getApplicationCache(Application app, EntityManager em) {
        synchronized(LOCK) {
            ApplicationCache cache = applicationCache.get(app.getId());
            Date allServicesAuthLastChanged = null;
            if(cache != null) {
                // Check if the data was not cached before the authorizations
                // were modified
                if(!cache.modified.before(app.getAuthorizationsModified())) {

                    try {
                        // Because the cached data is also stale when authorizations
                        // for a service used in the application change, check if
                        // the cache was made before a change to authorizations to
                        // a service (any service, not only those used in app -
                        // checking only services used is not worth it because the
                        // authorizations for services should only change infrequently)

                        allServicesAuthLastChanged = (Date)em.createQuery("select max(authorizationsModified) from GeoService").getSingleResult();

                        if(allServicesAuthLastChanged != null && !cache.modified.before(allServicesAuthLastChanged)) {
                            return cache;
                        }
                    } catch(NoResultException nre) {
                        // no services apparently
                    }
                }
            }

            cache = new ApplicationCache();
            applicationCache.put(app.getId(), cache);
            if(allServicesAuthLastChanged != null ){
                cache.modified = allServicesAuthLastChanged.after(app.getAuthorizationsModified() ) ? allServicesAuthLastChanged : app.getAuthorizationsModified();
            }else{
                cache.modified = app.getAuthorizationsModified();
            }
            cache.protectedLevels = new HashMap<>();
            cache.protectedAppLayers = new HashMap<>();

            Application.TreeCache treeCache = app.loadTreeCache(em);
            treeCache.initializeLevels("left join fetch l.readers",em);
            treeCache.initializeApplicationLayers("left join fetch al.readers left join fetch al.writers",em);

            walkLevel(app.getRoot(), EVERYBODY, cache, treeCache, em);

            return cache;
        }
    }

    private static void walkLevel(Level l, Set<String> currentReaders, ApplicationCache cache, Application.TreeCache treeCache, EntityManager em) {
        currentReaders = inheritAuthorizations(currentReaders, l.getReaders());

        if(!currentReaders.equals(EVERYBODY)) {
            cache.protectedLevels.put(l.getId(), new Read(currentReaders));
        }

        for(ApplicationLayer al: l.getLayers()) {
            if(al != null) {
                walkAppLayer(al, currentReaders, cache,em);
            }
        }

        for(Level child: treeCache.getChildren(l)) {
            walkLevel(child, currentReaders, cache, treeCache, em);
        }
    }

    private static void walkAppLayer(ApplicationLayer al, Set<String> currentReaders, ApplicationCache cache, EntityManager em) {

        currentReaders = inheritAuthorizations(currentReaders, al.getReaders());

        // check the layer referenced by this appLayer
        Layer l = al.getService().getLayer(al.getLayerName(),em);

        Set<String> currentWriters = al.getWriters();

        if(l != null) {
            ReadWrite layerAuth = getLayerAuthorizations(l,em);
            if(layerAuth != null) {
                currentReaders = inheritAuthorizations(currentReaders, layerAuth.readers);
                currentWriters = inheritAuthorizations(currentWriters, layerAuth.writers);
            }
        }

        if(!currentReaders.equals(EVERYBODY) || !currentWriters.equals(EVERYBODY)) {
            cache.protectedAppLayers.put(al.getId(), new ReadWrite(currentReaders, currentWriters));
        }
    }

    public static boolean isLayerReadAuthorized(Layer l, HttpServletRequest request, EntityManager em) {
        return isReadAuthorized(getRoles(request, em), getLayerAuthorizations(l,em));
    }

    public static boolean isLayerWriteAuthorized(Layer l, HttpServletRequest request, EntityManager em) {
        return isWriteAuthorized(getRoles(request, em), getLayerAuthorizations(l,em),em);
    }

    /**
     * See if a user can edit geometry attribute of a layer in addition to
     * regular writing. Calling this will also call
     * {@link AuthorizationsHelper#isAppLayerWriteAuthorized(Application, ApplicationLayer, HttpServletRequest, EntityManager)}
     *
     * @param l the layer
     * @param request the servlet request that has the user credential
     * @param em the entity manager to use
     * @return {@code true} if the user is allowed to edit the geometry
     * attribute of the layer (the user is not in any of the groups that prevent
     * editing geometry).
     */
    public static boolean isLayerGeomWriteAuthorized(Layer l, HttpServletRequest request, EntityManager em) {
        if (isLayerWriteAuthorized(l, request,em)) {
            Set<String> preventEditGeomGroup = l.getPreventGeomEditors();
            for (String group : preventEditGeomGroup) {
                if (request.isUserInRole(group)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isApplicationReadAuthorized(Application app, Set<String> roles, EntityManager em) {
        Read auths = new Read(app.getReaders());
        return isReadAuthorized(roles, auths);
    }

    public static class GeoServiceCache {
        Date modified;
        Map<Long, ReadWrite> protectedLayers;

        public Map<Long, ReadWrite> getProtectedLayers() {
            return protectedLayers;
        }

        public Date getModified() {
            return modified;
        }
    }

    public static class ApplicationCache {
        Date modified;
        Map<Long, Read> protectedLevels;
        Map<Long, ReadWrite> protectedAppLayers;

        public Date getModified() {
            return modified;
        }

        public Map<Long, ReadWrite> getProtectedAppLayers() {
            return protectedAppLayers;
        }

        public Map<Long, Read> getProtectedLevels() {
            return protectedLevels;
        }
    }

    public static class AppConfiguredComponentsReadersCache {
        Date modified;

        Map<Long, Set<String>> readersByConfiguredComponentId;
    }

    public static class Read {
        Set<String> readers;

        public Read(Set<String> readers) {
            this.readers = readers;
        }

        public Set<String> getReaders() {
            return readers;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            JSONArray jReaders = new JSONArray(readers);
            obj.put("readers", jReaders);
            return obj;
        }
    }

    public static class ReadWrite extends Read {
        Set<String> writers;

        public ReadWrite(Set<String> readers, Set<String> writers) {
            super(readers);
            this.writers = writers;
        }

        public Set<String> getWriters() {
            return writers;
        }

        public JSONObject toJSON() throws JSONException{
            JSONObject obj = new JSONObject();
            JSONArray jWriters = new JSONArray(writers);
            JSONArray jReaders = new JSONArray(readers);
            obj.put("readers", jReaders);
            obj.put("writers", jWriters);
            return obj;
        }
    }
}
