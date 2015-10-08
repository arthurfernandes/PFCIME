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
package br.eb.ime.pfc.geoserver;

import br.eb.ime.pfc.domain.HTTP_STATUS;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.decoder.RESTLayerList;
import it.geosolutions.geoserver.rest.decoder.RESTStyleList;
import it.geosolutions.geoserver.rest.decoder.utils.NameLinkElem;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author arthurfernandes
 */
public class GeoServerCommunication {
    //private final static String GEOSERVER_URL = "http://ec2-54-94-206-253.sa-east-1.compute.amazonaws.com/geoserver";
    private final static String GEOSERVER_URL = "http://localhost:9090";
    private final static String GEOSERVER_RESTUSER = "admin";
    private final static String GEOSERVER_RESTPW = "geoserver";
    private final static String BASE64_AUTHORIZATION = new String(Base64.encodeBase64((GEOSERVER_RESTUSER + ":" + GEOSERVER_RESTPW).getBytes()));
    
    private final GeoServerRESTReader reader;
    private final GeoServerRESTPublisher publisher;
    
    public static GeoServerCommunication makeGeoserverCommunication() throws GeoserverCommunicationException{
        GeoServerRESTReader reader = null;
        GeoServerRESTPublisher publisher = null;
        
        try{
            reader = new GeoServerRESTReader(GEOSERVER_URL, GEOSERVER_RESTUSER, GEOSERVER_RESTPW);
            publisher = new GeoServerRESTPublisher(GEOSERVER_URL, GEOSERVER_RESTUSER, GEOSERVER_RESTPW);
        }
        catch(MalformedURLException | IllegalArgumentException e){
            throw new GeoserverCommunicationException("Could not stablish GeoserverCommunication due to malformed URL: "+GEOSERVER_URL);
        }
        if((reader != null) && (publisher != null)){
            return new GeoServerCommunication(reader,publisher);
        }
        else{
            throw new GeoserverCommunicationException("Could not stablish GeoserverCommunication due to unknown Problem.");
        }
    }
    
    private static void sendError(HTTP_STATUS status,HttpServletResponse response){
        try(Writer writer = response.getWriter()){
            response.sendError(status.getCode());
        }
        catch(IOException e){
        }
    }
    
    public static void redirectStreamFromRequest(HttpServletRequest request,HttpServletResponse response){
        
        final String urlName = GEOSERVER_URL + request.getRequestURI().replace(request.getContextPath()+"/geoserver","")+ "?" +request.getQueryString();
        request.getServletContext().log("URL"+urlName);
        request.getServletContext().log("CONTEXT"+request.getContextPath());
        request.getServletContext().log("URL"+request.getRequestURI());
        redirectStream(urlName,request,response);
    }
    
    public static void getLegendGraphic(String layerId,int width,int height,HttpServletRequest request,HttpServletResponse response){
        final String urlName = GEOSERVER_URL + "/wms?" + "REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH="
                + width+"&HEIGHT="+height+"&LAYER="+layerId + "&EXCEPTIONS=application/vnd.ogc.se_blank";
        redirectStream(urlName,request,response);
    }
    
    private static void redirectStream(String urlName,HttpServletRequest request, HttpServletResponse response){
        URL url = null;
        try{
            url = new URL(urlName);
        }
        catch(MalformedURLException e){
            //Internal error, the user will receive no data.
            sendError(HTTP_STATUS.BAD_REQUEST,response);
            return;
        }
        HttpURLConnection conn = null;
        try{
            conn = (HttpURLConnection) url.openConnection();
            conn.addRequestProperty("Authorization", "Basic "+BASE64_AUTHORIZATION);
            //conn.setRequestMethod("GET");
            //conn.setDoOutput(true);
            conn.connect();
        }
        catch(IOException e){
            sendError(HTTP_STATUS.INTERNAL_ERROR,response);
            return;
        }
        
        try(InputStream is = conn.getInputStream();OutputStream os = response.getOutputStream()){
            response.setContentType(conn.getContentType());
            IOUtils.copy(is, os);
        }
        catch(IOException e){
            request.getServletContext().log("IO");
            sendError(HTTP_STATUS.INTERNAL_ERROR,response);
            return;
        }
        finally{ //Close connection to save resources
            conn.disconnect();
        }
    }
    
