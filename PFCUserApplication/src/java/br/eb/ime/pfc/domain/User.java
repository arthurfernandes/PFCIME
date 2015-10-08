package br.eb.ime.pfc.domain;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A representation of a User in this system.
 * 
 * A user contains an username and a password and it is uniquely identified
 * by its username.
 * A user also contains an access level that allows him to access a limited 
 * group of layers.
 */
@Entity
@Table(name = "users")
public class User implements Serializable{
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_NAME = "";
    private static final String DEFAULT_EMAIL = "";
    private static final String DEFAULT_TELEPHONE = "";
    
    @Id
    @Column(name = "USER_ID") private final String username;
    @Column(name = "PASSWORD") private String password;

    @Column(name= "NAME") private String name;
    @Column(name= "EMAIL") private String email;
    @Column(name ="TELEPHONE") private String telephone;
    
    @ManyToOne(optional=false,cascade = CascadeType.REFRESH)
    @JoinColumn(name = "ACCESSLEVEL_ID",referencedColumnName="ACCESSLEVEL_ID")
    private AccessLevel accessLevel;
    
    public static User makeUser(String username,String password,AccessLevel accessLevel){
        if(isValid(username)){
            return new User(username,password,accessLevel);
        }
        else{
            throw new ObjectInvalidIdException("Could not create User because the specified username is invalid");
        }
    }
    
    public static boolean isValid(String username){
        if(username.equals("")){
            return false;
        }
        if(username.contains(" ") || username.contains("\n")){
            return false;
        }
        else{
            return true;
        }
    }
    
    private static String encryptPassword(String password){
        String encryptedPassword = password;
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(password.getBytes());
            encryptedPassword = new String(messageDigest.digest());
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, "Couldn't encrypt user password because the"
                    + "specified algorithm doesn't exist.", ex);
        }
        return password;
    }
    
    /**
     * Constructs a user with the specified username, password and Access Level
     * @param username
     *      The name that identifies this user. This name must not be empty.
     * @param password
     *      The password used by this user to identify himself in the system.
     * @param accessLevel 
     *      The access level used by this user.
     */
    protected User(String username,String password,AccessLevel accessLevel){
        this.username = username;
        this.password = encryptPassword(password);
        this.accessLevel = accessLevel;
        this.name = DEFAULT_NAME;
        this.telephone = DEFAULT_TELEPHONE;
        this.email = DEFAULT_EMAIL;
        checkRep();
    }
    
    /**
     * Default Constructor for serialization/deserialization processes only.
     * This constructor is used by classes that inherit from this class to extend
     * some plugin functionality such as Hibernate Proxy's.
     */
    protected User(){
        this.username = null;
        this.password = null;
        this.accessLevel = null;
        this.name = DEFAULT_NAME;
        this.telephone = DEFAULT_TELEPHONE;
        this.email = DEFAULT_EMAIL;
    }
    
    /**
     * Checks the representation invariant of this object.
     */
    private void checkRep(){
        assert !this.username.equals("");
        assert !this.password.equals("");
        assert accessLevel != null;
    }
    
    /**
     * Returns the username of this user.
     * @return username
     */
    public String getUsername(){
        return this.username;
    }
    
    /**
     * Sets the password for this user.
     * @param password
     */
    public void setPassword(String password){
        this.password = encryptPassword(password);
        checkRep();
    }
   
    /**
     * 
     * @param password
     * @return 
     */
    public boolean authenticatePassword(String password){
       return this.password.equals(encryptPassword(password));
    }
    
    /**
     * Returns the access level of this user.
     * @return access level
     */
    public AccessLevel getAccessLevel(){
        return this.accessLevel;
    }
    
    
    public void setAccessLevel(AccessLevel accessLevel){
        this.accessLevel = accessLevel;
    }
    
    public String getName(){
        return this.name;
    }
    
    public void setName(String name){
        this.name = name;
    }
    
    public String getEmail(){
        return this.email;
    }
    
    public void setEmail(String email){
        this.email = email;
    }
    
    public String getTelephone(){
        return this.telephone;
    }
    
    public void setTelephone(String telephone){
        this.telephone = telephone;
    }
    @Override
    public boolean equals(Object o){
        if(o instanceof User){
            final User other = (User) o;
            return (other.getUsername().equals(this.username));
        }
        else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.username);
    }

}