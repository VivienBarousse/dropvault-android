/*  
 * This file is part of dropvault.
 *
 * dropvault is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dropvault is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dropvault.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aperigeek.dropvault.android.service;

import android.content.Context;
import android.content.SharedPreferences;
import com.aperigeek.dropvault.android.Resource;
import com.aperigeek.dropvault.android.dao.FilesDAO;
import com.aperigeek.dropvault.android.dav.DAVException;
import com.aperigeek.dropvault.android.dav.DropDAVClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.List;

/**
 *
 * @author Vivien Barousse
 */
public class FilesService {
    
    private String username;
    
    private String password;
    
    private Context context;
    
    private FilesDAO dao;
    
    public FilesService(String username, String password, Context context) {
        this.username = username;
        this.password = password;
        this.context = context;
        dao = new FilesDAO(context);
    }
    
    public Resource getRoot() {
        String baseUri = getBaseURI();
        
        if (baseUri == null) {
            return null;
        }
        
        return dao.getResource(baseUri);
    }
    
    public Resource getParent(Resource res) {
        return dao.getParent(res);
    }
    
    public List<Resource> getChildren(Resource parent) {
        return dao.getChildren(parent);
    }
    
    public void sync() throws SyncException {
        try {
            dao.clear();
            
            DropDAVClient client = new DropDAVClient(username, password);
            
            setBaseURI(client.getBaseURI());
            
            insert(client, null, client.getRootResource());
        } catch (DAVException ex) {
            throw new SyncException(ex);
        } catch (IOException ex) {
            throw new SyncException(ex);
        }
    }
    
    public File getFile(Resource res) {
        File folder = context.getExternalFilesDir(null);
        folder = new File(folder, "DropVault");
        
        String path = res.getHref().substring(getBaseURI().length());
        path = URLDecoder.decode(path);
        
        File file = new File(folder, path);
        return file;
    }
    
    private void insert(DropDAVClient client, Resource parent, Resource current) 
            throws DAVException, IOException {
        if (parent != null) {
            dao.insert(parent, current);
        } else {
            dao.insert(current);
        }
        
        if (current.getType() == Resource.ResourceType.FILE) {
            dump(client, current);
        }
        
        for (Resource child : client.getResources(current, 1)) {
            insert(client, current, child);
        }
    }
    
    private void dump(DropDAVClient client, Resource res) throws DAVException, IOException {
        File file = getFile(res);
        file.getParentFile().mkdirs();
        
        FileOutputStream out = new FileOutputStream(file);
        InputStream in = client.get(res);
        byte[] buffer = new byte[4096];
        int readed;
        while ((readed = in.read(buffer)) != -1) {
            out.write(buffer, 0, readed);
        }
        out.close();
        in.close();
    }
    
    protected String getBaseURI() {
        SharedPreferences prefs = context.getSharedPreferences("URI_PREFS", 0);
        return prefs.getString("baseURI", null);
    }
    
    protected void setBaseURI(String baseUri) {
        SharedPreferences prefs = context.getSharedPreferences("URI_PREFS", 0);
        prefs.edit()
                .putString("baseURI", baseUri)
                .commit();
    }
}
