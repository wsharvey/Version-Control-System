package gitlet;

import java.io.File;
import java.io.Serializable;

/** Blob class.
 * @author willharvey.
 */
public class Blob implements Serializable {

    /** Contents of Blob. */
    private String _contents;

    /** Blob constructor.
     *
     * @param name - name of file.
     */
    public Blob(String name) {
        File file = new File(name);
        _contents = Utils.readContentsAsString(file);
    }

    /** Getter method for contents.
     *
     * @return String.
     */
    public String getContents() {
        return _contents;
    }
}