    private GeoServerCommunication(GeoServerRESTReader reader, GeoServerRESTPublisher publisher){    
        this.reader = reader;
        this.publisher = publisher;
    }
    
    public boolean existsGeoserver(){
        return this.reader.existGeoserver();
    }
    
    public List<String> getLayerNames() throws GeoserverCommunicationException{
        if(this.reader.existGeoserver()){
            final List<String> layerNames = new ArrayList<>();
            final RESTLayerList restLayerList = this.reader.getLayers();

            if(restLayerList == null){
                throw new GeoserverCommunicationException("Communication issue with Geoserver REST API at:"+GEOSERVER_URL);
            }
            for(NameLinkElem elem : restLayerList){
                layerNames.add(elem.getName());
            }
            return layerNames;
        }
        else{
            throw new GeoserverCommunicationException("Could not establish REST Communication with Server at +"+GEOSERVER_URL);
        }
    }
    
    public List<String> getStyleNames(){
        if(this.reader.existGeoserver()){
            final List<String> styleNames = new ArrayList<>();
            final RESTStyleList restStyleList = this.reader.getStyles();
            if(restStyleList == null){
                throw new GeoserverCommunicationException("Communication issue with Geoserver REST API at:"+GEOSERVER_URL);
            }
            for(NameLinkElem elem : restStyleList){
                styleNames.add(elem.getName());
            }
            return styleNames;
        }
        else{
            throw new GeoserverCommunicationException("Could not establish REST Communication with Server at +"+GEOSERVER_URL);
        }
    }
    
    public boolean setDefaultStyleToLayer(String workspace,String layer,String style){
        RESTLayer restLayer = reader.getLayer(workspace,layer);
        if(restLayer == null){
            return false;
        }
        else{
            final GSLayerEncoder layerEnc = new GSLayerEncoder();
            layerEnc.addStyle(style);
            layerEnc.setDefaultStyle(style);
            return publisher.configureLayer(workspace, layer, layerEnc);
        }
    }
    
    public boolean existsStyle(String styleName){
        return reader.existsStyle(styleName);
    }
    
    public boolean addStyle(String name,String resourceURL,String format,Integer size){
        return publisher.publishStyle(this.getSLDFileBody(resourceURL, format, size), name);
    }

    public boolean removeStyle(String styleName){
        return publisher.removeStyle(styleName);
    }
    
    public boolean updateStyle(String name,String resourceURL,String format,Integer size){
        return publisher.updateStyle(this.getSLDFileBody(resourceURL, format, size), name);
    }
    
    private String getSLDFileBody(String resourceURL,String format,Integer size){
        final String[] strSLDArray = {
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n",
            "<StyledLayerDescriptor version=\"1.0.0\"\n",
            "xsi:schemaLocation=\"http://www.opengis.net/sld StyledLayerDescriptor.xsd\"\n",
            "xmlns=\"http://www.opengis.net/sld\"\n",
            "xmlns:ogc=\"http://www.opengis.net/ogc\"\n",
            "xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n",
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n",
                "<NamedLayer>\n",
                    "<Name>Point as graphic</Name>\n",
                    "<UserStyle>\n",
                        "<Title>Point as Graphic</Title>\n",
                        "<FeatureTypeStyle>\n",
                            "<Rule>\n",
                                "<PointSymbolizer>\n",
                                    "<Graphic>\n",
                                        "<ExternalGraphic>\n",
                                        "<OnlineResource xlink:type=\"simple\"\n",
                                        "xlink:href=\""+resourceURL+ "\"/>\n",
                                        "<Format>"+format+"</Format>\n",
                                        "</ExternalGraphic>\n",
                                        "<Size>"+size+"</Size>\n",
                                    "</Graphic>\n",
                                "</PointSymbolizer>\n",
                            "</Rule>\n",
                        "</FeatureTypeStyle>\n",
                    "</UserStyle>\n",
                "</NamedLayer>\n",
            "</StyledLayerDescriptor>\n"
        };
        final StringBuilder builder = new StringBuilder();
        for(String str : strSLDArray){
            builder.append(str);
        }
        return builder.toString();
    }
    
