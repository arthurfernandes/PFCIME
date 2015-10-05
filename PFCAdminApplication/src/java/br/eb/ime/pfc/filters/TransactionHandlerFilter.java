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
import br.eb.ime.pfc.hibernate.HibernateUtil;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.context.internal.ManagedSessionContext;

/**
 *
 * @author arthurfernandes
 */
@WebFilter(filterName = "TransactionHandlerFilter", servletNames={"LoginServlet","ListLayersServlet","MapServlet","LayerHandlerServlet","AccessLevelHandlerServlet","UserHandlerServlet"})
public class TransactionHandlerFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(HibernateUtil.class.getName());
    
    private FilterConfig filterConfig = null;
    private Session session = null;
    
    public TransactionHandlerFilter() {
    }    
    
    private boolean doBeforeProcessing(ServletRequest request, ServletResponse response){
        boolean createdSession = false;
        
        LOGGER.log(Level.CONFIG,"TransactionHandlerFilter:DoBeforeProcessing");
        
	try{
            this.session = HibernateUtil.openSession();   
            session.beginTransaction();
            ManagedSessionContext.bind(session);
            createdSession = true;
        }
        catch(Throwable e){
            LOGGER.log(Level.SEVERE,"Could not retrieve session Factory",e);
        }
        return createdSession;
    }    
    
    private void doAfterProcessing(ServletRequest request, ServletResponse response){
        
        LOGGER.log(Level.CONFIG,"TransactionHandlerFilter:DoAfterProcessing");
        
        if(this.session != null){
            ManagedSessionContext.unbind(this.session.getSessionFactory());
            this.session.flush();
            try{
                this.session.getTransaction().commit();
            }
            catch(HibernateException e){
                this.session.getTransaction().rollback();
            }
            finally{
                this.session.close();
            }
        }
    }

    /**
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param chain The filter chain we are processing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        LOGGER.log(Level.CONFIG,"TransactionHandlerFilter:doFilter()");
        
        final boolean createdSession = doBeforeProcessing(request, response);
        Throwable problem = null;
        
        if(createdSession){
            try {
                chain.doFilter(request, response);
            } catch (Throwable t) {
                /* If an exception is thrown somewhere down the filter chain,
                 we still want to execute our after processing, and then
                 rethrow the problem after that.*/
                problem = t;
            }
        }
        else{
            httpResponse.sendError(HTTP_STATUS.INTERNAL_ERROR.getCode());
        }
        
        doAfterProcessing(request, response);

	/* If there was a problem, we want to rethrow it if it is
        / a known type, otherwise log it.*/
        if (problem != null) {
            if (problem instanceof ServletException) {
                throw (ServletException) problem;
            }
            if (problem instanceof IOException) {
                throw (IOException) problem;
            }
            LOGGER.log(Level.SEVERE,"Unknown exception in TransactionHandlerFilter",problem);
        }
    }

    /**
     * Destroy method for this filter
     */
    @Override
    public void destroy() {        
    }

    /**
     * Init method for this filter
     * @param filterConfig
     */
    @Override
    public void init(FilterConfig filterConfig) {        
        this.filterConfig = filterConfig;
    }

    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {
        if (filterConfig == null) {
            return ("TransactionHandlerFilter()");
        }
        StringBuilder sb = new StringBuilder("TransactionHandlerFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }
}
