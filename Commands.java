package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.Formatter;
import java.util.List;


/** Class containing all possible commands in the Gitlet Library.
 * @author willharvey  */
public class Commands implements Serializable {

    /**
     * Current Working Directory.
     */
    static final File CWD = new File(".");

    /**
     * Main metadata folder.
     */
    static final File GITLET_DIR = Utils.join(CWD, ".gitlet");

    /**
     * Directory of Commit History.
     */
    static final File COMMITS = Utils.join(GITLET_DIR, "commits");

    /**
     * Directory of Branches.
     */
    static final File BRANCH_DIR = Utils.join(GITLET_DIR, "branches");

    /**
     * Directory of Blobs.
     */
    static final File BLOBS_DIR = Utils.join(GITLET_DIR, "blobs");

    /**
     * File that contains the LinkedListHashMap of the staging area to be added.
     */
    static final File STAGE_AREA_ADD = Utils.join(GITLET_DIR,
            "add");

    /**
     * File that contains the LinkedListHashMap of the staging area
     * to be removed.
     */
    static final File STAGE_AREA_RM = Utils.join(GITLET_DIR,
            "rm");

    /**
     * File that contains the HEAD branch.
     */
    static final File HEAD = Utils.join(GITLET_DIR, "HEAD");

    /**
     * File that contains the current branch.
     */
    static final File CURRENT_BRANCH = Utils.join(GITLET_DIR,
            "current branch");

