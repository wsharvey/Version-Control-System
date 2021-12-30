package gitlet;

import java.io.Serializable;
import java.util.ArrayList;

/** Branch class.
 * @author willharvey.
 */
public class Branch implements Serializable {

    /** returns whether or not this branch is the Current Branch. */
    private boolean _current;

    /** name of Branch. */
    private String _name;

    /** ArrayList of commits. */
    private ArrayList<String> _commit;

    /** Initializes a branch.
     *
     * @param current - boolean.
     * @param name - string.
     * @param commit - AL.
     */
    public Branch(boolean current, String name, ArrayList<String> commit) {
        _current = current;
        _name = name;
        _commit = commit;
    }

    /** getter method for _name.
     *
     * @return string.
     */
    public String getName() {
        return _name;
    }

    /** Getter method for _commit.
     *
     * @return AL.
     */
    public ArrayList<String> getCommit() {
        return _commit;
    }
}
