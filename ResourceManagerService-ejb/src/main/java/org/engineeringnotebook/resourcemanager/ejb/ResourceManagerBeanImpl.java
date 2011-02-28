/*
 * Copyright 2011 Jeff Coble <jeffrey.a.coble@gmail.com> http://engineeringnotebook.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.engineeringnotebook.resourcemanager.ejb;

import javax.ejb.Stateless;
import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import com.spaceprogram.simplejpa.EntityManagerFactoryImpl;
import org.scribe.model.Token;
import org.engineeringnotebook.snrdm.core.oauth.OAuthHandler;
import org.engineeringnotebook.snrdm.core.oauth.OAuthUtilities;
import org.engineeringnotebook.snrdm.entity.UserCredential;
import org.engineeringnotebook.resourcemanager.core.ResourceManagerLocalInterface;
import org.engineeringnotebook.resourcemanager.core.ResourceManagerRemoteInterface;
import org.engineeringnotebook.snrdm.core.utilities.ClassPathBuilder;


/**
 * Stateless EJB that interacts with the Twitter API to support OAuth authorization.
 * Stores the user credentials in an Amazon SimpleDB database
 * 
 * @author Jeff Coble <jeffrey.a.coble@gmail.com> http://engineeringnotebook.org
 */
@Stateless
public class ResourceManagerBeanImpl implements ResourceManagerRemoteInterface, ResourceManagerLocalInterface {

    private static final Logger logger = Logger.getLogger(ResourceManagerBeanImpl.class.getName());
    private static EntityManagerFactory factory; 
    private OAuthHandler oauthHandler;
    
    //Amazon API keys
    @Resource(name="org/engineeringnotebook/amazonaccesskey") 
    private String amazonAccessKeyValue;
    @Resource(name="org/engineeringnotebook/amazonsecretkey")    
    private String amazonSecretKeyValue;    
    
    //Twitter OAuth Keys
    @Resource(name="org/engineeringnotebook/twitterconsumerkey") 
    private String twitterConsumerKey;
    @Resource(name="org/engineeringnotebook/twitterconsumersecretkey") 
    private String twitterConsumerSecretKey;    
        
    public ResourceManagerBeanImpl() {
        logger.log(Level.INFO, "Creating the RegistrationServiceBean");  
    }
    
    /**
     * We need this because dependency injection of the member variables seems 
     * to happen after the constructor is called.
     */
    @PostConstruct
    private void initialize() {
        initializeSimpleJPA();  
        oauthHandler = new OAuthHandler(twitterConsumerKey, twitterConsumerSecretKey);
        
    }
    
    /**
     * Sets up the simplejpa entity manager
     */
    private void initializeSimpleJPA() {
        
        List<Class> classList = new ArrayList();
        ClassPathBuilder cpBuilder = new ClassPathBuilder();
        
        classList.add(UserCredential.class);
        
        Map<String,String> props = new HashMap<String,String>();
        props.put("accessKey",amazonAccessKeyValue);
        props.put("secretKey",amazonSecretKeyValue);
        
        Set<String> libPaths = cpBuilder.getScanPaths(classList);
        
        factory = new EntityManagerFactoryImpl("RGSPersistenceUnit", props, libPaths); 
   
    }
    @Override
    public String requestTwitterResource(String twitterScreenName, String twitterURL){

        String response;
        
        logger.log(Level.FINE, ">>>EJB: Get Protected Resource {0} for User: {1}", new Object[]{twitterURL, twitterScreenName});
        
        UserCredential user = retrieveUserCredential(twitterScreenName);
        if(user != null) {
            System.out.println("access token: " + user.getTwitterAccessToken() + " access secret = " + user.getTwitterAccessSecret());
            Token accessToken = OAuthUtilities.createToken(user.getTwitterAccessToken(), user.getTwitterAccessSecret());
            response = oauthHandler.getResource(accessToken, twitterURL);
        }
        else {
            logger.log(Level.INFO, "No Credentials Found For User {0}", twitterScreenName); 
            response = "OAuth Credentials Not Found";
        }
            
        return response;
    }
    
    /**
     * 
     * @param twitterScreenName
     * @return 
     */
    private UserCredential retrieveUserCredential(String twitterScreenName) {
        logger.log(Level.FINE, "Retrieving User Credential from SimpleDB for user: {0}", twitterScreenName);
        
        UserCredential user = null;
        
        List<UserCredential> results = getUC(twitterScreenName);
        logger.log(Level.FINEST, "Got {0} Results from SimpleDB", results.size());

        //We shouldn't have duplicat records, but if we do, just return the first one
        if(results.size() > 0) {
            user = results.get(0);
            logger.log(Level.FINEST, "Retrieved User: {0} from SimpleDB", user.getTwitterScreenName());
        }
        
        return user;      
    } 
    
    /**
     * Retrieves the users matching the screen name from SimpleDB
     * 
     * @param twitterScreenName
     * @return 
     */
    private List<UserCredential> getUC(String twitterScreenName) {
        //get the user credential that corresponds to the twitter screen name
        EntityManager em = factory.createEntityManager();
        Query query = em.createQuery("select M from UserCredential M where M.twitterScreenName = :twitterScreenName");
        query.setParameter("twitterScreenName", twitterScreenName);
        List<UserCredential> results = query.getResultList();
        
        logger.log(Level.FINEST, "Got {0} Results from SimpleDB for user: {1}", new Object[]{results.size(), twitterScreenName});
        
        em.close();
        
        return results;
    }
    
    
}
