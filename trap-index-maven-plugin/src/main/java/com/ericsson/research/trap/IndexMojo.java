package com.ericsson.research.trap;

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

/*
 * Copyright (c) 2014, Ericsson AB
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.ericsson.research.trap.utils.PackageScanner;

@Mojo(configurator = "include-project-dependencies", name = "trapindex", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDirectInvocation = false)
public class IndexMojo extends AbstractMojo
{
    /**
     * The project currently being build.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    private MavenProject            mavenProject;
    
    /**
     * @parameter default-value="${project.build.outputDirectory}/trap-packages.txt"
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/trap-packages.txt")
    private File                    targetFile;
    
    /**
     * The entry point to Aether, i.e. the component doing all the work.
     * 
     * @required
     * @component
     */
    @Component
    private RepositorySystem        repoSystem;
    
    /**
     * The current repository/network configuration of Maven.
     * 
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repoSession;
    
    /**
     * The project's remote repositories to use for the resolution of project dependencies.
     * 
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository>  projectRepos;
    
    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     * 
     * @parameter default-value="${project.remotePluginRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}")
    private List<RemoteRepository>  pluginRepos;
    
    /**
     * @parameter
     */
    @Parameter
    private String[]                packages;
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        
        try
        {
            //System.out.println("Using class loader " + this.getClass().getClassLoader());
            
            File parent = this.targetFile.getParentFile();
            
            if (parent.exists() && !parent.isDirectory())
                throw new MojoExecutionException("Parent directory " + parent + " is actually a file");
            
            if (!parent.exists() && !parent.mkdirs())
                throw new MojoExecutionException("Could not create target directory " + parent);
            
            fos = new FileOutputStream(this.targetFile);
            osw = new OutputStreamWriter(fos, "UTF-8");
            
            osw = new FileWriter(this.targetFile);
            for (String pkg : this.packages)
            {
                //System.out.println("Scanning " + pkg);
                Class<?>[] cs = PackageScanner.scan(pkg, this.getClass().getClassLoader());
                
                //System.out.println("Got cs: " + cs.length);
                
                for (Class<?> c : cs)
                {
                    // Skip adding ourselves. The receiver don't care.
                    if (c.getName().equals(this.getClass().getName()) || IndexConfigurator.class.getName().equals(c.getName()))
                        continue;
                    //System.out.println("Scanning " + c);
                    osw.write(c.getName());
                    osw.write("\n");
                }
            }
            
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Error while writing the packages file", e);
        }
        finally
        {
            if (osw != null)
                try
                {
                    osw.close();
                }
                catch (IOException e)
                {
                    throw new MojoFailureException("Could not write packages file", e);
                }
            if (fos != null)
                try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                    throw new MojoFailureException("Could not write packages file", e);
                }
        }
    }
    
}
