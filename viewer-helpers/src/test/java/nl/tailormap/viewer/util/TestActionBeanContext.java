package nl.tailormap.viewer.util;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockServletContext;
import nl.tailormap.viewer.config.security.Group;
import nl.tailormap.viewer.config.security.User;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
public class TestActionBeanContext extends ActionBeanContext {

    private User user = null;

    public TestActionBeanContext() {

    }

    public TestActionBeanContext(User user) {
        this.user = user;
    }

    /**
     * Retrieves the HttpServletRequest object that is associated with the current request.
     *
     * @return HttpServletRequest the current request
     */
    @Override
    public HttpServletRequest getRequest() {
        MockHttpSession session = new MockHttpSession(new MockServletContext("test"));
        MockHttpServletRequest request = new MockHttpServletRequest("", "");
        request.setUserPrincipal(user);
        request.setSession(session);
        if (user != null) {
            Set<String> roles = new HashSet<>();
            for (Group group : user.getGroups()) {
                roles.add(group.getName());
            }
            request.setRoles(roles);
        }
        return request;
    }

    @Override
    public List<Message> getMessages() {
        return new ArrayList<>();
    }
}
