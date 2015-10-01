/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.jersey;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * @author daniele
 */
public class BaseSecurityContext implements SecurityContext {

    @Override
    public boolean isUserInRole(final String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }

}
