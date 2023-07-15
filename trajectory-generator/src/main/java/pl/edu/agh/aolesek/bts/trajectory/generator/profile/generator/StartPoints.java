package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

//nowa klasa
//wczytywanie punktów początkowych z pliku
//dodana klasa wczytująca z pliku startpoints.txt zestaw punktów początkowych, które są losowo przydzielane do generowanych profili
public class StartPoints{

    public static List<Point> getStartPoints(){
        List<Point> START_POINTS = new ArrayList<Point>();
        try{
            START_POINTS = _getStartPoints();
        }
        catch(Exception e) {
        }
        return START_POINTS;
    }

    private static List<Point> _getStartPoints() throws IOException {
        List<Point> START_POINTS = new ArrayList<Point>();
        File file=new File("startpoints.txt");
        try (FileInputStream fis=new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr)){
            String line;
            String[] elements;
            while((line = br.readLine()) != null){
                elements = line.split(";");
                START_POINTS.add(new Point(Double.parseDouble(elements[0]),Double.parseDouble(elements[1])));
            }
            return START_POINTS;
        }
    }
}
