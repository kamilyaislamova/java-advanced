package info.kgeorgiy.ja.islamova.walk;

public class Walk {

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("wrong arguments");
            return;
        }
        RecursiveWalk walk = new RecursiveWalk(0);
        walk.walks(args);
    }
}
