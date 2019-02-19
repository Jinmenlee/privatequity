package aoa.tarot.privatequity;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

public class Sms {

    private String _id;
    private String _address;
    private String _msg;
    private String _readState; //"0" for have not read btnSms and "1" for have read btnSms
    private String _time;
    private String _folderName;

    public String getId(){
        return _id;
    }
    public String getAddress(){
        return _address;
    }
    public String getMsg(){
        return _msg;
    }
    public String getReadState(){
        return _readState;
    }
    public String getTime(){
        return _time;
    }
    public String getFolderName(){
        return _folderName;
    }


    public void setId(String id){
        _id = id;
    }
    public void setAddress(String address){
        _address = address;
    }
    public void setMsg(String msg){
        _msg = msg;
    }
    public void setReadState(String readState){
        _readState = readState;
    }
    public void setTime(String time){
        _time = time;
    }
    public void setFolderName(String folderName){
        _folderName = folderName;
    }

    @Override
    public String toString() {
        return "Sms{" +
                "\t_id='" + _id + '\'' +
                ",\n\t _address='" + _address + '\'' +
                ",\n\t _msg='" + _msg + '\'' +
                ",\n\t _readState='" + _readState + '\'' +
                ",\n\t _time='" + getDatetime(Long.parseLong(_time)) + '\'' +
                ",\n\t _folderName='" + _folderName + '\'' +
                "}\n\n";
    }

    private String getDatetime(long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time);
        String date = DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString();
        return date;
    }
}