    public static class GeoserverCommunicationException extends RuntimeException{
        private static final long serialVersionUID = 1L;
        
        /**
         * Creates a GeoserverCommunicationException with a detail message.
         * @param message 
         * The message that specify the error.
         */
        public GeoserverCommunicationException(String message){
            super(message);
        }
    }
    
    public static String[] fileArray(){
                String fileArray[] = {
        "accommodation/accommodation_alpinehut",
        "accommodation/accommodation_bed_and_breakfast",
        "accommodation/accommodation_camping",
        "accommodation/accommodation_caravan_park",
        "accommodation/accommodation_hotel",
        "accommodation/accommodation_hotel2",
        "accommodation/accommodation_house",
        "accommodation/accommodation_shelter",
        "accommodation/accommodation_shelter2",
        "accommodation/accommodation_youth_hostel",
        "amenity/amenity_bench",
        "amenity/amenity_court",
        "amenity/amenity_firestation",
        "amenity/amenity_firestation2",
        "amenity/amenity_firestation3",
        "amenity/amenity_fountain",
        "amenity/amenity_information",
        "amenity/amenity_library",
        "amenity/amenity_police",
        "amenity/amenity_police2",
        "amenity/amenity_post_box",
        "amenity/amenity_post_office",
        "amenity/amenity_prison",
        "amenity/amenity_recycling",
        "amenity/amenity_survey_point",
        "amenity/amenity_telephone",
        "amenity/amenity_toilets",
        "amenity/amenity_toilets_disabled",
        "amenity/amenity_toilets_men",
        "amenity/amenity_toilets_women",
        "amenity/amenity_waste_bin",
        "arrows/Arrow_01",
        "arrows/Arrow_02",
        "arrows/Arrow_03",
        "arrows/Arrow_04",
        "arrows/Arrow_05",
        "arrows/Arrow_06",
        "arrows/NorthArrow_01",
        "arrows/NorthArrow_02",
        "arrows/NorthArrow_03",
        "arrows/NorthArrow_04",
        "arrows/NorthArrow_05",
        "arrows/NorthArrow_06",
        "arrows/NorthArrow_07",
        "arrows/NorthArrow_08",
        "arrows/NorthArrow_09",
        "arrows/NorthArrow_10",
        "arrows/NorthArrow_11",
        "backgrounds/background_circle",
        "backgrounds/background_forbidden",
        "backgrounds/background_octogon",
        "backgrounds/background_safety",
        "backgrounds/background_security",
        "backgrounds/background_security_02",
        "backgrounds/background_square",
        "backgrounds/background_square_rounded",
        "backgrounds/background_tilted_square",
        "backgrounds/background_tilted_square_rounded",
        "backgrounds/background_triangle",
        "components/component_indoor",
        "crosses/Cross1",
        "crosses/Cross2",
        "crosses/Cross4",
        "crosses/Cross5",
        "crosses/Cross6",
        "crosses/Star1",
        "crosses/Star2",
        "crosses/Star3",
        "emergency/amenity=fire_station",
        "emergency/amenity=hospital",
        "emergency/amenity=police",
        "entertainment/amenity=bar",
        "entertainment/amenity=cafe",
        "entertainment/amenity=cinema",
        "entertainment/amenity=fast_food",
        "entertainment/amenity=pub",
        "entertainment/amenity=restaurant",
        "entertainment/amenity=theatre",
        "food/food_bar",
        "food/food_cafe",
        "food/food_drinkingtap",
        "food/food_fastfood",
        "food/food_fastfood2",
        "food/food_pub",
        "food/food_restaurant",
        "gpsicons/anchor",
        "gpsicons/bank",
        "gpsicons/boat",
        "gpsicons/camera",
        "gpsicons/car",
        "gpsicons/city_building",
        "gpsicons/city_large",
        "gpsicons/city_medium",
        "gpsicons/city_small",
        "gpsicons/conveneince",
        "gpsicons/couple",
        "gpsicons/cross",
        "gpsicons/deer",
        "gpsicons/dollar",
        "gpsicons/fish",
        "gpsicons/flag",
        "gpsicons/food",
        "gpsicons/gas",
        "gpsicons/golf",
        "gpsicons/h",
        "gpsicons/house",
        "gpsicons/parachute",
        "gpsicons/parking",
        "gpsicons/phone",
        "gpsicons/plane",
        "gpsicons/plane_orange",
        "gpsicons/point",
        "gpsicons/question",
        "gpsicons/shipwreck",
        "gpsicons/skier",
        "gpsicons/skull",
        "gpsicons/swimmer",
        "gpsicons/table",
        "gpsicons/teepee",
        "gpsicons/tree",
        "gpsicons/walker",
        "gpsicons/waypoint",
        "health/health_dentist",
        "health/health_doctors",
        "health/health_hospital",
        "health/health_hospital_emergency",
        "health/health_hospital_emergency2",
        "health/health_opticians",
        "health/health_pharmacy",
        "health/health_veterinary",
        "landmark/amenity=place_of_worship",
        "landmark/amenity=school",
        "landmark/religion=buddhist",
        "landmark/religion=christian",
        "landmark/religion=hindu",
        "landmark/religion=jewish",
        "landmark/religion=muslim",
        "landmark/religion=pastafarian",
        "landmark/religion=sikh",
        "landmark/tourism=museum",
        "money/money_atm",
        "money/money_atm2",
        "money/money_bank2",
        "money/money_currency_exchange",
        "religion/place_of_worship",
        "religion/place_of_worship_bahai",
        "religion/place_of_worship_bahai3",
        "religion/place_of_worship_buddhist",
        "religion/place_of_worship_buddhist3",
        "religion/place_of_worship_christian",
        "religion/place_of_worship_christian3",
        "religion/place_of_worship_hindu",
        "religion/place_of_worship_hindu3",
        "religion/place_of_worship_islamic",
        "religion/place_of_worship_islamic3",
        "religion/place_of_worship_jain",
        "religion/place_of_worship_jain3",
        "religion/place_of_worship_jewish",
        "religion/place_of_worship_jewish3",
        "religion/place_of_worship_shinto",
        "religion/place_of_worship_shinto3",
        "religion/place_of_worship_sikh",
        "religion/place_of_worship_sikh3",
        "religion/place_of_worship_unknown3",
        "services/amenity=atm",
        "services/amenity=pharmacy,dispensing=yes",
        "services/amenity=pharmacy",
        "services/amenity=post_box",
        "services/amenity=recycling",
        "services/amenity=telephone",
        "services/shop=convenience",
        "services/shop=supermarket",
        "services/tourism=hotel",
        "shopping/shopping_alcohol",
        "shopping/shopping_bakery",
        "shopping/shopping_bicycle",
        "shopping/shopping_book",
        "shopping/shopping_butcher",
        "shopping/shopping_car",
        "shopping/shopping_car_repair",
        "shopping/shopping_clothes",
        "shopping/shopping_confectionery",
        "shopping/shopping_convenience",
        "shopping/shopping_diy",
        "shopping/shopping_estateagent",
        "shopping/shopping_estateagent2",
        "shopping/shopping_fish",
        "shopping/shopping_garden_centre",
        "shopping/shopping_gift",
        "shopping/shopping_greengrocer",
        "shopping/shopping_hairdresser",
        "shopping/shopping_hifi",
        "shopping/shopping_jewelry",
        "shopping/shopping_laundrette",
        "shopping/shopping_mobile_phone",
        "shopping/shopping_motorcycle",
        "shopping/shopping_music",
        "shopping/shopping_pet",
        "shopping/shopping_pet2",
        "shopping/shopping_photo",
        "shopping/shopping_supermarket",
        "shopping/shopping_tackle",
        "shopping/shopping_video_rental",
        "sport/sport_archery",
        "sport/sport_baseball",
        "sport/sport_cricket",
        "sport/sport_diving",
        "sport/sport_golf",
        "sport/sport_gym",
        "sport/sport_gymnasium",
        "sport/sport_gymnasium2",
        "sport/sport_hillclimbing",
        "sport/sport_horse_racing",
        "sport/sport_iceskating",
        "sport/sport_jetski",
        "sport/sport_leisure_centre",
        "sport/sport_motorracing",
        "sport/sport_playground",
        "sport/sport_sailing",
        "sport/sport_skiing_crosscountry",
        "sport/sport_skiing_downhill",
        "sport/sport_snooker",
        "sport/sport_soccer",
        "sport/sport_swimming_indoor",
        "sport/sport_swimming_outdoor",
        "sport/sport_tennis",
        "sport/sport_windsurfing",
        "symbol/blue-marker",
        "symbol/education_nursery",
        "symbol/education_school",
        "symbol/education_university",
        "symbol/fountain",
        "symbol/landuse_coniferous",
        "symbol/landuse_coniferous_and_deciduous",
        "symbol/landuse_deciduous",
        "symbol/landuse_grass",
        "symbol/landuse_hills",
        "symbol/landuse_quary",
        "symbol/landuse_scrub",
        "symbol/landuse_swamp",
        "symbol/poi_boundary_administrative",
        "symbol/poi_cave",
        "symbol/poi_embassy",
        "symbol/poi_embassy2",
        "symbol/poi_military_bunker",
        "symbol/poi_mine",
        "symbol/poi_mine_abandoned",
        "symbol/poi_peak",
        "symbol/poi_place_city",
        "symbol/poi_place_town",
        "symbol/poi_place_village",
        "symbol/poi_point_of_interest",
        "symbol/poi_tower_communications",
        "symbol/poi_tower_power",
        "symbol/poi_tower_water",
        "symbol/red-marker",
        "symbol/water_tower",
        "tourist/tourist_archaeological",
        "tourist/tourist_archaeological2",
        "tourist/tourist_art_gallery",
        "tourist/tourist_art_gallery2",
        "tourist/tourist_battlefield",
        "tourist/tourist_beach",
        "tourist/tourist_casino",
        "tourist/tourist_castle",
        "tourist/tourist_cinema",
        "tourist/tourist_cinema2",
        "tourist/tourist_fountain",
        "tourist/tourist_memorial",
        "tourist/tourist_monument",
        "tourist/tourist_museum",
        "tourist/tourist_picnic",
        "tourist/tourist_ruin",
        "tourist/tourist_steam_train",
        "tourist/tourist_theatre",
        "tourist/tourist_view_point",
        "tourist/tourist_waterwheel",
        "tourist/tourist_windmill",
        "tourist/tourist_wreck",
        "tourist/tourist_zoo",
        "tourist/tourisum_fountain",
        "transport/amenity=airport",
        "transport/amenity=ferry_terminal",
        "transport/amenity=parking",
        "transport/amenity=taxi",
        "transport/barrier_bollard",
        "transport/barrier_enterance",
        "transport/barrier_gate",
        "transport/barrier_lift_gate",
        "transport/barrier_stile",
        "transport/barrier_toll_booth",
        "transport/highway=bus_stop",
        "transport/railway=station",
        "transport/transport_aerodrome",
        "transport/transport_aerodrome2",
        "transport/transport_airport",
        "transport/transport_airport2",
        "transport/transport_bus_station",
        "transport/transport_bus_stop",
        "transport/transport_bus_stop2",
        "transport/transport_car_share",
        "transport/transport_ford",
        "transport/transport_fuel",
        "transport/transport_fuel_lpg",
        "transport/transport_lighthouse",
        "transport/transport_marina",
        "transport/transport_parking",
        "transport/transport_parking_bicycle",
        "transport/transport_parking_car",
        "transport/transport_parking_car_paid",
        "transport/transport_parking_disabled",
        "transport/transport_parking_private",
        "transport/transport_parking_private2",
        "transport/transport_parking_private3",
        "transport/transport_port",
        "transport/transport_rental_bicycle",
        "transport/transport_rental_car",
        "transport/transport_roundabout_anticlockwise",
        "transport/transport_roundabout_clockwise",
        "transport/transport_taxi_rank",
        "transport/transport_traffic_lights",
        "transport/transport_train_station",
        "transport/transport_train_station2",
        "transport/transport_tram_stop",
        "wind_roses/WindRose_01",
        "wind_roses/WindRose_02",
        };
        return fileArray;
    }
    
