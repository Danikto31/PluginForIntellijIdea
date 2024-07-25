package my.SpringProject.DiffOfStringFiles;

public class DifFinder {
    private static String FileText;

    public static void setFileText(String fileText) {
        FileText = fileText;
    }

    public static boolean Diff(String text) {
        if(!FileTextNull() && !FileText.equals(text)){
            FileText=text;
            return true;
        }
        return false;
    }
    public static boolean FileTextNull(){
        if(FileText==null)return true;
        else return false;
    }

    public static String getFinaleText() {
        return FileText;
    }






}
