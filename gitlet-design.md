# Gitlet Design Document

**Name**: Will Harvey

## Classes and Data Structures
### Commit:
#### Instance Variables:
* String message - contains the message of each commit
* String parent - the parent of the commit (by SHAI name)
* timestamp - when it was created (obj pending)
* ArrayList<String> myBranches - arraylist of all branches pointing to this commit instance
* LinkedHashMap<String, String> - linked hash map with key pertaining to fileName (wug.txt) and value pertaining to the shai of the blob of that file


### Blobs:
#### Instance Variables
* Object contents - (rn its a string, fix later) whatever is in the parent file
### Adding Staging Area:

* linked hash map with file name as key (ex wug.txt) and blob Sha1 as value

### Remove Staging Area:
*  same thing as adding staging area

### Branch
#### Instance Variables:
* a boolean that corresponds to whether or not the branch is the current branch
* String name - a string that represents the name of branch (not sha1)
* String headCommit - a string that represents the sha1 of the commit the branch is pointing to

### Commands
* no instance variables, simply a class to have all command logic that each gitlet command should follow

## Algorithms



## Persistence
### CWD:
* current working directory that contains all files and direcotries

### GITLET_DIR:
* gitlet directory that is created when init() is called. Contains all files that follow

### COMMITS:
* single file in GITLET_DIR that houses a linked hash map that contains the commit history.
the shaI is the key and the commit is the value

### BRANCH_DIR:
* directory in GITLET_DIR that contains each branch as a file. Every time branch() is called, a new file under this directory is created.
Each branch file is named whatever the branches name is (ex. master, cool_beans)

### BLOBS_DIR:
* directory in GITLET_DIR that contains every blob created. Each file's name is it's unique shaI and the object within is the blob itself

### STAGE_AREA_ADD:
* file in GITLERT_DIR that houses the staging area add linked hash map. See earlier description of this object.

### STAGE_AREA_RM:
* sanme thing as STAGE_AREA_ADD but with the remove staging object

### HEAD:
* file in GITLET_DIR that contains the current head commit. It is overwritten every time commit() is called

### CURRENT_BRANCH:
* file in BRANCH_DIR that contains the current branch



