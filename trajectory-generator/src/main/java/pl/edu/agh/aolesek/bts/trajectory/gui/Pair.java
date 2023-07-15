/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.edu.agh.aolesek.bts.trajectory.gui;

/**
 *
 * @author godle
 */
public class Pair {
    private double key;
    private double value;
    
    public Pair(double key, double value){
        this.key=key;
        this.value=value;
    }
    
    public double key(){
        return this.key;
    }
    
    public double value(){
        return this.value;
    }
}
