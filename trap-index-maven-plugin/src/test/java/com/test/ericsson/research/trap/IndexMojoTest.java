package com.test.ericsson.research.trap;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;

import junit.framework.Assert;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

import com.ericsson.research.trap.IndexConfigurator;
import com.ericsson.research.trap.IndexMojo;
import com.ericsson.research.trap.utils.PackageScanner;

public class IndexMojoTest extends AbstractMojoTestCase
{
    @Test
    public void testGenerate() throws Exception
    {
        File pom = new File(this.getClassLoader().getResource("test-index.xml").toURI());
        IndexMojo lookupMojo = (IndexMojo) this.lookupMojo("trapindex", pom);
        this.configureMojo(lookupMojo, "trap-index-maven-plugin", pom);
        
        File tmp = File.createTempFile("foo", "bar");
        tmp.deleteOnExit();
        this.setVariableValueToObject(lookupMojo, "targetFile", tmp);
        lookupMojo.execute();
        
        Class<?>[] scan = PackageScanner.scan("com.ericsson");
        HashSet<String> classNames = new HashSet<String>();
        
        for (Class<?> c : scan)
            if (!IndexMojoTest.class.getName().equals(c.getName()) && !IndexMojo.class.getName().equals(c.getName()) && !IndexConfigurator.class.getName().equals(c.getName()))
                classNames.add(c.getName());
        
        System.out.println(classNames);
        
        FileReader fr = new FileReader(tmp);
        BufferedReader br = new BufferedReader(fr);
        
        HashSet<String> resultNames = new HashSet<String>();
        
        String line;
        
        while ((line=br.readLine()) != null)
            resultNames.add(line);
        
        br.close();
        
        System.err.println(resultNames);

        Assert.assertTrue(classNames.containsAll(resultNames));
        Assert.assertTrue(resultNames.containsAll(classNames));
    }
}
