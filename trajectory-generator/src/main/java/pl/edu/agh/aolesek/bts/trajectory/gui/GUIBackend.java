/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.agh.aolesek.bts.trajectory.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author godle
 */
public class GUIBackend{
    
    public static void visualiseTrajectory(List<String[]> lines, String trajectoryName){
        lines = removeOtherTrajectories(lines,trajectoryName);
        getImage(lines);
    }
    
    public static void getImage(List<String[]> lines){
        int zoom = 13;
        Double minlat = 180.;
        Double maxlat = -180.;
        Double minlon = 180.;
        Double maxlon = -180.;
        for(String[] s:lines){
            if(Double.parseDouble(s[4].replace(",","."))<minlat){
                minlat = Double.parseDouble(s[4].replace(",","."));
            }
            if(Double.parseDouble(s[4].replace(",","."))>maxlat){
                maxlat = Double.parseDouble(s[4].replace(",","."));
            }
            if(Double.parseDouble(s[5].replace(",","."))<minlon){
                minlon = Double.parseDouble(s[5].replace(",","."));
            }
            if(Double.parseDouble(s[5].replace(",","."))>maxlon){
                maxlon = Double.parseDouble(s[5].replace(",","."));
            }
        }
        Double midlat = (maxlat+minlat)/2;
        Double midlon = (maxlon+minlon)/2;
        String ml1 = midlat.toString().replace(".",",");
        String ml2 = midlon.toString().replace(".",",");
        String path = "https://maps.googleapis.com/maps/api/staticmap?center=" + 
                ml1 + "," + ml2 + "&zoom=" + zoom + "&size=640x640&maptype=roadmap";
        for(String[] t:lines){
            path = path + "&markers=color:blue%7C" + t[4] + "," + t[5];
        }
        path = path + "&path=color:0x0000ff|weight:5";
        for(String[] t:lines){
            path = path + "|" + t[4] + "," + t[5];
        }
        List<String> cf = getLines("config.cfg");
        String googleKey = cf.get(32);
        path = path + "&key=" + googleKey;
        System.out.println(path.length());
        System.out.println(path);
        try{
            createFile(path);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Double strToDouble(String str) throws ParseException{
        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        Number number = format.parse(str);
        return number.doubleValue();
    }
    
    public static void createFile(String path) throws MalformedURLException, IOException {
        URL url;
        url = new URL(path);
        BufferedImage img = ImageIO.read(url);
        File file = new File("visualisations/visualisation " + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".png");
        ImageIO.write(img, "png", file);
        System.out.println("Wygenerowano obraz");
    }
    
    public static List<String[]> removeOtherTrajectories(List<String[]> lines, String trajectoryName){
        int i=0;
        while(i<lines.size()){
            if(!lines.get(i)[2].equals(trajectoryName)){
                lines.remove(i);
            }
            else{
                i++;
            }
        }
        return lines;
    }
    
    public static List<String[]> readTrajectories(String fileName){
        try{
            List<String[]> lines = _readTrajectories(fileName);
            return lines;
        }
        catch(Exception e) {
            System.out.println("OSM API Connecting Exception");
            return new ArrayList();
        }
    }
    
    private static List<String[]> _readTrajectories(String fileName) throws IOException{
        List<String[]> lines = new ArrayList();
        File file=new File(fileName);
        try (FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)){
            String line;
            String[] place;
            while((line = br.readLine()) != null){
                place = line.split(";");
                lines.add(place);
            }
            //lines=removeWaypoints(lines);
            return lines;
        }
    }
    
    public static List<String[]> removeWaypoints(List<String[]> lines){
        int i=0;
        while(i<lines.size()){
            if(lines.get(i)[6].equals("waypoint")){
                lines.remove(i);
            }
            else{
                i++;
            }
        }
        return lines;
    }
    
    public static void resetFile(String path){
        try{
            _resetFile(path);
        }
        catch(Exception e) {
            System.out.println("File modifying Exception");
        }
    }
    
    public static void _resetFile(String path) throws IOException {
        String extension = path.substring(path.length()-4,path.length());
        String name = path.substring(0,path.length()-4);
        File backup = new File(name + "_reset" + extension);
        File file = new File(path);
        Files.copy(backup.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    public static Pair getOSMCoordinate(String text){
        try{
            Pair pair = _getOSMCoordinate(text);
            return pair;
        }
        catch(Exception e) {
            System.out.println("OSM API Connecting Exception");
            return new Pair(-1,-1);
        }
    }
    
    public static Pair _getOSMCoordinate (String text) throws MalformedURLException, ProtocolException, IOException{
        text = text.replace(" ", "+");
        text=text.replaceAll("ą","a");
        text=text.replaceAll("ć","c");
        text=text.replaceAll("ę","e");
        text=text.replaceAll("ł","l");
        text=text.replaceAll("ń","n");
        text=text.replaceAll("ó","o");
        text=text.replaceAll("ś","s");
        text=text.replaceAll("ź","z");
        text=text.replaceAll("ż","z");
        text=text.replaceAll("Ą","A");
        text=text.replaceAll("Ć","C");
        text=text.replaceAll("Ę","E");
        text=text.replaceAll("Ł","L");
        text=text.replaceAll("Ń","N");
        text=text.replaceAll("Ó","O");
        text=text.replaceAll("Ś","S");
        text=text.replaceAll("Ź","Z");
        text=text.replaceAll("Ż","Z");
        String strURL = "https://nominatim.openstreetmap.org/search.php?q=" + text + "&format=jsonv2";
        URL url = new URL(strURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String result = br.readLine();
        if (result.equals("") || result.toCharArray()[1] != '{') {
            System.out.println("OSM API nie potrafił wykonać poprawnego geokodowania");
        }
        JSONObject jsonObj = new JSONObject(result.replaceAll("^.|.$", ""));
        Double latitude = jsonObj.getDouble("lat");
        Double longitude = jsonObj.getDouble("lon");
        Formatter formatter = new Formatter();
        formatter.format("%.7f", latitude);
        formatter.format("%.7f", longitude);
        Pair pair = new Pair(latitude,longitude);
        //sonarqube sugestia
        formatter.close();
        return pair;
    }
    
    public static Pair getGoogleCoordinate(String googleKey, String text){
        try{
            Pair pair = _getGoogleCoordinate(googleKey,text);
            return pair;
        }
        catch(Exception e) {
            System.out.println("Google API Connecting Exception");
            return new Pair(-1,-1);
        }
    }
    
    public static Pair _getGoogleCoordinate(String googleKey, String text) throws ApiException, InterruptedException, IOException{
        GeoApiContext context = new GeoApiContext.Builder().apiKey(googleKey).build();
        GeocodingResult[] results =  GeocodingApi.geocode(context,text).await();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String str1=gson.toJson(results[0].geometry.location.lat);
        String str2=gson.toJson(results[0].geometry.location.lng);
        Pair pair = new Pair(Double.parseDouble(str1),Double.parseDouble(str2));
        return pair;
    }
    
    public static void savePairs(List<Pair> pairs, String fileName){
        try{
            _savePairs(pairs, fileName);
        }
        catch(Exception e) {
            System.out.println("IO Exception");
        }
    }
    
    public static List<Pair> getPairs(String fileName){
        List<Pair> pairs = new ArrayList<Pair>();
        try{
            pairs = _getPairs(fileName);
        }
        catch(Exception e) {
            System.out.println("IO Exception");
        }
        return pairs;
    }
    
    public static List<String> getLines(String fileName){
        List<String> lines = new ArrayList<String>();
        try{
            lines = _getLines(fileName);
        }
        catch(Exception e) {
            System.out.println("IO Exception");
        }
        return lines;
    }
    
    public static void updateFile(List<String> lines, String fileName){
        try{
            _updateFile(lines, fileName);
        }
        catch(Exception e) {
            System.out.println("IO Exception");
        }
    }
    
    private static void _savePairs(List<Pair> pairs, String fileName) throws IOException{
        File file = new File(fileName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        for(Pair p:pairs){
            bw.write(p.key()+";"+p.value()+"\n");
        }
        bw.close();
    }
    
    private static List<Pair> _getPairs(String fileName) throws IOException{
        List<Pair> pairs = new ArrayList();
        File file=new File(fileName);
        try (FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)){
            String line;
            String[] elements;
            while((line = br.readLine()) != null){
                elements = line.split(";");
                pairs.add(new Pair(Double.parseDouble(elements[0]),Double.parseDouble(elements[1])));
            }
            return pairs;
        }
    }

    private static void _updateFile(List<String> lines, String fileName) throws IOException {
        File file = new File(fileName);
        StringBuffer sb = new StringBuffer();
        int lineNumber = 0;
        try (FileInputStream fis=new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)){
            String lineOfFile;
            String lineOfList;
            while((lineOfFile = br.readLine()) != null){
                lineOfList = lines.get(lineNumber);
                lineOfFile = lineOfFile.substring(0,lineOfFile.indexOf("="));
                lineOfList = lineOfList.replace("\n",""); //usunięcie przypadkowych enterów
                sb.append(lineOfFile);
                sb.append("=");
                sb.append(lineOfList);
                sb.append("\n");
                lineNumber++;
            }
        }
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.write(sb.toString().getBytes());
        fos.close();
    }
    
    private static List<String> _getLines(String fileName) throws IOException {
        File file=new File(fileName);
        try (FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)){
            List<String> lines = new ArrayList();
            String line;
            while((line = br.readLine()) != null){
                lines.add(processLine(line));
            }
            return lines;
        }
        
    }
    
    private static String processLine(String line){
        line = line.substring(line.indexOf("=")+1);
        //sugestia Sonarqube 
        line = line.trim();
        return line;

    }
}