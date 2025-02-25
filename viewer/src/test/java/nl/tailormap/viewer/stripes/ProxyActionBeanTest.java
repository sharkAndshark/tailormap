/*
 * Copyright (C) 2017 B3Partners B.V.
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

import net.sourceforge.stripes.action.ActionBeanContext;
import nl.tailormap.commons.HttpClientConfigured;
import nl.tailormap.viewer.config.security.User;
import nl.tailormap.viewer.util.TestActionBeanContext;
import nl.tailormap.viewer.util.TestUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mark Prins
 */
public class ProxyActionBeanTest extends TestUtil {

    private ActionBeanContext context;
    private ProxyActionBean ab;

    public ProxyActionBeanTest() {
    }

    // test of beveiligde service met gebruiker met onvoldoende rechten geen user/pass heeft
    @Test
    public void testSecureServiceNoRights() throws MalformedURLException {
        User geb = null;
        String url = "http://x12.b3p.nl/cgi-bin/mapserv?map=/srv/maps/solparc/groen_productie.map&";
        context = new TestActionBeanContext(geb);
        ab = new ProxyActionBean();
        ab.setContext(context);
        ab.setUrl(url);
        ab.setMustLogin(true);
        ab.setServiceId(2L);

        HttpClientConfigured client = ab.getHttpClient(new URL(url), entityManager);

        assertNull(client.getPassword());
        assertNull(client.getUsername());
    }

    // test of beveiligde service met gebruiker met onvoldoende rechten geen user/pass heeft
    @Test
    @Disabled(value = "lalal, stupid refactoring")
    public void testSecureServiceWrongRights() throws MalformedURLException {
        User geb = entityManager.find(User.class, "pietje");
        String url = "http://x12.b3p.nl/cgi-bin/mapserv?map=/srv/maps/solparc/groen_productie.map&";
        context = new TestActionBeanContext(geb);
        ab = new ProxyActionBean();
        ab.setContext(context);
        ab.setUrl(url);
        ab.setMustLogin(true);
        ab.setServiceId(2L);

        HttpClientConfigured client = ab.getHttpClient(new URL(url), entityManager);

        assertNull(client.getPassword());
        assertNull(client.getUsername());
    }


    // test of beveiligde service met voloende rechten wel user pass heeft
    @Test
    @Disabled(value = "lalal, stupid refactoring")
    public void testSecureServiceRights() throws MalformedURLException{
        User geb = entityManager.find(User.class, "admin");
        String url = "http://x12.b3p.nl/cgi-bin/mapserv?map=/srv/maps/solparc/groen_productie.map&";
        context = new TestActionBeanContext(geb);
        ab = new ProxyActionBean();
        ab.setContext(context);
        ab.setUrl(url);
        ab.setMustLogin(true);
        ab.setServiceId(2L);

        HttpClientConfigured client = ab.getHttpClient(new URL(url), entityManager);

        assertNotNull(client.getPassword());
        assertNotNull(client.getUsername());
    }

    // test of url van service uit db gebruikt wordt (en dus niet aangepast kan worden)
    @Test
    public void testModifiedServiceUrl() throws MalformedURLException, UnsupportedEncodingException, IllegalAccessException {
        User geb = entityManager.find(User.class, "admin");
        String url = "http://fakeurl.com?map=/srv/maps/solparc/groen_productie.map&";
        String originalUrl = "http://x12.b3p.nl/cgi-bin/mapserv?map=/srv/maps/solparc/groen_productie.map&";
        context = new TestActionBeanContext(geb);
        ab = new ProxyActionBean();
        ab.setContext(context);
        ab.setUrl(url);
        ab.setMustLogin(true);
        ab.setServiceId(2L);
        URL u = ab.getRequestRL(entityManager);
        String real = u.toString();
        assertTrue(real.contains(originalUrl));
    }

    /**
     * test of getlegend met scale param werkt.
     *
     * @throws java.lang.Exception if any
     */
    @Test
    public void testScaledLegendUrl() throws Exception {
        User geb = entityManager.find(User.class, "admin");
        String legendUrl = "https://flamingo5.b3p.nl/geoserver/test_omgeving_fla5/wms?request=GetLegendGraphic&format=image%2Fpng&width=20&height=20&layer=antenne_register&SERVICE=WMS&SCALE=3.167838379715733";
        context = new TestActionBeanContext(geb);
        ab = new ProxyActionBean();
        ab.setContext(context);
        ab.setUrl(legendUrl);
        ab.setMustLogin(true);
        ab.setServiceId(4L);
        URL u = ab.getRequestRL(entityManager);
        String real = u.toString();
        assertTrue(real.contains("SCALE"), "The url must contain the SCALE param");
    }
}
