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

package org.engineeringnotebook.resourcemanager.resource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.engineeringnotebook.resourcemanager.core.ResourceManagerRemoteInterface;

/**
 * The resource backing restful web service calls to register a user
 * 
 * @author Jeff Coble <jeffrey.a.coble@gmail.com> http://engineeringnotebook.org
 */

@Produces("text/plain")
@Path("/retrieve")
public class RetrieverResource {
    @javax.ws.rs.core.Context
    UriInfo uriInfo;
    private ResourceManagerRemoteInterface resourceManagerEJB; 
    private static final Logger logger = Logger.getLogger(RetrieverResource.class.getName());
   
    /**
     * Called to get the reference to the EJB
   */
    private void connectEJB() {

        Context context;
        try
        {
            logger.log(Level.FINE, "Trying to get the EJB reference");
            context = new InitialContext();
            resourceManagerEJB = (ResourceManagerRemoteInterface)context.lookup("org.engineeringnotebook.resourcemanager.core.ResourceManagerRemoteInterface");
        } catch (NamingException e)
        {
            logger.log(Level.FINE, "Failed to get the EJB reference");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
   
    
    /**
     * 
     */
    @GET
    @Produces("text/plain")
    public String getProtectedResource(@QueryParam("screenname") String twitterScreenName, @QueryParam("twitterurl") String twitterURL) {
        logger.log(Level.FINE, ">>>Web: Get Protected Resource {0} for User: {1}", new Object[]{twitterURL, twitterScreenName});
        
        String result = null;
        if(twitterScreenName != null){
            connectEJB();
            result = resourceManagerEJB.requestTwitterResource(twitterScreenName, twitterURL);
        }
        return result;    
    }
   
    

}
