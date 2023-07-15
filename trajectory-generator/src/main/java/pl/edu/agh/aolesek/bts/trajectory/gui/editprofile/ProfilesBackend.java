/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.agh.aolesek.bts.trajectory.gui.editprofile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author godle
 */
public class ProfilesBackend {
    
    public static List<String> removeFile(String fileName){
        File fileToDelete = new File("profiles/" + fileName + ".json");
        fileToDelete.delete();
        File folder = new File("profiles/");
        File[] list = folder.listFiles();
        List<String> profileNames = new ArrayList();
        if(list.length>0){
            String fn;
            for(File f:list){
                fn = f.getName().substring(0,f.getName().length()-5);
                profileNames.add(fn);
            }
        }
        return profileNames;
    }
    
    public static List<String> getProfilesNames(){
        File folder = new File("profiles/");
        File[] list = folder.listFiles();
        List<String> profileNames = new ArrayList();
        if(list.length>0){
            String fileName;
            for(File f:list){
                fileName = f.getName().substring(0,f.getName().length()-5);
                profileNames.add(fileName);
            }
        }
        return profileNames;
    }
    
}
