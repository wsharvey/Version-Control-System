package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/** Commit class.
 * @author willharvey.
 */
public class Commit implements Serializable {

    /** Message of commit. */
    private String _message;

    /** List of parents. */
    private ArrayList<String> _parent;

    /** Date initialized. */
    private Date _stamp;

    /** LHM of all Files and Blobs. */
    private LinkedHashMap<String, String> _myFiles;

    /** Initializes a Commit with a message, parent, and timestamp.
     *
     * @param message - string.
     * @param parent - AL.
     * @param myFiles - AL.
     */
    public Commit(String message, ArrayList<String> parent,
                  LinkedHashMap<String, String> myFiles) {
        _message = message;
        _parent = parent;
        _stamp = new Date();
        _myFiles = myFiles;
    }

    /** Getter method for _message.
     *
     * @return String.
     */
    public String getMessage() {
        return _message;
    }

    /** Getter method for _parent.
     *
     * @return AL.
     */
    public ArrayList<String> getParent() {
        return _parent;
    }

    /** Getter method for _stamp.
     *
     * @return Date.
     */
    public Date getStamp() {
        return _stamp;
    }

    /** Getter method for _myFiles.
     *
     * @return LHM.
     */
    public LinkedHashMap<String, String> getFiles() {
        return _myFiles;
    }
}
