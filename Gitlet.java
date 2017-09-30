package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Created by yingliaowang on 7/13/17.
 */
public class Gitlet implements Serializable {

    private GitletInfo info;

    private class GitletInfo implements Serializable {

        //The current commit user is working on
        private Commit head;

        //The current branch we are on
        private String currBranch;

        //All branches stored in this .gitlet
        //Key is branch name, Value is the SHA1ID of the head commit in that branch
        private HashMap<String, String> branches;

        //Files that are staged for commit
        private HashMap<String, String> stagingArea;

        //Files that are currently being tracked but to be untracked in the next commit
        private HashSet<String> toUntrack;

        //the SHA1ID of all commits
        private HashSet<String> allCommits;

        //all removed files
        private HashSet<String> removedFiles;

        private HashMap<String, ArrayList<String>> splitpoints;


        GitletInfo() {
            head = null;
            currBranch = null;
            branches = new HashMap<>();
            stagingArea = new HashMap<>();
            toUntrack = new HashSet<>();
            allCommits = new HashSet<>();
            removedFiles = new HashSet<>();
            splitpoints = new HashMap<>();
        }

        public void setHead(Commit h) {
            head = h;
        }

        public void setCurrBranch(String b) {
            currBranch = b;
        }

        public void setBranches(HashMap<String, String> b) {
            branches = b;
        }

        public void setStagingArea(HashMap<String, String> s) {
            stagingArea = s;
        }

        public void setToUntrack(HashSet<String> r) {
            toUntrack = r;
        }


    }


    public Gitlet() {
        info = new GitletInfo();
    }


    //Static method Serialization that
    // takes the GitletInfo as an object and serialize that object to a file
    public static void serialization(GitletInfo g) {
        File outFile = new File(".gitlet/info.ser");
        GitletInfo now = g;
        try {
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(now);
            out.close();
        } catch (IOException excp) {
            System.out.println("gitletinfo serialization wrong!");
        }
    }


    //Static method Deserialization that deserialize the
    // GitletInfo package and get the GitletInfo back
    public static GitletInfo deserialization() {
        GitletInfo now;
        File inFile = new File(".gitlet/info.ser");
        try {
            FileInputStream fi = new FileInputStream(inFile);
            ObjectInputStream oi = new ObjectInputStream(fi);

            now = (GitletInfo) oi.readObject();
            return now;

        } catch (IOException e) {
            System.out.println("gitletinfo deserialization wrong!");
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found exception");
            return null;
        }
    }


    //Static method to serialize commits;
    public static void serialization(Commit c) {
        Commit now = c;
        File outFile = new File(".gitlet/commits/"
                + c.getSHA1() + ".ser");
        try {
            ObjectOutputStream out =
                    new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(now);
            out.close();
        } catch (IOException excp) {
            System.out.println("commit serialization wrong");
        }
    }


    //Static method to deserialize commits
    // @para the SHA1ID of the commit we want to deserialize
    public static Commit deserialization(String id) {
        Commit now;
        File inFile = new File(".gitlet/commits/" + id + ".ser");
        try {
            FileInputStream fi = new FileInputStream(inFile);
            ObjectInputStream oi = new ObjectInputStream(fi);

            now = (Commit) oi.readObject();
            return now;

        } catch (IOException e) {
            System.out.println("commit deserialization wrong!");
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found exception");
            return null;
        }
    }


    /**
     * Below are methods that are called from main
     */

    public void init() {
        File original = new File(System.getProperty("user.dir"));
        File gitletDir = new File(original, ".gitlet");
        if (gitletDir.exists()) {
            System.out.println("A gitlet version-control system "
                    + "already exists in the current directory.");
        } else {
            try {
                //Create a new .gitlet directory
                gitletDir.mkdir();


                //Make the initial commit
                Commit initCommit = new Commit();
                info.setHead(initCommit);


                //Set the current branch to "master"
                info.setCurrBranch("master");

                //Store the branch information
                info.branches.put("master", info.head.getSHA1());


                //Store the initial commits to allCommits
                info.allCommits.add(info.head.getSHA1());

                //Create the staging area directory to store the files
                Path stagingArea = Paths.get(".gitlet/stagingarea");
                Files.createDirectory(stagingArea);


                //Create the blobs directory to store all blobs
                Path blobs = Paths.get(".gitlet/blobs");
                Files.createDirectory(blobs);

                //Create the commit directory to store all the serialized commits
                Path commits = Paths.get(".gitlet/commits");
                Files.createDirectory(commits);


                serialization(initCommit);

                //Serialize the info object and store it
                serialization(info);


            } catch (IOException e) {
                System.out.println("Init wrong!");
            }
        }
    }

