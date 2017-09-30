package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    public static void initSub(int c, Gitlet g) {
        if (c != 1) {
            System.out.println("Incorrect operands.");
        } else {
            g.init();
        }
    }

    public static void logSub(int c, Gitlet g) {
        if (c != 1) {
            System.out.println("Incorrect operands.");
        } else {
            g.log();
        }
    }

    public static void globallogSub(int c, Gitlet g) {
        if (c != 1) {
            System.out.println("Incorrect operands.");
        } else {
            g.globallog();
        }
    }

    public static void statusSub(int c, Gitlet g) {
        if (c != 1) {
            System.out.println("Incorrect operands.");
        } else {
            g.status();
        }
    }

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Gitlet mygitlet = new Gitlet();
        switch (args[0]) {
            case "init": {
                initSub(args.length, mygitlet);
                break;
            }
            case "add": {
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                } else {
                    mygitlet.add(args[1]);
                }
                break;
            }
            case "commit": {
                if (args.length == 1 || args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                } else {
                    mygitlet.commit(args[1]);
                }
                break;
            }
            case "rm": {
                mygitlet.rm(args[1]);
                break;
            }
            case "log": {
                logSub(args.length, mygitlet);
                break;
            }
            case "global-log": {
                globallogSub(args.length, mygitlet);
                break;
            }
            case "find": {
                mygitlet.find(args[1]);
                break;
            }
            case "status": {
                statusSub(args.length, mygitlet);
                break;
            }
            case "checkout": {
                if (args.length == 3) {
                    mygitlet.checkoutf(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    mygitlet.checkoutcf(args[1], args[3]);
                } else {
                    mygitlet.checkoutb(args[1]);
                }
                break;
            }
            case "branch": {
                mygitlet.branch(args[1]);
                break;
            }
            case "rm-branch": {
                mygitlet.rmbranch(args[1]);
                break;
            }
            case "reset": {
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                } else {
                    mygitlet.reset(args[1]);
                }
                break;
            }
            case "merge": {
                mygitlet.merge(args[1]);
                break;
            }
            default: { }
        }
    }
}
