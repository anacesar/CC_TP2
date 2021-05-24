package Common;

public class Global {
        public static String makePath(String folder, String file){
            String[] so = System.getProperty("os.name").split(" ");
            String fullPath = null;
            switch (so[0]){
                case "Windows" : fullPath = folder + "\\" + file;
                    break;
                case "Mac"     : fullPath = folder + "/" + file;
                    break;
            }
            return fullPath;
        }

}
