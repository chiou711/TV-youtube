package com.cw.tv_yt.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class YouTubeTimeConvert {
    /**
     *
     * @param duration
     * @return "01:02:30"
     */
    public static String convertYouTubeDuration(String duration) {
        String youtubeDuration = duration; //"PT1H2M30S"; // "PT1M13S";
        Calendar c = new GregorianCalendar();
        try {
            DateFormat df = new SimpleDateFormat("'PT'mm'M'ss'S'");
            Date d = df.parse(youtubeDuration);
            c.setTime(d);
        } catch (ParseException e) {
            try {
                DateFormat df = new SimpleDateFormat("'PT'hh'H'mm'M'ss'S'");
                Date d = df.parse(youtubeDuration);
                c.setTime(d);
            } catch (ParseException e1) {
                try {
                    DateFormat df = new SimpleDateFormat("'PT'ss'S'");
                    Date d = df.parse(youtubeDuration);
                    c.setTime(d);
                } catch (ParseException e2) {
                }
            }
        }
        c.setTimeZone(TimeZone.getDefault());

        String time = "";
        if ( c.get(Calendar.HOUR) > 0 ) {
            if ( String.valueOf(c.get(Calendar.HOUR)).length() == 1 ) {
                time += "0" + c.get(Calendar.HOUR);
            }
            else {
                time += c.get(Calendar.HOUR);
            }
            time += ":";
        }
        // test minute
        if ( String.valueOf(c.get(Calendar.MINUTE)).length() == 1 ) {
            time += "0" + c.get(Calendar.MINUTE);
        }
        else {
            time += c.get(Calendar.MINUTE);
        }
        time += ":";
        // test second
        if ( String.valueOf(c.get(Calendar.SECOND)).length() == 1 ) {
            time += "0" + c.get(Calendar.SECOND);
        }
        else {
            time += c.get(Calendar.SECOND);
        }
        return time ;
    }
}