    public void add(String fn) {
        info = deserialization();
        File f = new File(fn);
        if (f.exists()) {
            //If this file is marked to be untracked, remove the mark
            if (info.toUntrack.contains(fn)) {
                info.toUntrack.remove(fn);
                info.removedFiles.remove(fn);
            } else {

                int size = info.stagingArea.size();
                //if this file is already in the staging area, check whether it has been modified
                if (info.stagingArea.containsKey(fn)) {

                    String newID = fileIDGenerator(f);
                    String oldID = info.stagingArea.get(fn);

                    if (!newID.equals(oldID)) {
                        //Delete the old file in .gitlet/stagingarea folder and create the new file
                        try {
                            File oldFile = new File(".gitlet/stagingarea/"
                                    + info.stagingArea.get(fn));
                            oldFile.delete();
                            File newFile = new File(".gitlet/stagingarea/" + newID);
                            newFile.createNewFile();
                            Utils.writeContents(newFile, Utils.readContents(f));


                            //replace the information in the stagingArea field
                            info.stagingArea.remove(fn);
                            info.stagingArea.put(fn, newID);
                        } catch (IOException e) {
                            System.out.println("Add wrong 1");
                        }
                    }
                } else {
                    //If this file not tracked in the current commit, add it to staging area
                    if (!info.head.getFilesTracked().containsKey(fn)) {
                        try {
                            //get the SHA1ID for that file
                            String id = fileIDGenerator(f);
                            //Make a copy of that file and store it in the staging area
                            // folder with its SHA1ID as its name
                            String path = ".gitlet/stagingarea/" + id;
                            File newFile = new File(path);
                            newFile.createNewFile();
                            Utils.writeContents(newFile, Utils.readContents(f));

                            //Store information about this file into
                            // stagingarea field in info package
                            info.stagingArea.put(fn, id);
                        } catch (IOException e) {
                            System.out.println("Add wrong 2");
                        }
                    } else {
                        //Get the SHA1ID of the version tracked by the most recent commit and
                        // the current version in the working directory
                        String newID = fileIDGenerator(f);
                        String oldID = info.head.getFilesTracked().get(fn);
                        //If the IDs are different, this file has been modified.
                        // Add it to staging area.
                        if (!newID.equals(oldID)) {
                            try {
                                File newFile = new File(".gitlet/stagingarea/" + newID);
                                newFile.createNewFile();
                                Utils.writeContents(newFile, Utils.readContents(f));

                                info.stagingArea.put(fn, newID);
                            } catch (IOException e) {
                                System.out.println("Add wrong 3");
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("File does not exist!");
        }
        serialization(info);
    }


    public static String fileIDGenerator(File f) {
        return Utils.sha1(Utils.readContents(f));
    }


    public void commit(String m) {
        //Deserialize my info package to get everything back
        info = deserialization();
        //Check if there is need to make the commit. If there is stagingArea is empty and there is
        //nothing to untrack, there is no need to commit
        if (info.stagingArea.size() > 0 || info.toUntrack.size() > 0) {
            //Make copies of every file in the staging area
            // and put them into .gitlet/blobs directory
            for (String a : info.stagingArea.keySet()) {
                try {
                    String newPath = ".gitlet/blobs/" + info.stagingArea.get(a);
                    File newFile = new File(newPath);
                    newFile.createNewFile();

                    String oldPath = ".gitlet/stagingarea/" + info.stagingArea.get(a);
                    File oldFile = new File(oldPath);

                    Utils.writeContents(newFile, Utils.readContents(oldFile));

                } catch (IOException e) {
                    System.out.println("Commit wrong 1");
                }
            }
            //Make the new commit with the message user types in
            Commit c = new Commit(m);
            //Set the parent of the new commit to the previous commit's SHA1ID
            c.setParent(info.head.getSHA1());
            //Set the new commit's trackedFiles to the previous commit's trackedFiles
            c.setFilesTracked(info.head.getFilesTracked());

            //For everything in the staging area,
            // if it already exists in the previous commit,
            // replace it with the new version
            //If it does not exist in the previous commit, add it to the new commit
            for (String a : info.stagingArea.keySet()) {
                if (c.getFilesTracked().containsKey(a)) {
                    c.getFilesTracked().remove(a);
                }
                c.getFilesTracked().put(a, info.stagingArea.get(a));
            }
            //Untrack every file that are to be untracked
            for (String b : info.toUntrack) {
                if (c.getFilesTracked().containsKey(b)) {
                    c.getFilesTracked().remove(b);
                }
            }
            //Once message, filesTracked, parent,
            // time are all set for the commit, we can generate the SHA1 ID.
            c.setSHA1();
            //Update the head for this whole gitlet.
            info.head = c;
            //Update branches field
            info.branches.replace(info.currBranch, info.head.getSHA1());

            //Update allCommits field
            info.allCommits.add(c.getSHA1());

            //Clear the stagingarea folder inside .gitlet.
            for (String a : info.stagingArea.keySet()) {
                File f = new File(".gitlet/stagingarea/" + info.stagingArea.get(a));
                f.delete();
            }
            //Clear stagingArea HashMap;
            info.stagingArea.clear();

            info.removedFiles.clear();

            info.toUntrack.clear();


            //Serialize the commit we just created
            serialization(c);

        } else {
            System.out.println("No changes added to the commit.");
        }

        //Serialize everything to put it into a package
        serialization(info);
    }

    public void rm(String m) {

        info = deserialization();

        if (info.head.getFilesTracked().containsKey(m)) {
            File toRemove = new File(m);
            if (toRemove.exists()) {
                toRemove.delete();
            }

            if (info.stagingArea.containsKey(m)) {
                info.stagingArea.remove(m);
                File rmFromSA = new File(".gitlet/stagingarea/" + info.stagingArea.get(m));
                rmFromSA.delete();
            }

            info.removedFiles.add(m);


            //System.out.println(info.removedFiles);

            info.toUntrack.add(m);

        } else {

            if (info.stagingArea.containsKey(m)) {
                info.stagingArea.remove(m);
                File rmFromSA = new File(".gitlet/stagingarea/" + info.stagingArea.get(m));
                rmFromSA.delete();
            } else {
                System.out.println("No reason to remove the file.");
            }

        }

        //Serialize everything back to the file
        serialization(info);
    }

    public void log() {

        info = deserialization();

        Commit head = info.head;
        String headID = head.getSHA1();
        System.out.println("===");
        System.out.println("Commit " + headID);
        System.out.println(head.getTime());
        System.out.println(head.getMessage());
        System.out.println();

        String curr = head.getParent();

        while (curr != null) {
            Commit a = deserialization(curr);
            System.out.println("===");
            System.out.println("Commit " + curr);
            System.out.println(a.getTime());
            System.out.println(a.getMessage());
            System.out.println();
            curr = a.getParent();
        }

        serialization(info);
    }


    public void globallog() {

        info = deserialization();

        for (String a : info.allCommits) {

            Commit toPrint = deserialization(a);
            System.out.println("===");
            System.out.println("Commit " + toPrint.getSHA1());
            System.out.println(toPrint.getTime());
            System.out.println(toPrint.getMessage());
            System.out.println();

        }
    }


    public void find(String m) {

        info = deserialization();

        int count = 0;

        for (String a : info.allCommits) {

            Commit toFind = deserialization(a);

            if (toFind.getMessage().equals(m)) {
                System.out.println(a);
                count++;
            }
        }

        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }


    public void status() {

        info = deserialization();

        System.out.println("=== Branches ===");

        System.out.println("*" + info.currBranch);

        for (String a : info.branches.keySet()) {
            if (!a.equals(info.currBranch)) {
                System.out.println(a);
            }
        }

        System.out.println();

        System.out.println("=== Staged Files ===");

        for (String a : info.stagingArea.keySet()) {
            System.out.println(a);
        }

        System.out.println();


        System.out.println("=== Removed Files ===");
        for (String a : info.removedFiles) {
            System.out.println(a);
        }

        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println();

        System.out.println("=== Untracked Files ===");

        System.out.println();

    }


        /*
        System.out.println("=== Modifications Not Staged For Commit ===");

        //Trying to find files modified but not staged
        and put into a HashMap with first string the file name
        //and second string the type of modification(modified or deleted)
        HashMap<String,String> modifiedNotStaged = new HashMap();
        for (String a: head.filesList){
            File f = new File(a);
            if (f.exists()){
                if(!fileComparator(f, head.filesTracked.get(a))){
                    if (!stagingArea.containsKey(a)){
                        modifiedNotStaged.put(a, "modified");
                    }
                }
            }
        }

        for (String a: stagingArea.keySet()){
            File f = new File(a);
            if (!f.exists()){
                modifiedNotStaged.put(a, "deleted");
            } else{
                if (!fileComparator(f, stagingArea.get(a))){
                    modifiedNotStaged.put(a, "modified");
                }
            }
        }

        for (String a: modifiedNotStaged.keySet()){
            System.out.println(a + " (" + modifiedNotStaged.get(a) + ")");
        }

        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String a: unTrackedFiles){
            System.out.println(a);
        }

    }
    */


    public void checkoutf(String s) {
        info = deserialization();

        if (!info.head.getFilesTracked().containsKey(s)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            File f = new File(s);
            if (f.exists()) {
                f.delete();
            }

            try {
                f.createNewFile();
                File n = new File(".gitlet/blobs/" + info.head.getFilesTracked().get(s));
                Utils.writeContents(f, Utils.readContents(n));
            } catch (IOException e) {
                System.out.println("checkoutf wrong!");
            }
        }

        serialization(info);
    }

    public void checkoutcf(String a, String b) {
        info = deserialization();

        if (a.length() != 40) {
            for (String i : info.allCommits) {
                if (i.substring(0, a.length()).equals(a)) {
                    a = i;
                    break;
                }
            }
        }

        if (!info.allCommits.contains(a)) {
            System.out.println("No commit with that id exists.");
            return;
        }


        Commit temp = info.head;
        Commit checkouted = deserialization(a);
        info.head = checkouted;


        if (!info.head.getFilesTracked().containsKey(b)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            File f = new File(b);
            if (f.exists()) {
                f.delete();
            }

            try {
                f.createNewFile();
                File n = new File(".gitlet/blobs/" + info.head.getFilesTracked().get(b));
                Utils.writeContents(f, Utils.readContents(n));
            } catch (IOException e) {
                System.out.println("checkoutf wrong!");
            }
        }

        info.head = temp;

        serialization(info);
    }


    public void checkoutb(String s) {
        info = deserialization();

        if (!info.branches.containsKey(s)) {
            System.out.println("No such branch exists.");
            return;
        }

        if (info.currBranch.equals(s)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }


        String commitID = info.branches.get(s);
        Commit checkouted = deserialization(commitID);

        File currentDir = new File("").getAbsoluteFile();
        for (String a : Utils.plainFilenamesIn(currentDir)) {
            if (checkouted.getFilesTracked().containsKey(a)
                    && !info.head.getFilesTracked().containsKey(a)
                    && !info.stagingArea.keySet().contains(a)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                return;
            }
        }


        checkoutc(checkouted);
        info.currBranch = s;
        info.head = checkouted;
        info.stagingArea.clear();
        info.removedFiles.clear();


        serialization(info);
    }

    public void branch(String s) {
        info = deserialization();

        if (info.branches.containsKey(s)) {
            System.out.println("A branch with that name already exists.");
            return;
        } else {
            info.branches.put(s, info.head.getSHA1());
            if (!info.splitpoints.containsKey(info.head.getSHA1())) {
                ArrayList<String> newsplit = new ArrayList<>();
                newsplit.add(info.currBranch);
                newsplit.add(s);
                info.splitpoints.put(info.head.getSHA1(), newsplit);
            } else {
                info.splitpoints.get(info.head.getSHA1()).add(info.currBranch);
                info.splitpoints.get(info.head.getSHA1()).add(s);
            }
        }

        serialization(info);
    }

    public void rmbranch(String s) {
        info = deserialization();

        if (s.equals(info.currBranch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        } else if (!info.branches.containsKey(s)) {
            System.out.println("A branch with that name does not exist.");
        } else {
            info.branches.remove(s);
            for (String a : info.splitpoints.keySet()) {
                for (String i : info.splitpoints.get(a)) {
                    if (i.equals(s)) {
                        info.splitpoints.remove(a);
                    }
                }
            }
        }

        serialization(info);
    }

    public void reset(String s) {
        info = deserialization();

        if (s.length() != 6) {
            for (String i : info.allCommits) {
                if (i.substring(0, s.length()).equals(s)) {
                    s = i;
                    break;
                }
            }
        }

        if (!info.allCommits.contains(s)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        //track back that commit
        Commit checkouted = deserialization(s);

        File currentDir = new File("").getAbsoluteFile();
        for (String a : Utils.plainFilenamesIn(currentDir)) {
            if (!info.head.getFilesTracked().containsKey(a)
                    && !info.stagingArea.containsKey(a)
                    && checkouted.getFilesTracked().containsKey(a)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it or add it first.");
                return;
            }
        }

        checkoutc(checkouted);
        for (String a : info.splitpoints.keySet()) {
            for (String i : info.splitpoints.get(a)) {
                if (i.equals(info.currBranch)) {
                    info.splitpoints.remove(a);
                }
            }
        }
        for (String a : info.splitpoints.keySet()) {
            for (String i : info.splitpoints.get(a)) {
                if (i.equals(s)) {
                    int j = info.splitpoints.get(a).indexOf(i);
                    if (j % 2 == 0) {
                        ArrayList<String> newsplit = new ArrayList<>();
                        newsplit.add(info.splitpoints.get(a).get(j + 1));
                        newsplit.add(info.currBranch);
                        info.splitpoints.put(a, newsplit);
                    } else {
                        ArrayList<String> newsplit = new ArrayList<>();
                        newsplit.add(info.splitpoints.get(a).get(j - 1));
                        newsplit.add(info.currBranch);
                        info.splitpoints.put(a, newsplit);
                    }
                }
            }
        }


        info.head = checkouted;
        info.branches.replace(info.currBranch, s);
        info.stagingArea.clear();
        info.removedFiles.clear();



        serialization(info);
    }

    public void checkoutc(Commit c) {

        for (String i : info.head.getFilesTracked().keySet()) {
            if (!c.getFilesTracked().containsKey(i)) {
                info.removedFiles.add(i);
                File f = new File(i);
                f.delete();
            }
        }

        for (String j : c.getFilesTracked().keySet()) {
            File f = new File(j);
            if (f.exists()) {
                f.delete();
            }
            try {
                f.createNewFile();
                File n = new File(".gitlet/blobs/" + c.getFilesTracked().get(j));
                Utils.writeContents(f, Utils.readContents(n));
            } catch (IOException e) {
                System.out.println("checkoutf inside checkc wrong!");
            }
        }
    }

    public String findsplitpoint(String s) {
        String splitpoint = null;
        for (String a : info.splitpoints.keySet()) {
            for (String i : info.splitpoints.get(a)) {
                if (i.equals(info.currBranch)) {
                    int j = info.splitpoints.get(a).indexOf(i);
                    if (j % 2 == 0) {
                        if (info.splitpoints.get(a).get(j + 1).equals(s)) {
                            splitpoint = a;
                        }
                    } else {
                        if (info.splitpoints.get(a).get(j - 1).equals(s)) {
                            splitpoint = a;
                        }
                    }
                }
            }
        }
        return splitpoint;
    }

    public void merge(String s) {
        info = deserialization();
        boolean nonexist = true;
        if (!info.stagingArea.isEmpty() || !info.toUntrack.isEmpty()) {
            System.out.println(" You have uncommitted changes.");
            return;
        }
        if (!info.branches.keySet().contains(s)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (info.currBranch.equals(s)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        String currcommitID = info.branches.get(info.currBranch);
        Commit currcommit = deserialization(currcommitID);
        String givencommitID = info.branches.get(s);
        Commit givencommit = deserialization(givencommitID);
        String splitpoint = findsplitpoint(s);
        Commit splitcommit = deserialization(splitpoint);
        if (givencommitID.equals(splitpoint)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (currcommitID.equals(splitpoint)) {
            info.branches.replace(info.currBranch, splitpoint);
            info.head = splitcommit;
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        HashMap<String, String> allpossible = new HashMap<>();
        allpossible.putAll(currcommit.getFilesTracked());
        allpossible.putAll(splitcommit.getFilesTracked());
        allpossible.putAll(givencommit.getFilesTracked());
        List<String> inside = Utils.plainFilenamesIn(new File("").getAbsoluteFile());

        for (String i : inside) {
            if (!currcommit.getFilesTracked().containsKey(i)
                    && !splitcommit.getFilesTracked().containsKey(i)
                    && givencommit.getFilesTracked().containsKey(i)) {
                System.out.println("There is an untracked file in the way;"
                        + " delete it or add it first.");
                return;
            }
        }
        for (String a : allpossible.keySet()) {
            boolean cu = currcommit.getFilesTracked().containsKey(a);
            boolean sp = splitcommit.getFilesTracked().containsKey(a);
            boolean gi = givencommit.getFilesTracked().containsKey(a);
            String x = currcommit.getFilesTracked().get(a);
            String y = splitcommit.getFilesTracked().get(a);
            String z = givencommit.getFilesTracked().get(a);
            if (cu && sp && gi && x.equals(y) && !z.equals(y)) {
                info.head = givencommit;
                mergecheckout(a);
                info.head = currcommit;
                mergeadd(a);
            } else if (!cu && !sp && gi) {
                info.head = givencommit;
                mergecheckout(a);
                info.head = currcommit;
                mergeadd(a);
            } else if (sp && Objects.equals(x, y) && !gi) {
                mergerm(a);
            } else if (!Objects.equals(y, x) && !Objects.equals(y, z)) {
                writecontent(currcommit.getFilesTracked().get(a),
                        givencommit.getFilesTracked().get(a), a);

                System.out.println("Encountered a merge conflict.");
                nonexist = false;
            }
        }
        if (nonexist) {
            mergecommit("Merged " + info.currBranch + " with " + s + ".");
        }
        serialization(info);
    }

    public static void writecontent(String a, String b, String c) {
        try {
            PrintWriter writer = new PrintWriter(c, "UTF-8");
            writer.println("<<<<<<< HEAD");
            if (a != null) {
                Scanner input1 = new Scanner(new File(".gitlet/blobs/" + a));
                while (input1.hasNextLine()) {
                    writer.println(input1.nextLine());
                }
            }
            writer.println("=======");
            if (b != null) {
                Scanner input2 = new Scanner(new File(".gitlet/blobs/" + b));
                while (input2.hasNextLine()) {
                    writer.println(input2.nextLine());
                }
            }
            writer.println(">>>>>>>");
            writer.close();
        } catch (IOException e) {
            System.out.println("Printing contents wrong!");
        }
    }

    public void mergecheckout(String s) {

        if (!info.head.getFilesTracked().containsKey(s)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            File f = new File(s);
            if (f.exists()) {
                f.delete();
            }

            try {
                f.createNewFile();
                File n = new File(".gitlet/blobs/" + info.head.getFilesTracked().get(s));
                Utils.writeContents(f, Utils.readContents(n));
            } catch (IOException e) {
                System.out.println("checkoutf wrong!");
            }
        }

    }

    public void mergerm(String m) {


        if (info.head.getFilesTracked().containsKey(m)) {
            File toRemove = new File(m);
            if (toRemove.exists()) {
                toRemove.delete();
            }

            if (info.stagingArea.containsKey(m)) {
                info.stagingArea.remove(m);
                File rmFromSA = new File(".gitlet/stagingarea/" + info.stagingArea.get(m));
                rmFromSA.delete();
            }

            info.removedFiles.add(m);


            //System.out.println(info.removedFiles);

            info.toUntrack.add(m);
        } else {

            if (info.stagingArea.containsKey(m)) {
                info.stagingArea.remove(m);
                File rmFromSA = new File(".gitlet/stagingarea/" + info.stagingArea.get(m));
                rmFromSA.delete();
            } else {
                System.out.println("No reason to remove the file.");
            }

        }
    }

    public void mergecommit(String m) {

        //Check if there is need to make the commit. If there is stagingArea is empty and there is
        //nothing to untrack, there is no need to commit
        if (info.stagingArea.size() > 0 || info.toUntrack.size() > 0) {
            //Make copies of every file in the staging area
            // and put them into .gitlet/blobs directory
            for (String a : info.stagingArea.keySet()) {
                try {
                    String newPath = ".gitlet/blobs/" + info.stagingArea.get(a);
                    File newFile = new File(newPath);
                    newFile.createNewFile();

                    String oldPath = ".gitlet/stagingarea/" + info.stagingArea.get(a);
                    File oldFile = new File(oldPath);

                    Utils.writeContents(newFile, Utils.readContents(oldFile));

                } catch (IOException e) {
                    System.out.println("Commit wrong 1");
                }
            }
            //Make the new commit with the message user types in
            Commit c = new Commit(m);
            //Set the parent of the new commit to the previous commit's SHA1ID
            c.setParent(info.head.getSHA1());
            //Set the new commit's trackedFiles to the previous commit's trackedFiles
            c.setFilesTracked(info.head.getFilesTracked());

            //For everything in the staging area,
            // if it already exists in the previous commit,
            // replace it with the new version
            //If it does not exist in the previous commit, add it to the new commit
            for (String a : info.stagingArea.keySet()) {
                if (c.getFilesTracked().containsKey(a)) {
                    c.getFilesTracked().remove(a);
                }
                c.getFilesTracked().put(a, info.stagingArea.get(a));
            }
            //Untrack every file that are to be untracked
            for (String b : info.toUntrack) {
                if (c.getFilesTracked().containsKey(b)) {
                    c.getFilesTracked().remove(b);
                }
            }
            //Once message, filesTracked, parent,
            // time are all set for the commit, we can generate the SHA1 ID.
            c.setSHA1();
            //Update the head for this whole gitlet.
            info.head = c;
            //Update branches field
            info.branches.replace(info.currBranch, info.head.getSHA1());

            //Update allCommits field
            info.allCommits.add(c.getSHA1());

            //Clear the stagingarea folder inside .gitlet.
            for (String a : info.stagingArea.keySet()) {
                File f = new File(".gitlet/stagingarea/" + info.stagingArea.get(a));
                f.delete();
            }
            //Clear stagingArea HashMap;
            info.stagingArea.clear();

            info.removedFiles.clear();

            info.toUntrack.clear();

        } else {
            System.out.println("No changes added to the commit.");
        }
    }

    public void mergeadd(String fn) {
        File f = new File(fn);
        if (f.exists()) {
            //If this file is marked to be untracked, remove the mark
            if (info.toUntrack.contains(fn)) {
                info.toUntrack.remove(fn);
                info.removedFiles.remove(fn);
            } else {

                int size = info.stagingArea.size();
                //if this file is already in the staging area, check whether it has been modified
                if (info.stagingArea.containsKey(fn)) {

                    String newID = fileIDGenerator(f);
                    String oldID = info.stagingArea.get(fn);

                    if (!newID.equals(oldID)) {
                        //Delete the old file in .gitlet/stagingarea folder and create the new file
                        try {
                            File oldFile = new File(".gitlet/stagingarea/"
                                    + info.stagingArea.get(fn));
                            oldFile.delete();
                            File newFile = new File(".gitlet/stagingarea/" + newID);
                            newFile.createNewFile();
                            Utils.writeContents(newFile, Utils.readContents(f));


                            //replace the information in the stagingArea field
                            info.stagingArea.remove(fn);
                            info.stagingArea.put(fn, newID);
                        } catch (IOException e) {
                            System.out.println("Add wrong 1");
                        }
                    }
                } else {
                    //If this file not tracked in the current commit, add it to staging area
                    if (!info.head.getFilesTracked().containsKey(fn)) {
                        try {
                            //get the SHA1ID for that file
                            String id = fileIDGenerator(f);
                            //Make a copy of that file and store it in the staging area
                            // folder with its SHA1ID as its name
                            String path = ".gitlet/stagingarea/" + id;
                            File newFile = new File(path);
                            newFile.createNewFile();
                            Utils.writeContents(newFile, Utils.readContents(f));

                            //Store information about this file into
                            // stagingarea field in info package
                            info.stagingArea.put(fn, id);
                        } catch (IOException e) {
                            System.out.println("Add wrong 2");
                        }
                    } else {
                        //Get the SHA1ID of the version tracked by the most recent commit and
                        // the current version in the working directory
                        String newID = fileIDGenerator(f);
                        String oldID = info.head.getFilesTracked().get(fn);
                        //If the IDs are different, this file has been modified.
                        // Add it to staging area.
                        if (!newID.equals(oldID)) {
                            try {
                                File newFile = new File(".gitlet/stagingarea/" + newID);
                                newFile.createNewFile();
                                Utils.writeContents(newFile, Utils.readContents(f));

                                info.stagingArea.put(fn, newID);
                            } catch (IOException e) {
                                System.out.println("Add wrong 3");
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("File does not exist!");
        }
    }
}




