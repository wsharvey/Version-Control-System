package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Will Harvey
 */
public class Main {

    /** Prints errors if thrown.
     *
     * @param args - list of strings.
     */
    public static void main(String... args) {
        try {
            Main.operate(args);
            return;
        } catch (GitletException | IOException excp) {
            System.err.printf("%s%n", excp.getMessage());
        }
        System.exit(0);
    }


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void operate(String... args) throws IOException {
        if (args.length == 0) {
            throw new GitletException("Please enter a command.");
        }
        switch (args[0]) {
        case "init":
            Commands.init();
            break;
        case "add":
            Commands.add(args[1]);
            break;
        case "commit":
            if (args.length == 1 || args[1].equals("")) {
                throw new GitletException("Please enter a commit message.");
            }
            Commands.commit(args[1]);
            break;
        case "checkout":
            if (args.length == 2) {
                Commands.checkout3(args[1]);
            } else if (args[2].equals("--")) {
                Commands.checkout2(args[1], args[3]);
            } else if (args[1].equals("--")) {
                Commands.checkout1(args[2]);
            } else {
                throw new GitletException("Incorrect Operands.");
            }
            break;
        case "log": Commands.log();
            break;
        case "global-log": Commands.globalLog();
            break;
        case "find": Commands.find(args[1]);
            break;
        case "status": Commands.status();
            break;
        case "branch": Commands.branch(args[1]);
            break;
        case "rm": Commands.remove(args[1]);
            break;
        case "rm-branch": Commands.removeBranch(args[1]);
            break;
        case "reset": Commands.reset(args[1]);
            break;
        case "merge": Commands.merge(args[1]);
            break;
        default: throw new GitletException(
                "No command with that name exists.");
        } return;
    }
}


