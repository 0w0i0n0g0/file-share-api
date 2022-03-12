package project.scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FilesScheduler {
    
    @Scheduled(cron = "0 * * * * ?") //run every 1 minuite
	public void deleteExpiredFolderSchedule() throws Exception {

        System.out.println("schedule running.\nschedule : deleteExpiredFolderSchedule");
        deleteExpiredFolder("/app/files", 300);//delete file created 300 sec ago

	}
    
    void deleteExpiredFolder(String path, int sec) {
		
	    File folder = new File(path);
	    try {
            for (File adminDirs : folder.listFiles()) {
                File files[] = adminDirs.listFiles();
                if (files == null) continue;
                for (File f : files) {
                    try {
                        long fModify = getSecondsFromModification(f);
                        if (fModify > sec) {
                            System.out.println("expired folder deleted.\npath : " + f.getAbsolutePath() + "\nmodify time(s) : " + fModify);
                            f.delete();
                            f.getParentFile().delete();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
           }
	   } catch (Exception e) {
		e.getStackTrace();
	   }
        
    }
    
    //returns the modified date of the file converted to the current time and the elapsed time in seconds
    long getSecondsFromModification(File f) throws IOException {

		Path attribPath = f.toPath();
		BasicFileAttributes basicAttribs = Files.readAttributes(attribPath, BasicFileAttributes.class);
        long res = (System.currentTimeMillis() - basicAttribs.lastModifiedTime().to(TimeUnit.MILLISECONDS)) / 1000;
        System.out.println("file created seconds.\npath : " + attribPath + "\nseconds : " + res);
		return res;

	}
    
}
