package com.ericsson.research.trap.utils;

/*
 * ##_BEGIN_LICENSE_##
 * Transport Abstraction Package (trap)
 * ----------
 * Copyright (C) 2014 Ericsson AB
 * ----------
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Ericsson AB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ##_END_LICENSE_##
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FilePackageScannerTest
{
    
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        
        Class<?>[] classes = new PackageScannerImpl().performScan("com.ericsson", FilePackageScannerTest.class.getClassLoader());
        System.out.println(Arrays.toString(classes));
        
        URL url = FilePackageScannerTest.class.getClassLoader().getResource("trap-packages.txt");
        System.out.println(url);
        
        FileWriter fw = new FileWriter(new File(url.toURI()));
        BufferedWriter bw = new BufferedWriter(fw);
        
        for (Class<?> c : classes)
        {
            bw.write(c.getName());
            bw.write("\n");
        }
        bw.close();
        
    }
    
    @Test
    public void testFileScanner() throws Exception
    {
        PackageScannerFile ps = new PackageScannerFile();
        Class<?>[] classes = new PackageScannerImpl().performScan("com.ericsson", FilePackageScannerTest.class.getClassLoader());
        Class<?>[] cs = ps.performScan("com.ericsson", this.getClass().getClassLoader());
        
        Comparator<Class<?>> c = new Comparator<Class<?>>() {

            @Override
            public int compare(Class<?> o1, Class<?> o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        };

        Arrays.sort(classes, c);
        Arrays.sort(cs, c);
        
        Assert.assertArrayEquals(classes, cs);
    }
    
}
