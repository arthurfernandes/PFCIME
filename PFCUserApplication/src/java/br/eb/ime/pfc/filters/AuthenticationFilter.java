/*
 * The MIT License
 *
 * Copyright 2015 arthurfernandes.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package br.eb.ime.pfc.filters;

import br.eb.ime.pfc.domain.HTTP_STATUS;
import br.eb.ime.pfc.domain.Layer;
import br.eb.ime.pfc.domain.ObjectNotFoundException;
import br.eb.ime.pfc.domain.User;
import br.eb.ime.pfc.domain.UserManager;
import br.eb.ime.pfc.hibernate.HibernateUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.context.internal.ManagedSessionContext;

/**
 *
 * This class is a Filter responsible to intercept requests and verify whether 
 * the user is logged in this session or not.
 * 
 * If the user is logged in this Filter it will chain to other filters or servlets,
 * otherwise it will try to find out if the user sends http basic authorization headers
 * to log in the user.
 * If no data is found regarding the basic http authorization or the user:password,
 * is not a match a 403 Http Error Code is sent to the user.
 */
@WebFilter(filterName = "AuthenticationFilter", servletNames = {"MapServlet","WMSProxyServlet","LegendGraphicServlet","ListLayersServlet"})
public class AuthenticationFilter implements Filter{
    private static final Logger LOGGER = Logger.getLogger(AuthenticationFilter.class.getName());

    private FilterConfig filterConfig = null;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        //Creates session for user if it wasn't created before with (true)
        final HttpSession httpSession = httpRequest.getSession(true);
        final String username = (String) httpSession.getAttribute("user");
        if(username == null){
            HttpServletResponse resp = (HttpServletResponse) response;
            try{
                final boolean basicAuthorized = this.basicAuthentication(httpRequest,httpResponse);
                if(basicAuthorized){
                    LOGGER.log(Level.CONFIG, "BASIC HTTP AUTHORIZATION for: {0}", username);
                    chain.doFilter(request, response);
                }
                else{
                    resp.sendError(HTTP_STATUS.UNAUTHORIZED.getCode());
                }
            }
            catch(HibernateException e){
                resp.sendError(HTTP_STATUS.INTERNAL_ERROR.getCode());
            }
        }
        else{
            chain.doFilter(request, response);
        }
    }
    
    public static boolean authenticateUser(HttpServletRequest request, String username,String password) throws HibernateException{
        if(!User.isValid(username)){
            return false;
        }
        //Try to retrieve user from database
        Session session = null;
        try{
            session = HibernateUtil.getCurrentSession();
        }
        catch(Throwable ex){
            throw new HibernateException("Could not create session for user.");
        }
        
        final UserManager userManager = new UserManager(session);
        try{
            User user = userManager.getById(username);
        
            if(user.authenticatePassword(password)){
                Set<String> layersIds = new HashSet<>();
                for(Layer layer : user.getAccessLevel().getLayers()){
                    layersIds.add(layer.getWmsId());
                }
                request.getSession().setAttribute("user", username);
                request.getSession().setAttribute("layers", layersIds);
                return true;
            }
        }
        catch(ObjectNotFoundException e){
            return false;
        }
        return false;
    }
    
    protected boolean basicAuthentication(HttpServletRequest request,HttpServletResponse response) throws HibernateException{
        final Enumeration<String> headers = request.getHeaderNames();
        if(headers != null){
            String authorizationHeader = null;
            while(headers.hasMoreElements()){
                final String header = headers.nextElement();
                if(header.equalsIgnoreCase("authorization")){
                    authorizationHeader = request.getHeader(header);
                    break;
                }
            }
            if(authorizationHeader == null || !authorizationHeader.toUpperCase().contains("BASIC")){
                return false;
            }
            else{
                request.getServletContext().log("HEADER"+authorizationHeader);
                final String base64EncodedData = authorizationHeader.replace("Basic", "").replace(" ", "");
                byte[] decodedData = DatatypeConverter.parseBase64Binary(base64EncodedData);
                final String decodedString;
                try {
                    decodedString = new String(decodedData,"UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    return false;
                }
                final String[] usernamePasswordArray = decodedString.split(":");
                if(usernamePasswordArray.length != 2){
                    return false;
                }
                else{
                    final String username = usernamePasswordArray[0];
                    final String password = usernamePasswordArray[1];
                    request.getServletContext().log("USERNAME"+username);
                    request.getServletContext().log("PASSWORD"+password);
                    Session session = createSessionForBasicAuthentication();
                    try{
                        boolean isAuthorized = AuthenticationFilter.authenticateUser(request,username, password);
                        return isAuthorized;
                    }
                    finally{
                        session.close();
                    }
                }
            }
        }
        else{
            return false;
        }
    }
    
    protected Session createSessionForBasicAuthentication(){
        Session session = null;
        try{
            session = HibernateUtil.openSession();   
            session.beginTransaction();
            ManagedSessionContext.bind(session);
        }
        catch(Throwable e){
            LOGGER.log(Level.SEVERE,"Could not retrieve session Factory for basic http authorization",e);
            throw new HibernateException("Could not create session for basic http authorization");
        }
        return session;
    }
    
    @Override
    public void destroy() {        
    }
    
}
