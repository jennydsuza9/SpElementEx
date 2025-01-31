/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.spelementex.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Jenny D'Souza
 */
public class ExternalCommand {
    
    public static void run(String command, FileOutputStream result) {
        try
        {     
            int exitVal = -1;
            //while (exitVal != 0) {
                Runtime rt = Runtime.getRuntime();
                Process process = rt.exec(command);

                // Get input streams
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                // Read command standard output
                String s;
                //System.out.println("Standard output: ");
                while ((s = stdInput.readLine()) != null) {
                    if (result != null)
                        result.write((s+"\n").getBytes());
                    else
                        System.out.println(s);
                }

                // Read command errors
                //System.out.println("Standard error: ");

                while ((s = stdError.readLine()) != null) {
                    System.out.println(s);
                }         
                

                exitVal = process.waitFor();
                //System.out.println("Process exitValue: " + exitVal);
            //}
            
        } catch (IOException | InterruptedException t)
          {
              System.err.print(t);
          }
    }        
    
}
