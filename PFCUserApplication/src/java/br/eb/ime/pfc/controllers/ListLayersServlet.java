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
package br.eb.ime.pfc.controllers;

import br.eb.ime.pfc.domain.AccessLevel;
import br.eb.ime.pfc.domain.Layer;
import br.eb.ime.pfc.domain.ObjectNotFoundException;
import br.eb.ime.pfc.domain.User;
import br.eb.ime.pfc.domain.UserManager;
import br.eb.ime.pfc.hibernate.HibernateUtil;
import flexjson.JSONSerializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;

/**
 *
 * This class is responsible for listing the WMS layers accessed by the user and 
 * its features as a JSON object.
 * 
 */
@WebServlet(name = "ListLayersServlet", urlPatterns = {"/layers"})
public class ListLayersServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final String username = (String) request.getSession().getAttribute("user");
        if(username != null){
            try{
                final UserManager userManager = new UserManager(HibernateUtil.getCurrentSession());
                final User user = userManager.getById(username);
                final AccessLevel accessLevel = user.getAccessLevel();
                Hibernate.initialize(accessLevel);

                final Collection<Layer> layers = accessLevel.getLayers();
                final List<Layer> orderedLayers = new ArrayList<>();
                for(Layer layer : layers){
                    Hibernate.initialize(layer);
                    orderedLayers.add(layer);
                    request.getServletContext().log(layer.getName());
                }
                
                Collections.sort(orderedLayers,new Comparator<Layer>(){
                    @Override
                    public int compare(Layer o1, Layer o2) {
                        return o1.getWmsId().compareTo(o2.getWmsId());
                    }
                });
                
                JSONSerializer serializer = new JSONSerializer();
                response.setContentType("application/json");
                serializer.rootName("layers").
                    include("features").
                    exclude("*.class").serialize(orderedLayers,response.getWriter());
            }
            catch(HibernateException e){
                e.printStackTrace();
                response.sendError(500);
                return;
            }
            catch(ObjectNotFoundException e){
                response.sendError(403);
                return;
            }     
        }
        else{
            response.sendError(403); //User has no permission to access the resource.
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
