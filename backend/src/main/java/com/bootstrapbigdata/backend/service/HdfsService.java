package com.bootstrapbigdata.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bootstrapbigdata.backend.config.AppConfig;

@Service
public class HdfsService {

    @Autowired
    private AppConfig appConfig;

    private FileSystem getFileSystem() throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", appConfig.getHdfsUri());
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        
        // Cấu hình để handle localhost và replication issues
        conf.set("dfs.client.use.datanode.hostname", "false");
        conf.set("dfs.datanode.use.datanode.hostname", "false");
        conf.set("dfs.nameservices", "");
        
        // Giảm replication requirement để tránh lỗi
        conf.setInt("dfs.replication", 1); // Chỉ cần 1 replica thay vì 2
        conf.setInt("dfs.namenode.replication.min", 1);
        conf.setLong("dfs.client.socket-timeout", 60000);
        conf.setInt("dfs.client.max.block.acquire.failures", 3);
        
        return FileSystem.get(conf);
    }

    public String uploadFile(MultipartFile file, String hdfsPath) throws IOException {
        FileSystem fs = getFileSystem();
        Path path = new Path(hdfsPath);
        
        // Create parent directories if they don't exist
        fs.mkdirs(path.getParent());
        
        try (InputStream inputStream = file.getInputStream();
             OutputStream outputStream = fs.create(path, true)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        fs.close();
        return hdfsPath;
    }

    public String uploadFile(InputStream inputStream, String fileName) throws IOException {
        try (FileSystem fs = getFileSystem()) {
            String hdfsPath = appConfig.getPaths().getInputDir() + "/" + System.currentTimeMillis() + "_" + fileName;
            Path path = new Path(hdfsPath);
            
            // Tạo directory nếu chưa có
            fs.mkdirs(path.getParent());
            
            // Upload với replication = 1
            try (OutputStream out = fs.create(path, true)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            // Verify file was written
            if (!fs.exists(path)) {
                throw new IOException("File upload failed - file not found after write");
            }
            
            return hdfsPath;
        } catch (Exception e) {
            throw new IOException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public boolean exists(String hdfsPath) throws IOException {
        FileSystem fs = getFileSystem();
        boolean exists = fs.exists(new Path(hdfsPath));
        fs.close();
        return exists;
    }

    public List<String> listFiles(String hdfsDir) throws IOException {
        FileSystem fs = getFileSystem();
        List<String> files = new ArrayList<>();
        
        if (fs.exists(new Path(hdfsDir))) {
            FileStatus[] fileStatuses = fs.listStatus(new Path(hdfsDir));
            for (FileStatus status : fileStatuses) {
                if (status.isFile()) {
                    files.add(status.getPath().toString());
                }
            }
        }
        
        fs.close();
        return files;
    }

    public InputStream downloadFile(String hdfsPath) throws IOException {
        FileSystem fs = getFileSystem();
        return fs.open(new Path(hdfsPath));
    }

    public void deleteFile(String hdfsPath) throws IOException {
        FileSystem fs = getFileSystem();
        fs.delete(new Path(hdfsPath), false);
        fs.close();
    }
}
