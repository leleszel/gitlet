package gitlet;
import java.io.Serializable;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Commit implements Serializable {

    private String parent;
    private HashMap<String, String> filesTracked;
    private String time;
    private String message;
    private String shA1;

    public Commit() {
        message = "initial commit";
        parent = null;
        filesTracked = new HashMap<>();
        time = timeGenerator();
        shA1 = shaGenerator();
    }

    //Constructor that takes in a message
    public Commit(String m) {
        parent = null;
        filesTracked = new HashMap<>();
        time = timeGenerator();
        message = m;
    }

    public Commit(String parent, HashMap<String, String> files, String message) {
        this.parent = parent;
        this.filesTracked = files;
        this.message = message;
        this.time = timeGenerator();
        shA1 = shaGenerator();

    }



    public String shaGenerator() {
        String input = "";
        if (!filesTracked.isEmpty()) {
            for (String a : filesTracked.keySet()) {
                input += a;
            }
        }

        if (!(parent == null)) {
            input += parent;
        }

        if (!(time == null)) {
            input += time;
        }

        if (!(message == null)) {
            input += message;
        }

        return Utils.sha1(input);
    }





    public static String timeGenerator() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date timeNow = new Date();
        return dateFormat.format(timeNow);
    }




    public HashMap<String, String> getFilesTracked() {
        return filesTracked;
    }



    public String getSHA1() {
        return shA1;
    }

    public String getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    public String getParent() {
        return parent;
    }


    public void add(File f) {
        ArrayList<Object> newFile = new ArrayList<>();
        newFile.add(fileIDGenerator(f));
        newFile.add(f);
        filesTracked.put(f.getName(), fileIDGenerator(f));
    }


    public void remove(File f) {
        filesTracked.remove(f.getName());
    }


    public static String fileIDGenerator(File f) {
        Object[] fileReference = new Object[2];
        fileReference[0] = f.getName();
        fileReference[1] = Utils.readContents(f);
        return Utils.sha1(fileReference);
    }

    public void setFilesTracked(HashMap<String, String> a) {
        filesTracked = a;
    }


    public void setParent(String p) {
        this.parent = p;
    }

    public void setMessage(String m) {
        this.message = m;
    }

    public void setSHA1() {
        shA1 = this.shaGenerator();
    }

    public static Commit copier(Commit c) {
        Commit a = new Commit();
        a.parent = c.parent;
        a.filesTracked = c.filesTracked;
        a.time = c.time;
        a.message = c.message;
        a.shA1 = c.shA1;
        return a;
    }
}

