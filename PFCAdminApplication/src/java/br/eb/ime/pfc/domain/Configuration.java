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
package br.eb.ime.pfc.domain;

import flexjson.JSONDeserializer;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 *
 * @author arthurfernandes
 */
public class Configuration {
    private Collection<Layer> layers;
    private Collection<AccessLevel> accessLevels;
    private Collection<User> users;
    
    public Configuration(){
        layers = new LinkedHashSet<>();
        accessLevels = new LinkedHashSet<>();
        users = new LinkedHashSet<>();
    }
    
    public void addLayers(Collection layers){
        layers.addAll(layers);
    }
    
    public void addAccessLevels(Collection accessLevels){
        accessLevels.addAll(accessLevels);
    }
    
    public void addUsers(Collection users){
        users.addAll(users);
    }
    
    public static String jsonSerialize(Configuration config){
        return "";
    }
    
    public static Configuration jsonDeserialize(String jsonString){
        return new Configuration();
    }
    
    public static void main(String args[]){
        JSONDeserializer deserializer;
        
    }
}
