package project.controller;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.bucket4j.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/files")
public class FilesController {

    String path = "/app/files/"; //uploaded file path
    Bandwidth bandwidth = Bandwidth.simple(1, Duration.ofSeconds(1));
    Bucket bucket = Bucket.builder().addLimit(bandwidth).build();
    
    String keyGen() { 

        Random random = new Random();
        int createNum = 0;
        String ranNum = "";
        int letter = 4; //random number size = 4
        String resultNum = "";
        for (int i=0; i<letter; i++) { 
            createNum = random.nextInt(9);
            ranNum =  Integer.toString(createNum);
            resultNum += ranNum;
        }	
        return resultNum;
        
    }
    
    int fileLength(File dir) {
        
        File[] files = dir.listFiles();
        FileFilter fileFilter = new FileFilter()
        {
            public boolean accept(File file)
            {
                return file.isFile();
            }
        };
        files = dir.listFiles(fileFilter);
        return files.length;
        
    }
    
    String fileName(File dir) {
        
        String[] filenames = dir.list();
        return filenames[0];
        
    }
    
    void zipFolder(File srcFolder, File destZipFile) throws Exception {
		
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
                ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
            addFolderToZip(srcFolder, srcFolder, zip);
        }
		
    }

    void addFileToZip(File rootPath, File srcFile, ZipOutputStream zip) throws Exception {

        if (srcFile.isDirectory()) {
            addFolderToZip(rootPath, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            try (FileInputStream in = new FileInputStream(srcFile)) {
                String name = srcFile.getPath();
                name = name.replace(rootPath.getPath(), "");
                name = name.replace("/", ""); //delete absolute path to fix unzip error
                System.out.println("zip success.\npath : " + srcFile + "\nfile : " + name);
                zip.putNextEntry(new ZipEntry(name));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
		
    }

    void addFolderToZip(File rootPath, File srcFolder, ZipOutputStream zip) throws Exception {
		
        for (File fileName : srcFolder.listFiles()) {
            addFileToZip(rootPath, fileName, zip);
        }
		
    }
    
    void deleteFolder(String path) {
		
	    File folder = new File(path);
	    try {
		if(folder.exists()){
                File[] folder_list = folder.listFiles();
				
		for (int i = 0; i < folder_list.length; i++) {
		    if(folder_list[i].isFile()) {
			folder_list[i].delete();
			System.out.println("file deleted.\nfile : " + folder_list[i].getName());
		    }else {
			deleteFolder(folder_list[i].getPath());
			System.out.println("folder deleted.\nfolder : " + folder_list[i].getName());
		    }
		    folder_list[i].delete();
		 }
		 folder.delete();
	       }
	   } catch (Exception e) {
		e.getStackTrace();
	   }
        
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> upload(@RequestPart List<MultipartFile> files) throws Exception {

        Boolean keyGenSuccess = false;
        String key = "";
        Map<String, Object> res = new LinkedHashMap<>(); //result
        List<String> list = new ArrayList<>(); //uploaded file list
        if(bucket.tryConsume(1)) {
            if(files.size() == 1) { //if uploaded file is 1
                while(!keyGenSuccess){
                    key = keyGen();
                    File folder = new File(path + key);
                    if (!folder.exists()) {
                        try {
                            folder.mkdir(); //gen key folder
                            System.out.println("key gen success.\nkey : " + key);
                            keyGenSuccess = true;
                            break;
                        } 
                        catch(Exception e) {
                            e.getStackTrace();
                        }        

                    } else {
                            System.out.println("key already exists.\nkey : " + key);
                    }
                }
                res.put("key", key); 
                for (MultipartFile file : files) {
                    String originalfileName = file.getOriginalFilename();
                    File dest = new File(path + key + "/" + originalfileName);
                    list.add(originalfileName);
                    file.transferTo(dest);
                }
            } else { //if uploaded files are more than 1
                while(!keyGenSuccess){
                    key = keyGen();
                    File folder = new File(path + key);
                    if (!folder.exists()) {
                        try {
                            folder.mkdir(); //gen key folder
                            folder = new File(path + key +"/" + key);
                            folder.mkdir(); //gen folder for zip
                            System.out.println("key gen success.\nkey : " + key);
                            keyGenSuccess = true;
                            break;
                        } 
                        catch(Exception e) {
                            e.getStackTrace();
                        }        
                    } else {
                            System.out.println("key already exists.\nkey : " + key);
                    }
                }
                res.put("key", key); 
                for (MultipartFile file : files) {
                    String originalfileName = file.getOriginalFilename();
                    File dest = new File(path + key + "/" + key + "/" + originalfileName);
                    list.add(originalfileName);
                    file.transferTo(dest);
                }
                zipFolder(new File(path + key + "/" + key), new File(path + key + "/" + key + ".zip"));
                deleteFolder(path + key + "/" + key); //delete folder after zip finished

            }   
            res.put("files", list);
            return ResponseEntity.ok().body(res);
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        
    }
    
    @GetMapping("/{key}")
    public ResponseEntity<Resource> download(@PathVariable("key") int key) throws IOException {
        
        if(bucket.tryConsume(1)) {
            String keyPath = path + key;
            File dir = new File(keyPath);
            String downloadFile= fileName(dir);
            Path filePath = Paths.get(keyPath + "/" + downloadFile);
            String contentType = Files.probeContentType(filePath);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
            Resource resource = new InputStreamResource(Files.newInputStream(filePath));
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        
    }

}