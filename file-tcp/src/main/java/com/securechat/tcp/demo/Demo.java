package com.securechat.tcp.demo;

import com.securechat.tcp.FileClient;

import java.io.File;
import java.io.IOException;

public class Demo {
    public static void main(String[] args) {
        try {
            FileClient client = new FileClient("localhost", 6000);
            File file = new File("testfile.txt");
            client.sendFile(file, "demo-user");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
