/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.infinit.chatservice.utils;

import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author xxbamar
 */
public class DateHelper {

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }
    
    public static Date roundUp(Date date, int secondsToRoundUp){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        int sec = cal.get(Calendar.SECOND);
        int modulo = secondsToRoundUp - (sec % secondsToRoundUp);
        cal.add(Calendar.SECOND, modulo);        
        
        return cal.getTime();
    }
}