    public static void main(String args[]) throws MalformedURLException, IOException{
        
        GeoServerCommunication geoComm = GeoServerCommunication.makeGeoserverCommunication();
        for(String file : fileArray()){
            System.out.println(geoComm.addStyle(file, "localhost:8080/resources/img/svg/"+file+".svg", "img/svg", Integer.SIZE));
            
        }
        
        //System.out.println(geoComm.addStyle("test","localhost:8080/resources/img/test", "img/svg", 28));
        
        
        
        /*final String RESTURL = GEOSERVER_URL;
        String RESTUSER = "admin";
        String RESTPW   = "geoserver";
        
        GeoServerRESTReader reader = new GeoServerRESTReader(RESTURL, RESTUSER, RESTPW);
        GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(RESTURL, RESTUSER, RESTPW);
        
        //RESTLayer layer = reader.getLayer("rio2016", "hoteis");
        //RESTFeatureType feature = reader.getFeatureType(layer);
        
        
        /*Iterator<Attribute> featIterator = feature.attributesIterator();
        while(featIterator.hasNext()){
            System.out.println(featIterator.next());
        }*/
        
        /*
        GeoServerCommunication com = GeoServerCommunication.makeGeoserverCommunication();
        com.setDefaultStyleToLayer("rio2016", "atracoes", "pinpoint");
        
        List<String> names = com.getLayerNames();
        
        for(String name : names){
            System.out.println(name);
        }
        
        for(String work : reader.getWorkspaceNames()){
            System.out.println(work);
        }*/
        //System.out.println(com.removeStyle("novo_estilo"));
        //System.out.println(com.addStyle("novo_estilo","http://com.cartodb.users-assets.production.s3.amazonaws.com/simpleicon/map43.svg" , "image/svg", 32));
        //System.out.println(com.updateStyle("novo_estilo","http://com.cartodb.users-assets.production.s3.amazonaws.com/simpleicon/map43.svg" , "image/svg", 32));
        
        /*
        
        RESTLayerList list = reader.getLayers();
        System.out.println();
        for(NameLinkElem layer : list){
            System.out.println(layer.getName());
        }*/
        //RESTStyleList styleList = reader.getStyles();
        //Iterator styleListIterator = styleList.iterator();
        /*
        while(styleListIterator.hasNext()){
            System.out.println((styleListIterator.next()));
        }
        //String sldFile = reader.getSLD("point");
        //System.out.println(publisher.publishStyleInWorkspace(null,sldFile,"novsa_camada"));*/
    }
}