    /**
     * Initializes the gitlet Repo.
     */
    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
            throw new GitletException("A Gitlet version-control "
                    + "system already exists in the current directory.");
        }
        setUpPersistence();
        LinkedHashMap<String, String> copyFiles = new LinkedHashMap<>();
        LinkedHashMap<String, Commit> myCommits = new LinkedHashMap<>();
        ArrayList<String> initialParents = new ArrayList<>();
        initialParents.add(null);
        initialParents.add("");
        Commit initial = new Commit("initial commit", initialParents,
                copyFiles);
        byte[] initString = Utils.serialize(initial);
        String initShai = Utils.sha1(initString);
        ArrayList<String> commits = new ArrayList<>();
        commits.add(initShai);
        Branch master = new Branch(true, "master", commits);
        myCommits.put(initShai, initial);
        File branch1 = Utils.join(BRANCH_DIR, master.getName());
        Utils.writeObject(COMMITS, myCommits);
        Utils.writeObject(branch1, master);
        Utils.writeObject(CURRENT_BRANCH, master);
        Utils.writeObject(HEAD, initial);
    }

    /**
     * Sets up the directories to be persisted.
     */
    public static void setUpPersistence() throws IOException {
        GITLET_DIR.mkdir();
        COMMITS.createNewFile();
        BRANCH_DIR.mkdir();
        BLOBS_DIR.mkdir();
        STAGE_AREA_ADD.createNewFile();
        STAGE_AREA_RM.createNewFile();
        HEAD.createNewFile();
        CURRENT_BRANCH.createNewFile();
        LinkedHashMap<String, String> add = new LinkedHashMap<>();
        ArrayList<String> rm = new ArrayList<>();
        Utils.writeObject(STAGE_AREA_ADD, add);
        Utils.writeObject(STAGE_AREA_RM, rm);
    }

    /** Stages fileName to be added, checking failure conidtions
     * on the way.
     * @param fileName - String of fileName.
     */
    public static void add(String fileName) {
        File copy = new File(fileName);
        if (!copy.exists()) {
            throw new GitletException("File doesn't exist");
        }
        Blob copyBlob = new Blob(fileName);
        byte[] blobStr = Utils.serialize(copyBlob.getContents());
        String blobShai = Utils.sha1(blobStr);
        File newBlob = Utils.join(BLOBS_DIR, blobShai);
        Utils.writeObject(newBlob, copyBlob);
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        ArrayList<String> stageRM = getStageRm();
        stageRM.remove(fileName);
        Commit head = Utils.readObject(HEAD, Commit.class);
        LinkedHashMap<String, String> headFiles = head.getFiles();
        String blobShaiInHead = headFiles.get(fileName);
        File versionInCWD = Utils.join(CWD, fileName);
        String contentsOfCWD = Utils.readContentsAsString(versionInCWD);
        byte[] cereal = Utils.serialize(contentsOfCWD);
        String sha1OfCWD = Utils.sha1(cereal);
        if (stageAdd.containsKey(fileName)) {
            stageAdd.replace(fileName, blobShai);
        } else if (sha1OfCWD.equals(blobShaiInHead)) {
            stageAdd.remove(fileName);
        } else {
            stageAdd.put(fileName, blobShai);
        }
        Utils.writeObject(STAGE_AREA_ADD, stageAdd);
        Utils.writeObject(STAGE_AREA_RM, stageRM);
    }

    /** Takes all files staged to be added and adds them to a new
     * commit instance, clearing both staging areas.
     * @param message - String commit message.
     */
    public static void commit(String message) {
        commitHelper(message, "");
    }

    /** Has all logic for the commit command, but also deals
     * with merge instances. Is called in merge and commit.
     * @param message - commit message.
     * @param mergeParentShai - sha1 of the merge parent ("") if
     *                        not a merge instance.
     */
    public static void commitHelper(String message, String mergeParentShai) {
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        ArrayList<String> rm = getStageRm();
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        if (stageAdd.isEmpty() && rm.isEmpty() && mergeParentShai.equals("")) {
            throw new GitletException("No changes added to the commit.");
        }
        Commit parent = Utils.readObject(HEAD, Commit.class);
        byte[] cereal = Utils.serialize(parent);
        String parentShai = Utils.sha1(cereal);
        Branch currentBranch = Utils.readObject(
                CURRENT_BRANCH, Branch.class);
        LinkedHashMap<String, String> files = deepCopyLHM(
                parent.getFiles());
        if (!rm.isEmpty()) {
            for (String removedFile : rm) {
                files.remove(removedFile);
            }
        }
        ArrayList<String> parents = new ArrayList<>();
        parents.add(parentShai);
        parents.add(mergeParentShai);
        Commit current = new Commit(message, parents, files);
        for (String fileName : stageAdd.keySet()) {
            if (current.getFiles().containsKey(fileName)) {
                current.getFiles().replace(fileName,
                        stageAdd.get(fileName));
            } else {
                current.getFiles().put(fileName, stageAdd.get(fileName));
            }
        }
        stageAdd.clear();
        Utils.writeObject(STAGE_AREA_ADD, stageAdd);
        byte[] newCommit = Utils.serialize(current);
        String name = Utils.sha1(newCommit);
        myCommits.put(name, current);
        Utils.writeObject(COMMITS, myCommits);
        Utils.writeObject(HEAD, current);
        currentBranch.getCommit().remove(parentShai);
        currentBranch.getCommit().add(name);
        Utils.writeObject(CURRENT_BRANCH, currentBranch);
        rm.clear();
        Utils.writeObject(STAGE_AREA_RM, rm);
        File currentBranchFile = Utils.join(
                BRANCH_DIR, currentBranch.getName());
        Utils.writeObject(currentBranchFile, currentBranch);
    }

    /** Starting from the head commit, prints out the history
     * of each commit along the current branch. Excludes any history
     * that differs at split point.
     */
    public static void log() {
        Formatter out = new Formatter();
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        Commit v = Utils.readObject(HEAD, Commit.class);
        byte[] cereal = Utils.serialize(v);
        String shai = Utils.sha1(cereal);
        String parentShai = v.getParent().get(0);
        while (v != null) {
            out.format("===\n");
            out.format("commit " + shai + "\n");
            Date now = v.getStamp();
            String pattern = ("EEE MMM d HH:mm:ss yyyy Z");
            SimpleDateFormat cherb = new SimpleDateFormat(pattern);
            out.format("Date: " + cherb.format(now) + "\n");
            out.format(v.getMessage() + "\n");
            if (v.getParent().get(0) == null) {
                break;
            } else {
                out.format("\n");
            }
            v = myCommits.get(parentShai);
            cereal = Utils.serialize(v);
            shai = Utils.sha1(cereal);
            parentShai = v.getParent().get(0);
        }
        System.out.println(out);
    }

    /** Prints out the commit history in random order. Prints out
     * every commit, even though in different paths after split points.
     */
    public static void globalLog() {
        Formatter out = new Formatter();
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        Set<String> alKeys = myCommits.keySet();
        int i = 1;
        for (String k : alKeys) {
            Commit v = myCommits.get(k);
            out.format("===\n");
            out.format("commit " + k + "\n");
            Date now = v.getStamp();
            String pattern = ("EEE MMM d HH:mm:ss yyyy Z");
            SimpleDateFormat cherb = new SimpleDateFormat(pattern);
            out.format("Date: " + cherb.format(now) + "\n");
            out.format(v.getMessage() + "\n");
            if (i == alKeys.size()) {
                break;
            } else {
                out.format("\n");
            }
        }
        System.out.println(out);
    }

    /** Checks out the file given, replacing its contents in the CWD
     * with its contents in the head commit. Does not stage file.
     * @param file - String name of file.
     */
    public static void checkout1(String file) {
        Commit head = Utils.readObject(HEAD, Commit.class);
        LinkedHashMap<String, String> allFiles = head.getFiles();
        if (!allFiles.containsKey(file)) {
            throw new GitletException("File does not exist in that commit.");
        }
        String blob = allFiles.get(file);
        File fileInCWD = Utils.join(CWD, file);
        File currBlob = Utils.join(BLOBS_DIR, blob);
        Blob checkoutBlob = Utils.readObject(currBlob, Blob.class);
        String contents = checkoutBlob.getContents();
        Utils.writeContents(fileInCWD, contents);
    }


    /** Checks out the given file, replacing the contents of the file in CWD
     * with the contents of the file in the commit with sha1 ID "shai". Does not
     * stage file.
     * @param shai - String containing the sha1 ID of the commit.
     * @param file - String fileName.
     */
    public static void checkout2(String shai, String file) {
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        shai = sha1Elongator(shai);
        if (!myCommits.containsKey(shai)) {
            throw new GitletException("No commit with that id exists.");
        }
        Commit correct = myCommits.get(shai);
        LinkedHashMap<String, String> allFiles = correct.getFiles();
        if (!allFiles.containsKey(file)) {
            throw new GitletException("File does not exist in that commit.");
        }
        String blobShai = allFiles.get(file);
        File blobToBeTaken = Utils.join(BLOBS_DIR, blobShai);
        File fileInCWD = Utils.join(CWD, file);
        Blob blobToOverwriteWith = Utils.readObject(blobToBeTaken, Blob.class);
        String contents = blobToOverwriteWith.getContents();
        Utils.writeContents(fileInCWD, contents);
    }

    /** Checks out the given branch, replacing all files in the
     * CWD with their versions in the head commit of the given
     * branch, deleting if necessary. Makes the given branch the
     * current branch and the head commit of the given branch the
     * new head commit.
     * @param brName - String referring to the name of the
     *               Branch to be checkout out.
     */
    public static void checkout3(String brName) {
        List<String> allBr = Utils.plainFilenamesIn(BRANCH_DIR);
        Branch currBr = Utils.readObject(CURRENT_BRANCH, Branch.class);
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        ArrayList<String> currCommShai = currBr.getCommit();
        Commit commitOfCurrentBranch = myCommits.get(
                currCommShai.get(currCommShai.size() - 1));
        LinkedHashMap<String, String> commFiles =
                commitOfCurrentBranch.getFiles();
        if (!allBr.contains(brName)) {
            throw new GitletException("No such branch exists.");
        } else if (brName.equals(currBr.getName())) {
            throw new GitletException("No need to checkout the "
                    + "current branch.");
        }
        checkout3FailureHelper(brName);
        File fileOfBranch = Utils.join(BRANCH_DIR, brName);
        Branch checkoutBr = Utils.readObject(fileOfBranch, Branch.class);
        ArrayList<String> checkoutCommShai = checkoutBr.getCommit();
        Commit commitOfCheckoutBranch = myCommits.get(
                checkoutCommShai.get(checkoutCommShai.size() - 1));
        LinkedHashMap<String, String> checkoutFiles =
                commitOfCheckoutBranch.getFiles();
        for (String fileName : commFiles.keySet()) {
            if (checkoutFiles.containsKey(fileName)) {
                checkout2(checkoutCommShai.get(
                        checkoutCommShai.size() - 1), fileName);
            } else if (!checkoutFiles.containsKey(fileName)) {
                File youFinnaBeGone = Utils.join(CWD, fileName);
                Utils.restrictedDelete(youFinnaBeGone);
            }
        }
        for (String checkoutFileName : checkoutFiles.keySet()) {
            if (!commFiles.containsKey(checkoutFileName)) {
                String blobShai = checkoutFiles.get(checkoutFileName);
                File blobToBeTaken = Utils.join(BLOBS_DIR, blobShai);
                File fileInCWD = Utils.join(CWD, checkoutFileName);
                Blob blobToOverwriteWith = Utils.readObject(
                        blobToBeTaken, Blob.class);
                String contents = blobToOverwriteWith.getContents();
                Utils.writeContents(fileInCWD, contents);
            }
        }
        Utils.writeObject(CURRENT_BRANCH, checkoutBr);
        Utils.writeObject(fileOfBranch, checkoutBr);
        File currentBranchFile = Utils.join(BRANCH_DIR, currBr.getName());
        Utils.writeObject(currentBranchFile, currBr);
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        stageAdd.clear();
        Utils.writeObject(STAGE_AREA_ADD, stageAdd);
        Utils.writeObject(HEAD, commitOfCheckoutBranch);
    }

    /** Helper for a specific failure case in checkout3.
     * If a file is untracked in the current branch and
     * would be overwritten by the checkout, throw error.
     * @param brName - String branch name.
     */
    public static void checkout3FailureHelper(String brName) {
        Branch currentBranch = Utils.readObject(CURRENT_BRANCH, Branch.class);
        String commitIDOfCurrentBranch = currentBranch.getCommit().get(
                currentBranch.getCommit().size() - 1);
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        Commit actualCommitOfCurrentBranch = myCommits.get(
                commitIDOfCurrentBranch);
        LinkedHashMap<String, String> filesOfCommit =
                actualCommitOfCurrentBranch.getFiles();
        List<String> checker = Utils.plainFilenamesIn(CWD);
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        for (String fileInCWD : checker) {
            if (stageAdd.containsKey(fileInCWD)) {
                break;
            } else if (!filesOfCommit.containsKey(fileInCWD)
                    && !fileInCWD.equals(".gitignore")
                    && !fileInCWD.equals("Makefile")
                    && !fileInCWD.equals("proj3.iml")) {
                File fileBeingChecked = Utils.join(CWD, fileInCWD);
                String contentsOfFile = Utils.readContentsAsString(
                        fileBeingChecked);
                byte[] contentsCereal = Utils.serialize(contentsOfFile);
                String contentsShai = Utils.sha1(contentsCereal);
                File fileOfBranch = Utils.join(BRANCH_DIR, brName);
                Branch currBr = Utils.readObject(fileOfBranch, Branch.class);
                ArrayList<String> currCommShai = currBr.getCommit();
                Commit commitOfCheckoutBranch = myCommits.get(
                        currCommShai.get(currCommShai.size() - 1));
                LinkedHashMap<String, String> commFiles =
                        commitOfCheckoutBranch.getFiles();
                String shaiInCheckoutBranch = commFiles.get(fileInCWD);
                if (!contentsShai.equals(shaiInCheckoutBranch)) {
                    throw new GitletException("There is an untracked file "
                            + "in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
        }
    }

    /** Prints out each commit ID that refers to a commit instance
     * with its message instance variable equalling the parammeter
     * passed in.
     * @param message - String referring to a commit message.
     */
    public static void find(String message) {
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        Formatter out = new Formatter();
        int i = 0;
        for (String k : myCommits.keySet()) {
            Commit curr = myCommits.get(k);
            String currMessage = curr.getMessage();
            if (currMessage.equals(message)) {
                if (i > 0) {
                    out.format("\n");
                }
                out.format(k);
                i++;
            }
        }
        if (i == 0) {
            throw new GitletException("Found no commit with that message.");
        }
        System.out.println(out);
    }

    /** Prints out the status of the repository of the current branch.
     * Prints out every branch that exists, every file in the staging area,
     * and every file that has been removed.
     */
    public static void status() {
        if (!GITLET_DIR.exists()) {
            throw new GitletException("Not in an initialized Gitlet "
                    + "directory.");
        }
        Formatter out = new Formatter();
        List<String> allBr = Utils.plainFilenamesIn(BRANCH_DIR);
        Branch currentBr = Utils.readObject(CURRENT_BRANCH, Branch.class);
        out.format("=== Branches ===" + "\n");
        for (String brName : allBr) {
            if (brName.equals(currentBr.getName())) {
                out.format("*" + brName);
            } else {
                out.format(brName);
            }
            out.format("\n");
        }
        out.format("\n" + "=== Staged Files ===" + "\n");
        List<String> filesInCWD = Utils.plainFilenamesIn(CWD);
        if (!getStageAdd().isEmpty()) {
            for (String fileInCWD : filesInCWD) {
                if (styleIndicator(fileInCWD)
                        && getStageAdd().containsKey(fileInCWD)) {
                    out.format(fileInCWD + "\n");
                }
            }
        }
        out.format("\n" + "=== Removed Files ===" + "\n");
        ArrayList<String> rm = getStageRm();
        if (!rm.isEmpty()) {
            rm.sort(String::compareTo);
            for (String removedFile : rm) {
                out.format(removedFile + "\n");
            }
        }
        out.format("\n" + "=== Modifications Not Staged For Commit ===" + "\n");
        LinkedHashMap<String, String> modNoStage = modNoStage();
        int j = modNoStage.size();
        for (String modFiles : modNoStage.keySet()) {
            out.format(modFiles + " (" + modNoStage.get(modFiles) + ")");
            if (j != 0) {
                out.format("\n");
                j--;
            }
        }
        out.format("\n" + "=== Untracked Files ===");
        ArrayList<String> untracked = untracked();
        if (!untracked.isEmpty()) {
            out.format("\n");
        }
        int k = untracked.size();
        for (String file : untracked) {
            out.format(file);
            if (k != 0) {
                out.format("\n");
                k--;
            }
        }
        System.out.println(out);
    }

    /** Can't have shit in Detroit man.
     *
     * @param fileInCWD - yur.
     * @return -wassup.
     */
    public static boolean styleIndicator(String fileInCWD) {
        return (!fileInCWD.equals(".gitignore")
                && !fileInCWD.equals("Makefile")
                && !fileInCWD.equals("proj3.iml"));
    }

    /** Creates a new branch with "name", and points it to the current
     * head commit. Does NOT make it the current branch.
     * @param name - String referring to name of new branch
     */
    public static void branch(String name) {
        List<String> myBranches = Utils.plainFilenamesIn(BRANCH_DIR);
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        for (String brName : myBranches) {
            if (brName.equals(name)) {
                throw new GitletException("A branch with that "
                        + "name already exists.");
            }
        }
        Commit head = Utils.readObject(HEAD, Commit.class);
        byte[] cerealBefore = Utils.serialize(head);
        String sha1Before = Utils.sha1(cerealBefore);
        byte[] cereal = Utils.serialize(head);
        String sha1 = Utils.sha1(cereal);
        ArrayList<String> commits = new ArrayList<>();
        commits.add(sha1);
        Branch newBranch = new Branch(false, name, commits);
        File newBranchFile = Utils.join(BRANCH_DIR, newBranch.getName());
        Utils.writeObject(newBranchFile, newBranch);
        Utils.writeObject(HEAD, head);
        LinkedHashMap<String, Commit> newMyCommits = branchHelper(
                myCommits, sha1Before, sha1, head);
        Utils.writeObject(COMMITS, newMyCommits);
        Branch currentBranch = Utils.readObject(CURRENT_BRANCH, Branch.class);
        currentBranch.getCommit().clear();
        currentBranch.getCommit().add(sha1);
        Utils.writeObject(CURRENT_BRANCH, currentBranch);
    }

    /** Removes given file from the CWD, and adds it to the
     * staging area to be removed.
     * @param fileName - String name of file to be deleted
     */
    public static void remove(String fileName) {
        Commit head = Utils.readObject(HEAD, Commit.class);
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        ArrayList<String> stageRm = getStageRm();
        if (!head.getFiles().containsKey(fileName)
                && !stageAdd.containsKey(fileName)) {
            throw new GitletException("No reason to remove the file.");
        } else if (stageAdd.containsKey(fileName)) {
            stageAdd.remove(fileName);
            Utils.writeObject(STAGE_AREA_ADD, stageAdd);
        } else if (head.getFiles().containsKey(fileName)) {
            stageRm.add(fileName);
            Utils.restrictedDelete(fileName);
            Utils.writeObject(STAGE_AREA_ADD, stageAdd);
            Utils.writeObject(STAGE_AREA_RM, stageRm);
        }
    }

    /** Removes branch from its current head commit, destroying the pointer
     * related to this branch and commit. Does not change commit.
     * @param brName - name of Branch to delete
     */
    public static void removeBranch(String brName) {
        List<String> allBr = Utils.plainFilenamesIn(BRANCH_DIR);
        Branch current = Utils.readObject(CURRENT_BRANCH, Branch.class);
        if (!allBr.contains(brName)) {
            throw new GitletException(
                    "A branch with that name does not exist.");
        } else if (brName.equals(current.getName())) {
            throw new GitletException("Cannot remove the current branch.");
        }
        ArrayList<String> shaiOfCommitWithBranch = current.getCommit();
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        Commit commitBranch = myCommits.get(shaiOfCommitWithBranch.get(0));
        Utils.writeObject(COMMITS, myCommits);
        File toDelete = Utils.join(BRANCH_DIR, brName);
        toDelete.delete();
    }

    /** Replaces each file in CWD to be the version in the commit
     * represented by commitID, deleting if absent. Resets
     * the current branch's head commit to be the commit
     * represented by commitID.
     * @param commitID - sha1 ID of commit to reset.
     */
    public static void reset(String commitID) {
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        Branch currentBr = Utils.readObject(CURRENT_BRANCH, Branch.class);
        Commit head = Utils.readObject(HEAD, Commit.class);
        LinkedHashMap<String, String> headAllFiles = head.getFiles();
        ArrayList<String> stageRm = getStageRm();
        if (!myCommits.containsKey(commitID)) {
            throw new GitletException("No commit with that id exists.");
        }
        checkout3FailureHelper(currentBr.getName());
        Commit resetCommit = myCommits.get(commitID);
        LinkedHashMap<String, String> allFiles = resetCommit.getFiles();
        for (String fileName : headAllFiles.keySet()) {
            if (!allFiles.containsKey(fileName)) {
                remove(fileName);
            } else {
                checkout2(commitID, fileName);
            }
        }
        for (String file : allFiles.keySet()) {
            if (!headAllFiles.containsKey(file)) {
                File newFile = Utils.join(CWD, file);
                String blobShai = allFiles.get(file);
                File blobToBeTaken = Utils.join(BLOBS_DIR, blobShai);
                Blob blobToOverwriteWith = Utils.readObject(
                        blobToBeTaken, Blob.class);
                String contents = blobToOverwriteWith.getContents();
                Utils.writeContents(newFile, contents);

            }
        }
        currentBr.getCommit().clear();
        currentBr.getCommit().add(commitID);
        File newBr = Utils.join(BRANCH_DIR, currentBr.getName());
        Utils.writeObject(CURRENT_BRANCH, currentBr);
        Utils.writeObject(newBr, currentBr);
        Utils.writeObject(HEAD, resetCommit);
        Utils.writeObject(COMMITS, myCommits);
        if (!stageAdd.isEmpty() || !stageRm.isEmpty()) {
            for (String addFile : stageAdd.keySet()) {
                Utils.restrictedDelete(addFile);
            }
            stageAdd.clear();
            Utils.writeObject(STAGE_AREA_ADD, stageAdd);
            stageRm.clear();
            Utils.writeObject(STAGE_AREA_RM, stageRm);
        }
    }

    /** Merges the given branch into the given branch, replacing files
     * and finding merge conflicts as necessary. Creates a new merge commit
     * instance by calling commitHelper with the head commit of the given branch
     * as the second parent.
     * @param brName - given branch to merge into current branch.
     */
    public static void merge(String brName) {
        boolean conflict = false;
        existFailure(Utils.plainFilenamesIn(BRANCH_DIR), brName);
        File fileOfMergedBranch = Utils.join(BRANCH_DIR, brName);
        Branch mBranch = Utils.readObject(fileOfMergedBranch, Branch.class);
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        failures(brName, mBranch);
        String splitPoint = splitPointFinder(brName);
        spFailures(splitPoint, mBranch, getCurrBr(), brName);
        Commit spCommit = myCommits.get(splitPoint);
        LinkedHashMap<String, String> filesInSP = spCommit.getFiles();
        Commit currBrCommit = myCommits.get(getRightCommit(getCurrBr()));
        Commit mergeBrCommit = myCommits.get(getRightCommit(mBranch));
        LinkedHashMap<String, String> mergeFiles = mergeBrCommit.getFiles();
        LinkedHashMap<String, String> currentFiles = currBrCommit.getFiles();
        Set<String> allFiles = new TreeSet<>(filesInSP.keySet());
        allFiles.addAll(mergeBrCommit.getFiles().keySet());
        allFiles.addAll(currBrCommit.getFiles().keySet());
        for (String fileName : allFiles) {
            String cInSP = spCommit.getFiles().get(fileName);
            String mBrC = mergeBrCommit.getFiles().get(fileName);
            String cBrC = currBrCommit.getFiles().get(fileName);
            if (filesInSP.containsKey(fileName)) {
                if (!mergeFiles.containsKey(fileName) && cInSP.equals(cBrC)) {
                    remove(fileName);
                } else if (indicatorHelper(cInSP, mBrC, cBrC)) {
                    checkout2(getRightCommit(mBranch), fileName);
                    add(fileName);
                } else if (mergeConflictIndicator(mBrC, cInSP, cBrC)) {
                    mergeConflict(fileName, mBrC, cBrC);
                    add(fileName);
                    conflict = true;
                }
            } else if (mergeBrCommit.getFiles().containsKey(fileName)) {
                if (!currentFiles.containsKey(fileName)) {
                    checkout2(getRightCommit(mBranch), fileName);
                    add(fileName);
                } else {
                    if (mergeConflictIndicator(mBrC, cInSP, cBrC)) {
                        mergeConflict(fileName, mBrC, cBrC);
                        add(fileName);
                        conflict = true;
                    }
                }
            } else if (currBrCommit.getFiles().containsKey(fileName)) {
                if (mergeConflictIndicator(mBrC, cInSP, cBrC)) {
                    mergeConflict(fileName, mBrC, cBrC);
                    add(fileName);
                    conflict = true;
                }
            }
        }
        String message = myMess(brName, getCurrBr().getName());
        if (conflict) {
            commitHelper(message, getRightCommit(mBranch));
            throw new GitletException("Encountered a merge conflict.");
        }
        commitHelper(message, getRightCommit(mBranch));
    }

    /** Style check bullshit.
     *
     * @param cInSP - string.
     * @param mBrC - string.
     * @param cBrC - string.
     * @return boolean.
     */
    public static boolean indicatorHelper(
            String cInSP, String mBrC, String cBrC) {
        return (cBrC != null && cBrC.equals(cInSP)
                && !mBrC.equals(cInSP));
    }

    /** Style check message.
     *
     * @param brName - string.
     * @param currName - string.
     * @return string.
     */
    public static String myMess(String brName, String currName) {
        return "Merged " + brName + " into "
                + currName + ".";
    }

    /** Style check BS.
     *
     * @param allBr - list.
     * @param brName - string.
     */
    public static void existFailure(List<String> allBr, String brName) {
        if (!allBr.contains(brName)) {
            throw new GitletException(
                    "A branch with that name does not exist.");
        }
    }


    /** Throws general merge errors according to spec.
     *
     *
     * @param brName - string.
     * @param mergeBranch - branch.
     *
     */
    public static void failures(
            String brName, Branch mergeBranch) {
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        ArrayList<String> rm = getStageRm();
        Branch currentBranch = getCurrBr();
        if (mergeBranch != null) {
            checkout3FailureHelper(brName);
        }
        if (!stageAdd.isEmpty() || !rm.isEmpty()) {
            throw new GitletException("You have uncommitted changes.");
        }
        if (currentBranch.getName().equals(brName)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
    }

    /** Throws errors for split points according to spec.
     *
     * @param splitPoint - string.
     * @param mergeBranch - string.
     * @param currentBranch - branch.
     * @param brName - string.
     */
    public static void spFailures(String splitPoint, Branch mergeBranch,
                                  Branch currentBranch, String brName) {
        if (splitPoint.equals(getRightCommit(mergeBranch))) {
            throw new GitletException(
                    "Given branch is an ancestor of the current branch.");
        } else if (splitPoint.equals(getRightCommit(currentBranch))) {
            checkout3(brName);
            throw new GitletException("Current branch fast-forwarded.");
        }
    }

    /** Deep copies a LinkedHashMap<String, String>.
     * @param toCopy - LHM to be copied.
     * @return Deep copy of toCopy.
     */
    public static LinkedHashMap<String, String> deepCopyLHM(
            LinkedHashMap<String, String> toCopy) {
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        for (String id : toCopy.keySet()) {
            toReturn.put(id, toCopy.get(id));
        }
        return toReturn;
    }

    /** Helper function to assist the branch method to successfully update
     * the myCommits variable.
     * @param toHelp LHM myCommits to update
     * @param current old value of sha1
     * @param replace new value of sha1
     * @param commit commit to now be the "replace" value
     * @return updated LHM myCommits
     */
    public static LinkedHashMap<String, Commit> branchHelper(
            LinkedHashMap<String, Commit> toHelp,
            String current, String replace, Commit commit) {
        LinkedHashMap<String, Commit> toReturn = new LinkedHashMap<>();
        for (String id : toHelp.keySet()) {
            if (id.equals(current)) {
                toReturn.put(replace, commit);
            } else {
                toReturn.put(id, toHelp.get(id));
            }
        }
        return toReturn;
    }

    /** Given a branch name, get a history of all parents of the head commit.
     * @param brName - branch name to get history of.
     * @return ArrayList where each value is a sha1 ID of a commit.
     */
    public static ArrayList<String> getBranchHistory(String brName) {
        File fileOfBranch = Utils.join(BRANCH_DIR, brName);
        Branch branch = Utils.readObject(fileOfBranch, Branch.class);
        LinkedHashMap<String, Commit> myCommits = getMyCommits();
        ArrayList<String> commitOfCurrBr = branch.getCommit();
        String startingPointCurrBr = commitOfCurrBr.get(
                commitOfCurrBr.size() - 1);
        Commit currentCommit = myCommits.get(startingPointCurrBr);
        ArrayList<String> branchHist = new ArrayList<>();
        while (currentCommit != null) {
            branchHist.add(startingPointCurrBr);
            if (!currentCommit.getParent().get(1).equals("")) {
                branchHist.add(currentCommit.getParent().get(1));
            }
            startingPointCurrBr = currentCommit.getParent().get(0);
            currentCommit = myCommits.get(startingPointCurrBr);
        }
        return branchHist;
    }

    /** Given a branch name, finds the split point between that
     * branch and the current branch.
     * @param brName - branch name.
     * @return - sha1 ID of the split point that refers to a commit.
     */
    public static String splitPointFinder(String brName) {
        Commit head = Utils.readObject(HEAD, Commit.class);
        byte[] cereal = Utils.serialize(head);
        String headSha1 = Utils.sha1(cereal);
        Branch currBr = Utils.readObject(CURRENT_BRANCH, Branch.class);
        if (brName.equals(currBr.getName())) {
            return headSha1;
        }
        ArrayList<String> currentHistory = getBranchHistory(currBr.getName());
        ArrayList<String> mergeHistory = getBranchHistory(brName);
        for (int i = 0; i < currentHistory.size(); i += 1) {
            if (mergeHistory.contains(currentHistory.get(i))) {
                return currentHistory.get(i);
            }
        }
        return null;
    }

    /** Helper function for merge that is only called when there is a merge
     * conflict. Modifies the conflicted file as defined in the spec.
     * @param file - conflicted file name.
     * @param mBrContents - contents of conflicted file in merge Branch.
     * @param currBrContents - contents of conflicted file in current Branch.
     */
    public static void mergeConflict(
            String file, String mBrContents, String currBrContents) {
        if (mBrContents == null) {
            File blobFileOfCurr = Utils.join(BLOBS_DIR, currBrContents);
            Blob currBlob = Utils.readObject(blobFileOfCurr, Blob.class);
            String contentsCurr = currBlob.getContents();
            formatHelper(file, "", contentsCurr);
        } else if (currBrContents == null) {
            File blobFileOfMerge = Utils.join(BLOBS_DIR, mBrContents);
            Blob mergeBlob = Utils.readObject(blobFileOfMerge, Blob.class);
            String contentsMerge = mergeBlob.getContents();
            formatHelper(file, contentsMerge, "");
        } else {
            File blobFileOfMerge = Utils.join(BLOBS_DIR, mBrContents);
            File blobFileOfCurr = Utils.join(BLOBS_DIR, currBrContents);
            Blob currBlob = Utils.readObject(blobFileOfCurr, Blob.class);
            String contentsCurr = currBlob.getContents();
            Blob mergeBlob = Utils.readObject(blobFileOfMerge, Blob.class);
            String contentsMerge = mergeBlob.getContents();
            formatHelper(file, contentsMerge, contentsCurr);
        }
    }

    /** Helper function to format the conflicted file
     * according to the spec.
     * @param file - file name.
     * @param contentsMerge - contents of file in merged branch.
     * @param contentsCurr - contents of file in current branch.
     */
    public static void formatHelper(String file,
                                    String contentsMerge, String contentsCurr) {
        String overwrite = "<<<<<<< HEAD" + "\n"
                + contentsCurr
                + "=======" + "\n"
                + contentsMerge
                + ">>>>>>>" + "\n";
        File conflictFile = Utils.join(CWD, file);
        Utils.writeContents(conflictFile, overwrite);
    }

    /** Helper function used in merge command to indicate
     * whether or not there is a merge conflict.
     * @param mBrContents - contents of file in merged branch.
     * @param contentsInSP - contents of file in split point.
     * @param currBrContents - contents of file in current branch.
     * @return boolean indicating if there is a conflict.
     */
    public static boolean mergeConflictIndicator(
            String mBrContents, String contentsInSP, String currBrContents) {
        if (mBrContents == null && currBrContents != null) {
            if (!currBrContents.equals(contentsInSP)
                    && contentsInSP != null) {
                return true;
            }
        } else if ((mBrContents != null && currBrContents == null)) {
            if (!mBrContents.equals(contentsInSP)
                    && contentsInSP != null) {
                return true;
            }
        } else if ((mBrContents != null && currBrContents != null)) {
            if ((!mBrContents.equals(currBrContents)
                    && !mBrContents.equals(contentsInSP)
                    && !currBrContents.equals(contentsInSP))) {
                return true;
            }
        } else if (contentsInSP == null) {
            if (!mBrContents.equals(currBrContents)) {
                return true;
            }
            return true;
        }
        return false;
    }

    /** Just for style check.
     *
     */
    public static final int TARGET = 40;

    /** Helper function to elongate a sha1 ID if
     * a shortened version was provided.
     * @param shai - sha1 ID.
     * @return shai or elongated shai.
     */
    public static String sha1Elongator(String shai) {
        if (shai.length() == TARGET) {
            return shai;
        } else {
            LinkedHashMap<String, Commit> myCommits = getMyCommits();
            for (String fullShai : myCommits.keySet()) {
                String sub = fullShai.substring(0, 8);
                if (shai.equals(sub)) {
                    shai = fullShai;
                    break;
                }
            } return shai;
        }
    }

    /** Helper for style check.
     *
     * @param branch - string.
     * @return -string.
     */
    public static String getRightCommit(Branch branch) {
        return branch.getCommit().get(
                branch.getCommit().size() - 1);
    }


    /** Helper for style check.
     *
     * @return LHM.
     */
    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, Commit> getMyCommits() {
        return Utils.readObject(
                COMMITS, LinkedHashMap.class);
    }


    /** Helper for style check.
     *
     * @return LHM.
     */
    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, String> getStageAdd() {
        return Utils.readObject(
                STAGE_AREA_ADD, LinkedHashMap.class);
    }


    /** Helper for style check.
     *
     * @return AL.
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<String> getStageRm() {
        return  Utils.readObject(
                STAGE_AREA_RM, ArrayList.class);
    }

    /** Helper for style check.
     *
     * @return Branch.
     */
    public static Branch getCurrBr() {
        return Utils.readObject(
                CURRENT_BRANCH, Branch.class);
    }

    /** Helper to get all untracked files.
     *
     * @return sheeeeesh.
     */
    public static ArrayList<String> untracked() {
        Commit head = Utils.readObject(HEAD, Commit.class);
        List<String> filesCWD = Utils.plainFilenamesIn(CWD);
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        ArrayList<String> rm = getStageRm();
        ArrayList<String> toReturn = new ArrayList<>();
        for (String fileInCWD : filesCWD) {
            if (!fileInCWD.equals(".gitignore")
                    && !fileInCWD.equals("Makefile")
                    && !fileInCWD.equals("proj3.iml")) {
                if (!head.getFiles().containsKey(fileInCWD)
                        && !stageAdd.containsKey(fileInCWD)
                        && !rm.contains(fileInCWD)) {
                    toReturn.add(fileInCWD);
                }
            }

        } return toReturn;
    }

    /** Helper to get all files modified but not staged.
     *
     * @return yo mama.
     */
    public static LinkedHashMap<String, String> modNoStage() {
        Commit head = Utils.readObject(HEAD, Commit.class);
        List<String> filesCWD = Utils.plainFilenamesIn(CWD);
        LinkedHashMap<String, String> stageAdd = getStageAdd();
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        ArrayList<String> rm = getStageRm();
        for (String headFile : head.getFiles().keySet()) {
            if (filesCWD.contains(headFile)
                    && !checkContents(headFile, "head")
                    && !stageAdd.containsKey(headFile)) {
                toReturn.put(headFile, "modified");
            } else if (!filesCWD.contains(headFile)
                    && !rm.contains(headFile)) {
                toReturn.put(headFile, "deleted");
            }
        }
        for (String stagedFile : stageAdd.keySet()) {
            if (!filesCWD.contains(stagedFile)) {
                toReturn.put(stagedFile, "deleted");
            } else if  (!checkContents(stagedFile, "stage")) {
                toReturn.put(stagedFile, "modified");
            }
        } return toReturn;
    }

    /** Checks contents of a file.
     *
     * @param fileName LFG.
     * @param check cherb.
     * @return whaaaaa.
     */
    public static boolean checkContents(String fileName, String check) {
        File fileCWD = Utils.join(CWD, fileName);
        if (check.equals("head")) {
            Commit head = Utils.readObject(HEAD, Commit.class);
            String contentsH = head.getFiles().get(fileName);
            String contentsCWD = Utils.readContentsAsString(fileCWD);
            byte[] cereal = Utils.serialize(contentsCWD);
            String sha1OfCWD = Utils.sha1(cereal);
            return contentsH.equals(sha1OfCWD);
        } else {
            LinkedHashMap<String, String> stageAdd = getStageAdd();
            String contentsSA = stageAdd.get(fileName);
            String contentsCWD = Utils.readContentsAsString(fileCWD);
            byte[] cereal = Utils.serialize(contentsCWD);
            String sha1OfCWD = Utils.sha1(cereal);
            return contentsSA.equals(sha1OfCWD);
        }
    }
}



