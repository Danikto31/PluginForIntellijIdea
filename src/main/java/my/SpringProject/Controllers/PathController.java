package my.SpringProject.Controllers;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PathController {
    public static ConcurrentHashMap<String,String> AllPathMap = new ConcurrentHashMap<>();
    public static void setAllPaths(String DirectoryPath) {

        File[] file = new File(DirectoryPath).listFiles();
        if (file != null) {
            for (File value : file) {
                if (!value.isDirectory()) {
                    AllPathMap.put(value.getName(), value.getAbsolutePath());
                } else {
                    AllPathMap.put("Directory:"+value.getName(),value.getAbsolutePath());
                    setAllPaths(value.getAbsolutePath());}
            }
        }


    }
    public static boolean CompareTwoMaps(ConcurrentHashMap<String,String> map1,ConcurrentHashMap<String,String> map2){
        final int[] count = {0};
        map1.forEach((K,V)->{
            if(map2.get(K)==null){
              count[0]++;
            }
        });
return count[0]==0;
    }
    public static ConcurrentHashMap<String,String> getAllPathMap(){
        return AllPathMap;
    }
    public static String PathToCreateFile(String DirectoryPath,String path,String ProjectName) {
    return "";
    }
    public static String PathToCreateDirectory(String DirectoryPath,String path,String ProjectName) {
return "";
    }
    public static void clearMap(){
        AllPathMap.clear();
    }
}
