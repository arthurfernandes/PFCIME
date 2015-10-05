package br.eb.ime.pfc.domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Represents the Access Level of a User.
 * 
 * This class represents an access level which contains a group of Layers that
 * can be accessed by this Access Level.
 * Users of the system have an unique Access Level that allows them to view the 
 * layers specified by the Access Level.
 * The Access Level is identified by its name.
 */
@Entity
@Table(name = "access_levels")
public class AccessLevel implements Serializable{
    
    private static final long serialVersionUID = 1L;
    
    @Id @Column(name = "ACCESSLEVEL_ID")
    private final String name;
    
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(name = "ACCESSLEVEL_LAYER",
                joinColumns = {@JoinColumn(name="ACCESSLEVEL_ID",referencedColumnName="ACCESSLEVEL_ID")},
                inverseJoinColumns = {@JoinColumn(name="LAYER_ID",referencedColumnName="LAYER_ID",nullable = false)})
    private final Set<Layer> layers;
    
    @OneToMany(fetch=FetchType.LAZY,cascade = CascadeType.ALL,mappedBy="accessLevel")
    private final Set<User> users;
    
    //Constructors
    
    /**
     * Creates an AccessLevel with the specified name
     * @param name
     * Name that identifies this Access Level
     * @return
     * AccessLevel created
     * @throws ObjectInvalidIdException 
     * If the name is not a valid name for an Access Level.
     * Access Level names must not be empty and contain trailing spaces
     */
    public static AccessLevel makeAccessLevel(String name) throws ObjectInvalidIdException{
        if(isValidId(name)){
            return new AccessLevel(name);
        }
        else{
            throw new ObjectInvalidIdException("Could not create AccessLevel because it's not a valid name");
        }
    }
    
    /**
     * Checks if the specified name is a valid name for an Access Level.
     * Valid names are composed only by digits and spaces, but can not be empty 
     * or contain trailing spaces.
     * @param name
     * The name of an access level
     * @return 
     * true if the name is a valid name
     */
    public static boolean isValidId(String name){
        if(name.equals("")){
            return false;
        }
        else if(!name.equals(name.trim())){
            return false;
        }
        else{
            return true;
        }
    }
    
    /**
     * Creates an Access Level with the specified name.
     * @param name 
     */
    protected AccessLevel(String name){
        this.name = name;
        this.layers = new LinkedHashSet<>();
        this.users = new LinkedHashSet<>();
        checkRep();
    }
    
    /**
     * Default Constructor for serialization/deserialization processes only.
     * This constructor is used by classes that inherit from this class to extend
     * some plugin functionality such as Hibernate Proxy's.
     */
    protected AccessLevel(){
        name = null;
        this.users = new LinkedHashSet<>();
        this.layers = new LinkedHashSet<>();
    }
    
    /**
     * Checks the representation invariant.
     */
    private void checkRep(){
        assert isValidId(this.name) == true;
        assert this.users != null;
        assert this.layers != null;
    }
    
    /**
     * Returns the name of this Access Level
     * @return name
     */
    public String getName(){
        return this.name;
    }
    
    /**
     * Returns the layers accessed by this Access Level.
     * @return layers
     * Effects: layers is an unmodifiable list that specifies each layer in 
     * the order they were added to this Access Level.
     */
    public Set<Layer> getLayers(){
        return Collections.unmodifiableSet(layers);
    }
    
    /**
     * Returns a collection of users that belong to this access level.
     * @return 
     */
    public Collection<User> getUsers(){
        return Collections.unmodifiableSet(users);
    }
    
    /**
     * Indicates whether this access level contains an user with the specified username.
     * @param username
     * Username of user
     * @return 
     * True if this access level contains an user with username param.
     */
    public boolean containsUser(String username){
        for(User user : this.users){
            if(user.getUsername().equalsIgnoreCase(username)){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Specifies whether a layer is accessed by this Access Level or not.
     * @param layerWmsId
     * The wmsId of the layer.
     * @return true if this Access Level has access to the layer with the specified
     * wmsId.
     */
    public boolean hasAccessToLayer(String layerWmsId){
        for(Layer layer : this.layers){
            if(layer.getWmsId().equals(layerWmsId)){
                return true;
            }
        }
        return false;
    }
    
    /*Mutators*/
    
    /**
     * Adds an user to this access level
     * @param user 
     * User that will be added to this access level.
     * @throws RepeatedItemException
     * If user has the same username of an user added to this access level.
     */
    public void addUser(User user) throws RepeatedItemException{
        assert user != null;
        if(this.containsUser(user.getUsername())){
            throw new RepeatedItemException("Access Level already contains user with the specified username");
        }
        else{
            this.users.add(user);
        }
    }
    
    /**
     * Add another Layer to this Access Level. 
     * @param layer to be accessed by this Access Level
     * @throws RepeatedItemException
     * When the layer added has the same wmsId of another layer already added.
     */
    public void addLayer(Layer layer) throws RepeatedItemException{
        if(hasAccessToLayer(layer.getWmsId())){
            throw new RepeatedItemException("Cannot add Layer with the same wmsId of a Layer in use.");
        }
        this.layers.add(layer);
        
        checkRep();
    }
}